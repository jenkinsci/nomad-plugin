package org.jenkinsci.plugins.nomad;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.google.common.base.Strings;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.model.Node;
import hudson.security.ACL;
import hudson.slaves.AbstractCloudImpl;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.slaves.JnlpSlaveAgentProtocol;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.jenkinsci.plugins.nomad.Api.JobInfo;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.cloudbees.plugins.credentials.CredentialsMatchers.filter;
import static com.cloudbees.plugins.credentials.CredentialsMatchers.withId;
import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import static org.apache.commons.lang.StringUtils.trimToEmpty;

public class NomadCloud extends AbstractCloudImpl {

    private static final Logger LOGGER = Logger.getLogger(NomadCloud.class.getName());

    private final List<? extends NomadWorkerTemplate> templates;

    private final String nomadUrl;
    private final String nomadACLCredentialsId;
    private final Boolean prune;
    private boolean tlsEnabled;
    private String clientCertificate;
    private String clientPassword;
    private String serverCertificate;
    private String serverPassword;
    private String jenkinsUrl;
    private String jenkinsTunnel;
    private String workerUrl;
    private int workerTimeout = 1;
    private NomadApi nomad;

    private int pending = 0;

    @DataBoundConstructor
    public NomadCloud(
            String name,
            String nomadUrl,
            boolean tlsEnabled,
            String clientCertificate,
            String clientPassword,
            String serverCertificate,
            String serverPassword,
            String jenkinsUrl,
            String jenkinsTunnel,
            String workerUrl,
            String workerTimeout,
            String nomadACLCredentialsId,
            Boolean prune,
            List<? extends NomadWorkerTemplate> templates) {
        super(name, null);

        this.nomadACLCredentialsId = nomadACLCredentialsId;
        this.nomadUrl = nomadUrl;
        this.tlsEnabled = tlsEnabled;
        this.clientCertificate = clientCertificate;
        this.clientPassword = clientPassword;
        this.serverCertificate = serverCertificate;
        this.serverPassword = serverPassword;
        this.jenkinsUrl = jenkinsUrl;
        this.jenkinsTunnel = jenkinsTunnel;
        this.workerUrl = workerUrl;
        setWorkerTimeout(workerTimeout);
        this.prune = prune;

        if (templates == null) {
            this.templates = Collections.emptyList();
        } else {
            this.templates = templates;
        }

        readResolve();
    }

    private static String secretFor(String credentialsId) {
        List<StringCredentials> creds = filter(
                lookupCredentials(StringCredentials.class,
                        Jenkins.get(),
                        ACL.SYSTEM,
                        Collections.emptyList()),
                withId(trimToEmpty(credentialsId))
        );
        if (creds.size() > 0) {
            return creds.get(0).getSecret().getPlainText();
        } else {
            return null;
        }
    }

    private Object readResolve() {
        nomad = new NomadApi(this);

        if (jenkinsUrl.equals("")) {
            jenkinsUrl = Jenkins.get().getRootUrl();
        }

        if (workerUrl.equals("")) {
            workerUrl = jenkinsUrl + "jnlpJars/slave.jar";
        }

        return this;
    }

    @Override
    public Collection<NodeProvisioner.PlannedNode> provision(Label label, int excessWorkload) {

        List<NodeProvisioner.PlannedNode> nodes = new ArrayList<>();
        final NomadWorkerTemplate template = getTemplate(label);

        if (template != null) {
            if (getPrune())
                pruneOrphanedWorkers(template);

            try {
                while (excessWorkload > 0) {
                    LOGGER.log(Level.INFO, "Excess workload of " + excessWorkload + ", provisioning new Jenkins worker on Nomad cluster");

                    final String workerName = template.createWorkerName();
                    nodes.add(new NodeProvisioner.PlannedNode(
                            workerName,
                            NomadComputer.threadPoolForRemoting.submit(
                                    new ProvisioningCallback(workerName, template, this)
                            ), template.getNumExecutors()));
                    excessWorkload -= template.getNumExecutors();
                    pending += template.getNumExecutors();
                }
                return nodes;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unable to schedule new Jenkins worker on Nomad cluster, message: " + e.getMessage());
            }
        }

        return Collections.emptyList();
    }

    private void pruneOrphanedWorkers(NomadWorkerTemplate template) {
        JobInfo[] nomadWorkers = this.nomad.getRunningWorkers(template.getPrefix(), getNomadACL());

        for (JobInfo worker : nomadWorkers) {
            if (worker.getStatus().equalsIgnoreCase("running")) {
                LOGGER.log(Level.FINE, "Found worker: " + worker.getName() + " - " + worker.getID());
                Node node = Jenkins.get().getNode(worker.getName());

                if (node == null) {
                    LOGGER.log(Level.FINE, "Found Orphaned Node: " + worker.getID());
                    this.nomad.stopWorker(worker.getID(), getNomadACL());
                }
            }
        }

    }

    // Find the correct template for job
    public NomadWorkerTemplate getTemplate(Label label) {
        for (NomadWorkerTemplate t : templates) {
            if (label == null && !t.getLabelSet().isEmpty()) {
                continue;
            }
            if ((label == null && t.getLabelSet().isEmpty()) || (label != null && label.matches(t.getLabelSet()))) {
                return t;
            }
        }
        return null;
    }

    @Override
    public boolean canProvision(Label label) {
        return Optional.ofNullable(getTemplate(label)).isPresent();
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getNomadUrl() {
        return nomadUrl;
    }

    public String getJenkinsUrl() {
        return jenkinsUrl;
    }

    public void setJenkinsUrl(String jenkinsUrl) {
        this.jenkinsUrl = jenkinsUrl;
    }

    public String getWorkerUrl() {
        return workerUrl;
    }

    public void setWorkerUrl(String workerUrl) {
        this.workerUrl = workerUrl;
    }

    public int getWorkerTimeout() {
        return workerTimeout;
    }

    public void setWorkerTimeout(String workerTimeout) {
        try {
            this.workerTimeout = Integer.parseInt(workerTimeout);
        } catch (NumberFormatException ex) {
            LOGGER.log(Level.WARNING, "Failed to parse timeout defaulting to current value (default: 1 minute): " + workerTimeout + " minutes");
        }
    }

    public String getNomadACLCredentialsId() {
        return nomadACLCredentialsId;
    }

    public String getNomadACL() {
        return secretFor(this.getNomadACLCredentialsId());
    }

    public Boolean getPrune() {
        if (prune == null)
            return false;

        return prune;
    }

    public void setNomad(NomadApi nomad) {
        this.nomad = nomad;
    }

    public int getPending() {
        return pending;
    }

    public String getJenkinsTunnel() {
        return jenkinsTunnel;
    }

    public void setJenkinsTunnel(String jenkinsTunnel) {
        this.jenkinsTunnel = jenkinsTunnel;
    }

    public List<NomadWorkerTemplate> getTemplates() {
        return Collections.unmodifiableList(templates);
    }

    public NomadApi nomad() {
        return nomad;
    }

    public boolean isTlsEnabled() {
        return tlsEnabled;
    }

    public void setTlsEnabled(boolean tlsEnabled) {
        this.tlsEnabled = tlsEnabled;
    }

    public String getClientCertificate() {
        return clientCertificate;
    }

    public void setClientCertificate(String clientCertificate) {
        this.clientCertificate = clientCertificate;
    }

    public String getClientPassword() {
        return clientPassword;
    }

    public void setClientPassword(String clientPassword) {
        this.clientPassword = clientPassword;
    }

    public String getServerCertificate() {
        return serverCertificate;
    }

    public void setServerCertificate(String serverCertificate) {
        this.serverCertificate = serverCertificate;
    }

    public String getServerPassword() {
        return serverPassword;
    }

    public void setServerPassword(String serverPassword) {
        this.serverPassword = serverPassword;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Cloud> {

        public DescriptorImpl() {
            load();
        }

        public String getDisplayName() {
            return "Nomad";
        }

        @POST
        public FormValidation doTestConnection(
                @QueryParameter String nomadUrl,
                @QueryParameter boolean tlsEnabled,
                @QueryParameter String clientCertificate,
                @QueryParameter String clientPassword,
                @QueryParameter String serverCertificate,
                @QueryParameter String serverPassword) {
            Objects.requireNonNull(Jenkins.get()).checkPermission(Jenkins.ADMINISTER);
            try {
                Request request = new Request.Builder()
                        .url(nomadUrl + "/v1/agent/self")
                        .build();

                OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
                if (tlsEnabled) {
                    OkHttpClientHelper.initTLS(clientBuilder, clientCertificate, clientPassword, serverCertificate, serverPassword);
                }

                ResponseBody response = clientBuilder.build().newCall(request).execute().body();
                if (response != null) {
                    response.close();
                }
                return FormValidation.ok("Nomad API request succeeded.");
            } catch (Exception e) {
                return FormValidation.error(e.getMessage());
            }
        }

        @POST
        public FormValidation doCheckName(@QueryParameter String name) {
            Objects.requireNonNull(Jenkins.get()).checkPermission(Jenkins.ADMINISTER);
            if (Strings.isNullOrEmpty(name)) {
                return FormValidation.error("Name must be set");
            } else {
                return FormValidation.ok();
            }
        }

        public ListBoxModel doFillNomadACLCredentialsIdItems(@QueryParameter("nomadACLCredentialsId") String credentialsId) {
            if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                return new StandardListBoxModel().includeCurrentValue(credentialsId);
            }
            return new StandardListBoxModel()
                    .withEmptySelection()
                    .withMatching(
                            CredentialsMatchers.always(),
                            CredentialsProvider.lookupCredentials(StringCredentials.class,
                                    Jenkins.get(),
                                    ACL.SYSTEM,
                                    Collections.emptyList()));
        }
    }

    private class ProvisioningCallback implements Callable<Node> {

        String workerName;
        NomadWorkerTemplate template;
        NomadCloud cloud;

        public ProvisioningCallback(String workerName, NomadWorkerTemplate template, NomadCloud cloud) {
            this.workerName = workerName;
            this.template = template;
            this.cloud = cloud;
        }

        public Node call() throws Exception {
            final NomadWorker worker = new NomadWorker(
                    workerName,
                    name,
                    template,
                    template.getLabels(),
                    new NomadRetentionStrategy(template.getIdleTerminationInMinutes()),
                    Collections.emptyList()
            );
            Jenkins.get().addNode(worker);

            // Support for Jenkins security
            String jnlpSecret = "";
            if (Jenkins.get().isUseSecurity() || template.isDockerDriver()) {
                // docker driver uses the classpath approach -> secret is mandatory
                jnlpSecret = JnlpSlaveAgentProtocol.SLAVE_SECRET.mac(workerName);
            }

            LOGGER.log(Level.INFO, "Asking Nomad to schedule new Jenkins worker");
            nomad.startWorker(cloud, workerName, getNomadACL(), jnlpSecret, template);

            // Check scheduling success
            Callable<Boolean> callableTask = () -> {
                try {
                    LOGGER.log(Level.INFO, "Worker scheduled, waiting for connection");
                    Objects.requireNonNull(worker.toComputer()).waitUntilOnline();
                } catch (InterruptedException e) {
                    LOGGER.log(Level.SEVERE, "Waiting for connection was interrupted");
                    return false;
                }
                return true;
            };

            // Schedule a worker and wait for the computer to come online
            ExecutorService executorService = Executors.newCachedThreadPool();
            Future<Boolean> future = executorService.submit(callableTask);

            try {
                future.get(cloud.workerTimeout, TimeUnit.MINUTES);
                LOGGER.log(Level.INFO, "Connection established");
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Worker computer did not come online within " + workerTimeout + " minutes, terminating worker" + worker);
                worker.terminate();
                throw new RuntimeException("Timed out waiting for agent to start up. Timeout: " + workerTimeout + " minutes.");
            } finally {
                future.cancel(true);
                executorService.shutdown();
                pending -= template.getNumExecutors();
            }
            return worker;
        }
    }
}

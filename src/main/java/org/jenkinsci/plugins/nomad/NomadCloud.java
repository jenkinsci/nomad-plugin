package org.jenkinsci.plugins.nomad;

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
import hudson.util.Secret;
import jenkins.model.Jenkins;
import jenkins.slaves.JnlpSlaveAgentProtocol;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.jenkinsci.plugins.nomad.Api.JobInfo;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import static com.cloudbees.plugins.credentials.CredentialsMatchers.filter;
import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import static com.cloudbees.plugins.credentials.CredentialsMatchers.withId;
import static com.cloudbees.plugins.credentials.domains.URIRequirementBuilder.fromUri;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.apache.commons.lang.StringUtils.trimToEmpty;

public class NomadCloud extends AbstractCloudImpl {

    private static final Logger LOGGER = Logger.getLogger(NomadCloud.class.getName());

    private final List<? extends NomadSlaveTemplate> templates;

    private final String nomadUrl;
    private final String nomadACLCredentialsId;
    private String jenkinsUrl;
    private String jenkinsTunnel;
    private String slaveUrl;
    private int workerTimeout = 1;

    private final Boolean prune;

    private NomadApi nomad;

    private int pending = 0;

    @DataBoundConstructor
    public NomadCloud(
            String name,
            String nomadUrl,
            String jenkinsUrl,
            String jenkinsTunnel,
            String slaveUrl,
            String workerTimeout,
            String nomadACLCredentialsId,
            Boolean prune,
            List<? extends NomadSlaveTemplate> templates)
    {
        super(name, null);

        this.nomadACLCredentialsId = nomadACLCredentialsId;
        this.nomadUrl = nomadUrl;

        this.jenkinsUrl = jenkinsUrl;
        this.jenkinsTunnel = jenkinsTunnel;
        this.slaveUrl = slaveUrl;
        setWorkerTimeout(workerTimeout);
        this.prune = prune;

        if (templates == null) {
            this.templates = Collections.emptyList();
        } else {
            this.templates = templates;
        }

        readResolve();
    }

    private Object readResolve() {
        nomad = new NomadApi(nomadUrl);

        if (jenkinsUrl.equals("")) {
            jenkinsUrl = Jenkins.get().getRootUrl();
        }

        if (slaveUrl.equals("")) {
            slaveUrl = jenkinsUrl + "jnlpJars/slave.jar";
        }

        return this;
    }

    @Override
    public Collection<NodeProvisioner.PlannedNode> provision(Label label, int excessWorkload) {

        List<NodeProvisioner.PlannedNode> nodes = new ArrayList<>();
        final NomadSlaveTemplate template = getTemplate(label);

        if (template != null) {
            if (getPrune())
                pruneOrphanedWorkers(template);

            try {
                while (excessWorkload > 0) {
                    LOGGER.log(Level.INFO, "Excess workload of " + excessWorkload + ", provisioning new Jenkins slave on Nomad cluster");

                    final String slaveName = template.createSlaveName();
                    nodes.add(new NodeProvisioner.PlannedNode(
                            slaveName,
                            NomadComputer.threadPoolForRemoting.submit(
                                    new ProvisioningCallback(slaveName, template, this)
                            ), template.getNumExecutors()));
                    excessWorkload -= template.getNumExecutors();
                    pending += template.getNumExecutors();
                }
                return nodes;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unable to schedule new Jenkins slave on Nomad cluster, message: " + e.getMessage());
            }
        }

        return Collections.emptyList();
    }

    private void pruneOrphanedWorkers(NomadSlaveTemplate template) {
        JobInfo[] nomadWorkers = this.nomad.getRunningWorkers(template.getPrefix(), getNomadACL());

        for (JobInfo worker : nomadWorkers) {
            if (worker.getStatus().equalsIgnoreCase("running")) {
                LOGGER.log(Level.FINE, "Found worker: " + worker.getName() + " - " + worker.getID());
                Node node = Jenkins.get().getNode(worker.getName());

                if (node == null) {
                    LOGGER.log(Level.FINE, "Found Orphaned Node: " + worker.getID());
                    this.nomad.stopSlave(worker.getID(), getNomadACL());
                }
            }
        }

    }

    private class ProvisioningCallback implements Callable<Node> {

        String slaveName;
        NomadSlaveTemplate template;
        NomadCloud cloud;

        public ProvisioningCallback(String slaveName, NomadSlaveTemplate template, NomadCloud cloud) {
            this.slaveName = slaveName;
            this.template = template;
            this.cloud = cloud;
        }

        public Node call() throws Exception {
            final NomadSlave slave = new NomadSlave(
                    slaveName,
                    name,
                    template,
                    template.getLabels(),
                    new NomadRetentionStrategy(template.getIdleTerminationInMinutes()),
                    Collections.emptyList()
            );
            Jenkins.get().addNode(slave);

            // Support for Jenkins security
            String jnlpSecret = "";
            if (Jenkins.get().isUseSecurity()) {
                jnlpSecret = JnlpSlaveAgentProtocol.SLAVE_SECRET.mac(slaveName);
            }

            LOGGER.log(Level.INFO, "Asking Nomad to schedule new Jenkins slave");
            nomad.startSlave(cloud, slaveName, getNomadACL(), jnlpSecret, template);

            // Check scheduling success
            Callable<Boolean> callableTask = () -> {
                try {
                    LOGGER.log(Level.INFO, "Slave scheduled, waiting for connection");
                    Objects.requireNonNull(slave.toComputer()).waitUntilOnline();
                } catch (InterruptedException e) {
                    LOGGER.log(Level.SEVERE, "Waiting for connection was interrupted");
                    return false;
                }
                return true;
            };

            // Schedule a slave and wait for the computer to come online
            ExecutorService executorService = Executors.newCachedThreadPool();
            Future<Boolean> future = executorService.submit(callableTask);

            try{
                future.get(cloud.workerTimeout, TimeUnit.MINUTES);
                LOGGER.log(Level.INFO, "Connection established");
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Slave computer did not come online within " + workerTimeout + " minutes, terminating slave"+ slave);
                slave.terminate();
                throw new RuntimeException("Timed out waiting for agent to start up. Timeout: " + workerTimeout + " minutes.");
            } finally {
                future.cancel(true);
                executorService.shutdown();
                pending -= template.getNumExecutors();
            }
            return slave;
        }
    }

    // Find the correct template for job
    public NomadSlaveTemplate getTemplate(Label label) {
        for (NomadSlaveTemplate t : templates) {
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


    @Extension
    public static final class DescriptorImpl extends Descriptor<Cloud> {

        public DescriptorImpl() {
            load();
        }

        public String getDisplayName() {
            return "Nomad";
        }

        @POST
        public FormValidation doTestConnection(@QueryParameter("nomadUrl") String nomadUrl) {
            Objects.requireNonNull(Jenkins.get()).checkPermission(Jenkins.ADMINISTER);
            try {
                Request request = new Request.Builder()
                        .url(nomadUrl + "/v1/agent/self")
                        .build();

                OkHttpClient client = new OkHttpClient();
                client.newCall(request).execute().body().close();

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
            if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
                return new StandardListBoxModel().includeCurrentValue(credentialsId);
            }
            return new StandardListBoxModel()
                    .withEmptySelection()
                    .withMatching(
                            CredentialsMatchers.always(),
                            CredentialsProvider.lookupCredentials(StringCredentials.class,
                                    Jenkins.getInstance(),
                                    ACL.SYSTEM,
                                    Collections.emptyList()));
        }
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

    public String getSlaveUrl() {
        return slaveUrl;
    }

    public int getWorkerTimeout() {
        return workerTimeout;
    }

    public String getNomadACLCredentialsId() {
        return nomadACLCredentialsId;
    }

    public String getNomadACL() {
        return secretFor(this.getNomadACLCredentialsId());
    }

    private static String secretFor(String credentialsId) {
        List<StringCredentials> creds = filter(
                lookupCredentials(StringCredentials.class,
                        Jenkins.getInstance(),
                        ACL.SYSTEM,
                        Collections.<DomainRequirement>emptyList()),
                withId(trimToEmpty(credentialsId))
        );
        if (creds.size() > 0) {
            return creds.get(0).getSecret().getPlainText();
        } else {
            return null;
        }
    }

    public void setJenkinsUrl(String jenkinsUrl) {
        this.jenkinsUrl = jenkinsUrl;
    }

    public void setSlaveUrl(String slaveUrl) {
        this.slaveUrl = slaveUrl;
    }

    public Boolean getPrune() {
        if (prune == null)
            return false;

        return prune;
    }

    public void setWorkerTimeout(String workerTimeout) {
        try {
            this.workerTimeout = Integer.parseInt(workerTimeout);
        } catch(NumberFormatException ex) {
            LOGGER.log(Level.WARNING, "Failed to parse timeout defaulting to current value (default: 1 minute): " + workerTimeout + " minutes");
        }
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

    public List<NomadSlaveTemplate> getTemplates() {
        return Collections.unmodifiableList(templates);
    }

    public NomadApi Nomad() {
        return nomad;
    }
}

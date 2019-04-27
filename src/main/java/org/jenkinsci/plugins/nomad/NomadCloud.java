package org.jenkinsci.plugins.nomad;

import com.google.common.base.Strings;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.model.Node;
import hudson.slaves.AbstractCloudImpl;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import jenkins.slaves.JnlpSlaveAgentProtocol;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NomadCloud extends AbstractCloudImpl {

    private static final Logger LOGGER = Logger.getLogger(NomadCloud.class.getName());
    private final List<? extends NomadSlaveTemplate> templates;
    private final String nomadUrl;
    private String jenkinsUrl;
    private String jenkinsTunnel;
    private String slaveUrl;
    private NomadApi nomad;
    private int pending = 0;

    @DataBoundConstructor
    public NomadCloud(
            String name,
            String nomadUrl,
            String jenkinsUrl,
            String jenkinsTunnel,
            String slaveUrl,
            List<? extends NomadSlaveTemplate> templates) {
        super(name, null);

        this.nomadUrl = nomadUrl;
        this.jenkinsUrl = jenkinsUrl;
        this.jenkinsTunnel = jenkinsTunnel;
        this.slaveUrl = slaveUrl;

        if (templates == null) {
            this.templates = Collections.emptyList();
        } else {
            this.templates = templates;
        }

        readResolve();
    }

    private Object readResolve() {
        for (NomadSlaveTemplate template : this.templates) {
            template.setCloud(this);
        }
        nomad = new NomadApi(nomadUrl);

        if (jenkinsUrl.equals("")) {
            jenkinsUrl = Jenkins.getInstance().getRootUrl();
        }

        if (jenkinsTunnel.equals("")) {
            jenkinsTunnel = jenkinsUrl;
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
                    cloud,
                    slaveName,
                    "Nomad Jenkins Slave",
                    template,
                    template.getLabels(),
                    new NomadRetentionStrategy(template.getIdleTerminationInMinutes()),
                    Collections.emptyList()
            );
            Jenkins.getInstance().addNode(slave);

            // Support for Jenkins security
            String jnlpSecret = "";
            if (Jenkins.getInstance().isUseSecurity()) {
                jnlpSecret = JnlpSlaveAgentProtocol.SLAVE_SECRET.mac(slaveName);
            }

            LOGGER.log(Level.INFO, "Asking Nomad to schedule new Jenkins slave");
            nomad.startSlave(slaveName, jnlpSecret, template);

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

            try {
                future.get(5, TimeUnit.MINUTES);
                LOGGER.log(Level.INFO, "Connection established");
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Slave computer did not come online within {0} minutes, terminating slave");
                slave.terminate();
            } finally {
                future.cancel(true);
                executorService.shutdown();
            }
            pending -= template.getNumExecutors();
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

        @RequirePOST
        public FormValidation doTestConnection(@QueryParameter("nomadUrl") String nomadUrl) {
            Objects.requireNonNull(Jenkins.getInstance()).checkPermission(Jenkins.ADMINISTER);
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

        public FormValidation doCheckName(@QueryParameter String name) {
            if (Strings.isNullOrEmpty(name)) {
                return FormValidation.error("Name must be set");
            } else {
                return FormValidation.ok();
            }
        }
    }

    // Getters
    protected String getNomadUrl() {
        return nomadUrl;
    }

    public String getJenkinsUrl() {
        return jenkinsUrl;
    }

    public String getSlaveUrl() {
        return slaveUrl;
    }

    public void setJenkinsUrl(String jenkinsUrl) {
        this.jenkinsUrl = jenkinsUrl;
    }

    public void setSlaveUrl(String slaveUrl) {
        this.slaveUrl = slaveUrl;
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

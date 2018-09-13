package org.jenkinsci.plugins.nomad;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.*;
import jenkins.model.Jenkins;

import java.io.IOException;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;

public class NomadSlave extends AbstractCloudSlave implements EphemeralNode {

    private static final Logger LOGGER = Logger.getLogger(NomadSlave.class.getName());

    private final Boolean reusable;

    private NomadSlaveTemplate template;

    private static final String NODE_DESCRIPTION = "Nomad Jenkins Slave";

    private final String cloudName;
    private final int idleTerminationInMinutes;

    public NomadSlave(
        String name,
        String cloudName,
        NomadSlaveTemplate template,
        String labelString,
        NomadRetentionStrategy retentionStrategy,
        List<? extends NodeProperty<?>> nodeProperties
    ) throws Descriptor.FormException, IOException {
        super(
            name,
            NODE_DESCRIPTION,
            template.getRemoteFs(),
            template.getNumExecutors(),
            template.getMode(),
            labelString,
            new JNLPLauncher(),
            retentionStrategy,
            nodeProperties
        );

        this.cloudName = cloudName;

        this.reusable = template.getReusable();
        this.idleTerminationInMinutes = template.getIdleTerminationInMinutes();
    }

    @DataBoundConstructor
    // {"name":"jenkins-95266550a531","cloudName":"NomadTest","labelString":"test","mode":"NORMAL","remoteFS":"/","numExecutors":"1","idleTerminationInMinutes":"10","reusable":true}
    public NomadSlave(String name, String cloudName, String remoteFS, String numExecutors, Mode mode, String labelString, String idleTerminationInMinutes, boolean reusable) throws FormException, IOException{
        super(name, NODE_DESCRIPTION, remoteFS, numExecutors, mode, labelString, new JNLPLauncher(), new NomadRetentionStrategy(idleTerminationInMinutes), Collections.<NodeProperty<?>>emptyList());

        this.cloudName = cloudName;
        this.reusable = reusable;

        this.idleTerminationInMinutes = Integer.parseInt(idleTerminationInMinutes);
    }

    @Override
    public Node asNode() {
        return this;
    }

    @Extension
    public static class DescriptorImpl extends SlaveDescriptor {
        public DescriptorImpl() {
            load();
        }

        @Override
        public String getDisplayName() {
            return "Nomad Slave";
        }

        /**
         * We only create these kinds of nodes programatically.
         */
        @Override
        public boolean isInstantiable() {
            return false;
        }
    }

    @Override
    public AbstractCloudComputer<NomadSlave> createComputer() {
        return new NomadComputer(this);
    }

    @Override
    protected void _terminate(TaskListener listener)  {
        LOGGER.log(Level.INFO, "Asking Nomad to deregister slave '" + getNodeName() + "'");
        getCloud().Nomad().stopSlave(getNodeName());
    }

    public NomadCloud getCloud() {
        return (NomadCloud) Jenkins.getInstance().getCloud(cloudName);
    }

    public String getCloudName() {
        return cloudName;
    }

    public Boolean getReusable() {
        return reusable;
    }

    public int getIdleTerminationInMinutes() {
        return this.idleTerminationInMinutes;
    }
}

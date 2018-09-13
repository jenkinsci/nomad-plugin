package org.jenkinsci.plugins.nomad;

import hudson.model.Descriptor;
import hudson.slaves.CloudRetentionStrategy;

public class NomadRetentionStrategy extends CloudRetentionStrategy{

    public NomadRetentionStrategy(int idleMinutes) {
        super(idleMinutes);
    }

    public NomadRetentionStrategy(String idleMinutes) {
        super(Integer.parseInt(idleMinutes));
    }

    public static class DescriptorImpl extends Descriptor<hudson.slaves.RetentionStrategy<?>> {
        @Override
        public String getDisplayName() {
            return "Nomad Retention Strategy";
        }
    }

}

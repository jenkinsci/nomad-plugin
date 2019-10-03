package org.jenkinsci.plugins.nomad;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

public class NomadPortTemplate implements Describable<NomadPortTemplate> {

    private final String label;
    private final String value;

    private NomadSlaveTemplate slave;

    @DataBoundConstructor
    public NomadPortTemplate(String label, String value) {
        this.label = label;
        this.value = value;
        readResolve();
    }

    protected Object readResolve() {
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Descriptor<NomadPortTemplate> getDescriptor() {
        return Jenkins.get().getDescriptor(getClass());
    }

    public String getLabel() {
        return label;
    }

    public String getValue() {
        return value;
    }

    public NomadSlaveTemplate getNomadSlaveTemplate() {
        return slave;
    }

    public void setNomadSlaveTemplate(NomadSlaveTemplate slave) {
        this.slave = slave;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<NomadPortTemplate> {

        public DescriptorImpl() {
            load();
        }

        @Override
        public String getDisplayName() {
            return null;
        }
    }
}

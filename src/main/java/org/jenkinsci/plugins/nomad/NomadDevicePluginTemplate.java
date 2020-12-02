package org.jenkinsci.plugins.nomad;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

public class NomadDevicePluginTemplate implements Describable<NomadDevicePluginTemplate> {

    private final String name;
    private final Integer count;

    private NomadWorkerTemplate worker;

    @DataBoundConstructor
    public NomadDevicePluginTemplate(
            String name,
            Integer count
    ) {
        this.name = name;
        this.count = count;
        readResolve();
    }

    protected Object readResolve() {
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Descriptor<NomadDevicePluginTemplate> getDescriptor() {
        return Jenkins.get().getDescriptor(getClass());
    }

    public String getName() {
        return name;
    }

    public Integer getCount() {
        return count;
    }

    public NomadWorkerTemplate getNomadWorkerTemplate() {
        return worker;
    }

    public void setNomadWorkerTemplate(NomadWorkerTemplate worker) {
        this.worker = worker;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<NomadDevicePluginTemplate> {

        public DescriptorImpl() {
            load();
        }

        @Override
        public String getDisplayName() {
            return "NomadWorkerTemplate";
        }
    }
}
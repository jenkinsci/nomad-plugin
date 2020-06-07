package org.jenkinsci.plugins.nomad;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

public class NomadConstraintTemplate implements Describable<NomadConstraintTemplate> {

    private final String ltarget;
    private final String operand;
    private final String rtarget;

    private NomadWorkerTemplate worker;

    @DataBoundConstructor
    public NomadConstraintTemplate(
            String ltarget,
            String operand,
            String rtarget
    ) {
        this.ltarget = ltarget;
        this.operand = operand;
        this.rtarget = rtarget;
        readResolve();
    }

    protected Object readResolve() {
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Descriptor<NomadConstraintTemplate> getDescriptor() {
        return Jenkins.get().getDescriptor(getClass());
    }

    public String getLtarget() {
        return ltarget;
    }

    public String getOperand() {
        return operand;
    }

    public String getRtarget() {
        return rtarget;
    }

    public NomadWorkerTemplate getNomadWorkerTemplate() {
        return worker;
    }

    public void setNomadWorkerTemplate(NomadWorkerTemplate worker) {
        this.worker = worker;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<NomadConstraintTemplate> {

        public DescriptorImpl() {
            load();
        }

        @Override
        public String getDisplayName() {
            return "NomadWorkerTemplate";
        }
    }
}
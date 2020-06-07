package org.jenkinsci.plugins.nomad.Api;

import org.jenkinsci.plugins.nomad.NomadConstraintTemplate;

public class Constraint {
    private String LTarget;
    private String Operand;
    private String RTarget;

    public Constraint(
            String ltarget,
            String operand,
            String rtarget
    ) {
        LTarget = ltarget;
        Operand = operand;
        RTarget = rtarget;
    }

    public Constraint(
            NomadConstraintTemplate nomadConstraintTemplate
    ) {
        LTarget = nomadConstraintTemplate.getLtarget();
        Operand = nomadConstraintTemplate.getOperand();
        RTarget = nomadConstraintTemplate.getRtarget();
    }

    public String getLtarget() {
        return LTarget;
    }

    public void setLtarget(String ltarget) {
        LTarget = ltarget;
    }

    public String getOperand() {
        return Operand;
    }

    public void setOperand(String operand) {
        Operand = operand;
    }

    public String getRtarget() {
        return RTarget;
    }

    public void setRtarget(String rtarget) {
        RTarget = rtarget;
    }
}
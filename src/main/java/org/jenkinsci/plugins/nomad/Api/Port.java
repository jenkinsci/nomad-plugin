package org.jenkinsci.plugins.nomad.Api;

import org.jenkinsci.plugins.nomad.NomadPortTemplate;

public class Port {

    private String Label;
    private Integer Value;

    public Port(String label, Integer value) {
        this.Label = label;
        this.Value = value;
    }

    public Port(NomadPortTemplate template) {
        this.Label = template.getLabel();
        this.Value = Integer.parseInt(template.getValue());
    }

    public String getLabel() {
        return Label;
    }

    public void setLabel(String label) {
        this.Label = label;
    }

    public Integer getValue() {
        return Value;
    }

    public void setValue(Integer value) {
        this.Value = value;
    }
}

package org.jenkinsci.plugins.nomad.Api;

import org.jenkinsci.plugins.nomad.NomadPortTemplate;

import java.util.ArrayList;
import java.util.List;

public class PortGroup {

    private List<Port> ports = new ArrayList<>();

    public PortGroup(List<? extends NomadPortTemplate> portTemplate) {
        for (NomadPortTemplate template : portTemplate) {
            ports.add(new Port(template));
        }
    }

    public List<Port> getPorts() {
        return ports;
    }
}

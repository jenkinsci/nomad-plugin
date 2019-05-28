package org.jenkinsci.plugins.nomad.Api;

import java.util.List;

public class Network {

    private Integer MBits;
    private List<Port> ReservedPorts;

    public Network(Integer mbits, List<Port> reservedPorts) {
        MBits = mbits;
        ReservedPorts = reservedPorts;
    }

    public Integer getMBits() {
        return MBits;
    }

    public void setMBits(Integer MBits) {
        this.MBits = MBits;
    }

    public List<Port> getReservedPorts() {
        return ReservedPorts;
    }

    public void setReservedPorts(List<Port> reservedPorts) {
        this.ReservedPorts = reservedPorts;
    }
}

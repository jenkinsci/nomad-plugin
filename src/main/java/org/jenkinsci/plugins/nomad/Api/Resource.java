package org.jenkinsci.plugins.nomad.Api;

import java.util.List;

public class Resource {

    private Integer CPU;
    private Integer MemoryMB;
    private List<Network> Networks;

    public Resource(Integer CPU, Integer memoryMB, List<Network> networks) {
        this.CPU = CPU;
        this.MemoryMB = memoryMB;
        this.Networks = networks;
    }

    public List<Network> getNetworks() {
        return Networks;
    }

    public void setNetworks(List<Network> networks) {
        this.Networks = networks;
    }

    public Integer getCPU() {
        return CPU;
    }

    public void setCPU(Integer CPU) {
        this.CPU = CPU;
    }

    public Integer getMemoryMB() {
        return MemoryMB;
    }

    public void setMemoryMB(Integer memoryMB) {
        this.MemoryMB = memoryMB;
    }
}

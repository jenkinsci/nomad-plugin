package org.jenkinsci.plugins.nomad.Api;

import java.util.List;

public class Resource {

    private Integer CPU;
    private Integer MemoryMB;
    private List<Network> Networks;
    private List<Device> Devices;

    public Resource(Integer CPU,
                    Integer memoryMB,
                    List<Network> networks,
                    List<Device> devices) {
        this.CPU = CPU;
        this.MemoryMB = memoryMB;
        this.Networks = networks;
        this.Devices = devices;
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

    public List<Device> getDevicePlugins() {
        return this.Devices;
    }

    public void setDevicePlugins(List<Device> devicePlugins) {
        this.Devices = devicePlugins;
    }
}

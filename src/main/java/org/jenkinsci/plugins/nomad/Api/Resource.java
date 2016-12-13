package org.jenkinsci.plugins.nomad.Api;

public class Resource {
    private Integer CPU;
    private Integer MemoryMB;
	
    public Resource(Integer CPU, Integer memoryMB) {
        this.CPU = CPU;
        MemoryMB = memoryMB;
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
        MemoryMB = memoryMB;
    }
}

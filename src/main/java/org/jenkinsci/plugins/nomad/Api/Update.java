package org.jenkinsci.plugins.nomad.Api;

public class Update {
    private Integer Stagger;
    private Integer MaxParallel;

    public Update(Integer stagger, Integer maxParallel) {
        Stagger = stagger;
        MaxParallel = maxParallel;
    }

    public Integer getMaxParallel() {
        return MaxParallel;
    }

    public void setMaxParallel(Integer maxParallel) {
        MaxParallel = maxParallel;
    }

    public Integer getStagger() {
        return Stagger;
    }

    public void setStagger(Integer stagger) {
        Stagger = stagger;
    }
}

package org.jenkinsci.plugins.nomad.Api;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Vault {

    private List<String> Policies;

    public Vault( String policies ) {
        if (policies.isEmpty())
        {
            Policies = null;
        }
        else {
            Policies = Arrays.asList(policies.split(","));
        }
        
    }

    public List<String> getPolicies() {
        return Policies;
    }

    public void setPolicies(List<String> policies) {
        Policies = policies;
    }

    public Boolean isEmpty() {
        return Policies == null;
    }
}

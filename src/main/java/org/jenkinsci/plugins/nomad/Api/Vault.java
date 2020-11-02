package org.jenkinsci.plugins.nomad.Api;

import java.util.Arrays;
import java.util.List;

public final class Vault {

    private String[] Policies;

    public Vault( String[] policies ) {
        Policies = Arrays.copyOf(policies, policies.length);
    }

    public String[] getPolicies() {
        return Arrays.copyOf(Policies, Policies.length);
    }

    public void setPolicies(String[] policies) {
        Policies = Arrays.copyOf(policies, policies.length);
    }
}

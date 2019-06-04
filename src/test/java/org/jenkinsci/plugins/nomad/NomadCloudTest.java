package org.jenkinsci.plugins.nomad;

import hudson.model.labels.LabelAtom;
import hudson.slaves.NodeProvisioner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class NomadCloudTest {

    private NomadSlaveTemplate slaveTemplate = Mockito.mock(NomadSlaveTemplate.class);
    private LabelAtom label = Mockito.mock(LabelAtom.class);
    private NomadCloud nomadCloud = new NomadCloud(
            "nomad",
            "nomadUrl",
            "jenkinsUrl",
            "jenkinsTunnel",
            "slaveUrl",
            "1",
            "",
            Collections.singletonList(slaveTemplate));

    @Before
    public void setup() {
        Set<LabelAtom> labels = Collections.singleton(label);
        Mockito.when(label.matches(Mockito.anyCollectionOf(LabelAtom.class))).thenReturn(true);
        Mockito.when(slaveTemplate.createSlaveName()).thenReturn("slave-1", "slave-2", "slave-3");
        Mockito.when(slaveTemplate.getNumExecutors()).thenReturn(1);
        Mockito.when(slaveTemplate.getLabelSet()).thenReturn(labels);
    }

    @Test
    public void testCanProvision() {
        Assert.assertTrue(nomadCloud.canProvision(label));
    }

    @Test
    public void testProvision() {
        int workload = 3;
        Collection<NodeProvisioner.PlannedNode> plannedNodes = nomadCloud.provision(label, workload);

        Assert.assertEquals(plannedNodes.size(), workload);
    }

}

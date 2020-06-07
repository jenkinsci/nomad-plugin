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

    private final NomadWorkerTemplate workerTemplate = Mockito.mock(NomadWorkerTemplate.class);
    private final LabelAtom label = Mockito.mock(LabelAtom.class);
    private final NomadCloud nomadCloud = new NomadCloud(
            "nomad",
            "nomadUrl",
            "jenkinsUrl",
            "jenkinsTunnel",
            "workerUrl",
            "1",
            "",
            false,
            Collections.singletonList(workerTemplate));

    @Before
    public void setup() {
        Set<LabelAtom> labels = Collections.singleton(label);
        Mockito.when(label.matches(Mockito.anyCollectionOf(LabelAtom.class))).thenReturn(true);
        Mockito.when(workerTemplate.createWorkerName()).thenReturn("worker-1", "worker-2", "worker-3");
        Mockito.when(workerTemplate.getNumExecutors()).thenReturn(1);
        Mockito.when(workerTemplate.getLabelSet()).thenReturn(labels);
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

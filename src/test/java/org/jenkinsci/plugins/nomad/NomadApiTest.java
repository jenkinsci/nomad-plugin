package org.jenkinsci.plugins.nomad;

import hudson.model.Node;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * @author Yegor Andreenko
 */
public class NomadApiTest {

    private NomadApi nomadApi = new NomadApi("http://localhost");
    private List<NomadConstraintTemplate> constraintTest = new ArrayList<NomadConstraintTemplate>();
    private NomadSlaveTemplate slaveTemplate = new NomadSlaveTemplate(
            "300", "256", "100",
            null, constraintTest, "remoteFs", "3", true, "1", Node.Mode.NORMAL,
            "ams", "0", "image", "dc01", "", "", false, "bridge",
            "", true, "/mnt:/mnt", "jenkins", new ArrayList<NomadPortTemplate>() {
    }
    );

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
        slaveTemplate.setCloud(nomadCloud);
    }

    @Test
    public void testStartSlave() {
        String job = nomadApi.buildSlaveJob("slave-1", "secret", slaveTemplate);

        assertTrue(job.contains("\"Region\":\"ams\""));
        assertTrue(job.contains("\"CPU\":300"));
        assertTrue(job.contains("\"MemoryMB\":256"));
        assertTrue(job.contains("\"SizeMB\":100"));
        assertTrue(job.contains("\"GetterSource\":\"slaveUrl\""));
        assertTrue(job.contains("\"privileged\":false"));
        assertTrue(job.contains("\"network_mode\":\"bridge\""));
        assertTrue(job.contains("\"force_pull\":true"));
        assertTrue(job.contains("\"volumes\":[\"/mnt:/mnt\"]"));
        assertTrue(job.contains("\"User\":\"jenkins\""));
    }

}

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
            "test", "300", "256", "100",
            null, constraintTest, "remoteFs", false, "3", true, "1", Node.Mode.NORMAL,
            "ams", "0", "image", "dc01", "", "", false, "bridge",
            "", true, "/mnt:/mnt", "jenkins", new ArrayList<NomadPortTemplate>() {},
            "my_host:192.168.1.1,", "SYS_ADMIN, SYSLOG", "SYS_ADMIN, SYSLOG"
    );

    private NomadCloud nomadCloud = new NomadCloud(
            "nomad",
            "nomadUrl",
            "jenkinsUrl",
            "jenkinsTunnel",
            "slaveUrl",
            "1",
            "",
            false,
            Collections.singletonList(slaveTemplate));

    @Test
    public void testStartSlave() {
        String job = nomadApi.buildSlaveJob("slave-1", "secret", nomadCloud, slaveTemplate);

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
        assertTrue(job.contains("\"extra_hosts\":[\"my_host:192.168.1.1\"]"));
        assertTrue(job.contains("\"cap_add\":[\"SYS_ADMIN\",\"SYSLOG\"]"));
        assertTrue(job.contains("\"cap_drop\":[\"SYS_ADMIN\",\"SYSLOG\"]"));
    }

}

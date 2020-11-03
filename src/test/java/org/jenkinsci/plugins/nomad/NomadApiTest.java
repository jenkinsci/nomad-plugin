package org.jenkinsci.plugins.nomad;

import hudson.model.Node;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * @author Yegor Andreenko
 */
public class NomadApiTest {

    private final NomadApi nomadApi = new NomadApi("http://localhost");
    private final List<NomadConstraintTemplate> constraintTest = new ArrayList<NomadConstraintTemplate>();
    private final NomadWorkerTemplate workerTemplate = new NomadWorkerTemplate(
            "test", "300", "256", "100",
            null, constraintTest, "remoteFs", false, "3", true, "1", Node.Mode.NORMAL,
            "ams", "0", "image", "dc01", "", "", false, "bridge",
            "", true, "/mnt:/mnt", "jenkins", new ArrayList<NomadPortTemplate>() {
    },
            "my_host:192.168.1.1,", "8.8.8.8,1.1.1.1", "apparmor=unconfined, seccomp=unconfined", "SYS_ADMIN, SYSLOG", "SYS_ADMIN, SYSLOG", ""
    );

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

    @Test
    public void testStartWorker() {
        String job = nomadApi.buildWorkerJob("worker-1", "secret", nomadCloud, workerTemplate);

        assertTrue(job.contains("\"Region\":\"ams\""));
        assertTrue(job.contains("\"CPU\":300"));
        assertTrue(job.contains("\"MemoryMB\":256"));
        assertTrue(job.contains("\"SizeMB\":100"));
        assertTrue(job.contains("\"GetterSource\":\"workerUrl\""));
        assertTrue(job.contains("\"privileged\":false"));
        assertTrue(job.contains("\"network_mode\":\"bridge\""));
        assertTrue(job.contains("\"force_pull\":true"));
        assertTrue(job.contains("\"volumes\":[\"/mnt:/mnt\"]"));
        assertTrue(job.contains("\"User\":\"jenkins\""));
        assertTrue(job.contains("\"extra_hosts\":[\"my_host:192.168.1.1\"]"));
        assertTrue(job.contains("\"security_opt\":[\"apparmor=unconfined\",\"seccomp=unconfined\"]"));
        assertTrue(job.contains("\"cap_add\":[\"SYS_ADMIN\",\"SYSLOG\"]"));
        assertTrue(job.contains("\"cap_drop\":[\"SYS_ADMIN\",\"SYSLOG\"]"));
    }
}

package org.jenkinsci.plugins.nomad;

import hudson.model.Node;
import hudson.util.Secret;
import org.junit.Test;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Yegor Andreenko
 */
public class NomadApiTest {

    private final List<NomadConstraintTemplate> constraintTest = new ArrayList<NomadConstraintTemplate>();
    private final NomadDevicePluginTemplate deviceTest = new NomadDevicePluginTemplate("nvidia/gpu", 1);
    private final List<NomadDevicePluginTemplate> devicePluginsTest = Arrays.asList(deviceTest);
    private final NomadWorkerTemplate workerTemplate = new NomadWorkerTemplate(
            "test", "300", "256", "100",
            null, constraintTest, "remoteFs", false, "3", true, "1", Node.Mode.NORMAL,
            "ams", "0", "image", "dc01", "", Secret.fromString(""), false, "bridge",
            "", true, "/mnt:/mnt", "jenkins", new ArrayList<NomadPortTemplate>() {
    },
            "my_host:192.168.1.1,", "8.8.8.8,1.1.1.1", "apparmor=unconfined, seccomp=unconfined", "SYS_ADMIN, SYSLOG", "SYS_ADMIN, SYSLOG", "policy1,policy2", devicePluginsTest
    );

    private final NomadCloud nomadCloud = new NomadCloud(
            "nomad",
            "nomadUrl",
            false,
            null,
            null,
            null,
            null,
            "jenkinsUrl",
            "jenkinsTunnel",
            "workerUrl",
            "1",
            "",
            false,
            Collections.singletonList(workerTemplate));

    private final NomadApi nomadApi = new NomadApi(nomadCloud);

    @Test
    public void testStartWorker() {
        String job = nomadApi.buildWorkerJob("worker-1", "secret", nomadCloud, workerTemplate);
        assertTrue(job.contains("\"Region\":\"ams\""));
        assertTrue(job.contains("\"Resources\":{\"CPU\":300,\"MemoryMB\":256,\"Networks\":[{\"MBits\":1,\"ReservedPorts\":[]}],\"Devices\":[{\"Name\":\"nvidia/gpu\",\"Count\":1}]}"));
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
        assertTrue(job.contains("\"Vault\":{\"Policies\":[\"policy1\",\"policy2\"]}"));
    }

    private final NomadWorkerTemplate nullTemplate = new NomadWorkerTemplate(
            "test", "300", "256", "100",
            null, constraintTest, "remoteFs", false, "3", true, "1", Node.Mode.NORMAL,
            "ams", "0", "image", "dc01", "", Secret.fromString(""), false, "bridge",
            "", true, "/mnt:/mnt", "jenkins", new ArrayList<NomadPortTemplate>() {
    },
            "my_host:192.168.1.1,", "8.8.8.8,1.1.1.1", "apparmor=unconfined, seccomp=unconfined", "SYS_ADMIN, SYSLOG", "SYS_ADMIN, SYSLOG", null, devicePluginsTest
    );
    @Test
    public void testNullTemplate() {
        String job = nomadApi.buildWorkerJob("worker-1", "secret", nomadCloud, nullTemplate);
        assertFalse(job.contains("\"Vault\""));
    }

}

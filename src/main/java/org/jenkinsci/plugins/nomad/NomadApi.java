package org.jenkinsci.plugins.nomad;

import com.google.gson.*;
import hudson.Util;
import org.jenkinsci.plugins.nomad.Api.Job;
import okhttp3.*;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.nomad.Api.*;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class NomadApi {

    private static final Logger LOGGER = Logger.getLogger(NomadApi.class.getName());

    private static final OkHttpClient client = new OkHttpClient();

    private final String nomadApi;

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    NomadApi(String nomadApi) {
        this.nomadApi = nomadApi;
    }

    void startSlave(String slaveName, String nomadToken, String jnlpSecret, NomadSlaveTemplate template) {

        String slaveJob = buildSlaveJob(
            slaveName,
            jnlpSecret,
            template
        );

        LOGGER.log(Level.FINE, slaveJob);

        try {
            RequestBody body = RequestBody.create(JSON, slaveJob);
            Request.Builder builder = new Request.Builder()
                    .url(this.nomadApi + "/v1/job/" + slaveName + "?region=" + template.getRegion());

            if (StringUtils.isNotEmpty(nomadToken))
                builder = builder.header("X-Nomad-Token", nomadToken);

            Request request = builder.put(body)
                .build();

            client.newCall(request).execute().body().close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    void stopSlave(String slaveName, String nomadToken) {

        Request.Builder builder = new Request.Builder()
                .url(this.nomadApi + "/v1/job/" + slaveName);

        if (StringUtils.isNotEmpty(nomadToken))
            builder = builder.addHeader("X-Nomad-Token", nomadToken);

        Request request = builder.delete()
                .build();

        try {
            client.newCall(request).execute().body().close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }

    }

    JobInfo[] getRunningWorkers(String prefix, String nomadToken) {

        JobInfo[] nomadJobs = null;

        Request.Builder builder = new Request.Builder()
                .url(this.nomadApi + "/v1/jobs?prefix=" + prefix)
                .get();

        if (StringUtils.isNotEmpty(nomadToken))
            builder = builder.addHeader("X-Nomad-Token", nomadToken);

        Request request = builder.build();

        try {
            ResponseBody body = client.newCall(request).execute().body();

            if (body != null) {
                Gson gson = new Gson();

                nomadJobs = gson.fromJson(body.string(), JobInfo[].class);

                body.close();
            }
        } catch (IOException e){
            LOGGER.log(Level.SEVERE, "Failed to retrieve running jobs", e);
        }

        return nomadJobs;
    }

    private Map<String,Object> buildDriverConfig(String name, String secret, NomadSlaveTemplate template) {
        Map<String,Object> driverConfig = new HashMap<>();

        if (template.getUsername() != null && !template.getUsername().isEmpty()) {
            Map<String, String> authConfig = new HashMap<>();
            authConfig.put("username", template.getUsername());
            authConfig.put("password", template.getPassword());

            ArrayList<Map> credentials = new ArrayList<>();
            credentials.add(authConfig);

            driverConfig.put("auth", credentials);
        }

        ArrayList<String> args = new ArrayList<>();

        if (template.isJavaDriver()) {
            args.add("-jnlpUrl");

            args.add(Util.ensureEndsWith(template.getCloud().getJenkinsUrl(), "/") + "computer/" + name + "/slave-agent.jnlp");

            // java -cp /local/slave.jar [options...] <secret key> <agent name>
            if (!secret.isEmpty()) {
                args.add("-secret");
                args.add(secret);
            }

            driverConfig.put("jar_path", "/local/slave.jar");
            driverConfig.put("args", args);
        } else if (template.isDockerDriver()) {
            args.add("-headless");

            if (!template.getCloud().getJenkinsUrl().isEmpty()) {
                args.add("-url");
                args.add(template.getCloud().getJenkinsUrl());
            }

            if (!template.getCloud().getJenkinsTunnel().isEmpty()) {
                args.add("-tunnel");
                args.add(template.getCloud().getJenkinsTunnel());
            }

            if (!template.getRemoteFs().isEmpty()) {
                args.add("-workDir");
                args.add(Util.ensureEndsWith(template.getRemoteFs(), "/"));
            }

            // java -cp /local/slave.jar [options...] <secret key> <agent name>
            if (!secret.isEmpty()) {
                args.add(secret);
            }
            args.add(name);

            String prefixCmd = template.getPrefixCmd();
            // If an addtional command is defined - prepend it to jenkins slave invocation
            if (!prefixCmd.isEmpty())
            {
                driverConfig.put("command", "/bin/bash");
                String argString =
                        prefixCmd + "; java -cp /local/slave.jar hudson.remoting.jnlp.Main -headless ";
                argString += StringUtils.join(args, " ");
                args.clear();
                args.add("-c");
                args.add(argString);
            }
            else {
                driverConfig.put("command", "java");
                args.add(0, "-cp");
                args.add(1, "/local/slave.jar");
                args.add(2, "hudson.remoting.jnlp.Main");
            }
            driverConfig.put("image", template.getImage());

            String hostVolumes = template.getHostVolumes();
            if (!hostVolumes.isEmpty()) {
                driverConfig.put("volumes", StringUtils.split(hostVolumes, ","));
            }

            driverConfig.put("args", args);
            driverConfig.put("force_pull", template.getForcePull());
            driverConfig.put("privileged", template.getPrivileged());
            driverConfig.put("network_mode", template.getNetwork());
        }

        return driverConfig;
    }

    String buildSlaveJob(
            String name,
            String secret,
            NomadSlaveTemplate template
    ) {
        PortGroup portGroup = new PortGroup(template.getPorts());
        Network network = new Network(1, portGroup.getPorts());

        ArrayList<Network> networks = new ArrayList<>(1);
        networks.add(network);

        Task task = new Task(
                "jenkins-slave",
                template.getDriver(),
                template.getSwitchUser(),
                buildDriverConfig(name, secret, template),
                new Resource(
                    template.getCpu(),
                    template.getMemory(),
                    networks
                ),
                new LogConfig(1, 10),
                new Artifact[]{
                    new Artifact(template.getCloud().getSlaveUrl(), null, "/local/")
                }
        );

        TaskGroup taskGroup = new TaskGroup(
                "jenkins-slave-taskgroup",
                1,
                new Task[]{task},
                new RestartPolicy(0, 10000000000L, 1000000000L, "fail"),
                new EphemeralDisk(template.getDisk(), false, false)
        );

        ConstraintGroup constraintGroup = new ConstraintGroup(template.getConstraints());
        List<Constraint> Constraints = constraintGroup.getConstraints();

        Job job = new Job(
                name,
                name,
                template.getRegion(),
                "batch",
                template.getPriority(),
                template.getDatacenters().split(","),
                Constraints,
                new TaskGroup[]{taskGroup}
        );

        Gson gson = new Gson();
        JsonObject jobJson = new JsonObject();

        jobJson.add("Job", gson.toJsonTree(job));

        return gson.toJson(jobJson);
    }
}

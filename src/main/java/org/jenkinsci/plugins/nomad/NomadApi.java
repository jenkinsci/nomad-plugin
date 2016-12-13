package org.jenkinsci.plugins.nomad;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import hudson.Util;
import okhttp3.*;
import org.jenkinsci.plugins.nomad.Api.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;

public final class NomadApi {

    private static final Logger LOGGER = Logger.getLogger(NomadApi.class.getName());

    private static final OkHttpClient client = new OkHttpClient();

    private final String nomadApi;

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public NomadApi(String nomadApi) {
        this.nomadApi = nomadApi;
    }

    public void startSlave(String slaveName, String jnlpSecret, NomadSlaveTemplate template) {

        String slaveJob = buildSlaveJob(
            slaveName,
            jnlpSecret,
            template
        );

        LOGGER.log(Level.INFO, slaveJob);

        try {
            RequestBody body = RequestBody.create(JSON, slaveJob);
            Request request = new Request.Builder()
                    .url(this.nomadApi + "/v1/job/" + slaveName + "?region=" + template.getRegion())
                    .put(body)
                    .build();

            client.newCall(request).execute().body().close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }


    public void stopSlave(String slaveName) {

        Request request = new Request.Builder()
                .url(this.nomadApi + "/v1/job/" + slaveName)
                .delete()
                .build();

        try {
            client.newCall(request).execute().body().close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }

    }

    private Map<String,Object> buildDriverConfig(String name, String secret, NomadSlaveTemplate template) {
        Map<String,Object> driverConfig = new HashMap<>();

        ArrayList<String> args = new ArrayList<>();
        args.add("-jnlpUrl");

        args.add(Util.ensureEndsWith(template.getCloud().getJenkinsUrl(), "/") + "computer/" + name + "/slave-agent.jnlp");

        if (template.getUsername() != null && !template.getUsername().isEmpty()) {
            Map<String,String> authConfig = new HashMap<>();
            authConfig.put("username", template.getUsername());
            authConfig.put("password", template.getPassword());

            ArrayList<Map> credentials = new ArrayList<>();
            credentials.add(authConfig);

            driverConfig.put("auth", credentials);
        }

        if (!secret.isEmpty()) {
            args.add("-secret");
            args.add(secret);
        }

        if (template.getDriver().equals("java")) {
            driverConfig.put("jar_path", "/slave.jar");
            driverConfig.put("args", args);
        } else if (template.getDriver().equals("docker")) {
            String prefixCmd = template.getPrefixCmd();
            // If an addtional command is defined - prepend it to jenkins slave invocation
            if (!prefixCmd.isEmpty())
            {
                driverConfig.put("command", "/bin/bash");
                String argString = prefixCmd + "; java -jar /local/slave.jar ";
                argString += StringUtils.join(args, " ");
                args.clear();
                args.add("-c");
                args.add(argString);
            }
            else {
                driverConfig.put("command", "java");
                args.add(0, "-jar");
                args.add(1, "/local/slave.jar");
            }
            driverConfig.put("image", template.getImage());
         
            driverConfig.put("args", args);
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

        Task task = new Task(
                "jenkins-slave",
                template.getDriver(),
                buildDriverConfig(name, secret,template),
                new Resource(
                    template.getCpu(),
                    template.getMemory()
                ),
                new LogConfig(1, 10),
                new Artifact[]{
                    new Artifact(template.getCloud().getSlaveUrl(), null, "")
                }
        );

        TaskGroup taskGroup = new TaskGroup(
                "jenkins-slave-taskgroup",
                1,
                new Task[]{task},
                new RestartPolicy(0, 10000000000L, 1000000000L, "fail"),
                new EphemeralDisk(template.getDisk(), false, false)
        );

        Job job = new Job(
                name,
                name,
                template.getRegion(),
                "batch",
                template.getPriority(),
                template.getDatacenters().split(","),
                new TaskGroup[]{taskGroup}
        );

        Gson gson = new Gson();
        JsonObject jobJson = new JsonObject();

        jobJson.add("Job", gson.toJsonTree(job));

        return gson.toJson(jobJson);
    }
}

package org.jenkinsci.plugins.nomad.Api;

import java.util.Arrays;

public class TaskGroup {
    private String Name;
    private Integer Count;
    private Task[] Tasks;
    private RestartPolicy RestartPolicy;
    private EphemeralDisk EphemeralDisk;

    public TaskGroup(String name, Integer count, Task[] tasks, RestartPolicy restartPolicy, EphemeralDisk ephemeralDisk) {
        Name = name;
        Count = count;
        Tasks = Arrays.copyOf(tasks, tasks.length);
        RestartPolicy = restartPolicy;
        EphemeralDisk = ephemeralDisk;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public Integer getCount() {
        return Count;
    }

    public void setCount(Integer count) {
        Count = count;
    }

    public Task[] getTasks() {
        return Arrays.copyOf(Tasks, Tasks.length);
    }

    public void setTasks(Task[] tasks) {
        Tasks = Arrays.copyOf(tasks, tasks.length);
    }

    public RestartPolicy getRestartPolicy() {
        return RestartPolicy;
    }

    public void setRestartPolicy(RestartPolicy restartPolicy) {
        RestartPolicy = restartPolicy;
    }

    public EphemeralDisk getEphemeralDisk() {
        return EphemeralDisk;
    }

    public void setEphemeralDisk(EphemeralDisk ephemeralDisk) {
        EphemeralDisk = ephemeralDisk;
    }
}

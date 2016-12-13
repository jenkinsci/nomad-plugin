package org.jenkinsci.plugins.nomad.Api;

public class TaskGroup {
    private String Name;
    private Integer Count;
    private Task[] Tasks;
    private RestartPolicy RestartPolicy;
    private EphemeralDisk EphemeralDisk;

    public TaskGroup(String name, Integer count, Task[] tasks, RestartPolicy restartPolicy, EphemeralDisk ephemeralDisk) {
        Name = name;
        Count = count;
        Tasks = tasks;
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
        return Tasks;
    }

    public void setTasks(Task[] tasks) {
        Tasks = tasks;
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

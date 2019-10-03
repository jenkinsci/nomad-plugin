package org.jenkinsci.plugins.nomad.Api;

import java.util.List;

public final class Job {

    private String ID;
    private String Name;
    private String Region;
    private String Type;
    private Integer Priority;
    private String[] Datacenters;
    private List<Constraint> Constraints;
    private TaskGroup[] TaskGroups;

    public Job(
        String ID,
        String name,
        String region,
        String type,
        Integer priority,
        String[] datacenters,
        List<Constraint> constraints,
        TaskGroup[] taskGroups)
    {
        this.ID = ID;
        Name = name;
        Region = region;
        Type = type;
        Priority = priority;
        Datacenters = datacenters;
        Constraints = constraints;
        TaskGroups = taskGroups;
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getRegion() {
        return Region;
    }

    public void setRegion(String region) {
        Region = region;
    }

    public String getType() {
        return Type;
    }

    public void setType(String type) {
        Type = type;
    }

    public Integer getPriority() {
        return Priority;
    }

    public void setPriority(Integer priority) {
        Priority = priority;
    }

    public String[] getDatacenters() {
        return Datacenters;
    }

    public void setDatacenters(String[] datacenters) {
        Datacenters = datacenters;
    }

    public TaskGroup[] getTaskGroups() {
        return TaskGroups;
    }

    public void setTaskGroups(TaskGroup[] taskGroups) {
        TaskGroups = taskGroups;
    }

    public List<Constraint> getConstraints() {
        return Constraints;
    }

    public void setConstraints(List<Constraint> constraints) {
        Constraints = constraints;
    }

}

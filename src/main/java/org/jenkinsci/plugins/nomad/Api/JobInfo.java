package org.jenkinsci.plugins.nomad.Api;

public final class JobInfo
{

    private String ID;
    private String Name;
    private String Type;
    private String Status;
    private Integer Priority;

    public JobInfo(
        String ID,
        String name,
        String type,
        String status,
        Integer priority)
    {
        this.ID = ID;
        Name = name;
        Type = type;
        Status = status;
        Priority = priority;
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

    public String getStatus()
    {
        return Status;
    }

    public void setStatus(String status)
    {
        Status = status;
    }
}

package org.jenkinsci.plugins.nomad.Api;

public class EphemeralDisk {

    private Integer Size;
    private Boolean Migrate;
    private Boolean Sticky;
    
    public EphemeralDisk(Integer size, Boolean migrate, Boolean sticky) {
        Size = size;
        Migrate = migrate;
        Sticky = sticky;
    }

    public Integer getSize() {
        return Size;
    }

    public void setSize(Integer size) {
        Size = size;
    }

    public Boolean getMigrate() {
        return Migrate;
    }

    public void setMigrate(Boolean migrate) {
        Migrate = migrate;
    }
    
    public Boolean getSticky() {
        return Sticky;
    }

    public void setSticky(Boolean sticky) {
        Sticky = sticky;
    }
}

package org.jenkinsci.plugins.nomad.Api;

import org.jenkinsci.plugins.nomad.NomadDevicePluginTemplate;

public class Device {
    private String Name;
    private Integer Count;

    public Device(
            String name,
            Integer count
    ) {
        Name = name;
        Count = count;
    }

    public Device(
            NomadDevicePluginTemplate nomadDevicePluginTemplate
    ) {
        Name = nomadDevicePluginTemplate.getName();
        Count = nomadDevicePluginTemplate.getCount();
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
}
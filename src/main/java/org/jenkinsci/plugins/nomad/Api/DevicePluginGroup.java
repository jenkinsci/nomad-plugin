package org.jenkinsci.plugins.nomad.Api;

import org.jenkinsci.plugins.nomad.NomadDevicePluginTemplate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DevicePluginGroup {
    private final List<Device> devicePlugins = new ArrayList<Device>();

    public DevicePluginGroup(
            List<NomadDevicePluginTemplate> devicePluginTemplate
    ) {
        Iterator<NomadDevicePluginTemplate> devicePluginIterator = devicePluginTemplate.iterator();
        while (devicePluginIterator.hasNext()) {
            NomadDevicePluginTemplate nextTemplate = devicePluginIterator.next();
            devicePlugins.add(new Device(nextTemplate));
        }
    }

    public List<Device> getDevicePlugins() {
        Iterator<Device> devicePluginIterator = devicePlugins.iterator();

        List<Device> DevicePlugins = new ArrayList<Device>();

        while (devicePluginIterator.hasNext()) {
            Device nextDevicePlugin = devicePluginIterator.next();
            DevicePlugins.add(nextDevicePlugin);
        }
        return DevicePlugins;
    }
}
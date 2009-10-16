package com.l7tech.server.processcontroller.patching;

import java.util.Properties;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Patch builder configuration holder: specifies the patch properties, the files to be added,
 * and the Main-Class execution entry point.
 *
 * @author jbufu
 */
public class PatchSpec {

    // - PUBLIC

    public PatchSpec(String id, String desc, boolean rollbackAllowed, String mainClass) {
        // the required properties
        property(PatchPackage.Property.ID, id);
        property(PatchPackage.Property.DESCRIPTION, desc);
        property(PatchPackage.Property.ROLLBACK_ALLOWED, Boolean.toString(rollbackAllowed));
        mainClass(mainClass);
    }

    public PatchSpec property(PatchPackage.Property prop, String value) {
        properties.setProperty(prop.name(), value);
        return this;
    }

    public PatchSpec file(String zipEntryPath, String fileName) {
        entries.put(zipEntryPath, fileName);
        return this;
    }

    public PatchSpec mainClass(String mainClass) {
        this.mainClass = mainClass;
        return this;
    }

    public Properties getProperties() {
        return properties;
    }

    public String getMainClass() {
        return mainClass;
    }

    public Map<String, String> getEntries() {
        return entries;
    }

    // - PRIVATE

    private Properties properties = new Properties();
    private String mainClass;
    private Map<String, String> entries = new LinkedHashMap<String, String>();
}

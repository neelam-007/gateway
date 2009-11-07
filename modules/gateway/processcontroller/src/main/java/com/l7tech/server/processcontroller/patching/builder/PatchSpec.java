package com.l7tech.server.processcontroller.patching.builder;

import com.l7tech.server.processcontroller.patching.PatchPackage;

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
        validate();
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

    public PatchSpec outputFilename(String filename) {
        this.outputFilename = filename;
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

    public String getOutputFilename() {
        return outputFilename;
    }

    // - PACKAGE

    public PatchSpec() { }

    void validate() {
        for(PatchPackage.Property p : PatchPackage.Property.values()) {
            if (p.isRequired() && properties.get(p.name()) == null)
                throw new IllegalStateException("Cannot generate patch: required package property not specified: " + p.name());
        }

        if (mainClass == null)
            throw new IllegalStateException("Cannot generate patch: main class not specified.");
    }

    // - PRIVATE

    private Properties properties = new Properties();
    private String mainClass;
    private Map<String, String> entries = new LinkedHashMap<String, String>();
    private String outputFilename;
}

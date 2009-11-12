package com.l7tech.server.processcontroller.patching.builder;

import com.l7tech.server.processcontroller.patching.PatchPackage;
import com.l7tech.server.processcontroller.patching.PatchUtils;

import java.util.*;

/**
 * Patch builder configuration holder: specifies the patch properties, the jar entries to be added,
 * the Main-Class execution entry point and optionally the filename where the generated patch should be written.
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

    public PatchSpec entry(PatchSpecEntry entry) {
        if (entries.put(entry.getEntryName(), entry) != null)
            throw new IllegalStateException("Entry already exists in the patch spec: " + entry.getEntryName());
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

    public String getMainClassEntryName() {
        return PatchUtils.classToEntryName(mainClass);
    }

    public Map<String,PatchSpecEntry> getEntries() {
        return entries;
    }

    public String getOutputFilename() {
        return outputFilename;
    }

    // - PACKAGE

    PatchSpec() { }

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
    private Map<String,PatchSpecEntry> entries = new LinkedHashMap<String,PatchSpecEntry>();
    private String outputFilename;
}

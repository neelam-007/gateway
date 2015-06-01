package com.l7tech.policy.solutionkit;

import org.w3c.dom.Document;

/**
 * WARNING: this class is under development and is currently for CA internal use only.
 * This interface contract may change without notice.
 *
 * Provides data transport between the your UI and callback code.
 */
public class SolutionKitManagerContext {   // <T> {   // look into making this type safe
    /**
     * XML document representing the solution kit metadata (e.g. id, version, name, description, etc).
     */
    private Document solutionKitMetadata;

    /**
     * XML document representing the restman migration bundle to install the Solution Kit.
     */
    private Document migrationBundle;

    /**
     * A string to change entities so it's possible to install multiple instances of the same Solution Kit.
     * Depending on the entity the Solution Kit Manager can use the modifier as prefix or suffix.
     */
    private String instanceModifier;

    /**
     * Optionally you can provide a custom data object to pass between your UI and callback code.
     * Be sure to initialize your custom data object in your UI first, e.g. SolutionKitManagerUi.initialize().
     */
    private Object customDataObject;
    //    T customDataObject;

    public Document getSolutionKitMetadata() {
        return solutionKitMetadata;
    }

    public void setSolutionKitMetadata(Document solutionKitMetadata) {
        this.solutionKitMetadata = solutionKitMetadata;
    }

    public String getInstanceModifier() {
        return instanceModifier;
    }

    public void setInstanceModifier(String instanceModifier) {
        this.instanceModifier = instanceModifier;
    }

    public Object getCustomDataObject() {
        return customDataObject;
    }

    public void setCustomDataObject(Object customDataObject) {
        this.customDataObject = customDataObject;
    }

    public Document getMigrationBundle() {
        return migrationBundle;
    }

    public void setMigrationBundle(Document migrationBundle) {
        this.migrationBundle = migrationBundle;
    }

//    public T getCustomDataObject() {
//        return customDataObject;
//    }
//
//    public void setCustomDataObject(T customDataObject) {
//        this.customDataObject = customDataObject;
//    }
}

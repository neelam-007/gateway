package com.l7tech.policy.solutionkit;

import org.w3c.dom.Document;

import java.util.HashMap;
import java.util.Map;

/**
 * WARNING: this class is under development and is currently for CA internal use only.
 * This interface contract may change without notice.
 *
 * Provides data transport between the your UI and callback code.
 */
public class SolutionKitManagerContext {
    /**
     * XML document representing the solution kit metadata (e.g. id, version, name, description, etc).
     */
    private Document solutionKitMetadata;

    /**
     * XML document representing the restman migration bundle to install or upgrade the Solution Kit.
     */
    private Document migrationBundle;

    /**
     * XML document representing the restman migration bundle to uninstall the Solution Kit.
     */
    private Document uninstallBundle;

    /**
     * A string to change entities so it's possible to install multiple instances of the same Solution Kit.
     * Depending on the entity the Solution Kit Manager can use the modifier as prefix or suffix.
     */
    private String instanceModifier;

    /**
     * True if the current context is an upgrade (false if it's an install).
     */
    private boolean isUpgrade;

    /**
     * Optionally pass in key-value pairs from your GUI (or headless interface) to your callback code.
     */
    private Map<String, String> keyValues = new HashMap<String, String>();

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

    public Map<String, String> getKeyValues() {
        return keyValues;
    }

    public Document getMigrationBundle() {
        return migrationBundle;
    }

    public void setMigrationBundle(Document migrationBundle) {
        this.migrationBundle = migrationBundle;
    }

    public Document getUninstallBundle() {
        return uninstallBundle;
    }

    public void setUninstallBundle(Document uninstallBundle) {
        this.uninstallBundle = uninstallBundle;
    }

    public boolean isUpgrade() {
        return isUpgrade;
    }

    public void setUpgrade(boolean isUpgrade) {
        this.isUpgrade = isUpgrade;
    }
}

package com.l7tech.console.policy;

import com.l7tech.console.tree.PaletteFolderRegistry;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.Policy;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A simple registry that keeps track of currently-available encapsulated assertion configs.
 * <p/>
 * Reloads itself on logon, or when an encapsulated assertion config is added/removed/updated via the GUI.
 */
public class EncapsulatedAssertionRegistry {
    /** Cache of available encapsulated assertion configs.  To keep memory usage down, policy XML is not included for the embedded Policy objects.  */
    private Set<EncapsulatedAssertionConfig> configs = new HashSet<EncapsulatedAssertionConfig>();

    /**
     * Register an EncapsulatedAssertionConfig.
     *
     * @param config a config to add.  Required. A modified read-only copy will be stored.
     *               if a config with this OID is already registered, the old value will be replaced.
     */
    public void registerEncapsulatedAssertionConfig(@NotNull EncapsulatedAssertionConfig config) {
        config = config.getCopy();
        Policy policy = config.getPolicy();
        if (policy != null) {
            policy.setXml(null);
        }
        config = config.getReadOnlyCopy();
        configs.add(config);
    }

    /**
     * Un-register an EncapsulatedAssertionConfig.
     *
     * @param config the config to un-register.  Required.
     * @return true if a config was removed.
     */
    public boolean unregisterEncapsulatedAssertionConfig(@NotNull EncapsulatedAssertionConfig config) {
        return configs.remove(config);
    }

    /**
     * Load all current encapsulated assertions from Gateway and register them all locally.
     */
    public void updateEncapsulatedAssertions() throws FindException {
        Registry registry = Registry.getDefault();
        if (!registry.isAdminContextPresent()) {
            throw new FindException("Unable to load encapsulated assertions -- not connected to Gateway");
        }
        Collection<EncapsulatedAssertionConfig> configs = registry.getEncapsulatedAssertionAdmin().findAllEncapsulatedAssertionConfigs();
        replaceAllRegisteredConfigs(configs);
    }

    /**
     * Replace all configs with the specified ones.
     *
     * @param configs configs to register in place of existing ones.
     */
    public void replaceAllRegisteredConfigs(Collection<EncapsulatedAssertionConfig> configs) {
        this.configs.clear();
        for (EncapsulatedAssertionConfig config : configs) {
            registerEncapsulatedAssertionConfig(config);
        }
        notifyEncapsulatedAssertionsChanged();
    }

    /**
     * @return all currently-registered encapsulated assertion configs as unmodifiable Set.
     */
    public Set<EncapsulatedAssertionConfig> getRegisteredEncapsulatedAssertionConfigurations() {
        return Collections.unmodifiableSet(configs);
    }

    public void notifyEncapsulatedAssertionsChanged() {
        // TODO replace with event publish/subscribe mechanism; for now, we will just hardcode interested observers here
        reloadAssertionPaletteTree();
    }

    private static void reloadAssertionPaletteTree() {
        PaletteFolderRegistry paletteReg = TopComponents.getInstance().getPaletteFolderRegistry();
        if (paletteReg != null)
            paletteReg.refreshPaletteFolders();
    }
}

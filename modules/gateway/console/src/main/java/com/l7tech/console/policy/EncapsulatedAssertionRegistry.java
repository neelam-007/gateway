package com.l7tech.console.policy;

import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.tree.PaletteFolderRegistry;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.DefaultAssertionPolicyNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.*;

/**
 * A simple registry that keeps track of currently-available encapsulated assertion configs.
 * <p/>
 * Reloads itself on logon, or when an encapsulated assertion config is added/removed/updated via the GUI.
 */
public class EncapsulatedAssertionRegistry {
    /** Cache of available encapsulated assertion configs.  To keep memory usage down, policy XML is not included for the embedded Policy objects.  */
    private final Map<String,EncapsulatedAssertionConfig> configs = new HashMap<String, EncapsulatedAssertionConfig>();

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
        configs.put(config.getGuid(), config);
    }

    /**
     * Un-register an EncapsulatedAssertionConfig.
     *
     * @param config the config to un-register.  Required.
     * @return true if a config was removed.
     */
    public boolean unregisterEncapsulatedAssertionConfig(@NotNull EncapsulatedAssertionConfig config) {
        return configs.remove(config.getGuid()) != null;
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
     * @return all currently-registered encapsulated assertion configs as unmodifiable collection.
     */
    public Collection<EncapsulatedAssertionConfig> getRegisteredEncapsulatedAssertionConfigurations() {
        return Collections.unmodifiableCollection(configs.values());
    }

    public void notifyEncapsulatedAssertionsChanged() {
        // TODO replace with event publish/subscribe mechanism; for now, we will just hardcode interested observers here
        reloadAssertionPaletteTree();
        reloadPolicyEditorPanel();
    }

    private static void reloadAssertionPaletteTree() {
        PaletteFolderRegistry paletteReg = TopComponents.getInstance().getPaletteFolderRegistry();
        if (paletteReg != null)
            paletteReg.refreshPaletteFolders();
    }

    private void reloadPolicyEditorPanel() {
        final PolicyEditorPanel pep = TopComponents.getInstance().getPolicyEditorPanel();
        if (pep != null) {
            // update any encapsulated assertions open in the policy editor panel
            pep.visitCurrentlyOpenPolicyTreeNodes(new Functions.UnaryVoid<AssertionTreeNode>() {
                @Override
                public void call(@NotNull final AssertionTreeNode assertionTreeNode) {
                    final Assertion ass = assertionTreeNode.asAssertion();
                    if (ass instanceof EncapsulatedAssertion) {
                        final EncapsulatedAssertion encass = (EncapsulatedAssertion) ass;
                        final EncapsulatedAssertionConfig config = configs.get(encass.getEncapsulatedAssertionConfigGuid());
                        if (config != null) {
                            encass.config(config);
                        }
                        if (assertionTreeNode instanceof DefaultAssertionPolicyNode) {
                            final DefaultAssertionPolicyNode policyNode = (DefaultAssertionPolicyNode) assertionTreeNode;
                            policyNode.reloadPropertiesAction();
                        }
                        final JTree policyTree = TopComponents.getInstance().getPolicyTree();
                        if (policyTree != null && policyTree.getModel() instanceof PolicyTreeModel) {
                            ((PolicyTreeModel)(policyTree.getModel())).assertionTreeNodeChanged(assertionTreeNode);
                        }

                    }
                }
            });
        }
    }
}

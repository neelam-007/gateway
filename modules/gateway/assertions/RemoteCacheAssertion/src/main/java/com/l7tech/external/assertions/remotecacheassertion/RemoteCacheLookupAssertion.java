package com.l7tech.external.assertions.remotecacheassertion;

import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.remotecacheassertion.console.CacheServersDialog;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.AssertionNodeNameFactory;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 *
 */
public class RemoteCacheLookupAssertion extends MessageTargetableRemoteCacheAssertion {
    protected static final Logger logger = Logger.getLogger(RemoteCacheLookupAssertion.class.getName());

    private String cacheEntryKey = "${request.url}";

    public String getCacheEntryKey() {
        return cacheEntryKey;
    }

    public void setCacheEntryKey(String cacheEntryKey) {
        this.cacheEntryKey = cacheEntryKey;
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = RemoteCacheLookupAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        final DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED))) {
            return meta;
        }

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<>();
        props.put(Constants.CLUSTER_PROP_NAME, new String[]{
                "The remote cache server connections.",
                null
        });
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        meta.put(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.remotecacheassertion.server.ModuleLoadListener");

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Lookup from remote cache");
        meta.put(AssertionMetadata.LONG_NAME, "Lookup value from remote cache");

        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Remote Cache Lookup Properties");

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"misc"});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/external/assertions/cache/console/resources/load16.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/external/assertions/cache/console/resources/load16.gif");

        meta.put(AssertionMetadata.GLOBAL_ACTION_CLASSNAMES, new String[]{getClass().getName() + "$CustomAction"});

        // No need to register RemoteCacheEntityManagerServerSupport here. RemoteCacheStoreAssertion also registers
        // RemoteCacheEntityManagerServerSupport, which is in the same AAR as this assertion.
        // If an admin extension interface is registered multiple times, a SEVERE message is logged.
        //

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:Memcached" rather than "set:modularAssertions"
        //meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new AssertionNodeNameFactory<RemoteCacheLookupAssertion>() {
            @Override
            public String getAssertionName(final RemoteCacheLookupAssertion assertion, final boolean decorate) {
                final String displayName = meta.getString(AssertionMetadata.SHORT_NAME);
                return decorate ? assertion.getTargetName() + ": " + displayName + " [" + assertion.getRemoteCacheName() + "]" : displayName;
            }
        });

        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME,
                "com.l7tech.external.assertions.remotecacheassertion.RemoteCacheAssertionValidator");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[0];
    }

    @Override
    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames(cacheEntryKey);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static class CustomAction extends AbstractAction {
        public CustomAction() {
            super("Manage Remote Caches", ImageCache.getInstance().getIconAsIcon("com/l7tech/console/resources/Bean16.gif"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            CacheServersDialog dialog = new CacheServersDialog(TopComponents.getInstance().getTopParent());
            dialog.pack();
            Utilities.centerOnScreen(dialog);
            DialogDisplayer.display(dialog);
        }
    }

}

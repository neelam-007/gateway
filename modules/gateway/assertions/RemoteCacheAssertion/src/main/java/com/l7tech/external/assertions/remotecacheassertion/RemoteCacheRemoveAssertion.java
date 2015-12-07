package com.l7tech.external.assertions.remotecacheassertion;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.Syntax;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 *
 */
public class RemoteCacheRemoveAssertion extends RemoteCacheAssertion implements UsesVariables {
    protected static final Logger logger = Logger.getLogger(RemoteCacheRemoveAssertion.class.getName());

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
    private static final String META_INITIALIZED = RemoteCacheRemoveAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        final DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED))) {
            return meta;
        }

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<String, String[]>();
        props.put(Constants.CLUSTER_PROP_NAME, new String[]{
                "The remote cache server connections.",
                null
        });
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        meta.put(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.remotecacheassertion.server.ModuleLoadListener");

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Remove from remote cache");
        meta.put(AssertionMetadata.LONG_NAME, "Remove entry from remote cache");

        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Remote Cache Remove Properties");

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"misc"});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/external/assertions/cache/console/resources/store16.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/external/assertions/cache/console/resources/store16.gif");

        // No need to register RemoteCacheEntityManagerServerSupport here. RemoteCacheStoreAssertion also registers
        // RemoteCacheEntityManagerServerSupport, which is in the same AAR as this assertion.
        // If an admin extension interface is registered multiple times, a SEVERE message is logged.
        //

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:Memcached" rather than "set:modularAssertions"
        //meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new AssertionNodeNameFactory<RemoteCacheRemoveAssertion>() {
            @Override
            public String getAssertionName(final RemoteCacheRemoveAssertion assertion, final boolean decorate) {
                final String displayName = meta.getString(AssertionMetadata.SHORT_NAME);
                return decorate ? displayName + " [" + assertion.getRemoteCacheName() + "]" : displayName;
            }
        });

        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME,
                "com.l7tech.external.assertions.remotecacheassertion.RemoteCacheAssertionValidator");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    @Override
    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames(cacheEntryKey);
    }
}

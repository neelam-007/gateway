package com.l7tech.external.assertions.remotecacheassertion;

import com.l7tech.external.assertions.remotecacheassertion.server.CoherenceRemoteCache;
import com.l7tech.external.assertions.remotecacheassertion.server.RemoteCacheEntityManagerServerSupport;
import com.l7tech.external.assertions.remotecacheassertion.server.TerracottaRemoteCache;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.Functions;
import org.springframework.context.ApplicationContext;

import java.util.*;
import java.util.logging.Logger;

/**
 *
 */
public class RemoteCacheStoreAssertion extends MessageTargetableRemoteCacheAssertion {
    protected static final Logger logger = Logger.getLogger(RemoteCacheStoreAssertion.class.getName());

    public static final int DEFAULT_MAX_AGE_SECONDS = 5 * 60;
    public static final long DEFAULT_MAX_SIZE_BYTES = 10000;

    private String maxEntryAge = String.valueOf(DEFAULT_MAX_AGE_SECONDS);
    private String maxEntrySizeBytes = String.valueOf(DEFAULT_MAX_SIZE_BYTES);

    private String valueType;
    private String cacheEntryKey = "${request.url}";
    private boolean storeSoapFaults = false;

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public String getValueType() {
        return valueType != null ? valueType : "BYTE_ARRAY";
    }

    public String getMaxEntryAge() {
        return maxEntryAge;
    }

    public void setMaxEntryAge(String maxEntryAge) {
        this.maxEntryAge = maxEntryAge;
    }

    public String getMaxEntrySizeBytes() {
        return maxEntrySizeBytes;
    }

    public void setMaxEntrySizeBytes(String maxEntrySizeBytes) {
        this.maxEntrySizeBytes = maxEntrySizeBytes;
    }

    public String getCacheEntryKey() {
        return cacheEntryKey;
    }

    public void setCacheEntryKey(String cacheEntryKey) {
        this.cacheEntryKey = cacheEntryKey;
    }

    public boolean isStoreSoapFaults() {
        return storeSoapFaults;
    }

    public void setStoreSoapFaults(boolean storeSoapFaults) {
        this.storeSoapFaults = storeSoapFaults;
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = RemoteCacheStoreAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        final DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED))) {
            return meta;
        }

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<String, String[]>();

        props.put(CoherenceRemoteCache.CONNECTION_TIMEOUT_CLUSTER_WIDE_PROPERTY, new String[]{
                "The maximum amount of time to wait in milliseconds to establish a connection with Coherence cache server. Default is 5 seconds.",
                CoherenceRemoteCache.DEFAULT_CONNECTION_TIMEOUT
        });

        props.put(TerracottaRemoteCache.CONNECTION_TIMEOUT_CLUSTER_WIDE_PROPERTY, new String[]{
                "The maximum amount of time to wait in milliseconds to establish a connection with Terracotta cache server. Default is 5 seconds.",
                TerracottaRemoteCache.DEFAULT_CONNECTION_TIMEOUT
        });

        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Store to remote cache");
        meta.put(AssertionMetadata.LONG_NAME, "Store value in remote cache");

        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Remote Cache Storage Properties");

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"misc"});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/external/assertions/cache/console/resources/store16.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/external/assertions/cache/console/resources/store16.gif");

        meta.put(AssertionMetadata.EXTENSION_INTERFACES_FACTORY, new Functions.Unary<Collection<ExtensionInterfaceBinding>, ApplicationContext>() {
            @Override
            public Collection<ExtensionInterfaceBinding> call(ApplicationContext appContext) {
                return RemoteCacheEntityManagerServerSupport.getInstance(appContext).getExtensionInterfaceBindings();
            }
        });

        meta.put(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.remotecacheassertion.server.RemoteCacheAssertionModuleListener");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:Memcached" rather than "set:modularAssertions"
        //meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new AssertionNodeNameFactory<RemoteCacheStoreAssertion>() {
            @Override
            public String getAssertionName(final RemoteCacheStoreAssertion assertion, final boolean decorate) {
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
        return Syntax.getReferencedNames(cacheEntryKey, maxEntryAge, maxEntrySizeBytes);
    }
}

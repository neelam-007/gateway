package com.l7tech.external.assertions.cache;

import com.l7tech.external.assertions.cache.server.SsgCache;
import com.l7tech.policy.assertion.*;

/**
 * 
 */
public class CacheStorageAssertion extends MessageTargetableAssertion implements UsesVariables {

    public static final int kMAX_ENTRIES = 1000000;
    public static final long kMAX_ENTRY_AGE_SECONDS = 100000000L;
    public static final long kMAX_ENTRY_SIZE = 1000000000L;

    private String cacheId = "defaultCache";
    private String maxEntries = Integer.toString(SsgCache.Config.DEFAULT_MAX_ENTRIES);
    private String maxEntryAgeSeconds = Long.toString(SsgCache.Config.DEFAULT_MAX_AGE_MILLIS / 1000L);
    private String maxEntrySizeBytes = Long.toString(SsgCache.Config.DEFAULT_MAX_SIZE_BYTES);
    private String cacheEntryKey = "${request.url}";
    private boolean storeSoapFaults = false;

    public CacheStorageAssertion() {
        setTarget(TargetMessageType.RESPONSE);
        setTargetModifiedByGateway(false);
    }

    @Override
    protected VariablesUsed doGetVariablesUsed() {
        return super.doGetVariablesUsed().withExpressions( cacheId, cacheEntryKey, maxEntries, maxEntryAgeSeconds, maxEntrySizeBytes );
    }

    /** @return the name of the cache in which to store the item.  May contain variables that need interpolation. */
    public String getCacheId() {
        return cacheId;
    }

    /**
     * Configure the name of the cache in which the item is to be stored.
     * This would normally be a constant String, but it may contain interpolated
     * context variables if the name of the cache needs to be computed at runtime.
     * <p/>
     * If the named cache does not already exist on the runtime Gateway node, it will be created
     * with the maximum age, entries, and sizes specified by this assertion.
     *
     * @param cacheId the name of the cache in which the item is to be stored.  If null, the assertion will
     *                always fail.
     */
    public void setCacheId(final String cacheId) {
        this.cacheId = cacheId;
    }

    public String getMaxEntries() {
        return maxEntries;
    }

    /**
     * This method exists for backwards compatibility with serialized CacheStorageAssertions.
     *
     * This method simply calls {@link #setMaxEntries(String)} by converting the given int to a String.
     *
     * @deprecated Use {@link #setMaxEntries(String)} instead.
     * @see #setMaxEntries(String)
     */
    @Deprecated
    public void setMaxEntries(final int maxEntries) {
        setMaxEntries(Integer.toString(maxEntries));
    }

    /**
     * Configure the maximum number of entries allowed in the cache (if a new cache is created).
     * If the cache named by cacheId already exists, its maxEntries will already have been configured
     * when it was created and this value will be ignored.
     *
     * The parameter accepts two different Strings:
     * <ol>
     *     <li>A long between 0 and kMAX_ENTRIES (inclusive).</li>
     *     <li>A context variable (for example "${xyz}").
     *     It's contents are not interpolated yet but are expected to be an integer between 0 and kMAX_ENTRIES (inclusive).</li>
     * </ol>
     *
     * @param maxEntries maximum number of entries allowed in the cache (if a new cache is created), or zero for unlimited.
     */
    public void setMaxEntries(final String maxEntries) {
        this.maxEntries = maxEntries;
    }

    public String getMaxEntryAgeSeconds() {
        return maxEntryAgeSeconds;
    }

    /**
     * This method exists for backwards compatibility with serialized CacheStorageAssertions.
     *
     * This method simply calls {@link #setMaxEntryAgeSeconds} by converting the given long from
     * milliseconds to seconds and storing the value as a String.
     *
     * @deprecated Use {@link #setMaxEntryAgeSeconds} instead.
     * @see #setMaxEntryAgeSeconds
     */
    @Deprecated
    public void setMaxEntryAgeMillis(final long maxEntryAgeMillis) {
        setMaxEntryAgeSeconds(Long.toString(maxEntryAgeMillis / 1000L));
    }

    /**
     * Configure the maximum time an entry will be kept in the cache (if a new cache is created).
     * If the cache named by cacheId already exists, its max entry age will already have been configured
     * when it was created and this value will be ignored.
     *
     * @param maxEntryAgeSeconds maximum age of a cached entry in milliseconds. May be a variable reference
     */
    public void setMaxEntryAgeSeconds(final String maxEntryAgeSeconds) {
        this.maxEntryAgeSeconds = maxEntryAgeSeconds;
    }

    public String getMaxEntrySizeBytes() {
        return maxEntrySizeBytes;
    }

    /**
     * This method exists for backwards compatibility with serialized CacheStorageAssertions.
     *
     * This method simply calls {@link #setMaxEntrySizeBytes(String)} by converting the given long to a String.
     *
     * @deprecated Use {@link #setMaxEntrySizeBytes(String)} instead.
     * @see #setMaxEntrySizeBytes(String)
     */
    @Deprecated
    public void setMaxEntrySizeBytes(final long maxEntrySizeBytes) {
        setMaxEntrySizeBytes(Long.toString(maxEntrySizeBytes));
    }

    /**
     * Configure the maximum size of a single cached entry (if a new cache is created).
     * If the cache named by cacheId already exists, its maxEntrySizeBytes will already have been configured
     * when it was created and this value will be ignored.
     *
     * @param maxEntrySizeBytes maximum size of a single cache entry in bytes. A variable may be referenced.
     */
    public void setMaxEntrySizeBytes(final String maxEntrySizeBytes) {
        this.maxEntrySizeBytes = maxEntrySizeBytes;
    }

    public String getCacheEntryKey() {
        return cacheEntryKey;
    }

    /**
     * Configure key that will be associated with the item in the cache.  This is a context-variable-interpolated
     * String.
     * <p/>
     * Example: Suppose you are memoizing a service that provides information about a product identified
     * by a stock keeping unit, already extracted by XPath and saved in a context variable named "sku".
     * You could set the cache entry key to something like "getItemInfo:sku=${sku}".
     *
     * @param cacheEntryKey the key that should be used to associate the item in the cache.
     */
    public void setCacheEntryKey(final String cacheEntryKey) {
        this.cacheEntryKey = cacheEntryKey;
    }

    /**
     * Configures the assertion for storing or ignoring SOAP faults.
     *
     * @return true if SOAP faults are stored in this cache, false otherwise.
     */
    public boolean isStoreSoapFaults() {
        return storeSoapFaults;
    }

    public void setStoreSoapFaults(final boolean storeSoapFaults) {
        this.storeSoapFaults = storeSoapFaults;
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = CacheLookupAssertion.class.getName() + ".metadataInitialized";

    @Override
    public AssertionMetadata meta() {
        final DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.SHORT_NAME, "Store to Cache");
        meta.put(AssertionMetadata.DESCRIPTION, "Store value in cache");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Cache Storage Properties");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "misc" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/external/assertions/cache/console/resources/store16.gif");
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/external/assertions/cache/console/resources/store16.gif");
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new AssertionNodeNameFactory<CacheStorageAssertion>() {
            @Override
            public String getAssertionName(final CacheStorageAssertion assertion, final boolean decorate) {
                final String displayName = meta.getString(AssertionMetadata.SHORT_NAME);
                return decorate ? assertion.getTargetName() +": " + displayName + " [" + assertion.getCacheId() + "]" : displayName;
            }
        });
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "com.l7tech.external.assertions.cache.CacheAssertionAdvice");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

}

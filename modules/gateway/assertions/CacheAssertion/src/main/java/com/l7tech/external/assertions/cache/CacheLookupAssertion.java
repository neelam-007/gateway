package com.l7tech.external.assertions.cache;

import com.l7tech.policy.assertion.*;

/**
 * 
 */
public class CacheLookupAssertion extends MessageTargetableAssertion implements UsesVariables, SetsVariables {

    public final static long MIN_SECONDS_FOR_MAX_ENTRY_AGE = 0;
    public final static long MAX_SECONDS_FOR_MAX_ENTRY_AGE = Long.MAX_VALUE / 1000L;

    private String cacheId = "defaultCache";
    private String cacheEntryKey = "${request.url}";
    private String maxEntryAgeSeconds = String.valueOf(300); // 300s
    private String contentTypeOverride = null;

    public CacheLookupAssertion() {
        setTarget(TargetMessageType.RESPONSE);
        setTargetModifiedByGateway(true);
    }

    @Override
    protected VariablesUsed doGetVariablesUsed() {
        return super.doGetVariablesUsed().withExpressions( cacheId, cacheEntryKey, maxEntryAgeSeconds);
    }

    /** @return the name of the cache in which the item is to be looked up.  May contain variables that need interpolation. */
    public String getCacheId() {
        return cacheId;
    }

    /**
     * Configure the name of the cache in which the item is to be looked up.
     * This would normally be a constant String, but it may contain interpolated
     * context variables if the name of the cache needs to be computed at runtime.
     * <p/>
     * If the named cache does not already exist, the cache lookup assertion will fail.
     *
     * @param cacheId the name of the cache in which the item is to be looked up.  If null, the assertion will
     *                always fail.
     */
    public void setCacheId(final String cacheId) {
        this.cacheId = cacheId;
    }

    /** @return the key that should be used to look up a matching item from the cache.  May contain variables that need interpolation. */
    public String getCacheEntryKey() {
        return cacheEntryKey;
    }

    /**
     * Configure key that will be looked up in the cache.  This is a context-variable-interpolated
     * String.
     * <p/>
     * Example: Suppose you are memoizing a service that provides information about a product identified
     * by a stock keeping unit, already extracted by XPath and saved in a context variable named "sku".
     * You could set the cache entry key to something like "getItemInfo:sku=${sku}".
     *
     * @param cacheEntryKey the key that should be used to look up a matching item from the cache.
     *                      If null, the assertion will always fail.
     */
    public void setCacheEntryKey(final String cacheEntryKey) {
        this.cacheEntryKey = cacheEntryKey;
    }

    /**
     * This method exists for backwards compatibility with serialized CacheLookupAssertions.
     *
     * This method simply calls {@link #setMaxEntryAgeSeconds(String)} by converting the given long from
     * milliseconds to seconds and storing the value as a String.
     *
     * @deprecated Use {@link #setMaxEntryAgeSeconds(String)} instead.
     * @see #setMaxEntryAgeSeconds(String)
     */
    @Deprecated
    public void setMaxEntryAgeMillis(final long maxEntryAgeMillis) {
        setMaxEntryAgeSeconds(Long.toString(maxEntryAgeMillis / 1000));
    }

    /**
     * @return Max entry age in seconds. May reference a variable.
     * @see com.l7tech.external.assertions.cache.server.ServerCacheLookupAssertion
     */
    public String getMaxEntryAgeSeconds() {
        return maxEntryAgeSeconds;
    }

    /**
     * Set the maximum age of the cached entry that should be accepted.
     *
     * If a matching entry is found, but is older than this age, it will be left in the cache
     * for other cache users with less restrictive entry ages.
     *
     * This setting controls removal from the cache via this assertion; the cache itself has its own setting,
     * the {@link #cacheId}'s maximum entry age, that should not be confused with this setting.
     *
     * @param maxEntryAgeSeconds Max age value. May be a variable reference.
     */
    public void setMaxEntryAgeSeconds(final String maxEntryAgeSeconds) {
         this.maxEntryAgeSeconds = maxEntryAgeSeconds;
    }

    /**
     * Gets the value that will be used to override the content-type of the retrieved response.
     */
    public String getContentTypeOverride() {
        return contentTypeOverride;
    }

    /**
     * Sets the value that will be used to override the content-type of the retrieved response.
     *  
     * @param contentTypeOverride the content-type override value
     */
    public void setContentTypeOverride(final String contentTypeOverride) {
        this.contentTypeOverride = contentTypeOverride;
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

        meta.put(AssertionMetadata.SHORT_NAME, "Look Up in Cache");
        meta.put(AssertionMetadata.DESCRIPTION, "Look Up value in cache");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Cache Lookup Properties");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "misc" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/external/assertions/cache/console/resources/load16.gif");
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/external/assertions/cache/console/resources/load16.gif");
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");
        meta.put(AssertionMetadata.IS_ROUTING_ASSERTION, Boolean.TRUE);
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new AssertionNodeNameFactory<CacheLookupAssertion>() {
            @Override
            public String getAssertionName(final CacheLookupAssertion assertion, final boolean decorate) {
                final String displayName = meta.getString(AssertionMetadata.SHORT_NAME);
                return decorate ? assertion.getTargetName() +": " + displayName + " [" + assertion.getCacheId() + "]" : displayName;
            }
        });
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "com.l7tech.external.assertions.cache.CacheAssertionAdvice");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
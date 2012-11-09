package com.l7tech.external.assertions.cache;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.Syntax;

/**
 * 
 */
public class CacheLookupAssertion extends MessageTargetableAssertion implements UsesVariables, SetsVariables {

    public final static long MIN_MILLIS_FOR_MAX_ENTRY_AGE = 0;
    public final static long MIN_SECONDS_FOR_MAX_ENTRY_AGE = 0;
    public final static long MAX_MILLIS_FOR_MAX_ENTRY_AGE = Long.MAX_VALUE;
    public final static long MAX_SECONDS_FOR_MAX_ENTRY_AGE = MAX_MILLIS_FOR_MAX_ENTRY_AGE / 1000L;

    private String cacheId = "defaultCache";
    private String cacheEntryKey = "${request.url}";
    private String maxEntryAgeMillis = String.valueOf(1000L * 300); // 300s
    private String contentTypeOverride = null;

    public CacheLookupAssertion() {
        setTarget(TargetMessageType.RESPONSE);
        setTargetModifiedByGateway(true);
    }

    @Override
    protected VariablesUsed doGetVariablesUsed() {
        return super.doGetVariablesUsed().withExpressions( cacheId, cacheEntryKey, maxEntryAgeMillis );
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
     * Two different Strings can be returned:
     * <ol>
     *     <li>A long between the {@link CacheLookupAssertion#MIN_MILLIS_FOR_MAX_ENTRY_AGE} and {@link CacheLookupAssertion#MAX_MILLIS_FOR_MAX_ENTRY_AGE} values (inclusive) with the units being milliseconds.</li>
     *     <li>A context variable (for example "${xyz}").
     *     It's contents are not interpolated yet but are expected to be a long between {@link CacheLookupAssertion#MIN_SECONDS_FOR_MAX_ENTRY_AGE} and {@link CacheLookupAssertion#MAX_SECONDS_FOR_MAX_ENTRY_AGE} values (inclusive) with the units being seconds.</li>
     * </ol>
     *
     * @return Either a long or a context variable as described above.
     * @see com.l7tech.external.assertions.cache.server.ServerCacheLookupAssertion
     */
    public String getMaxEntryAgeMillis() {
        return maxEntryAgeMillis;
    }

    /**
     * This method exists for backwards compatibility with serialized CacheLookupAssertions.
     *
     * This method simply calls {@link #setMaxEntryAgeMillis(String)} by converting the given long to a String.
     *
     * @deprecated Use {@link #setMaxEntryAgeMillis(String)} instead.
     * @see #setMaxEntryAgeMillis(String)
     */
    @Deprecated
    public void setMaxEntryAgeMillis(final long maxEntryAgeMillis) {
        setMaxEntryAgeMillis(Long.toString(maxEntryAgeMillis));
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
     * The parameter accepts two different Strings:
     * <ol>
     *     <li>A long between the {@link CacheLookupAssertion#MIN_MILLIS_FOR_MAX_ENTRY_AGE} and {@link CacheLookupAssertion#MAX_MILLIS_FOR_MAX_ENTRY_AGE} values (inclusive) with the units being milliseconds.</li>
     *     <li>A context variable (for example "${xyz}").
     *     It's contents are not interpolated yet but are expected to be a long between {@link CacheLookupAssertion#MIN_SECONDS_FOR_MAX_ENTRY_AGE} and {@link CacheLookupAssertion#MAX_SECONDS_FOR_MAX_ENTRY_AGE} values (inclusive) with the units being seconds.</li>
     * </ol>
     *
     * @param maxEntryAgeMillis Either a long or a context variable as described above. If zero, this assertion will never return success.
     */
    public void setMaxEntryAgeMillis(final String maxEntryAgeMillis) {
         this.maxEntryAgeMillis = maxEntryAgeMillis;
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

    /**
     * Tests is the given String is a long.
     *
     * @param value The value to test.
     * @return True if the given String is a long, false otherwise.
     */
    public static boolean isLong(final String value) {
        try {
            Long.parseLong(value);
            return true;
        } catch (final NumberFormatException e) {
            return false;
        }
    }

    /**
     * Tests if the given String is a long within the given range (inclusive).
     *
     * @param value The value to test.
     * @param min Inclusive.
     * @param max Inclusive.
     * @return True if the given String is a long within the given range (inclusive), false otherwise.
     */
    public static boolean isLongWithinRange(final String value, final long min, final long max) {
        if (isLong(value)) {
            final long l = Long.parseLong(value);
            if (l >= min && l <= max) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tests if the given String is a single context variable (for example "${xyz}") or is a long within the given range (inclusive).
     *
     * @param value The value to test.
     * @param min Inclusive.
     * @param max Inclusive.
     * @return True if the given String is a single context variable or a long within the given range (inclusive), false otherwise.
     */
    public static boolean isSingleVariableOrLongWithinRange(final String value, final long min, final long max) {
        if (Syntax.isOnlyASingleVariableReferenced(value) || isLongWithinRange(value, min, max)) {
            return true;
        } else {
            return false;
        }
    }

}
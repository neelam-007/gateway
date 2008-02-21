package com.l7tech.external.assertions.cache;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * 
 */
public class CacheLookupAssertion extends Assertion implements UsesVariables, SetsVariables {
    protected static final Logger logger = Logger.getLogger(CacheLookupAssertion.class.getName());

    private String cacheId = "defaultCache";
    private String cacheEntryKey = "${request.url}";
    private long maxEntryAgeMillis = Long.MAX_VALUE;
    private String targetVariableName = null;
    private boolean useRequest = false;


    public String[] getVariablesUsed() {
        List<String> ret = new ArrayList<String>();
        if (cacheId != null) ret.addAll(Arrays.asList(Syntax.getReferencedNames(cacheId)));
        if (cacheEntryKey != null) ret.addAll(Arrays.asList(Syntax.getReferencedNames(cacheEntryKey)));
        return ret.toArray(new String[ret.size()]);
    }

    public VariableMetadata[] getVariablesSet() {
        return targetVariableName != null ? new VariableMetadata[] { new VariableMetadata(targetVariableName) } : new VariableMetadata[0];
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
    public void setCacheId(String cacheId) {
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
    public void setCacheEntryKey(String cacheEntryKey) {
        this.cacheEntryKey = cacheEntryKey;
    }

    /** @return maximum age of a cached entry in milliseconds. */
    public long getMaxEntryAgeMillis() {
        return maxEntryAgeMillis;
    }

    /**
     * Set the maximum age of the cached entry that should be accepted.
     * <p/>
     * If a maching entry is found, but is older than this age, it will be left in the cache
     * for other cache users with less restrictive entry ages.
     * <p/>
     * This setting controls removal from the cache via this assertion; the cache itself has its own setting,
     * the {@link #cacheId}'s maximum entry age, that should not be confused with this setting.
     *
     * @param maxEntryAgeMillis maximum age of a cached entry in milliseconds.  If zero, this assertion
     *                          will never return success.
     */
    public void setMaxEntryAgeMillis(long maxEntryAgeMillis) {
        this.maxEntryAgeMillis = maxEntryAgeMillis;
    }

    /** @return true if should load into request; otherwise should load into resposne */
    public boolean isUseRequest() {
        return useRequest;
    }

    /**
     * Control whether to load the cached value into the request or the response.
     * Ignored unless {@link #targetVariableName} is null.
     * @param useRequest if true, will load into request; otherwise will load into response
     */
    public void setUseRequest(boolean useRequest) {
        this.useRequest = useRequest;
    }

    /** @return name of target variable, or null to load into request/response */
    public String getTargetVariableName() {
        return targetVariableName;
    }

    /**
     * Specify a variable into which the cached value should be copied, or null to copy to either the Request or Response.
     * @param targetVariableName name of target variable, or null to load into request/response instead.
     * @see #setUseRequest(boolean) to control whether the request or response is used when this is null
     */
    public void setTargetVariableName(String targetVariableName) {
        this.targetVariableName = targetVariableName;
    }


    //
    // Metadata
    //
    private static final String META_INITIALIZED = CacheLookupAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.SHORT_NAME, "Cache Lookup");
        meta.put(AssertionMetadata.LONG_NAME, "Look up value in cache");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "misc" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/external/assertions/cache/console/resources/load16.png");
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/external/assertions/cache/console/resources/load16.png");
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}

package com.l7tech.external.assertions.cache;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 
 */
public class CacheLookupAssertion extends MessageTargetableAssertion implements UsesVariables, SetsVariables {

    private String cacheId = "defaultCache";
    private String cacheEntryKey = "${request.url}";
    private long maxEntryAgeMillis = 30000L; // 30s
    private String contentTypeOverride = null;

    public CacheLookupAssertion() {
        setTarget(TargetMessageType.RESPONSE);
    }

    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    @Override
    public String[] getVariablesUsed() {
        List<String> ret = new ArrayList<String>();
        if (cacheId != null) ret.addAll(Arrays.asList(Syntax.getReferencedNames(cacheId)));
        if (cacheEntryKey != null) ret.addAll(Arrays.asList(Syntax.getReferencedNames(cacheEntryKey)));
        return ret.toArray(new String[ret.size()]);
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return TargetMessageType.OTHER == getTarget() ?
            new VariableMetadata[] { new VariableMetadata(getOtherTargetMessageVariable(), false, false, null, false, DataType.MESSAGE) } : 
            new VariableMetadata[0];
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
    public void setContentTypeOverride(String contentTypeOverride) {
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

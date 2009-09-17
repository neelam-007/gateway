package com.l7tech.external.assertions.cache;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * 
 */
public class CacheStorageAssertion extends Assertion implements UsesVariables {
    protected static final Logger logger = Logger.getLogger(CacheLookupAssertion.class.getName());

    private String sourceVariableName = null;
    private boolean useRequest = false;
    private String cacheId = "defaultCache";
    private int maxEntries;
    private long maxEntryAgeMillis;
    private long maxEntrySizeBytes;
    private String cacheEntryKey = "${request.url}";


    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    @Override
    public String[] getVariablesUsed() {
        List<String> ret = new ArrayList<String>();
        if (cacheId != null) ret.addAll(Arrays.asList(Syntax.getReferencedNames(cacheId)));
        if (cacheEntryKey != null) ret.addAll(Arrays.asList(Syntax.getReferencedNames(cacheEntryKey)));
        if (sourceVariableName != null) ret.add(sourceVariableName);
        return ret.toArray(new String[ret.size()]);
    }


    /** @return the name of the variable whose contents are to be stored in the cache, or null if storing the request or response */
    public String getSourceVariableName() {
        return sourceVariableName;
    }

    /**
     * @param sourceVariableName the name of the variable whose contents are to be stored in the cache, or null if storing the request or response.
     *                           If this is null, use {@link #setUseRequest(boolean)} to control whether the Request or Response is stored.
     */
    public void setSourceVariableName(String sourceVariableName) {
        this.sourceVariableName = sourceVariableName;
    }

    /** @return true if the request is to be stored; false if the response.  Ignored unless sourceVariableName is null. */
    public boolean isUseRequest() {
        return useRequest;
    }

    /** @param useRequest true if the request is to be stored; false if the response.  Ignored unless sourceVariableName is null. */
    public void setUseRequest(boolean useRequest) {
        this.useRequest = useRequest;
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
    public void setCacheId(String cacheId) {
        this.cacheId = cacheId;
    }

    public int getMaxEntries() {
        return maxEntries;
    }

    /**
     * Configure the maximum number of entries allowed in the cache (if a new cache is created).
     * If the cache named by cacheId already exists, its maxEntries will already have been configured
     * when it was created and this value will be ignored.
     *
     * @param maxEntries maximum number of entries allowed in the cache (if a new cache is created), or zero for unlimited.
     */
    public void setMaxEntries(int maxEntries) {
        this.maxEntries = maxEntries;
    }

    public long getMaxEntryAgeMillis() {
        return maxEntryAgeMillis;
    }

    /**
     * Configure the maximum time an entry will be kept in the cache (if a new cache is created).
     * If the cache named by cacheId already exists, its maxEntryAgeMillis will already have been configured
     * when it was created and this value will be ignored.
     *
     * @param maxEntryAgeMillis maximum age of a cached entry in milliseconds.
     */
    public void setMaxEntryAgeMillis(long maxEntryAgeMillis) {
        this.maxEntryAgeMillis = maxEntryAgeMillis;
    }

    public long getMaxEntrySizeBytes() {
        return maxEntrySizeBytes;
    }

    /**
     * Configure the maximum size of a single cached entry (if a new cache is created).
     * If the cache named by cacheId already exists, its maxEntrySizeBytes will already have been configured
     * when it was created and this value will be ignored.
     *
     * @param maxEntrySizeBytes maximum size of a single cache entry in bytes
     */
    public void setMaxEntrySizeBytes(long maxEntrySizeBytes) {
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
    public void setCacheEntryKey(String cacheEntryKey) {
        this.cacheEntryKey = cacheEntryKey;
    }


    //
    // Metadata
    //
    private static final String META_INITIALIZED = CacheLookupAssertion.class.getName() + ".metadataInitialized";

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.SHORT_NAME, "Cache Storage");
        meta.put(AssertionMetadata.DESCRIPTION, "Store value in cache");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "misc" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/external/assertions/cache/console/resources/store16.gif");
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/external/assertions/cache/console/resources/store16.gif");
//        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
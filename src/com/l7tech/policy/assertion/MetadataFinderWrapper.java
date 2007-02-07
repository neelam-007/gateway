package com.l7tech.policy.assertion;

/**
 * Wraps an existing MetadataFinder in a new one that can augment, change, or ignore its output.
 */
public abstract class MetadataFinderWrapper implements MetadataFinder {
    private final MetadataFinder delegate;

    /**
     * Create a wrapper around the specified MetadataFinder instance.
     *
     * @param delegate the MetadataFinder to wrap.
     *                 If this is null, the wrapper will always be passed null for the original value
     *                 regardless of what key is queried for.
     */
    public MetadataFinderWrapper(MetadataFinder delegate) {
        this.delegate = delegate;
    }

    /**
     * Create a wrapper around the DefaultAssertionMetadata getter for the specified key.
     * <P/>
     * You can use this to wrap an existing default metadata finder to add additional capabilities.
     * Example: the following code installs a wrapper that ensures that the default metadata finder for
     * the assertion meta property "fooName" can never return null:
     * <pre>
     *   DefaultAssertionMetadata.putDefaultGetter("fooName", new MetadataFinderWrapper("fooName") {
     *     protected Object doGet(AssertionMetadata meta, String key, Object original) {
     *       return original == null ? "not set" : original;
     *     }
     *  });
     * </pre>
     *
     * @param defaultKey a key to look up with DefaultAssertionMetadata.getDefaultGetter
     */
    public MetadataFinderWrapper(String defaultKey) {
        this(DefaultAssertionMetadata.getDefaultGetter(defaultKey));
    }

    public final Object get(final AssertionMetadata meta, String key) {
        return doGet(meta, key, delegate == null ? null : delegate.get(new ReadOnlyAssertionMetadata(meta), key));
    }

    /**
     * Get the wrapped metadata parameter.
     *
     * @param meta the AssertionMetadata instance whose property to find or generate.  Must not be null.
     * @param key the name of the property that is being fetched.  Must not be null.
     * @param original the value returned for this property by the wrapped MetadataFinder.  May be null.
     * @return the value after being augmented/replaced/changed/ignored by the wrapper code.
     */
    protected abstract Object doGet(AssertionMetadata meta, String key, Object original);

    /**
     * A wrapper to protect AssertionMetadata from being downcast back to DefaultAssertionMetadata and modified.
     */
    static class ReadOnlyAssertionMetadata implements AssertionMetadata {
        private final AssertionMetadata meta;

        public ReadOnlyAssertionMetadata(AssertionMetadata meta) {
            this.meta = meta;
        }

        public Class getAssertionClass() {
            return meta.getAssertionClass();
        }

        public Object get(String key) {
            return meta.get(key);
        }
    }
}

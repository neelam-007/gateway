package com.l7tech.policy.assertion;

/**
 * Interface implemented by lazy getters of assertion properties.
 */
public interface MetadataFinder {
    /**
     * Get the specified property for the specified AssertionMetadata instance.
     * <p/>
     * Implementors should keep in mind that multiple threads may be calling
     * this method at the same time, possibly passing the same AssertionMetadata instance.
     *
     * @param meta the AssertionMetadata instance whose property to find or generate.  Must not be null.
     * @param key the name of the property that is being fetched.  Must not be null.
     * @return the property value, if able to find or generate one, or null.
     */
    Object get(AssertionMetadata meta, String key);
}

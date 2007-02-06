package com.l7tech.policy.wsp;

/**
 * Finds type mappings given an element's external name.
 */
public interface TypeMappingFinder {
    /**
     * Find a TypeMapping for the specified external name.
     *
     * @param externalName the external name to look up.  Must not be null or empty.
     * @return a TypeMapping that can parse an element with this external name, or null if it was not recognized.
     */
    TypeMapping getTypeMapping(String externalName);
}

package com.l7tech.policy.wsp;

import java.lang.reflect.Type;

/**
 * Finds type mappings given an element's external name.
 */
public interface TypeMappingFinder {
    /**
     * Find a TypeMapping for the specified external name encountered while thawing a property.
     *
     * @param externalName the external name to look up.  Must not be null or empty.
     * @return a TypeMapping that can parse an element with this external name, or null if it was not recognized.
     */
    TypeMapping getTypeMapping(String externalName);

    /**
     * Find a TypeMapping for the specified type encountered while freezing a property.
     *
     * Implementations should check if unrecognizedType is a ParameterizedType and if so they should check each
     * of their TypeMapping s to see if they have implemented ParameterizedMapaping. If an individual TypeMapping has
     * then the find logic should also take into account the Class [] returned from
     * ParameterizedMapaping.getMappedObjectsParameterizedClasses() when determining if a TypeMapping matches a Type
     *
     * @param unrecognizedType  the type to look up.  Required.
     * @param version the target software version (may be null)
     * @return a TypeMapping that can serialize instances of this class, or null if it was not recognized.
     */
    TypeMapping getTypeMapping(Type unrecognizedType, String version);
}

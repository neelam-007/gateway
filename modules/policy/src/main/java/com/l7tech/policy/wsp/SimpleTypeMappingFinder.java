package com.l7tech.policy.wsp;

import java.util.Collection;

/**
 * A TypeMappingFinder that scans a collection of TypeMappings for a match.
 */
public class SimpleTypeMappingFinder implements TypeMappingFinder {
    private final Collection<TypeMapping> mappings;

    /**
     * Create a TypeMappingFinder that will find type mappings by scanning the provided list.
     *
     * @param mappings  mappings to find.
     */
    public SimpleTypeMappingFinder(Collection<TypeMapping> mappings) {
        this.mappings = mappings;
    }

    @Override
    public TypeMapping getTypeMapping(String externalName) {
        for (TypeMapping tm : mappings) {
            if (tm.getExternalName().equals(externalName))
                return tm;
        }
        return null;
    }

    @Override
    public TypeMapping getTypeMapping(Class unrecognizedType, String version) {
        for (TypeMapping tm : mappings) {
            if (tm.getMappedClass().equals(unrecognizedType))
                return tm;
        }
        return null;
    }
}

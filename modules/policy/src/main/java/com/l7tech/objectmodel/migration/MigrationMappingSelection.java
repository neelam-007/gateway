package com.l7tech.objectmodel.migration;

/**
 * Migration mapping selection type.
 * Can be applied to either entity property names (identity mapping), or to property values (value mapping).
 *
 */
public enum MigrationMappingSelection {

    /**
     * Mapping MUST NOT be performed for the specified property's name or value.
     * The value form the source cluster must be exported and used on the target cluster.
     */
    NONE,

    /**
     * The referred property may optionally be mapped to a different one on the target cluster.
     */
    OPTIONAL,

    /**
     * The referred property must be mapped on the target cluster.
     *
     * At export, the property's value is not included in the bundle.
     */
    REQUIRED
}

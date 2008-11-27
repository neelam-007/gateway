package com.l7tech.objectmodel.migration;

/**
 * Migration mapping selection type.
 * Can be applied to either entity property names (identity mapping), or to property values (value mapping).
 *
 */
public enum MigrationMappingSelection {

    /**
     * No mapping is required for the specified property.
     *
     * For export, it signifies that the value form the source cluster must be exported and used on the target cluster.
     *
     * At import time, it means that no (further) mapping operation is required for the referred property.
     * If the property was marked as mappable at export, mapping must have been resolved.
     * The value for the referred property must exist either in the migration bundle, or on the target cluster.
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

package com.l7tech.cluster;

import java.io.Serializable;

/**
 * Metadata for a cluster property.
 *
 * @author Steve Jones
 */
public class ClusterPropertyDescriptor implements Serializable {

    //- PUBLIC

    public ClusterPropertyDescriptor( final String name,
                                      final String description,
                                      final String defaultValue,
                                      final boolean visible ) {
        if ( name==null ) throw new IllegalArgumentException("name must not be null");
        this.name = name;
        this.description = description==null ? "" : description;
        this.defaultValue = defaultValue==null ? "" : defaultValue;
        this.visible = visible;
    }

    /**
     * Get the name (key) for the cluster property.
     *
     * @return The name (never null)
     */
    public String getName() {
        return name;
    }

    /**
     * Get the description for the cluster property.
     *
     * @return The description (never null)
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the default value for the cluster property.
     *
     * @return The value as a string (never null)
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Get the visibility for this cluster property.
     *
     * <p>Note that this does not mean that the property is unaccessible to
     * a user. This flag only reduces the visiblity to the casual user, the
     * value can still be accessed and modified/deleted.</p>
     *
     * @return True if visible.
     */
    public boolean isVisible() {
        return visible;
    }

    //- PRIVATE

    private final String name;
    private final String description;
    private final String defaultValue;
    private final boolean visible;
}

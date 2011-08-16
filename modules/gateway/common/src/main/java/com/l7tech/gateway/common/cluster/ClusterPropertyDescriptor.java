package com.l7tech.gateway.common.cluster;

import com.l7tech.util.Option;
import com.l7tech.util.ValidationUtils.Validator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * Metadata for a cluster property.
 *
 * @author Steve Jones
 */
public class ClusterPropertyDescriptor implements Serializable {

    //- PUBLIC

    public ClusterPropertyDescriptor( @NotNull  final String name,
                                      @Nullable final String description,
                                      @Nullable final String defaultValue,
                                                final boolean visible,
                                      @NotNull  Option<Validator<String>> validator ) {
        this.name = name;
        this.description = description==null ? "" : description;
        this.defaultValue = defaultValue==null ? "" : defaultValue;
        this.visible = visible;
        this.validator = validator;
    }

    /**
     * Get the name (key) for the cluster property.
     *
     * @return The name (never null)
     */
    @NotNull
    public String getName() {
        return name;
    }

    /**
     * Get the description for the cluster property.
     *
     * @return The description (never null)
     */
    @NotNull
    public String getDescription() {
        return description;
    }

    /**
     * Get the default value for the cluster property.
     *
     * @return The value as a string (never null)
     */
    @NotNull
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

    /**
     * Is the given text a valid value for the described cluster property.
     *
     * @param text The text to validate
     * @return True if valid
     */
    public boolean isValid( final String text ) {
        return !validator.isSome() || validator.some().isValid( text );
    }

    //- PRIVATE

    private final String name;
    private final String description;
    private final String defaultValue;
    private final boolean visible;
    private final Option<Validator<String>> validator;
}

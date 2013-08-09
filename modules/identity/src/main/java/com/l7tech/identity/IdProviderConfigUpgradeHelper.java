package com.l7tech.identity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A helper class, to be used only by upgrade tasks, that provides access to the raw properties map of an ID
 * provider config.
 */
public class IdProviderConfigUpgradeHelper {

    /**
     * Access a raw property from an identity provider config.
     * <p/>
     * This method should not be used in most cases.  It is only to support upgrade use cases.
     *
     * @param config the config to peek into.  Required.
     * @param propertyName the name of the property.  Required.
     * @param <T> the property value type
     * @return the property value (may be null)
     */
    @Nullable
    public static <T> T getProperty( @NotNull IdentityProviderConfig config, @NotNull String propertyName ) {
        return config.getProperty(propertyName);
    }

    /**
     * Set a raw property value in an identity provider config.
     * <p/>
     * This method should not be used in most cases.  It is only to support upgrade use cases.
     *
     * @param config the config to prod into.  Required.
     * @param propertyName the name of the property.  Required.
     * @param propertyValue the property value to install.  May be null.
     * @param <T> the property value type
     */
    public static <T> void setProperty( @NotNull IdentityProviderConfig config, @NotNull String propertyName, @Nullable T propertyValue) {
        config.setProperty(propertyName, propertyValue);
    }
}

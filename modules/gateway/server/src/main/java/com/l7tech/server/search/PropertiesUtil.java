package com.l7tech.server.search;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * This is a utility used to get an option from a properties map
 *
 * @author vkazakov
 */
public class PropertiesUtil {
    /**
     * Retrieve an option from the get properties map, verifying it is the correct type and casting to it.
     *
     * @param optionKey     The option to retrieve
     * @param type          The type of the option
     * @param defaultValue  The default value to return if the option is not specified
     * @param propertiesMap The properties map to get the option from
     * @param <C>           This is the Type of the value that will be returned
     * @param <T>           This is the class type of the value
     * @return The option value cast to the correct type. This will be the default value if no such option is set.
     * @throws IllegalArgumentException This is thrown if the option value is the wrong type.
     */
    @NotNull
    public static <C, T extends Class<C>> C getOption(@NotNull final String optionKey, @NotNull final T type, @NotNull final C defaultValue, @NotNull final Map<String, Object> propertiesMap) {
        final Object value = propertiesMap.get(optionKey);
        if (value != null && type.isAssignableFrom(value.getClass())) {
            return type.cast(value);
        } else if (value == null) {
            return defaultValue;
        }
        throw new IllegalArgumentException("Search option value for option '" + optionKey + "' was not a valid type. Expected: " + type + " Given: " + value.getClass());
    }

}

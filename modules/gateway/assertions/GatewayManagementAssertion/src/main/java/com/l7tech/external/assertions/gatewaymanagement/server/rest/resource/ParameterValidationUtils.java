package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.exceptions.InvalidArgumentException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.ws.rs.core.MultivaluedMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;


/**
 * These are rest resource parameter utilities
 */
public class ParameterValidationUtils {
    //These are the query parameters that a list request can always have
    public static final Collection<String> defaultListQueryParams = Arrays.asList("sort", "order");

    //TODO: refactor this and merge with below method
    public static void validateNoOtherQueryParamsNoDefaults(@NotNull final MultivaluedMap<String, String> queryParameters, @NotNull final List<String> validParams) {
        for (final String param : queryParameters.keySet()) {
            if (!validParams.contains(param)) {
                throw new InvalidArgumentException("Unknown filter parameter '" + param + "'. Expected one of: " + validParams.toString());
            }
        }
    }

    //TODO: refactor this and merge with above method
    public static void validateNoOtherQueryParams(@NotNull final MultivaluedMap<String, String> queryParameters, @NotNull final List<String> validParams) {
        for (final String param : queryParameters.keySet()) {
            if (!defaultListQueryParams.contains(param) && !validParams.contains(param)) {
                throw new InvalidArgumentException("Unknown filter parameter '" + param + "'. Expected one of: " + validParams.toString());
            }
        }
    }

    /**
     * Converts the order string to the ascending boolean. This returns null if the order is null. true if the order is
     * 'asc' false otherwise.
     *
     * @param order The order string. It is assumed that it will either be null, 'asc', or 'desc'
     * @return the ascending boolean. It is null if the given order is null, true if the order is 'asc' and false
     * otherwise
     */
    @Nullable
    public static Boolean convertSortOrder(@Nullable final String order) {
        return order == null ? null : "asc".equals(order);
    }
}

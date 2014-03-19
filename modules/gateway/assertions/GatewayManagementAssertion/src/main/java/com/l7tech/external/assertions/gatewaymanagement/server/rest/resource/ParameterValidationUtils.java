package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.exceptions.InvalidArgumentException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.ws.rs.core.MultivaluedMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;


/**
 * These are rest resource parameter utilities
 */
public class ParameterValidationUtils {
    // This is used to validate annotated beans
    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    //These are the query parameters that a list request can always have
    public static final Collection<String> defaultListQueryParams = Arrays.asList("offset", "count", "sort", "order");


    /**
     * This will validate the entity using annotations that if has declared on it fields and methods.
     *
     * @throws InvalidArgumentException This is thrown if the entity is invalid.
     */
    public static <O> void validate(@NotNull O obj) {
        //validate the entity
        final Set<ConstraintViolation<O>> violations = validator.validate(obj);
        if (!violations.isEmpty()) {
            //the entity is invalid. Create a nice exception message.
            final StringBuilder validationReport = new StringBuilder("Invalid Value: ");
            boolean first = true;
            for (final ConstraintViolation<O> violation : violations) {
                if (!first) validationReport.append('\n');
                first = false;
                validationReport.append(violation.getPropertyPath().toString());
                validationReport.append(" - ");
                validationReport.append(violation.getMessage());
            }
            throw new InvalidArgumentException(validationReport.toString());
        }
    }

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

    /**
     * This will validate the the offset and count are valid. The offset must be 0 or greater. The count must be 1 or
     * greater and 500 or less. The sum of the count and offset must be less than Integer.MAX_VALUE (otherwise hibernate
     * will throw an exception)
     *
     * @param offset The offset to validate
     * @param count  The count to validate
     */
    public static void validateOffsetCount(@NotNull final Integer offset, @NotNull final Integer count) {
        if (offset < 0) {
            throw new InvalidArgumentException("offset", "The offset should be greater than or equal to zero");
        }
        if (count < 1 || count > 500) {
            throw new InvalidArgumentException("count", "The count should be greater than or equal to one and less than or equal to 500");
        }
        if ((long) offset + count > Integer.MAX_VALUE) {
            throw new InvalidArgumentException("offset and count", "The sum of the count and offset must be less then or equal to " + Integer.MAX_VALUE);
        }
    }
}

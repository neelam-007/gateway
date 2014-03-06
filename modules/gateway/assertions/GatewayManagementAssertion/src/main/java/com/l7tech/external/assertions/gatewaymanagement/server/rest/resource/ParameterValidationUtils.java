package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.exceptions.InvalidArgumentException;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.*;


/**
 * These are rest resource parameter utilities
 */
public class ParameterValidationUtils {
    // This is used to validate annotated beans
    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    //These are the query parameters that a list request can always have
    private static final Collection<String> constantListQueryParams = Arrays.asList("offset", "count", "sort", "order");


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

    /**
     * Validates list request parameters.
     *
     * @param listRequestParameters The list request bean
     * @param sortKeysMap           The map of sort keys
     * @param filtersInfo           The filters info map
     */
    public static void validateListRequestParameters(@NotNull final ListingResource.ListRequestParameters listRequestParameters, @NotNull final Map<String, String> sortKeysMap, @NotNull final Map<String, Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>> filtersInfo) {
        //Use the validator to validate the object
        ParameterValidationUtils.validate(listRequestParameters);

        //check if the sort value is a valid sort value.
        if (listRequestParameters.getSortKey() != null) {
            final String sort = sortKeysMap.get(listRequestParameters.getSortKey());
            if (sort != null) {
                //sets the sort key to use.
                listRequestParameters.setSort(sort);
            } else {
                throw new InvalidArgumentException("sort", "Cannot sort by: '" + listRequestParameters.getSortKey() + "'. Must be one of: " + sortKeysMap.values());
            }
        }


        final Map<String, List<Object>> filtersMap = new HashMap<>();
        //validate the filters
        if (listRequestParameters.getQueryParameters() != null && !listRequestParameters.getQueryParameters().isEmpty()) {
            //iterate through all the query parameters
            for (final Map.Entry<String, List<String>> queryParam : listRequestParameters.getQueryParameters().entrySet()) {
                //Get the filter info for this query parameter
                final Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>> filterInfo = filtersInfo.get(queryParam.getKey());
                if (filterInfo != null) {
                    // if this is a valid filter key then build the filter value map
                    final List<Object> filterValues = new ArrayList<>();
                    for (final String paramValue : queryParam.getValue()) {
                        try {
                            //parse and add the filter to the filter values map
                            filterValues.add(filterInfo.getValue().call(paramValue));
                        } catch (IllegalArgumentException e) {
                            //This is thrown if there was an error parsing the filter value. In this case throw an error
                            throw new InvalidArgumentException(queryParam.getKey(), "Invalid filter value: '" + paramValue + "'.");
                        }
                    }
                    //add the parsed filter values to the filters map
                    filtersMap.put(filterInfo.getKey(), filterValues);
                } else if (!constantListQueryParams.contains(queryParam.getKey())) {
                    //if the query parameter is not a filter key or one of the other used query parameters then throw an error.
                    //If we do not do this a user may think that a filter is applied but it actually does nothing
                    throw new InvalidArgumentException("Cannot filter by: '" + queryParam.getKey() + "'. Must be one of: " + filtersInfo.keySet().toString());
                }
            }
        }
        //set the filters map on the list request object
        listRequestParameters.setFiltersMap(filtersMap);
    }
}

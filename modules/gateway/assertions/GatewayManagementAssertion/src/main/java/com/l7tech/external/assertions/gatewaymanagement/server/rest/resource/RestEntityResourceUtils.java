package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.entities.Reference;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.entities.References;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This contains utilities used by resources.
 *
 * @author Victor Kazakov
 */
public class RestEntityResourceUtils {

    /**
     * Creates a new response for a list of ids.
     *
     * @param path        The path of the resources
     * @param resourceIds The list of resource id's to create the references to
     * @return A Response containing a list of references to the given resource id's
     */
    public static Response createReferenceListResponse(URI path, final List<String> resourceIds) {
        //Create the Reference list.
        List<Reference> resourceList = new ArrayList<>(resourceIds.size());
        for (String id : resourceIds) {
            UriBuilder ub = UriBuilder.fromUri(path).path(id);
            final URI uri = ub.build();
            resourceList.add(new Reference(uri.toString(), uri.toString()));
        }

        return Response.ok(new References(resourceList)).build();
    }

    /**
     * Converts the filters to a map of filters that can be used by the entity managers.
     *
     * @param filtersGiven The filters string.
     * @return The map of filters.
     */
    public static Map<String, List<Object>> createFiltersMap(Map<String, Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>> filtersInfo, MultivaluedMap<String, String> filtersGiven) {
        Map<String, List<Object>> matchers = new HashMap<>();
        if (filtersGiven != null && !filtersGiven.isEmpty()) {
            for (String filterName : filtersGiven.keySet()) {
                Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>> filterInfo = filtersInfo.get(filterName);
                if (filterInfo != null) {
                    List<String> filterValues = filtersGiven.get(filterName);
                    List<Object> values = new ArrayList<>();
                    for (String filterValue : filterValues) {
                        values.add(filterInfo.getValue().call(filterValue));
                    }
                    matchers.put(filterInfo.getKey(), values);
                }
            }
        }
        return matchers;
    }

    /**
     * Converts the order type string to an a boolean. Ascending = true;
     *
     * @param order the order String
     * @return true for ascending
     */
    public static Boolean convertOrder(String order) {
        if ("asc".equals(order)) {
            return true;
        } else if ("desc".equals(order)) {
            return false;
        }
        if (order == null) {
            return null;
        }
        throw new IllegalArgumentException("Invalid sort order. Require either 'asc' or 'desc'. Given: " + order);
    }
}

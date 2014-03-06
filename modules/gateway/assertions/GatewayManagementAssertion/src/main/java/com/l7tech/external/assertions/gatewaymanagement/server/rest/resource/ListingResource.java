package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource;

import com.l7tech.gateway.api.ItemsList;
import org.glassfish.jersey.server.ParamException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Map;

/**
 * The list resource interface. All resources that allow listing should implement this in order to support consistent
 * rest calls.
 *
 * @author Victor Kazakov
 */
public interface ListingResource<R> {
    /**
     * This will return a list of entity references. It will return a maximum of {@code count} references, it can return
     * fewer references if there are fewer then {@code count} entities found. Setting an offset will start listing
     * entities from the given offset. A sort can be specified to allow the resulting list to be sorted in either
     * ascending or descending order. Other params given will be used as search values. Examples:
     * <p/>
     * /restman/services?name=MyService
     * <p/>
     * Returns services with name = "MyService"
     * <p/>
     * /restman/storedpasswords?type=password&name=DevPassword,ProdPassword
     * <p/>
     * Returns stored passwords of password type with name either "DevPassword" or "ProdPassword"
     * <p/>
     * If a parameter is not a valid search value it will be ignored.
     *
     * @param listRequestParameters The ListRequestParameters object contains the parameters that are needed by the list
     *                              operation.
     * @return A list of entities. If the list is empty then no entities were found.
     */
    @GET
    @Produces(MediaType.APPLICATION_XML)
    //This xml header allows the list to be explorable when viewed in a browser
    //@XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    ItemsList<R> listResources(@BeanParam final ListRequestParameters listRequestParameters);

    /**
     * This contains the parameters that are needed for the list operation. This object will be validated with a
     * validator. These objects should only ever be created by jersey.
     */
    @SuppressWarnings("UnusedDeclaration")
    public class ListRequestParameters {

        //this is used to retrieve the remaining filter parameters.
        @Context
        private UriInfo uriInfo;
        @Min(0)
        private int offset;
        @Min(1)
        @Max(500)
        private int count;
        private String sortKey;
        private String sort;
        private Boolean order;
        private Map<String, List<Object>> filtersMap;

        /**
         * This will be automatically called by jersey. It will set the offset.
         *
         * @param offset The offset. This will be validated to make sure it is a valid integer
         */
        @QueryParam("offset")
        @DefaultValue("0")
        private void setOffset(String offset) {
            try {
                this.offset = Integer.parseInt(offset);
            } catch (NumberFormatException e) {
                throw new ParamException.QueryParamException(new IllegalArgumentException("Must be a valid integer. Given: '" + offset + "'"), "offset", null);
            }
        }

        /**
         * This will be automatically called by jersey. It will set the count.
         *
         * @param count The count. This will be validated to make sure it is a valid integer
         */
        @QueryParam("count")
        @DefaultValue("100")
        private void setCount(String count) {
            try {
                this.count = Integer.parseInt(count);
            } catch (NumberFormatException e) {
                throw new ParamException.QueryParamException(new IllegalArgumentException("Must be a valid integer. Given: '" + count + "'"), "count", null);
            }
        }

        /**
         * This will be automatically called by jersey. It will set the sort.
         *
         * @param sortKey The sort.
         */
        @QueryParam("sort")
        private void setSortKey(String sortKey) {
            this.sortKey = sortKey;
        }

        /**
         * This will be automatically called by jersey. It will set the order.
         *
         * @param order The sort. The order must either be asc, desc or null.
         */
        @QueryParam("order")
        @DefaultValue("asc")
        private void setOrder(String order) {
            if (order == null) {
                this.order = null;
            } else if ("asc".equalsIgnoreCase(order)) {
                this.order = true;
            } else if ("desc".equalsIgnoreCase(order)) {
                this.order = false;
            } else {
                throw new ParamException.QueryParamException(new IllegalArgumentException("Must be either asc or desc. Given: '" + order + "'"), "order", null);
            }
        }

        /**
         * This sets the filters map. It should only be set be the {@link com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ParameterValidationUtils}
         *
         * @param filtersMap The filters map to use.
         */
        void setFiltersMap(@NotNull Map<String, List<Object>> filtersMap) {
            this.filtersMap = filtersMap;
        }

        /**
         * This sets the sort. It should only be set be the {@link com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ParameterValidationUtils}
         *
         * @param sort The sort to use.
         */
        void setSort(String sort) {
            this.sort = sort;
        }

        /**
         * Returns the sort key. This should only be called by {@link com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ParameterValidationUtils}
         *
         * @return The sort key in the rest api call
         */
        String getSortKey() {
            return sortKey;
        }

        /**
         * Returns the query parameters. This should only be called by {@link com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ParameterValidationUtils}
         *
         * @return The query parameters in the rest api call
         */
        MultivaluedMap<String, String> getQueryParameters(){
            return uriInfo.getQueryParameters();
        }

        /**
         * This is checked by validation tool. The sum of the offset and count cannot be greater then max int. If they
         * are it will cause hibernate errors.
         *
         * @return True if the sum of the offset and count are less then max int
         */
        @AssertTrue(message = "Sum of offset and count must be less than " + Integer.MAX_VALUE)
        private boolean isOffsetPlusCount() {
            return ((long) offset) + count <= Integer.MAX_VALUE;
        }

        /**
         * Returns the offset to start the listing from
         *
         * @return The offset ot start the listing from
         */
        public int getOffset() {
            return offset;
        }

        /**
         * Returns the number of items to list
         *
         * @return the maximum number of items to list
         */
        public int getCount() {
            return count;
        }

        /**
         * Returns the key to sort the list by
         *
         * @return the key to sort the list by.
         */
        @Nullable
        public String getSort() {
            return sort;
        }

        /**
         * The order to sort the list. true for ascending, false for descending. null implies ascending
         *
         * @return the order to sort the list. true for ascending, false for descending. null implies ascending
         */
        @Nullable
        public Boolean getOrder() {
            return order;
        }

        /**
         * The filters map to filter the returned list with.
         *
         * @return The values to filter the returned list with.
         */
        @NotNull
        public Map<String, List<Object>> getFiltersMap() {
            return filtersMap;
        }
    }
}

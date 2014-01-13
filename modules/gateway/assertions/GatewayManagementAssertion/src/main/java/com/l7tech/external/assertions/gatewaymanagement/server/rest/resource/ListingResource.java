package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource;

import com.l7tech.gateway.api.ItemsList;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

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
     * @param offset  The offset from the start of the list to start listing from
     * @param count   The total number of entities to return. The returned list can be shorter is there are not enough
     *                entities
     * @param sort    The property to sort the results by.
     * @param order   The order to sort by. Either asc or desc
     * @return A list of entities. If the list is empty then no entities were found.
     */
    @GET
    @Produces(MediaType.APPLICATION_XML)
    //This xml header allows the list to be explorable when viewed in a browser
    //@XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    ItemsList<R> listResources(@QueryParam("offset") @DefaultValue("0") @Min(0) final int offset, @QueryParam("count") @DefaultValue("100") @Min(1) @Max(500) final int count, @QueryParam("sort") final String sort, @QueryParam("order") @DefaultValue("asc") @Pattern(regexp = "asc|desc") final String order);
}

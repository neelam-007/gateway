package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.SolutionKitAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.*;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.SolutionKitTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemsList;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.SolutionKitMO;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.util.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.List;

/**
 * This resource is used to list Solution Kits installed on the Gateway.
 */
@Provider
@Path(SolutionKitResource.RestEntityResource_version_URI + SolutionKitResource.SolutionKits_URI)
@Singleton
@Since(RestManVersion.VERSION_1_0_2)
public class SolutionKitResource  extends RestEntityResource<SolutionKitMO, SolutionKitAPIResourceFactory, SolutionKitTransformer> {

    public static final String SolutionKits_URI = "solutionKits";

    @Override
    @SpringBean
    public void setFactory(final SolutionKitAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(final SolutionKitTransformer transformer) {
        super.transformer = transformer;
    }

    /**
     * Returns a SolutionKit with the given id.
     *
     * @param id     The id of the SolutionKit to retrieve.
     * @return The SolutionKit associated with the given id.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{id}")
    public Item<SolutionKitMO> get(@PathParam("id") final String id) throws ResourceFactory.ResourceNotFoundException {
        final SolutionKitMO resource = factory.getResource(id);
        return RestEntityResourceUtils.createGetResponseItem(resource, transformer, this);
    }

    /**
     * <p>Returns a list of SolutionKits. Can optionally sort the resulting list in ascending or descending order.
     * Other params given will be used as search values.</p>
     * <p class="italicize">Examples:</p>
     * <div class="code indent"><pre>/restman/1.0/solutionKits?name=MySolutionKit</pre></div>
     * <p>Returns SolutionKit with name "MySolutionKit".</p>
     * <p>If a parameter is not a valid search value a bad request error will be returned.</p>
     *
     * @param sort         The sort key to sort the list by; 'id'=GOID, 'name'=name, 'guid'=sk_guid, 'parent'=parent_goid.
     * @param order        Sort order for the list; 'asc'=ascending, 'desc'=descending; defaults to ascending if not specified.
     * @param names        Name filter.
     * @param guids        Guid filter.
     * @return A list of SolutionKits. If the list is empty then no SolutionKits were found.
     */
    @GET
    public ItemsList<SolutionKitMO> list(
            @QueryParam("sort") @ChoiceParam({"id", "name", "guid", "parent"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) final String order,
            @QueryParam("name") final List<String> names,
            @QueryParam("guid") final List<String> guids
    ) {
        final Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParamsIncludeDefaults(
                uriInfo.getQueryParameters(),
                Arrays.asList("name", "guid")
        );

        final CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (names != null && !names.isEmpty()) {
            //noinspection unchecked
            filters.put("name", (List) names);
        }
        if (guids != null && !guids.isEmpty()) {
            //noinspection unchecked
            filters.put("solutionKitGuid", (List) guids);
        }

        if (StringUtils.equalsIgnoreCase("guid", sort)) {
            sort = "solutionKitGuid";
        } else if (StringUtils.equalsIgnoreCase("parent", sort)) {
            sort = "parentGoid";
        }

        return RestEntityResourceUtils.createItemsList(
                factory.listResources(sort, ascendingSort, filters.map()),
                transformer,
                this,
                uriInfo.getRequestUri().toString()
        );
    }

    private static final String TEMPLATE_MAPPINGS = "<l7:Item xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
            "    <l7:Name>Bundle mappings</l7:Name>\n" +
            "    <l7:Type>BUNDLE MAPPINGS</l7:Type>\n" +
            "    <l7:TimeStamp>2015-09-21T11:22:00.398-07:00</l7:TimeStamp>\n" +
            "    <l7:Link rel=\"self\" uri=\"/1.0/bundle?versionComment=Simple+Service+and+Other+Dependencies+%28v1.1%29\"/>\n" +
            "    <l7:Resource>\n" +
            "        <l7:Mappings>\n" +
            "            Your Mappings goes here\n" +
            "        </l7:Mappings>\n" +
            "    </l7:Resource>\n" +
            "</l7:Item>\n";

    private static final String TEMPLATE_UNINSTALL_BUNDLE = "<l7:Bundle xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
            "    <l7:Mappings>\n" +
            "        Your uninstall bundle goes here\n" +
            "    </l7:Mappings>\n" +
            "</l7:Bundle>";

    /**
     * Returns a template, which is an example SolutionKit that can be used as a reference for what SolutionKit objects should look like.
     *
     * @return The template SolutionKit.
     */
    @GET
    @Path("template")
    public Item<SolutionKitMO> template() {
        // todo: add better sample data for template, perhaps incude a parent and entity ownership descriptors
        //
        final SolutionKitMO solutionKitMO = ManagedObjectFactory.createSolutionKitMO();
        solutionKitMO.setName("TemplateSolutionKit");
        solutionKitMO.setSkGuid("b41a8e66-75d8-4379-8bac-9ce16dff4d1f");
        solutionKitMO.setSkVersion("1.0");
        solutionKitMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                        .put(SolutionKit.SK_PROP_DESC_KEY, "This is a template Solution Kit example.")
                        .put(SolutionKit.SK_PROP_TIMESTAMP_KEY, "2015-08-04T12:57:35.603-08:00")
                        .put(SolutionKit.SK_PROP_IS_COLLECTION_KEY, "false")
                        .put(SolutionKit.SK_PROP_FEATURE_SET_KEY, "foo")
                        .map()
        );
        solutionKitMO.setMappings(TEMPLATE_MAPPINGS);
        solutionKitMO.setUninstallBundle(TEMPLATE_UNINSTALL_BUNDLE);
        solutionKitMO.setLastUpdateTime(1442859720418L);
        return super.createTemplateItem(solutionKitMO);
    }
}

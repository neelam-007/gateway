package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.SiteMinderConfigurationAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ChoiceParam;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ParameterValidationUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.SiteMinderConfigurationTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemsList;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.SiteMinderConfigurationMO;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.CollectionUtils;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.List;

/**
 * A SiteMinder Configuration describes a connection to site minder.
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + SiteMinderConfigurationResource.siteMinderConfigurations_URI)
@Singleton
public class SiteMinderConfigurationResource extends RestEntityResource<SiteMinderConfigurationMO, SiteMinderConfigurationAPIResourceFactory, SiteMinderConfigurationTransformer> {

    protected static final String siteMinderConfigurations_URI = "siteMinderConfigurations";

    @Override
    @SpringBean
    public void setFactory(SiteMinderConfigurationAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(SiteMinderConfigurationTransformer transformer) {
        super.transformer = transformer;
    }

    /**
     * Creates a new SiteMinder configuration
     *
     * @param resource The SiteMinder configuration to create
     * @return A reference to the newly created SiteMinder configuration
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @POST
    public Response create(SiteMinderConfigurationMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return super.create(resource);
    }

    /**
     * Returns a SiteMinder configuration with the given ID.
     *
     * @param id The ID of the SiteMinder configuration to return
     * @return The SiteMinder configuration
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{id}")
    public Item<SiteMinderConfigurationMO> get(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return super.get(id);
    }

    /**
     * <p>Returns a list of SiteMinder configurations. Can optionally sort the resulting list in ascending or
     * descending order. Other params given will be used as search values.</p>
     * <p class="italicize">Examples:</p>
     * <div class="code indent">/restman/1.0/siteMinderConfigurations?name=MySiteMinderConfiguration</div>
     * <p>Returns SiteMinder configuration with name "MySiteMinderConfiguration".</p>
     * <p>If a parameter is not a valid search value a bad request error will be returned.</p>
     *
     * @param sort            Key to sort the list by
     * @param order           Sort order for the list; 'true'=ascending, 'false'=descending; defaults to
     *                        ascending if not specified
     * @param names           Name filter
     * @param enabled         Enabled filter
     * @param securityZoneIds Security zone ID filter
     * @return A list of SiteMinder configurations. If the list is empty then no SiteMinder configurations were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    public ItemsList<SiteMinderConfigurationMO> list(
            @QueryParam("sort") @ChoiceParam({"id", "name"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("name") List<String> names,
            @QueryParam("enabled") Boolean enabled,
            @QueryParam("securityZone.id") List<Goid> securityZoneIds) {
        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParamsIncludeDefaults(uriInfo.getQueryParameters(), Arrays.asList("name", "enabled", "securityZone.id"));

        CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (names != null && !names.isEmpty()) {
            filters.put("name", (List) names);
        }
        if (enabled != null) {
            filters.put("enabled", (List) Arrays.asList(enabled));
        }
        if (securityZoneIds != null && !securityZoneIds.isEmpty()) {
            filters.put("securityZone.id", (List) securityZoneIds);
        }
        return super.list(sort, ascendingSort,
                filters.map());
    }

    /**
     * Creates or Updates an existing SiteMinder configuration. If a SiteMinder configuration with the given ID does not
     * exist one
     * will be created, otherwise the existing one will be updated.
     *
     * @param resource SiteMinder configuration to create or update
     * @param id       ID of the SiteMinder configuration to create or update
     * @return A reference to the newly created or updated SiteMinder configuration.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    @Path("{id}")
    public Response update(SiteMinderConfigurationMO resource, @PathParam("id") String id) throws ResourceFactory.ResourceFactoryException {
        return super.update(resource, id);
    }

    /**
     * Deletes an existing SiteMinder configuration.
     *
     * @param id The ID of the SiteMinder configuration to delete.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     */
    @DELETE
    @Path("{id}")
    @Override
    public void delete(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        super.delete(id);
    }

    /**
     * Returns a template, which is an example SiteMinder configuration that can be used as a reference for what
     * SiteMinder configuration objects should look like.
     *
     * @return The template SiteMinder configuration.
     */
    @GET
    @Path("template")
    public Item<SiteMinderConfigurationMO> template() {
        SiteMinderConfigurationMO siteMinderConfiguration = ManagedObjectFactory.createSiteMinderConfiguration();
        siteMinderConfiguration.setName("TemplateSiteMinderConfiguration");
        siteMinderConfiguration.setAddress("SFTP");
        siteMinderConfiguration.setEnabled(true);
        siteMinderConfiguration.setProperties(CollectionUtils.MapBuilder.<String, String>builder().put("ConnectorProperty", "PropertyValue").map());
        return super.createTemplateItem(siteMinderConfiguration);
    }
}

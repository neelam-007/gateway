package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.HttpConfigurationAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ChoiceParam;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ParameterValidationUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.HttpConfigurationTransformer;
import com.l7tech.gateway.api.HttpConfigurationMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemsList;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.common.resources.HttpConfiguration;
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
 * HTTP Configuration are used to configure various options to be used by the Gateway for HTTP/HTTPS connections. For
 * example, you can configure the login credentials for an HTTPS host, define a proxy for the host, or specify a
 * private
 * key to be used for authentication.
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + HttpConfigurationResource.httpConfiguration_URI)
@Singleton
public class HttpConfigurationResource extends RestEntityResource<HttpConfigurationMO, HttpConfigurationAPIResourceFactory, HttpConfigurationTransformer> {

    protected static final String httpConfiguration_URI = "httpConfigurations";

    @Override
    @SpringBean
    public void setFactory(HttpConfigurationAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(HttpConfigurationTransformer transformer) {
        super.transformer = transformer;
    }

    /**
     * Creates a new HTTP configuration.
     *
     * @param resource The HTTP configuration to create
     * @return A reference to the newly created http configuration
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @POST
    public Response create(HttpConfigurationMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return super.create(resource);
    }

    /**
     * Returns an HTTP configuration with the given ID.
     *
     * @param id The ID of the HTTP configuration to return
     * @return The HTTP configuration.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{id}")
    public Item<HttpConfigurationMO> get(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return super.get(id);
    }

    /**
     * <p>Returns a list of HTTP configurations. Can optionally sort the resulting list in ascending or
     * descending order. Other params given will be used as search values.</p>
     * <p class="italicize">Examples:</p>
     * <div class="code indent"><pre>/restman/1.0/httpConfigurations?name=MyHTTPConfiguration</pre></div>
     * <p>Returns HTTP configuration with name "MyHTTPConfiguration".</p>
     * <p>If a parameter is not a valid search value a bad request error will be returned.</p>
     *
     * @param sort            Key to sort the list by
     * @param order           Sort order for the list; 'asc'=ascending, 'desc'=descending; defaults to
     *                        ascending if not specified
     * @param names           Name filter
     * @param protocol        Protocol filter
     * @param ntlmHosts       NtlmHost filter
     * @param ntlmDomains     NtlmDomain id filter
     * @param securityZoneIds Security zone ID filter
     * @return A list of HTTP configurations. If the list is empty then no HTTP configurations were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    public ItemsList<HttpConfigurationMO> list(
            @QueryParam("sort") @ChoiceParam({"id", "host"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("host") List<String> names,
            @QueryParam("protocol") HttpConfiguration.Protocol protocol,
            @QueryParam("ntlmHost") List<String> ntlmHosts,
            @QueryParam("ntlmDomain") List<String> ntlmDomains,
            @QueryParam("securityZone.id") List<Goid> securityZoneIds) {
        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParamsIncludeDefaults(uriInfo.getQueryParameters(), Arrays.asList("host", "protocol", "ntlmHost", "ntlmDomain", "securityZone.id"));

        CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (names != null && !names.isEmpty()) {
            filters.put("host", (List) names);
        }
        if (protocol != null) {
            filters.put("protocol", (List) Arrays.asList(protocol));
        }
        if (ntlmHosts != null && !ntlmHosts.isEmpty()) {
            filters.put("ntlmHost", (List) ntlmHosts);
        }
        if (ntlmDomains != null && !ntlmDomains.isEmpty()) {
            filters.put("ntlmDomain", (List) ntlmDomains);
        }
        if (securityZoneIds != null && !securityZoneIds.isEmpty()) {
            filters.put("securityZone.id", (List) securityZoneIds);
        }
        return super.list(sort, ascendingSort,
                filters.map());
    }

    /**
     * Creates or Updates an existing HTTP configuration. If an HTTP configuration with the given ID does not exist one
     * will be created, otherwise the existing one will be updated.
     *
     * @param resource HTTP configuration to create or update
     * @param id       ID of the HTTP configuration to create or update
     * @return A reference to the newly created or updated HTTP configuration.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    @Path("{id}")
    public Response update(HttpConfigurationMO resource, @PathParam("id") String id) throws ResourceFactory.ResourceFactoryException {
        return super.update(resource, id);
    }

    /**
     * Deletes an existing HTTP configuration.
     *
     * @param id The ID of the HTTP configuration to delete.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     */
    @DELETE
    @Path("{id}")
    @Override
    public void delete(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        super.delete(id);
    }

    /**
     * Returns a template, which is an example HTTP configuration that can be used as a reference for what HTTP
     * configuration objects should look like.
     *
     * @return The template HTTP configuration.
     */
    @GET
    @Path("template")
    public Item<HttpConfigurationMO> template() {
        HttpConfigurationMO httpConfigurationMO = ManagedObjectFactory.createHttpConfiguration();
        httpConfigurationMO.setUsername("userName");
        httpConfigurationMO.setPort(8080);
        httpConfigurationMO.setHost("templateHost");
        httpConfigurationMO.setPasswordId(new Goid(0, 0).toString());
        httpConfigurationMO.setProtocol(HttpConfigurationMO.Protocol.HTTP);
        httpConfigurationMO.setPath("path");
        return super.createTemplateItem(httpConfigurationMO);
    }
}

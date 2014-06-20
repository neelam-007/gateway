package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.GenericEntityAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ChoiceParam;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ParameterValidationUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.GenericEntityTransformer;
import com.l7tech.gateway.api.GenericEntityMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemsList;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.util.CollectionUtils;
import org.glassfish.jersey.message.XmlHeader;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.List;

/**
 * The generic entity resource
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + GenericEntityResource.genericEntity_URI)
@Singleton
public class GenericEntityResource extends RestEntityResource<GenericEntityMO, GenericEntityAPIResourceFactory, GenericEntityTransformer> {

    protected static final String genericEntity_URI = "genericEntities";

    @Override
    @SpringBean
    public void setFactory(GenericEntityAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(GenericEntityTransformer transformer) {
        super.transformer = transformer;
    }

    /**
     * Creates a new generic entity.
     *
     * @param resource The generic entity to create
     * @return a reference to the newly created generic entity
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @POST
    @XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    public Response create(GenericEntityMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return super.create(resource);
    }

    /**
     * Retrieves a generic entity given its id
     *
     * @param id The id of the generic entity to retrieve
     * @return The generic entity.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{id}")
    public Item<GenericEntityMO> get(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return super.get(id);
    }

    /**
     * This will return a list of generic entity references. A sort can be specified to allow the resulting list to be
     * sorted in either ascending or descending order. Other params given will be used as search values. Examples:
     * <p/>
     * /restman/genericEntities?name=MyGeneric
     * <p/>
     * Returns the generic entities with name = "MyGeneric"
     * <p/>
     * /restman/genericEntities?entityClassName=foo.GenericFoo&name=MyGeneric&name=MyGeneric2
     * <p/>
     * Returns generic entities with class name foo.GenericFoo and with name either "MyGeneric" or "MyGeneric2"
     * <p/>
     *
     * @param sort             the key to sort the list by.
     * @param order            the order to sort the list. true for ascending, false for descending. null implies
     *                         ascending
     * @param names            The name filter
     * @param enabled          the enabled filter
     * @param entityClassNames The entityClassName filter
     * @return A list of generic entities. If the list is empty then no entities were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    //This xml header allows the list to be explorable when viewed in a browser
    //@XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    public ItemsList<GenericEntityMO> list(
            @QueryParam("sort") @ChoiceParam({"id", "name", "entityClassName"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("name") List<String> names,
            @QueryParam("enabled") Boolean enabled,
            @QueryParam("entityClassName") List<String> entityClassNames) {
        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParamsIncludeDefaults(uriInfo.getQueryParameters(), Arrays.asList("name", "enabled", "entityClassName"));

        CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (names != null && !names.isEmpty()) {
            filters.put("name", (List) names);
        }
        if (enabled != null) {
            filters.put("enabled", (List) Arrays.asList(enabled));
        }
        if (entityClassNames != null && !entityClassNames.isEmpty()) {
            filters.put("entityClassName", (List) entityClassNames);
        }
        return super.list(sort, ascendingSort,
                filters.map());
    }

    /**
     * Updates an existing generic entity
     *
     * @param resource The updated generic entity
     * @param id       The id of the generic entity to update
     * @return a reference to the newly updated entity.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    @Path("{id}")
    @XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    public Response update(GenericEntityMO resource, @PathParam("id") String id) throws ResourceFactory.ResourceFactoryException {
        return super.update(resource, id);
    }

    /**
     * Deletes an existing generic entity.
     *
     * @param id The id of the generic entity to delete.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     */
    @DELETE
    @Path("{id}")
    @Override
    public void delete(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        super.delete(id);
    }

    /**
     * This will return a template, example generic entity that can be used as a reference for what generic entity
     * objects should look like.
     *
     * @return The template generic entity.
     */
    @GET
    @Path("template")
    public Item<GenericEntityMO> template() {
        GenericEntityMO genericEntityMO = ManagedObjectFactory.createGenericEntity();
        genericEntityMO.setName("TemplateGenericEntity");
        genericEntityMO.setEntityClassName("com.foo");
        genericEntityMO.setEnabled(true);
        genericEntityMO.setValueXml("value_xml");
        return super.createTemplateItem(genericEntityMO);
    }
}

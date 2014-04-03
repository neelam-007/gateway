package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.exceptions.InvalidArgumentException;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.DocumentAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ChoiceParam;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ParameterValidationUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.DocumentTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemsList;
import com.l7tech.gateway.api.ResourceDocumentMO;
import com.l7tech.gateway.common.resources.ResourceType;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.CollectionUtils;
import org.glassfish.jersey.message.XmlHeader;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The resource document resource
 * @title Service Document
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + DocumentResource.document_URI)
@Singleton
public class DocumentResource extends RestEntityResource<ResourceDocumentMO, DocumentAPIResourceFactory, DocumentTransformer> {

    protected static final String document_URI = "resources";

    @Override
    @SpringBean
    public void setFactory(DocumentAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(DocumentTransformer transformer) {
        super.transformer = transformer;
    }

    /**
     * Creates a new entity
     *
     * @param resource The entity to create
     * @return a reference to the newly created entity
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @POST
    @XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    public Response create(ResourceDocumentMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return super.create(resource);
    }

    /**
     * This implements the GET method to retrieve an entity by a given id.
     *
     * @param id The identity of the entity to select
     * @return The selected entity.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{id}")
    public Item<ResourceDocumentMO> get(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return super.get(id);
    }

    /**
     * This will return a list of entity references. A sort can be specified to allow the resulting list to be sorted in
     * either ascending or descending order. Other params given will be used as search values. Examples:
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
     * @param sort            the key to sort the list by.
     * @param order           the order to sort the list. true for ascending, false for descending. null implies
     *                        ascending
     * @param uris            The uri filter
     * @param descriptions    the description filter
     * @param types           The type filter
     * @param securityZoneIds the securityzone id filter
     * @return A list of entities. If the list is empty then no entities were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    //This xml header allows the list to be explorable when viewed in a browser
    //@XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    public ItemsList<ResourceDocumentMO> list(
            @QueryParam("sort") @ChoiceParam({"id", "uri"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("uri") List<String> uris,
            @QueryParam("description") List<String> descriptions,
            @QueryParam("type") @ChoiceParam({"dtd", "xmlschema"}) List<String> types,
            @QueryParam("securityZone.id") List<Goid> securityZoneIds) {
        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParams(uriInfo.getQueryParameters(), Arrays.asList("uri", "description", "type", "securityZone.id"));

        CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (uris != null && !uris.isEmpty()) {
            filters.put("uri", (List) uris);
        }
        if (descriptions != null && !descriptions.isEmpty()) {
            filters.put("description", (List) descriptions);
        }
        if (types != null && !types.isEmpty()) {
            filters.put("type", (List) convertTypes(types));
        }
        if (securityZoneIds != null && !securityZoneIds.isEmpty()) {
            filters.put("securityZone.id", (List) securityZoneIds);
        }
        return super.list(sort, ascendingSort,
                filters.map());
    }

    private List<ResourceType> convertTypes(List<String> types) {
        List<ResourceType> resourceTypes = new ArrayList<>(types.size());
        for(String typeString : types){
            switch(typeString){
                case "dtd":
                    resourceTypes.add(ResourceType.DTD);
                    break;
                case "xmlschema":
                    resourceTypes.add(ResourceType.XML_SCHEMA);
                    break;
                default:
                    throw new InvalidArgumentException("type", "Type is expected to be either 'dtd' or 'xsd'");
            }
        }
        return resourceTypes;
    }

    /**
     * Updates an existing entity
     *
     * @param resource The updated entity
     * @param id       The id of the entity to update
     * @return a reference to the newly updated entity.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    @Path("{id}")
    @XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    public Response update(ResourceDocumentMO resource, @PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return super.update(resource, id);
    }

    /**
     * Deletes an existing active connector.
     *
     * @param id The id of the active connector to delete.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     */
    @DELETE
    @Path("{id}")
    @Override
    public void delete(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        super.delete(id);
    }

    /**
     * This will return a template, example entity that can be used as a base to creating a new entity.
     *
     * @return The template entity.
     */
    @GET
    @Path("template")
    public Item<ResourceDocumentMO> template() {
        return super.template();
    }
}

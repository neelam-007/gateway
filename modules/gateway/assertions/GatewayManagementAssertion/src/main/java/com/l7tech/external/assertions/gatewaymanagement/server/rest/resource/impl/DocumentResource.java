package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.exceptions.InvalidArgumentException;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.DocumentAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ChoiceParam;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ParameterValidationUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.DocumentTransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.common.resources.ResourceType;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.CollectionUtils;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* NOTE: The java docs in this class get converted to API documentation seen by customers!*/

/**
 * Resource documents are documents schema documents. They are either a dtd or an xml schema.
 *
 * @title Resource Document
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
     * Creates a new resource document
     *
     * @param resource The resource document to create
     * @return a reference to the newly created resource document
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @POST
    public Response create(ResourceDocumentMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return super.create(resource);
    }

    /**
     * Returns a resource document with the given ID.
     *
     * @param id The ID of the resource document to return
     * @return The resource document.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{id}")
    public Item<ResourceDocumentMO> get(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return super.get(id);
    }

    /**
     * <p>Returns a list of resource documents. Can optionally sort the resulting list in ascending or
     * descending order. Other params given will be used as search values.</p>
     * <p class="italicize">Examples:</p>
     * <div class="code indent"><pre>/restman/1.0/resources?uri=documentURI</pre></div>
     * <p>Returns resource document with uri "documentURI".</p>
     * <div class="code indent"><pre>/restman/1.0/resources?type=xmlschema</pre></div>
     * <p>Returns resource documents of xmlschema type</p>
     * <p>If a parameter is not a valid search value a bad request error will be returned.</p>
     *
     * @param sort            Key to sort the list by
     * @param order           Sort order for the list; 'asc'=ascending, 'desc'=descending; defaults to
     *                        ascending if not specified
     * @param uris            Uri filter
     * @param descriptions    Description filter
     * @param types           Type filter
     * @param securityZoneIds Security zone ID filter
     * @return A list of resource documents. If the list is empty then no resource documents were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    public ItemsList<ResourceDocumentMO> list(
            @QueryParam("sort") @ChoiceParam({"id", "uri"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("uri") List<String> uris,
            @QueryParam("description") List<String> descriptions,
            @QueryParam("type") @ChoiceParam({"dtd", "xmlschema"}) List<String> types,
            @QueryParam("securityZone.id") List<Goid> securityZoneIds) {
        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParamsIncludeDefaults(uriInfo.getQueryParameters(), Arrays.asList("uri", "description", "type", "securityZone.id"));

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
        for (String typeString : types) {
            switch (typeString) {
                case "dtd":
                    resourceTypes.add(ResourceType.DTD);
                    break;
                case "xmlschema":
                    resourceTypes.add(ResourceType.XML_SCHEMA);
                    break;
                default:
                    throw new InvalidArgumentException("type", "Type is expected to be either 'dtd' or 'xmlschema'");
            }
        }
        return resourceTypes;
    }

    /**
     * Creates or Updates an existing resource document. If a resource document with the given ID does not exist one
     * will be created, otherwise the existing one will be updated.
     *
     * @param resource Resource document to create or update
     * @param id       ID of the resource document to create or update
     * @return A reference to the newly created or updated resource document.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    @Path("{id}")
    public Response update(ResourceDocumentMO resource, @PathParam("id") String id) throws ResourceFactory.ResourceFactoryException {
        return super.update(resource, id);
    }

    /**
     * Deletes an existing resource document.
     *
     * @param id The ID of the resource document to delete.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     */
    @DELETE
    @Path("{id}")
    @Override
    public void delete(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        super.delete(id);
    }

    /**
     * Returns a template, which is an example resource document that can be used as a reference for what resource
     * document objects should look like.
     *
     * @return The template resource document.
     */
    @GET
    @Path("template")
    public Item<ResourceDocumentMO> template() {
        ResourceDocumentMO docMO = ManagedObjectFactory.createResourceDocument();
        docMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder().put("ConnectorProperty", "PropertyValue").map());
        Resource resource = ManagedObjectFactory.createResource();
        resource.setId("TemplateId");
        resource.setContent("TemplateContent");
        resource.setType("dtd");
        docMO.setResource(resource);
        return super.createTemplateItem(docMO);
    }
}

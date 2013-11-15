package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.entities.Reference;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.entities.References;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.CollectionUtils;
import org.glassfish.jersey.message.XmlHeader;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * This is the base wiseman entity resources. It should be extended by rest entities that use the wiseman resource
 * factory to do their processing
 *
 * @author Victor Kazakov
 */
public abstract class RestWsmanEntityResource<R, F extends ResourceFactory<R>> {

    protected static final UriBuilder URI_BUILDER = UriBuilder.fromPath("{entity}").path("{id}");

    @Context
    UriInfo uriInfo;

    public static enum IdType {
        id, guid, name
    }

    protected F factory;

    // The setFactory method will be called automatically to inject the factory. The factory parameter must be annotated
    // with @Factory
    @SuppressWarnings("UnusedDeclaration")
    public abstract void setFactory(F factory);

    /**
     * Lists entities.
     *
     * @param start The offset from the start of the list to start listing from
     * @param count The total number of entities to return.
     * @return A list of entities.
     * @throws ResourceFactory.ResourceNotFoundException
     *
     */
    @GET
    @Produces(MediaType.APPLICATION_XML)
    @XmlHeader("<?xml-stylesheet type=\"text/xsl\" href=\"../" + XslStyleSheetResource.DefaultStyleSheetPath + "\"?>")
    public Response listResources(@QueryParam("start") @DefaultValue("0") @Min(0) int start, @QueryParam("count") @DefaultValue("100") @Min(0) @Max(500) int count) throws ResourceFactory.ResourceNotFoundException {
        final Collection<Map<String, String>> resources = factory.getResources(start, count);

        List<Reference> resourceList = new ArrayList<>(count);
        for (Map<String, String> selectorMap : resources) {
            UriBuilder ub = uriInfo.getAbsolutePathBuilder().path(selectorMap.get("id"));
            final URI uri = ub.build();
            resourceList.add(new Reference(uri.toString(), uri.toString()));
        }

        return Response.ok(new References(resourceList)).build();
    }

    /**
     * This implements the GET method to retrieve an entity by a given id.
     *
     * @param id     The identity of the entity to select
     * @param idType The type of the id.
     * @return The selected entity.
     * @throws ResourceFactory.ResourceNotFoundException
     *
     */
    @GET
    @Path("{id}")
    public Response getResource(@PathParam("id") String id, @QueryParam("idType") @DefaultValue("id") IdType idType) throws ResourceFactory.ResourceNotFoundException {
        R resource = factory.getResource(buildSelectorMap(id, idType));
        return Response.ok(resource).build();
    }

    /**
     * Creates a new entity
     *
     * @param resource The entity to create
     * @return a reference to the newly created entity
     * @throws ResourceFactory.ResourceNotFoundException
     *
     * @throws ResourceFactory.InvalidResourceException
     *
     */
    @POST
    @XmlHeader("<?xml-stylesheet type=\"text/xsl\" href=\"../" + XslStyleSheetResource.DefaultStyleSheetPath + "\"?>")
    public Response createResource(R resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        Map<String, String> selectorMap = factory.createResource(resource);
        UriBuilder ub = uriInfo.getAbsolutePathBuilder().path(selectorMap.get("id"));
        final URI uri = ub.build();
        return Response.created(uri).entity(new Reference(uri.toString(), uri.toString())).build();
    }

    //TODO:This needs to still be implemented.
    /**
     * Creates a new entity with the given id
     *
     * @param resource
     * @param id
     * @return
     * @throws ResourceFactory.ResourceNotFoundException
     *
     * @throws ResourceFactory.InvalidResourceException
     *
     */
    @POST
    @Path("{id}")
    public Response createResourceWithId(R resource, @PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        Goid goid = Goid.parseGoid(id);
        Map<String, String> selectorMap = factory.createResource(resource);
        UriBuilder ub = uriInfo.getAbsolutePathBuilder().path(selectorMap.get("id"));
        return Response.created(ub.build()).build();
    }

    /**
     * Updates an existing entity
     * @param resource
     * @param id
     * @param idType
     * @return
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    @Path("{id}")
    public Response updateResource(R resource, @PathParam("id") String id, @QueryParam("idType") @DefaultValue("id") IdType idType) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        R resourceUpdated = factory.putResource(buildSelectorMap(id, idType), resource);
        UriBuilder ub = uriInfo.getAbsolutePathBuilder().path(id);
        return Response.ok(resourceUpdated).contentLocation(ub.build()).build();
    }

    /**
     * deletes an existing entity.
     * @param id
     * @param idType
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @DELETE
    @Path("{id}")
    public void deleteResource(@PathParam("id") String id, @QueryParam("idType") @DefaultValue("id") IdType idType) throws ResourceFactory.ResourceNotFoundException {
        factory.deleteResource(buildSelectorMap(id, idType));
    }

    protected Map<String, String> buildSelectorMap(String id, IdType idType) {
        return CollectionUtils.MapBuilder.<String, String>builder().put(idType.name(), id).map();
    }
}

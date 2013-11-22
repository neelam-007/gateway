package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.RestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.TemplateFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.entities.Reference;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.entities.References;
import org.glassfish.jersey.message.XmlHeader;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * This is the base resource factory for a rest entity. It supports all crud operations:
 * <pre><ul>
 *     <li>Create</li>
 *     <li>Create with id</li>
 *     <li>Get</li>
 *     <li>delete</li>
 *     <li>update</li>
 *     <li>list. can specify offset, count and filters.</li>
 * </ul></pre>
 *
 * @author Victor Kazakov
 */
public abstract class RestEntityResource<R, F extends RestResourceFactory<R> & TemplateFactory<R>> {
    /**
     * This is used to create resource uri's
     */
    @Context
    private UriInfo uriInfo;

    /**
     * This is the rest resource factory method used to perform the crud operations on the entity.
     */
    protected F factory;

    /**
     * This method needs to be called to set the factory. It should be called in the initialization faze before any of
     * the Rest methods are called. It should likely be annotated with {@link com.l7tech.gateway.rest.SpringBean} to
     * have jersey automatically inject the factory dependency
     *
     * @param factory The factory for this resource
     */
    @SuppressWarnings("UnusedDeclaration")
    public abstract void setFactory(F factory);

    /**
     * This will return a list of entity references. It will return a maximum of {@code count} references, it can return
     * fewer references if there are fewer then {@code count} entities found. Setting an offset will start listing
     * entities from the given offset. Filters can be used to filter out entities based on thier properties. Currently
     * only equality filters are possible.
     *
     * @param offset  The offset from the start of the list to start listing from
     * @param count   The total number of entities to return. The returned list can be shorter is there are not enough
     *                entities
     * @param filters This is a collection of filters to apply to the list.
     * @return A list of entities. If the list is empty then no entities were found.
     */
    @GET
    @Produces(MediaType.APPLICATION_XML)
    //This xml header allows the list to be explorable when viewed in a browser
    @XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    public Response listResources(@QueryParam("offset") @DefaultValue("0") @Min(0) int offset, @QueryParam("count") @DefaultValue("100") @Min(1) @Max(500) int count, @QueryParam("filters") String filters) {
        //TODO: implement filtering.
        //gets the list of resource ids
        final List<String> resourceIds = factory.listResources(offset, count, null);

        //Create the Reference list.
        List<Reference> resourceList = new ArrayList<>(resourceIds.size());
        for (String id : resourceIds) {
            UriBuilder ub = uriInfo.getAbsolutePathBuilder().path(id);
            final URI uri = ub.build();
            resourceList.add(new Reference(uri.toString(), uri.toString()));
        }

        return Response.ok(new References(resourceList)).build();
    }

    /**
     * This implements the GET method to retrieve an entity by a given id.
     *
     * @param id The identity of the entity to select
     * @return The selected entity.
     * @throws ResourceFactory.ResourceNotFoundException
     *
     */
    @GET
    @Path("{id}")
    public Response getResource(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        R resource = factory.getResource(id);
        return Response.ok(resource).build();
    }

    /**
     * This will return a template, example entity that can be used as a base to creating a new entity.
     *
     * @return The template entity.
     */
    @GET
    @Path("template")
    public Response getResourceTemplate() {
        R resource = factory.getResourceTemplate();
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
    @XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    public Response createResource(R resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        String id = factory.createResource(resource);
        UriBuilder ub = uriInfo.getAbsolutePathBuilder().path(id);
        final URI uri = ub.build();
        return Response.created(uri).entity(new Reference(uri.toString(), uri.toString())).build();
    }

    /**
     * Creates a new entity with the given id
     *
     * @param resource The entity to create
     * @param id       The id to create the entity with.
     * @return a reference to the newly created entity
     * @throws ResourceFactory.ResourceNotFoundException
     *
     * @throws ResourceFactory.InvalidResourceException
     *
     */
    @POST
    @Path("{id}")
    @XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    public Response createResourceWithId(R resource, @PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        factory.createResource(id, resource);
        UriBuilder ub = uriInfo.getAbsolutePathBuilder();
        final URI uri = ub.build();
        return Response.created(uri).entity(new Reference(uri.toString(), uri.toString())).build();
    }

    /**
     * Updates an existing entity
     *
     * @param resource The updated entity
     * @param id       The id of the entity to update
     * @return a reference to the newly updated entity.
     * @throws ResourceFactory.ResourceNotFoundException
     *
     * @throws ResourceFactory.InvalidResourceException
     *
     */
    @PUT
    @Path("{id}")
    @XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    public Response updateResource(R resource, @PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        factory.updateResource(id, resource);
        UriBuilder ub = uriInfo.getAbsolutePathBuilder();
        final URI uri = ub.build();
        return Response.created(uri).entity(new Reference(uri.toString(), uri.toString())).build();
    }

    /**
     * Deletes an existing entity.
     *
     * @param id The id of the entity to delete.
     * @throws ResourceFactory.ResourceNotFoundException
     *
     */
    @DELETE
    @Path("{id}")
    public void deleteResource(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        factory.deleteResource(id);
    }
}

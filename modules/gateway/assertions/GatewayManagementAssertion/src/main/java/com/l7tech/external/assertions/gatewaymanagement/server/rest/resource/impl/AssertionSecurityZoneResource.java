package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ServerRESTGatewayManagementAssertion;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.AssertionSecurityZoneRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.*;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.Functions;
import org.glassfish.jersey.message.XmlHeader;

import javax.validation.constraints.Pattern;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import java.util.List;

/**
 * The active connector resource
 */
@Provider
@Path(AssertionSecurityZoneResource.Version_URI + AssertionSecurityZoneResource.activeConnectors_URI)
public class AssertionSecurityZoneResource implements  TemplatingResource {

    protected static final String Version_URI = ServerRESTGatewayManagementAssertion.Version1_0_URI;
    protected static final String activeConnectors_URI = "assertionSecurityZones";
    protected AssertionSecurityZoneRestResourceFactory factory;
    @Context
    protected UriInfo uriInfo;

    @SpringBean
    public void setFactory(AssertionSecurityZoneRestResourceFactory factory) {
        this.factory = factory;
    }

    public EntityType getEntityType(){
        return EntityType.ASSERTION_ACCESS;
    }

    protected Reference<AssertionSecurityZoneMO> toReference(AssertionSecurityZoneMO resource) {
        return new ReferenceBuilder<AssertionSecurityZoneMO>(resource.getName(), resource.getId(), getEntityType().name())
                .addLink(ManagedObjectFactory.createLink("self", RestEntityResourceUtils.createURI(uriInfo.getBaseUriBuilder().path(this.getClass()).build(), resource.getName())))
                .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_XML)
        //This xml header allows the list to be explorable when viewed in a browser
        //@XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    public References listResources(@QueryParam("sort") final String sort, @QueryParam("order") @DefaultValue("asc") @Pattern(regexp = "asc|desc") final String order)
    {
        final String sortKey = factory.getSortKey(sort);
        if(sort != null && sortKey == null) {
            throw new IllegalArgumentException("Invalid sort. Cannot sort by: " + sort);
        }

        List<Reference> references = Functions.map(factory.listResources(sortKey, RestEntityResourceUtils.convertOrder(order), RestEntityResourceUtils.createFiltersMap(factory.getFiltersInfo(), uriInfo.getQueryParameters())), new Functions.Unary<Reference, AssertionSecurityZoneMO>() {
            @Override
            public Reference call(AssertionSecurityZoneMO resource) {
                return toReference(resource);
            }
        });
        return ManagedObjectFactory.createReferences(references);
    }

    /**
     * This implements the GET method to retrieve an entity by a given name.
     *
     * @param name The name of the entity to select
     * @return The selected entity.
     * @throws ResourceFactory.ResourceNotFoundException
     *
     */
    @GET
    @Path("{name}")
    public Reference<AssertionSecurityZoneMO> getResource(@PathParam("name")String name) throws ResourceFactory.ResourceNotFoundException {
        AssertionSecurityZoneMO resource = factory.getResourceByName(name);
        return new ReferenceBuilder<>(toReference(resource))
                .setContent(resource)
                .addLink(ManagedObjectFactory.createLink("template", RestEntityResourceUtils.createURI(uriInfo.getBaseUriBuilder().path(this.getClass()).build(), "template")))
                .addLink(ManagedObjectFactory.createLink("list", uriInfo.getBaseUriBuilder().path(this.getClass()).build().toString()))
                .build();
    }

    @Override
    public Reference getResourceTemplate() {
        AssertionSecurityZoneMO resource = factory.getResourceTemplate();
        Reference<AssertionSecurityZoneMO> reference = ManagedObjectFactory.createReference();
        reference.setResource(resource);
        return reference;

    }
    /**
     * Updates an existing entity
     *
     * @param resource The updated entity
     * @param name       The name of the entity to update
     * @return a reference to the newly updated entity.
     * @throws ResourceFactory.ResourceNotFoundException
     *
     * @throws ResourceFactory.InvalidResourceException
     *
     */
    @PUT
    @Path("{name}")
    @XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    public Response updateResource(AssertionSecurityZoneMO resource, @PathParam("name") String name) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        AssertionSecurityZoneMO updatedResource = factory.updateResourceByName(name, resource);
        return Response.ok().entity(toReference(updatedResource)).build();
    }
}

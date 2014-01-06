package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ServerRESTGatewayManagementAssertion;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.PrivateKeyRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.*;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.PrivateKeyExportResult;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.Functions;
import org.glassfish.jersey.message.XmlHeader;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import java.net.URI;
import java.util.List;

/**
 * The private key resource
 */
@Provider
@Path(PrivateKeyResource.Version_URI + PrivateKeyResource.privateKey_URI)
@Singleton
public class PrivateKeyResource implements CreatingResource<PrivateKeyCreationContext>, ReadingResource, UpdatingResource<PrivateKeyMO>, DeletingResource, ListingResource, TemplatingResource {

    protected static final String Version_URI = ServerRESTGatewayManagementAssertion.Version1_0_URI;

    /**
     * This is the rest resource factory method used to perform the crud operations on the entity.
     */
    protected PrivateKeyRestResourceFactory factory;

    @Context
    protected UriInfo uriInfo;

    protected static final String privateKey_URI = "privateKeys";

    @SpringBean
    public void setFactory(PrivateKeyRestResourceFactory factory) {
        this.factory = factory;
    }

    protected Reference<PrivateKeyMO> toReference(PrivateKeyMO resource) {
        return new ReferenceBuilder<PrivateKeyMO>(resource.getAlias(), resource.getId(), factory.getEntityType().name())
                .addLink(ManagedObjectFactory.createLink("self", RestEntityResourceUtils.createURI(uriInfo.getBaseUriBuilder().path(this.getClass()).build(), resource.getId())))
                .build();
    }

    @Override
    public Reference<PrivateKeyMO> getResourceTemplate() {
        PrivateKeyMO resource = factory.getResourceTemplate();
        Reference<PrivateKeyMO> reference = ManagedObjectFactory.createReference();
        reference.setResource(resource);
        return reference;
    }

    @Override
    public Response createResource(PrivateKeyCreationContext resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        PrivateKeyMO mo = factory.createPrivateKey(resource);
        UriBuilder ub = uriInfo.getAbsolutePathBuilder().path(mo.getId());
        final URI uri = ub.build();
        return Response.created(uri).entity(toReference(mo)).build();
    }

    @Override
    public void deleteResource(String id) throws ResourceFactory.ResourceNotFoundException {
        factory.deleteResource(id);
    }

    @Override
    public Reference<References> listResources(int offset, int count, String sort, String order) {
        final String sortKey = factory.getSortKey(sort);
        if (sort != null && sortKey == null) {
            throw new IllegalArgumentException("Invalid sort. Cannot sort by: " + sort);
        }

        List<Reference> references = Functions.map(factory.listResources(offset, count, sortKey, RestEntityResourceUtils.convertOrder(order), RestEntityResourceUtils.createFiltersMap(factory.getFiltersInfo(), uriInfo.getQueryParameters())), new Functions.Unary<Reference, PrivateKeyMO>() {
            @Override
            public Reference call(PrivateKeyMO resource) {
                return toReference(resource);
            }
        });

        return new ReferenceBuilder<References>(EntityType.SSG_KEY_ENTRY + " list", "List").setContent(ManagedObjectFactory.createReferences(references))
                .addLink(ManagedObjectFactory.createLink("self", uriInfo.getRequestUri().toString()))
                .build();
    }

    @Override
    public Reference<PrivateKeyMO> getResource(String id) throws ResourceFactory.ResourceNotFoundException, FindException {
        PrivateKeyMO resource = factory.getResource(id);
        return new ReferenceBuilder<>(toReference(resource))
                .setContent(resource)
                .addLink(ManagedObjectFactory.createLink("template", RestEntityResourceUtils.createURI(uriInfo.getBaseUriBuilder().path(this.getClass()).build(), "template")))
                .addLink(ManagedObjectFactory.createLink("list", uriInfo.getBaseUriBuilder().path(this.getClass()).build().toString()))
                .build();
    }

    @Override
    public Response updateResource(PrivateKeyMO resource, String id) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        factory.updateResource(id, resource);
        return Response.ok().entity(toReference(resource)).build();
    }

    @POST
    @XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    @Path("{id}/export")
    public Response exportResource(@PathParam("id") String id, Password password, @QueryParam("alias") String alias) throws ResourceFactory.ResourceNotFoundException, FindException {
        PrivateKeyExportResult exportResult = factory.exportResource(id, password != null ? password.getValue() : "", alias);
        PrivateKeyRestExport restExport = ManagedObjectFactory.createPrivateKeyRestExportMO();
        restExport.setKeystoreID(id.substring(0, id.indexOf(":")));
        restExport.setAlias(alias != null ? alias : id.substring(id.indexOf(":") + 1));
        restExport.setPkcs12Data(exportResult.getPkcs12Data());
        return Response.ok().entity(restExport).build();
    }

    @PUT
    @XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    @Path("import")
    public Response importResource(PrivateKeyRestExport restExport) throws ResourceFactory.ResourceNotFoundException, FindException, ResourceFactory.InvalidResourceException {
        PrivateKeyMO importResult = factory.importResource(restExport.getKeystoreID(), restExport.getAlias(), restExport.getPkcs12Data(), restExport.getPassword());
        return Response.ok().entity(importResult).build();
    }

    @PUT
    @XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    @Path("specialPurpose")
    public Response importResource(@QueryParam("id")String id, @QueryParam("purpose")List<String> purpose) throws ResourceFactory.ResourceNotFoundException, FindException, ResourceFactory.InvalidResourceException {
        PrivateKeyMO resource = factory.setSpecialPurpose(id,purpose);
        return Response.ok().entity(toReference(resource)).build();
    }
}

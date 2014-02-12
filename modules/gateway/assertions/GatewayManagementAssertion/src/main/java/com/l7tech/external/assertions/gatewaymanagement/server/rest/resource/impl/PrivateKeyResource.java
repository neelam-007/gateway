package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ServerRESTGatewayManagementAssertion;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.PrivateKeyAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.PrivateKeyTransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.PrivateKeyExportResult;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.FindException;
import org.glassfish.jersey.message.XmlHeader;
import org.jetbrains.annotations.NotNull;

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
public class PrivateKeyResource extends RestEntityResource<PrivateKeyMO, PrivateKeyAPIResourceFactory, PrivateKeyTransformer> {

    protected static final String Version_URI = ServerRESTGatewayManagementAssertion.Version1_0_URI;

    @Context
    protected UriInfo uriInfo;

    protected static final String privateKey_URI = "privateKeys";

    @Override
    @SpringBean
    public void setFactory(PrivateKeyAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(PrivateKeyTransformer transformer) {
        super.transformer = transformer;
    }

    /**
     * This is not allowed to create private keys with a PrivateKeyMO.
     * Adding the @DefaultValue annotation makes this method get ignored by jersey
     */
    @Override
    public Response createResource(@DefaultValue("null") PrivateKeyMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        throw new UnsupportedOperationException("Cannot create a new private key given a PrivateKeyMO");
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
    public Response createResource(PrivateKeyCreationContext resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        PrivateKeyMO mo = factory.createPrivateKey(resource);
        UriBuilder ub = uriInfo.getAbsolutePathBuilder().path(mo.getId());
        final URI uri = ub.build();
        return Response.created(uri).entity(new ItemBuilder<>(
                transformer.convertToItem(mo))
                .setContent(null)
                .addLink(getLink(mo))
                .addLinks(getRelatedLinks(mo))
                .build())
                .build();
    }

    @NotNull
    @Override
    public String getUrl(@NotNull PrivateKeyMO privateKeyMO) {
        return getUrlString(privateKeyMO.getId());
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
        return Response.ok().entity(transformer.convertToItem(resource)).build();
    }
}

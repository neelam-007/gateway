package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ServerRESTGatewayManagementAssertion;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.PrivateKeyAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ChoiceParam;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ParameterValidationUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.PrivateKeyTransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.PrivateKeyExportResult;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.CollectionUtils;
import org.glassfish.jersey.message.XmlHeader;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.Provider;
import java.net.URI;
import java.util.Arrays;
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
     * This is not allowed to create private keys with a PrivateKeyMO. Adding the @DefaultValue annotation makes this
     * method get ignored by jersey
     */
    @Override
    public Response create(@DefaultValue("null") PrivateKeyMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        throw new UnsupportedOperationException("Cannot create a new private key given a PrivateKeyMO");
    }

    /**
     * Creates a new entity
     *
     * @param resource The entity to create
     * @param id The identity of the entity to create in the form of [keystore id]:[alias]
     * @return a reference to the newly created entity
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @POST
    @Path("{id}")
    @XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    public Response createResource(PrivateKeyCreationContext resource, @PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {

        PrivateKeyMO mo = factory.createPrivateKey(resource, id);
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

    /**
     * This implements the GET method to retrieve an entity by a given id.
     *
     * @param id The identity of the entity to select
     * @return The selected entity.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{id}")
    public Item<PrivateKeyMO> get(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
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
     * @param aliases         The alias filter
     * @param keystores       the keystore filter
     * @param securityZoneIds the securityzone id filter
     * @return A list of entities. If the list is empty then no entities were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    //This xml header allows the list to be explorable when viewed in a browser
    //@XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    public ItemsList<PrivateKeyMO> list(
            @QueryParam("sort") @ChoiceParam({"id", "alias", "keystore"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("alias") List<String> aliases,
            @QueryParam("keystore") List<String> keystores,
            @QueryParam("securityZone.id") List<Goid> securityZoneIds) {
        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParams(uriInfo.getQueryParameters(), Arrays.asList("alias", "keystore", "securityZone.id"));

        CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (aliases != null && !aliases.isEmpty()) {
            filters.put("alias", (List) aliases);
        }
        if (keystores != null && !keystores.isEmpty()) {
            filters.put("keystore", (List) keystores);
        }
        if (securityZoneIds != null && !securityZoneIds.isEmpty()) {
            filters.put("securityZone.id", (List) securityZoneIds);
        }
        return super.list(sort, ascendingSort,
                filters.map());
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
    public Response update(PrivateKeyMO resource, @PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return super.update(resource, id);
    }

    /**
     * Export a private key
     *
     * @param id       The id of the key store
     * @param password The password to export the private key with
     * @param alias    The alias of the private key
     * @return The exported private key
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws FindException
     */
    @POST
    @XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    @Path("{id}/export")
    public Response exportPrivateKey(@PathParam("id") String id, Password password, @QueryParam("alias") String alias) throws ResourceFactory.ResourceNotFoundException, FindException {
        PrivateKeyExportResult exportResult = factory.exportResource(id, password != null ? password.getValue() : "", alias);
        PrivateKeyRestExport restExport = ManagedObjectFactory.createPrivateKeyRestExportMO();
        restExport.setKeystoreID(id.substring(0, id.indexOf(":")));
        restExport.setAlias(alias != null ? alias : id.substring(id.indexOf(":") + 1));
        restExport.setPkcs12Data(exportResult.getPkcs12Data());
        return Response.ok().entity(restExport).build();
    }

    /**
     * Import a private key
     *
     * @param restExport The private key to import
     * @return The import result
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws FindException
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    @XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    @Path("import")
    public Response importPrivateKey(PrivateKeyRestExport restExport) throws ResourceFactory.ResourceNotFoundException, FindException, ResourceFactory.InvalidResourceException {
        PrivateKeyMO importResult = factory.importResource(restExport.getKeystoreID(), restExport.getAlias(), restExport.getPkcs12Data(), restExport.getPassword());
        return Response.ok().entity(importResult).build();
    }

    /**
     * Mark a private key as special purpose
     *
     * @param id      The keystore id
     * @param purpose The purpose to mark it with
     * @return The private key
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws FindException
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    @XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    @Path("specialPurpose")
    public Response markSpecialPurpose(@QueryParam("id") String id, @QueryParam("purpose") List<String> purpose) throws ResourceFactory.ResourceNotFoundException, FindException, ResourceFactory.InvalidResourceException {
        PrivateKeyMO resource = factory.setSpecialPurpose(id, purpose);
        return Response.ok().entity(transformer.convertToItem(resource)).build();
    }

    /**
     * Deletes an existing active connector.
     *
     * @param id The id of the active connector to delete.
     * @throws ResourceFactory.ResourceNotFoundException
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
    public Item<PrivateKeyMO> template() {
        return super.template();
    }
}

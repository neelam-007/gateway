package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.UserRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.*;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.CertificateTransformer;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.UserTransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.Link;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.*;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import org.glassfish.jersey.message.XmlHeader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.Provider;
import java.net.URI;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

/**
 * This resource handles user operations.
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + "users")
public class UserResource implements URLAccessible<UserMO> {

    protected static final String USERS_URI = "users";

    @SpringBean
    private UserRestResourceFactory userRestResourceFactory;

    @SpringBean
    private UserTransformer transformer;

    @SpringBean
    private CertificateTransformer certTransformer;

    @Context
    private UriInfo uriInfo;

    //The provider id to manage users for.
    private String providerId;

    public UserResource() {
        providerId = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID.toString();
    }

    /**
     * Creates a new user resource for handling user requests for the given provider id
     *
     * @param providerId The provider the users belongs to.
     */
    public UserResource(String providerId) {
        this.providerId = providerId;
    }

    /**
     * This will return a list of entity references. It will return a maximum of {@code count} references, it can return
     * fewer references if there are fewer then {@code count} entities found. Setting an offset will start listing
     * entities from the given offset. A sort can be specified to allow the resulting list to be sorted in either
     * ascending or descending order. Other params given will be used as search values. Examples:
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
     * @param offset The offset to start the listing from
     * @param count  The offset ot start the listing from
     * @param sort   the key to sort the list by.
     * @param order  the order to sort the list. true for ascending, false for descending. null implies ascending
     * @param logins The login filter
     * @return A list of entities. If the list is empty then no entities were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    //This xml header allows the list to be explorable when viewed in a browser
    //@XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    public ItemsList<UserMO> listResources(
            @QueryParam("offset") @DefaultValue("0") @NotEmpty Integer offset,
            @QueryParam("count") @DefaultValue("100") @NotEmpty Integer count,
            @QueryParam("sort") @ChoiceParam({"id", "login"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("login") List<String> logins) {
        ParameterValidationUtils.validateOffsetCount(offset, count);
        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParams(uriInfo.getQueryParameters(), Arrays.asList("login"));

        CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (logins != null && !logins.isEmpty()) {
            filters.put("login", (List) logins);
        }
        List<Item<UserMO>> items = Functions.map(userRestResourceFactory.listResources(providerId, offset, count, sort, ascendingSort, filters.map()), new Functions.Unary<Item<UserMO>, UserMO>() {
            @Override
            public Item<UserMO> call(UserMO resource) {
                return new ItemBuilder<>(transformer.convertToItem(resource))
                        .addLink(getLink(resource))
                        .build();
            }
        });
        return new ItemsListBuilder<UserMO>(EntityType.USER + " list", "List").setContent(items)
                .addLink(ManagedObjectFactory.createLink("self", uriInfo.getRequestUri().toString()))
                .build();
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
    public Response createResource(UserMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        String id = userRestResourceFactory.createResource(providerId, resource);
        UriBuilder ub = uriInfo.getAbsolutePathBuilder().path(id);
        final URI uri = ub.build();
        return Response.created(uri).entity(new ItemBuilder<>(
                transformer.convertToItem(resource))
                .setContent(null)
                .addLink(getLink(resource))
                .addLinks(getRelatedLinks(resource))
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
    public Item<UserMO> getResource(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException, FindException {
        UserMO user = userRestResourceFactory.getResource(providerId, id);
        return new ItemBuilder<>(transformer.convertToItem(user))
                .addLink(getLink(user))
                .addLinks(getRelatedLinks(user))
                .build();
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
    public Response updateResource(UserMO resource, @PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        boolean resourceExists = userRestResourceFactory.resourceExists(providerId, id);
        final Response.ResponseBuilder responseBuilder;
        if (resourceExists) {
            userRestResourceFactory.updateResource(providerId, id, resource);
            responseBuilder = Response.ok();
        } else {
            userRestResourceFactory.createResource(providerId, id, resource);
            responseBuilder = Response.created(uriInfo.getAbsolutePath());
        }
        return responseBuilder.entity(new ItemBuilder<>(
                transformer.convertToItem(resource))
                .setContent(null)
                .addLink(getLink(resource))
                .addLinks(getRelatedLinks(resource))
                .build()).build();
    }

    /**
     * Change this users password
     *
     * @param id       The id of the user
     * @param password The new password
     * @return The user that the password was changed for.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     * @throws FindException
     */
    @PUT
    @Path("{id}/changePassword")
    @XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    public Response changePassword(@PathParam("id") String id, String password) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException, FindException {
        userRestResourceFactory.changePassword(providerId, id, password);
        UserMO user = userRestResourceFactory.getResource(providerId, id);
        return Response.ok(new ItemBuilder<>(transformer.convertToItem(user))
                .addLink(getLink(user))
                .addLinks(getRelatedLinks(user))
                .build()).build();
    }

    /**
     * Deletes an existing user
     *
     * @param id The id of the user to delete.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @DELETE
    @Path("{id}")
    public void deleteResource(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        userRestResourceFactory.deleteResource(providerId, id);
    }

    /**
     * Set this user's certificate
     *
     * @param id       The id of the user
     * @param certificateId The trusted certificate id
     * @return The certificate set to
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     * @throws FindException
     */
    @PUT
    @Path("{id}/certificate")
    @XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    public Response setCertificate(@PathParam("id") String id, String certificateId) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException, ObjectModelException {
        X509Certificate cert  = userRestResourceFactory.setCertificate(providerId, id, certificateId);
        return Response.ok(certTransformer.getCertData(cert)).build();
    }


    /**
     * Gets the user's certificate
     * @param id    The id of the user
     * @return The certificate
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     * @throws ObjectModelException
     */
    @GET
    @Path("{id}/certificate")
    @XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    public Response getCertificate(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException, ObjectModelException {
        X509Certificate cert  = userRestResourceFactory.getCertificate(providerId, id);
        return Response.ok(certTransformer.getCertData(cert)).build();
    }

    /**
     * Removes the certificate from the user
     * @param id    The id of the user
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     * @throws ObjectModelException
     */
    @DELETE
    @Path("{id}/certificate")
    @XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    public void changeCertificate(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException, ObjectModelException {
        userRestResourceFactory.revokeCertificate(providerId, id);
    }

    @NotNull
    @Override
    public String getResourceType() {
        return EntityType.USER.toString();
    }

    @NotNull
    @Override
    public String getUrl(@NotNull final UserMO user) {
        return getUrlString(user.getProviderId(),user.getId());
    }

    @NotNull
    @Override
    public String getUrl(@NotNull final EntityHeader userHeader) {
        if(userHeader instanceof IdentityHeader){
            return getUrlString(((IdentityHeader)userHeader).getProviderGoid().toString(),userHeader.getStrId());
        }
        return getUrlString(providerId,userHeader.getStrId());
    }

    @NotNull
    @Override
    public Link getLink(@NotNull final UserMO user) {
        return ManagedObjectFactory.createLink("self", getUrl(user));
    }

    @NotNull
    @Override
    public List<Link> getRelatedLinks(@Nullable final UserMO user) {
        return Arrays.asList(
                ManagedObjectFactory.createLink("list", getUrlString(providerId,null)));
    }

    private String getUrlString(String providerId, @Nullable String userId) {
        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder()
                .path(IdentityProviderResource.class)
                .path(providerId)
                .path(USERS_URI);
        if (userId != null) {
            uriBuilder.path(userId);
        }
        return uriBuilder.build().toString();
    }
}

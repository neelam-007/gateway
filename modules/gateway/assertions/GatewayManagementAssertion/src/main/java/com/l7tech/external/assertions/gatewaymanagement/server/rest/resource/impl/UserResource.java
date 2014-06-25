package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.UserRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.*;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.CertificateTransformer;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.UserTransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.*;
import com.l7tech.util.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This resource handles user operations.
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + UserResource.USERS_URI)
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
    @NotNull
    private final String providerId;

    /**
     * Creates a user resource for handling group request for the internal identity provider
     */
    public UserResource() {
        providerId = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID.toString();
    }

    /**
     * Creates a new user resource for handling user requests for the given provider id
     *
     * @param providerId The provider the users belongs to.
     */
    public UserResource(@NotNull final String providerId) {
        this.providerId = providerId;
    }

    /**
     * This will return a list of entity references. Other params given will be used as search values. Examples:
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
     * @param sort   the key to sort the list by.
     * @param order  the order to sort the list. true for ascending, false for descending. null implies ascending
     * @param logins The login filter
     * @return A list of entities. If the list is empty then no entities were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    public ItemsList<UserMO> list(
            @QueryParam("sort") @ChoiceParam({"id", "login"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("login") List<String> logins) throws ResourceFactory.ResourceNotFoundException {

        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParamsIncludeDefaults(uriInfo.getQueryParameters(), Arrays.asList("login"));

        CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (logins != null && !logins.isEmpty()) {
            filters.put("login", (List) logins);
        }
        return RestEntityResourceUtils.createItemsList(
                userRestResourceFactory.listResources(sort, ascendingSort, providerId, filters.map()),
                transformer,
                this,
                uriInfo.getRequestUri().toString());
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
    public Response create(UserMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        userRestResourceFactory.createResource(providerId, resource);
        return RestEntityResourceUtils.createCreateOrUpdatedResponseItem(resource, transformer, this, true);
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
    public Item<UserMO> get(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException, FindException, ResourceFactory.InvalidResourceException {
        UserMO user = userRestResourceFactory.getResource(providerId, id);
        return RestEntityResourceUtils.createGetResponseItem(user, transformer, this);
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
    public Response update(UserMO resource, @PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        userRestResourceFactory.updateResource(providerId, id, resource);
        return RestEntityResourceUtils.createCreateOrUpdatedResponseItem(resource, transformer, this, false);
    }

    /**
     * Change this users password
     *
     * @param id       The id of the user
     * @param password The new password
     * @param format   The format of the password. "plain" or "sha512crypt"
     * @return The user that the password was changed for.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     * @throws FindException
     */
    @PUT
    @Path("{id}/password")
    public Item<UserMO> changePassword(@PathParam("id") String id, String password, @QueryParam("format") @DefaultValue("plain") String format) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException, FindException {
        userRestResourceFactory.changePassword(providerId, id, password, format);
        UserMO user = userRestResourceFactory.getResource(providerId, id);
        return RestEntityResourceUtils.createGetResponseItem(user, transformer, this);
    }

    /**
     * Deletes an existing user
     *
     * @param id The id of the user to delete.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @DELETE
    @Path("{id}")
    public void delete(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        userRestResourceFactory.deleteResource(providerId, id);
    }

    /**
     * Set this user's certificate
     *
     * @param id              The id of the user
     * @param certificateData The certificate data resource
     * @return The certificate set
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     * @throws FindException
     */
    @PUT
    @Path("{id}/certificate")
    public Item<CertificateData> setCertificate(@PathParam("id") String id, CertificateData certificateData) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException, ObjectModelException {
        X509Certificate cert = userRestResourceFactory.setCertificate(providerId, id, certificateData);
        CertificateData certificateDataOut = certTransformer.getCertData(cert);
        return new ItemBuilder<CertificateData>(certificateDataOut.getSubjectName() + " Certificate Data", id, getResourceType() + "CertificateData")
                .addLinks(getRelatedLinks(null))
                .setContent(certificateDataOut)
                .build();
    }


    /**
     * Gets the user's certificate
     *
     * @param id The id of the user
     * @return The certificate
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     * @throws ObjectModelException
     */
    @GET
    @Path("{id}/certificate")
    public Item<CertificateData> getCertificate(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException, ObjectModelException {
        X509Certificate cert = userRestResourceFactory.getCertificate(providerId, id);
        CertificateData certificateData = certTransformer.getCertData(cert);
        return new ItemBuilder<CertificateData>(certificateData.getSubjectName() + " Certificate Data", id, getResourceType() + "CertificateData")
                .addLinks(getRelatedLinks(null))
                .setContent(certificateData)
                .build();
    }

    /**
     * Removes the certificate from the user
     *
     * @param id The id of the user
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     * @throws ObjectModelException
     */
    @DELETE
    @Path("{id}/certificate")
    public void deleteCertificate(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException, ObjectModelException {
        userRestResourceFactory.revokeCertificate(providerId, id);
    }

    /**
     * This will return a template, example entity that can be used as a reference for what entity objects should look
     * like.
     *
     * @return The template entity.
     */
    @GET
    @Path("template")
    public Item<UserMO> template() {
        UserMO userMO = ManagedObjectFactory.createUserMO();
        userMO.setProviderId(providerId);
        userMO.setLogin("Login");
        return RestEntityResourceUtils.createTemplateItem(userMO, this, getUrlString(providerId, "template"));
    }

    @NotNull
    @Override
    public String getResourceType() {
        return EntityType.USER.toString();
    }

    @NotNull
    @Override
    public String getUrl(@NotNull final UserMO user) {
        return getUrlString(user.getProviderId(), user.getId());
    }

    @NotNull
    @Override
    public String getUrl(@NotNull final EntityHeader userHeader) {
        if (userHeader instanceof IdentityHeader) {
            return getUrlString(((IdentityHeader) userHeader).getProviderGoid().toString(), userHeader.getStrId());
        }
        return getUrlString(providerId, userHeader.getStrId());
    }

    @NotNull
    @Override
    public Link getLink(@NotNull final UserMO user) {
        return ManagedObjectFactory.createLink(Link.LINK_REL_SELF, getUrl(user));
    }

    @NotNull
    @Override
    public List<Link> getRelatedLinks(@Nullable final UserMO user) {
        List<Link> links = new ArrayList<>();
        links.add(ManagedObjectFactory.createLink(Link.LINK_REL_TEMPLATE, getUrlString(providerId, "template")));
        links.add(ManagedObjectFactory.createLink(Link.LINK_REL_LIST, getUrlString(providerId, null)));
        if (user != null) {
            links.add(ManagedObjectFactory.createLink("certificate", getUrl(user)));
            links.add(ManagedObjectFactory.createLink("provider", uriInfo.getBaseUriBuilder()
                    .path(IdentityProviderResource.class)
                    .path(providerId).build().toString()));
        }
        return links;
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

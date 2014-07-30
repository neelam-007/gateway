package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.IdentityProviderAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ChoiceParam;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ParameterValidationUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.IdentityProviderTransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.List;

/**
 * An Identity provider is used to store and provide identities.
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + IdentityProviderResource.identityProviders_URI)
@Singleton
public class IdentityProviderResource extends RestEntityResource<IdentityProviderMO, IdentityProviderAPIResourceFactory, IdentityProviderTransformer> {

    protected static final String identityProviders_URI = "identityProviders";

    @Context
    private ResourceContext resourceContext;

    @Override
    @SpringBean
    public void setFactory(IdentityProviderAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(IdentityProviderTransformer transformer) {
        super.transformer = transformer;
    }

    /**
     * Shows the users
     *
     * @param id The provider id. "default" for the default identity provider
     * @return The user resource for handling user requests.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     */
    @Path("{identityProviderID}/" + UserResource.USERS_URI)
    public UserResource users(@PathParam("identityProviderID") String id) throws ResourceFactory.ResourceNotFoundException {
        return resourceContext.initResource(new UserResource(resolveId(id)));
    }

    /**
     * Shows the groups
     *
     * @param id The provider id. "default" for the default identity provider
     * @return The group resource for handling group requests.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     */
    @Path("{identityProviderID}/" + GroupResource.GROUPS_URI)
    public GroupResource groups(@PathParam("identityProviderID") String id) throws ResourceFactory.ResourceNotFoundException {
        return resourceContext.initResource(new GroupResource(resolveId(id)));
    }

    /**
     * Creates an identity provider
     *
     * @param resource The identity provider to create
     * @return A reference to the newly created identity provider
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @POST
    public Response create(IdentityProviderMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return super.create(resource);
    }

    /**
     * Returns an identity provider with the given ID.
     *
     * @param id The ID of the identity provider to return
     * @return The identity provider.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{identityProviderID}")
    public Item<IdentityProviderMO> get(@PathParam("identityProviderID") String id) throws ResourceFactory.ResourceNotFoundException {
        return super.get(resolveId(id));
    }

    /**
     * <p>Returns a list of identity providers. Can optionally sort the resulting list in ascending or
     * descending order. Other params given will be used as search values.</p>
     * <p class="italicize">Examples:</p>
     * <div class="code indent"><pre>/restman/1.0/identityProviders?name=MyIDProvider</pre></div>
     * <p>Returns identity provider with name "MyIDProvider".</p>
     * <div class="code indent"><pre>/restman/1.0/identityProviders?type=LDAP</pre></div>
     * <p>Returns identity providers of LDAP type</p>
     * <p>If a parameter is not a valid search value a bad request error will be returned.</p>
     *
     * @param sort            Key to sort the list by
     * @param order           Sort order for the list; 'asc'=ascending, 'desc'=descending; defaults to
     *                        ascending if not specified
     * @param names           Name filter
     * @param types           Type filter
     * @param securityZoneIds Security zone ID filter
     * @return A list of entities. If the list is empty then no entities were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    public ItemsList<IdentityProviderMO> list(
            @QueryParam("sort") @ChoiceParam({"id", "name", "type"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("name") List<String> names,
            @QueryParam("type") @ChoiceParam({"LDAP", "Internal", "Federated", "Simple LDAP", "Policy-Backed"}) List<String> types,
            @QueryParam("securityZone.id") List<Goid> securityZoneIds) {
        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParamsIncludeDefaults(uriInfo.getQueryParameters(), Arrays.asList("name", "type", "securityZone.id"));

        CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (names != null && !names.isEmpty()) {
            filters.put("name", (List) names);
        }
        if (types != null && !types.isEmpty()) {
            filters.put("typeVal", (List) Functions.map(types, new Functions.Unary<Integer, String>() {
                @Override
                public Integer call(String s) {
                    if (s.equalsIgnoreCase("LDAP")) {
                        return IdentityProviderType.LDAP.toVal();
                    } else if (s.equalsIgnoreCase("Internal")) {
                        return IdentityProviderType.INTERNAL.toVal();
                    } else if (s.equalsIgnoreCase("Federated")) {
                        return IdentityProviderType.FEDERATED.toVal();
                    } else if (s.equalsIgnoreCase("Simple LDAP")) {
                        return IdentityProviderType.BIND_ONLY_LDAP.toVal();
                    } else if (s.equalsIgnoreCase("Policy-Backed")) {
                        return IdentityProviderType.POLICY_BACKED.toVal();
                    }
                    throw new IllegalArgumentException("Invalid parameter for identity provider type:" + s);
                }
            }));
        }
        if (securityZoneIds != null && !securityZoneIds.isEmpty()) {
            filters.put("securityZone.id", (List) securityZoneIds);
        }
        return super.list(convertSort(sort), ascendingSort,
                filters.map());
    }

    private String convertSort(String sort) {
        if (sort == null) return null;
        switch (sort) {
            case "type":
                return "typeVal";
            default:
                return sort;
        }
    }

    private String resolveId(String id) {
        if (id.equals("default")) {
            return IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID.toString();
        }
        return id;
    }


    /**
     * Creates or Updates an existing identity provider. If an identity provider with the given ID does not exist one
     * will be created, otherwise the existing one will be updated.
     *
     * @param resource Identity provider to create or update
     * @param id       ID of the identity provider to create or update
     * @return A reference to the newly created or updated identity provider.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    @Path("{identityProviderID}")
    public Response createOrUpdate(IdentityProviderMO resource, @PathParam("identityProviderID") String id) throws ResourceFactory.ResourceFactoryException {
        return super.update(resource, resolveId(id));
    }

    /**
     * Deletes an existing identity provider.
     *
     * @param id The ID of the identity provider to delete.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @DELETE
    @Path("{identityProviderID}")
    @Override
    public void delete(@PathParam("identityProviderID") String id) throws ResourceFactory.ResourceNotFoundException {
        super.delete(resolveId(id));
    }

    /**
     * Returns a template, which is an example identity provider that can be used as a reference for what identity
     * provider objects should look like.
     *
     * @return The template identity provider.
     */
    @GET
    @Path("template")
    public Item<IdentityProviderMO> template() {
        IdentityProviderMO identityProviderMO = ManagedObjectFactory.createIdentityProvider();
        identityProviderMO.setName("My New ID Provider");
        identityProviderMO.setIdentityProviderType(IdentityProviderMO.IdentityProviderType.BIND_ONLY_LDAP);
        identityProviderMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("certificateValidation", "Validate Certificate Path")
                .map());
        IdentityProviderMO.BindOnlyLdapIdentityProviderDetail detailsBindOnly = identityProviderMO.getBindOnlyLdapIdentityProviderDetail();
        detailsBindOnly.setServerUrls(Arrays.asList("server1", "server2"));
        detailsBindOnly.setUseSslClientAuthentication(true);
        detailsBindOnly.setBindPatternPrefix("prefix Pattern");
        detailsBindOnly.setBindPatternSuffix("suffix Pattern");
        return super.createTemplateItem(identityProviderMO);
    }

    @NotNull
    @Override
    public List<Link> getRelatedLinks(@Nullable final IdentityProviderMO identityProviderMO) {
        List<Link> links = super.getRelatedLinks(identityProviderMO);
        if (identityProviderMO != null) {
            links.add(ManagedObjectFactory.createLink("users", getUrlString(identityProviderMO.getId() + "/" + UserResource.USERS_URI)));
            links.add(ManagedObjectFactory.createLink("groups", getUrlString(identityProviderMO.getId() + "/" + GroupResource.GROUPS_URI)));
        }
        return links;
    }
}

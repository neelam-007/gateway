package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.IdentityProviderAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ChoiceParam;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ParameterValidationUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.IdentityProviderTransformer;
import com.l7tech.gateway.api.IdentityProviderMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemsList;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import org.glassfish.jersey.message.XmlHeader;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.List;

/**
 * The identity provider resource
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
    @Path("{id}/"+ UserResource.USERS_URI)
    public UserResource users(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return resourceContext.initResource(new UserResource(resolveId(id)));
    }

    /**
     * Shows the groups
     *
     * @param id The provider id. "default" for the default identity provider
     * @return The group resource for handling group requests.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     */
    @Path("{id}/groups")
    public GroupResource groups(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return resourceContext.initResource(new GroupResource(resolveId(id)));
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
    public Response create(IdentityProviderMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return super.create(resource);
    }

    /**
     * This implements the GET method to retrieve an entity by a given id.
     *
     * @param id The identity of the entity to select. "default" for the default identity provider
     * @return The selected entity.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{id}")
    public Item<IdentityProviderMO> get(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return super.get(resolveId(id));
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
     * @param names           The name filter
     * @param types           The type filter
     * @param securityZoneIds the securityzone id filter
     * @return A list of entities. If the list is empty then no entities were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    //This xml header allows the list to be explorable when viewed in a browser
    //@XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    public ItemsList<IdentityProviderMO> list(
            @QueryParam("sort") @ChoiceParam({"id", "name", "type"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("name") List<String> names,
            @QueryParam("type") @ChoiceParam({"LDAP", "Internal", "Federated", "Simple LDAP"}) List<String> types,
            @QueryParam("securityZone.id") List<Goid> securityZoneIds) {
        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParams(uriInfo.getQueryParameters(), Arrays.asList("name", "type", "securityZone.id"));

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
                    // TODO POLICY_BACKED
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

    private String resolveId(String id){
        if(id.equals("default")){
            return IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID.toString();
        }
        return id;
    }


    /**
     * Updates an existing entity
     *
     * @param resource The updated entity
     * @param id       The id of the entity to update. "default" for the default identity provider
     * @return a reference to the newly updated entity.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    @Path("{id}")
    @XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    public Response update(IdentityProviderMO resource, @PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return super.update(resource, resolveId(id));
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
        super.delete(resolveId(id));
    }

    /**
     * This will return a template, example entity that can be used as a reference for what entity objects should look
     * like.
     *
     * @return The template entity.
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
}

package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.FirewallRuleAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ChoiceParam;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ParameterValidationUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.FirewallRuleTransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.util.CollectionUtils;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.List;

/**
 * A firewall rule is a rule to accept or redirect traffic.  This is only available on hardware installations.
 */

@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + FirewallRuleResource.firewallRule_URI)
@Singleton
public class FirewallRuleResource extends RestEntityResource<FirewallRuleMO, FirewallRuleAPIResourceFactory, FirewallRuleTransformer> {

    protected static final String firewallRule_URI = "firewallRules";

    @Override
    @SpringBean
    public void setFactory(FirewallRuleAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(FirewallRuleTransformer transformer) {
        super.transformer = transformer;
    }

    /**
     * Creates a new firewall rule
     *
     * @param resource The firewall rule to create
     * @return A reference to the newly created firewall rule
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.InvalidResourceException
     */
    @POST
    public Response create(FirewallRuleMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        factory.isHardware();
        return super.create(resource);
    }

    /**
     * Returns a firewall rule with the given ID.
     *
     * @param id The ID of the firewall rule to return
     * @return The firewall rule.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{id}")
    public Item<FirewallRuleMO> get(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        factory.isHardware();
        return super.get(id);
    }

    /**
     * <p>Returns a list of firewall rules. Can optionally sort the resulting list in ascending or
     * descending order. Other params given will be used as search values.</p>
     * <p class="italicize">Examples:</p>
     * <div class="code indent"><pre>/restman/1.0/firewallRules?name=MyFirewallRule</pre></div>
     * <p>Returns firewall rule with name "MyFirewallRule".</p>
     * <p>If a parameter is not a valid search value a bad request error will be returned.</p>
     *
     * @param sort            Key to sort the list by
     * @param order           Sort order for the list; 'asc'=ascending, 'desc'=descending; defaults to
     *                        ascending if not specified
     * @param names           Name filter
     * @param enabled         Enabled filter
     * @param ordinal         Ordinal filter
     * @return A list of firewall rules. If the list is empty then no firewall rules were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    public ItemsList<FirewallRuleMO> list(
            @QueryParam("sort") @ChoiceParam({"id", "name", "enabled","ordinal"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("name") List<String> names,
            @QueryParam("enabled") Boolean enabled,
            @QueryParam("ordinal") List<Integer> ordinal) {

        factory.isHardware();

        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParamsIncludeDefaults(uriInfo.getQueryParameters(), Arrays.asList("name", "enabled", "ordinal"));

        CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (names != null && !names.isEmpty()) {
            filters.put("name", (List) names);
        }
        if (ordinal != null && !ordinal.isEmpty()) {
            filters.put("ordinal", (List) ordinal);
        }
        if (enabled != null) {
            filters.put("enabled", (List) Arrays.asList(enabled));
        }

        return super.list(sort, ascendingSort,
                filters.map());
    }


    /**
     * Creates or Updates an existing firewall rule. If a firewall rule with the given ID does not exist one
     * will be created, otherwise the existing one will be updated.
     *
     * @param resource Listen port to create or update
     * @param id       ID of the firewall rule to create or update
     * @return A reference to the newly created or updated firewall rule.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.InvalidResourceException
     */
    @PUT
    @Path("{id}")
    public Response update(FirewallRuleMO resource, @PathParam("id") String id) throws ResourceFactory.ResourceFactoryException {
        factory.isHardware();
        return super.update(resource, id);
    }

    /**
     * Deletes an existing firewall rule.
     *
     * @param id The ID of the firewall rule to delete.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     */
    @DELETE
    @Path("{id}")
    public void delete(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        factory.isHardware();
        super.delete(id);
    }

    /**
     * Returns a template, which is an example firewall rule that can be used as a reference for what firewall rule objects
     * should look like.
     *
     * @return The template firewall rule.
     */
    @GET
    @Path("template")
    public Item<FirewallRuleMO> template() {
        FirewallRuleMO emailListenerMO = ManagedObjectFactory.createFirewallRuleMO();
        emailListenerMO.setName("TemplateFirewallRule");
        emailListenerMO.setOrdinal(0);
        emailListenerMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder().put("RuleProperty", "PropertyValue").map());
        return super.createTemplateItem(emailListenerMO);
    }
}

package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.EmailListenerAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ChoiceParam;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ParameterValidationUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.EmailListenerTransformer;
import com.l7tech.gateway.api.EmailListenerMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemsList;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.common.transport.email.EmailListener;
import com.l7tech.gateway.common.transport.email.EmailServerType;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.CollectionUtils;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.List;

/* NOTE: The java docs in this class get converted to API documentation seen by customers!*/

/**
 * An email listener will periodically poll an email server for messages to process.
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + EmailListenerResource.emailListener_URI)
@Singleton
public class EmailListenerResource extends RestEntityResource<EmailListenerMO, EmailListenerAPIResourceFactory, EmailListenerTransformer> {

    protected static final String emailListener_URI = "emailListeners";

    @Override
    @SpringBean
    public void setFactory(EmailListenerAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(EmailListenerTransformer transformer) {
        super.transformer = transformer;
    }

    /**
     * Creates a new email listener
     *
     * @param resource The email listener to create
     * @return a reference to the newly email listener
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @POST
    public Response create(EmailListenerMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return super.create(resource);
    }

    /**
     * Returns an email listener with the given ID.
     *
     * @param id The ID of the email listener to return
     * @return The email listener.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{id}")
    public Item<EmailListenerMO> get(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return super.get(id);
    }

    /**
     * <p>Returns a list of email listeners. Can optionally sort the resulting list in ascending or
     * descending order. Other params given will be used as search values.</p>
     * <p class="italicize">Examples:</p>
     * <div class="code indent"><pre>/restman/1.0/emailListeners?name=MyEmailListener</pre></div>
     * <p>Returns email listener with name "MyEmailListener".</p>
     * <div class="code indent"><pre>/restman/1.0/emailListeners?serverTypes=IMAP</pre></div>
     * <p>Returns email listener of SFTP type</p>
     * <p>If a parameter is not a valid search value a bad request error will be returned.</p>
     *
     * @param sort            Key to sort the list by
     * @param order           Sort order for the list; 'true'=ascending, 'false'=descending; defaults to
     *                        ascending if not specified
     * @param names           Name filter
     * @param active          Active filter
     * @param serverTypes     Server type filter
     * @param securityZoneIds Security zone ID filter
     * @return A list of email listener. If the list is empty then no email listeners were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    public ItemsList<EmailListenerMO> list(
            @QueryParam("sort") @ChoiceParam({"id", "name", "host", "serverType"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("name") List<String> names,
            @QueryParam("active") Boolean active,
            @QueryParam("serverType") List<EmailServerType> serverTypes,
            @QueryParam("securityZone.id") List<Goid> securityZoneIds) {
        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParamsIncludeDefaults(uriInfo.getQueryParameters(), Arrays.asList("name", "active", "serverType", "securityZone.id"));

        CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (names != null && !names.isEmpty()) {
            filters.put("name", (List) names);
        }
        if (active != null) {
            filters.put("active", (List) Arrays.asList(active));
        }
        if (serverTypes != null && !serverTypes.isEmpty()) {
            filters.put("serverType", (List) serverTypes);
        }

        if (securityZoneIds != null && !securityZoneIds.isEmpty()) {
            filters.put("securityZone.id", (List) securityZoneIds);
        }
        return super.list(sort, ascendingSort,
                filters.map());
    }

    /**
     * Creates or Updates an existing email listener. If an email listener with the given ID does not exist one
     * will be created, otherwise the existing one will be updated.
     *
     * @param resource Email listener to create or update
     * @param id       ID of the email listener to create or update
     * @return A reference to the newly created or updated email listener.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    @Path("{id}")
    public Response createOrUpdate(EmailListenerMO resource, @PathParam("id") String id) throws ResourceFactory.ResourceFactoryException {
        return super.update(resource, id);
    }

    /**
     * Deletes an existing email listener.
     *
     * @param id The ID of the email listener to delete.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     */
    @DELETE
    @Path("{id}")
    @Override
    public void delete(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        super.delete(id);
    }

    /**
     * Returns a template, which is an example email listener that can be used as a reference for what email listener
     * objects should look like.
     *
     * @return The template email listener.
     */
    @GET
    @Path("template")
    public Item<EmailListenerMO> template() {
        EmailListenerMO emailListenerMO = ManagedObjectFactory.createEmailListener();
        emailListenerMO.setName("TemplateEmailListener");
        emailListenerMO.setHostname("hostName");
        emailListenerMO.setPort(1234);
        emailListenerMO.setActive(true);
        emailListenerMO.setUsername("username");
        emailListenerMO.setServerType(EmailListenerMO.EmailServerType.POP3);
        emailListenerMO.setFolder("INBOX");
        emailListenerMO.setPollInterval(1000);
        emailListenerMO.setUseSsl(false);
        emailListenerMO.setDeleteOnReceive(true);
        emailListenerMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                .put(EmailListener.PROP_HARDWIRED_SERVICE_ID, new Goid(123, 456).toString())
                .put(EmailListener.PROP_IS_HARDWIRED_SERVICE, Boolean.TRUE.toString()).map());
        return super.createTemplateItem(emailListenerMO);
    }
}

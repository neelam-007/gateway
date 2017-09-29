package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.AuditConfigurationAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResourceUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestManVersion;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.Since;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.AuditConfigurationTransformer;
import com.l7tech.gateway.api.AuditConfigurationMO;
import com.l7tech.gateway.api.AuditFtpConfig;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemsList;
import com.l7tech.gateway.api.Link;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.PolicyMO;
import com.l7tech.gateway.api.impl.ManagedObjectReference;
import com.l7tech.gateway.common.audit.AuditConfiguration;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import java.util.List;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This resource is used to manage audit configurations. These configurations are stored as cluster
 * properties in the gateway.
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + AuditConfigurationResource.AUDIT_CONFIGURATION_URI)
@Since( value= RestManVersion.VERSION_1_0_4)
@Singleton
public class AuditConfigurationResource extends RestEntityResource<AuditConfigurationMO, AuditConfigurationAPIResourceFactory, AuditConfigurationTransformer> {

    protected static final String AUDIT_CONFIGURATION_URI = "auditConfiguration";

    @Context
    private ResourceContext resourceContext;

    @Override
    @SpringBean
    public void setFactory(AuditConfigurationAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(AuditConfigurationTransformer transformer) {
        super.transformer = transformer;
    }

    /**
     * <p>Returns a list of the single audit configuration.<p>
     *
     * @return A list of audit configurations. There can only be one.
     */
    @SuppressWarnings("unchecked")
    @GET
    public ItemsList<AuditConfigurationMO> list() {
        return super.list(null, null, null);
    }

    /**
     * Returns a audit configuration with the given ID.
     *
     * @return The audit configuration.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("default")
    public Item<AuditConfigurationMO> get() throws ResourceFactory.ResourceNotFoundException {
        return super.get(AuditConfiguration.ENTITY_ID.toString());
    }


    /**
     * Returns the dependencies resource for the entity.
     *
     * @return The dependency resource that will resolve dependencies
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @Path("default/dependencies")
    public DependencyResource dependencies() throws ResourceFactory.ResourceNotFoundException {
        EntityHeader serviceHeader = new EntityHeader(AuditConfiguration.ENTITY_ID, EntityType.valueOf(getResourceType()), null, null);
        return resourceContext.initResource(new DependencyResource(serviceHeader));
    }

    /**
     * Updates an existing AuditConfiguration.
     * will be created, otherwise the existing one will be updated.
     *
     * @param resource AuditConfiguration to update
     * @return A reference to the updated AuditConfiguration.
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    @Path("default")
    public Response update(AuditConfigurationMO resource) throws ResourceFactory.ResourceFactoryException {

        factory.updateResource("", resource);
        return RestEntityResourceUtils.createCreateOrUpdatedResponseItem(resource, transformer, this, false);
    }

    /**
     * Returns a template, which is an example AuditConfiguration that can be used as a reference for what AuditConfiguration objects should
     * look like.
     *
     * @return The template AuditConfiguration.
     */
    @GET
    @Path("template")
    public Item<AuditConfigurationMO> template() {
        AuditConfigurationMO mo = ManagedObjectFactory.createAuditConfiguration();
        AuditFtpConfig ftpConfig = ManagedObjectFactory.createAuditFtpConfig();
        mo.setSinkPolicyReference(new ManagedObjectReference(PolicyMO.class, new Goid(23542,658568).toString()));
        mo.setLookupPolicyReference(new ManagedObjectReference(PolicyMO.class, new Goid(8546,23467).toString()));
        ftpConfig.setHost("Host");
        ftpConfig.setPort(123);
        ftpConfig.setTimeout(123);
        ftpConfig.setUser("user");
        ftpConfig.setPasswordValue("password");
        ftpConfig.setDirectory("directory");
        ftpConfig.setVerifyServerCert(true);
        ftpConfig.setSecurity(AuditFtpConfig.SecurityType.ftpsExplicit);
        ftpConfig.setEnabled(true);
        mo.setFtpConfig(ftpConfig);

        return super.createTemplateItem(mo);
    }

    @NotNull
    @Override
    public List<Link> getRelatedLinks(@Nullable final AuditConfigurationMO AuditConfiguration) {
        List<Link> links = super.getRelatedLinks(AuditConfiguration);
        if (AuditConfiguration != null) {
            links.add(ManagedObjectFactory.createLink("dependencies", getUrlString("default/dependencies")));
        }
        return links;
    }

    @NotNull
    @Override
    public Link getLink(@NotNull AuditConfigurationMO resource) {
        return ManagedObjectFactory.createLink(Link.LINK_REL_SELF, getUrlString("default"));
    }

    @NotNull
    @Override
    public String getUrl(@NotNull EntityHeader header) {
        return getUrlString("default");
    }
}

package com.l7tech.console.util;

import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.gateway.common.resources.HttpConfiguration;
import com.l7tech.gateway.common.resources.ResourceAdmin;
import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.keystore.SsgKeyMetadata;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.Policy;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for resolving names of entities.
 */
public class EntityNameResolver {
    private static final Logger logger = Logger.getLogger(EntityNameResolver.class.getName());
    private final ServiceAdmin serviceAdmin;
    private final PolicyAdmin policyAdmin;
    private final TrustedCertAdmin trustedCertAdmin;
    private final ResourceAdmin resourceAdmin;

    public EntityNameResolver(@NotNull final ServiceAdmin serviceAdmin,
                              @NotNull final PolicyAdmin policyAdmin,
                              @NotNull final TrustedCertAdmin trustedCertAdmin,
                              @NotNull final ResourceAdmin resourceAdmin) {
        this.serviceAdmin = serviceAdmin;
        this.policyAdmin = policyAdmin;
        this.trustedCertAdmin = trustedCertAdmin;
        this.resourceAdmin = resourceAdmin;
    }

    /**
     * Resolves a name for a given EntityHeader.
     * <p/>
     * If there is a non-empty name on the header, it will take precedence (this should happen in most cases).
     * <p/>
     * Otherwise name will be resolved by looking up the entity that is referenced by the header (this is usually the case is for entities that don't have a name).
     *
     * @param header the EntityHeader for which to determine a name. Usually a header that has just been retrieved via an Admin call.
     * @return a name for the given EntityHeader. Cannot be null.
     *         Can be empty if the name on the header is empty/null and the resolver does not know how to look up the referenced entity.
     * @throws FindException if a db error occurs or the entity referenced by the header does not exist.
     */
    @NotNull
    public String getNameForHeader(@NotNull final EntityHeader header) throws FindException {
        String name = header.getName();
        if (StringUtils.isBlank(name)) {
            final EntityType entityType = header.getType();
            switch (entityType) {
                case SERVICE_ALIAS:
                    final PublishedService owningService = serviceAdmin.findByAlias(header.getOid());
                    validateFoundEntity(header, owningService);
                    name = owningService.getName() + " alias";
                    break;
                case POLICY_ALIAS:
                    final Policy owningPolicy = policyAdmin.findByAlias(header.getOid());
                    validateFoundEntity(header, owningPolicy);
                    name = owningPolicy.getName() + " alias";
                    break;
                case SSG_KEY_METADATA:
                    final SsgKeyMetadata metadata = trustedCertAdmin.findKeyMetadata(header.getOid());
                    validateFoundEntity(header, metadata);
                    name = metadata.getAlias();
                    break;
                case RESOURCE_ENTRY:
                    final ResourceEntry resourceEntry = resourceAdmin.findResourceEntryByPrimaryKey(header.getOid());
                    validateFoundEntity(header, resourceEntry);
                    name = resourceEntry.getUri();
                    break;
                case HTTP_CONFIGURATION:
                    final HttpConfiguration httpConfig = resourceAdmin.findHttpConfigurationByPrimaryKey(header.getOid());
                    validateFoundEntity(header, httpConfig);
                    name = httpConfig.getProtocol() + " " + httpConfig.getHost() + " " + httpConfig.getPort();
                    break;
                default:
                    logger.log(Level.WARNING, "Name on header is null or empty but entity type is not supported: " + header.getType());
                    name = StringUtils.EMPTY;
            }
        }
        return name;
    }

    private void validateFoundEntity(final EntityHeader header, final Entity foundEntity) throws FindException {
        if (foundEntity == null) {
            throw new FindException("No entity found for type " + header.getType() + " and oid " + header.getOid());
        }
    }
}

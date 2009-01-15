package com.l7tech.server.migration;

import com.l7tech.objectmodel.migration.*;
import com.l7tech.objectmodel.*;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.service.ServiceDocumentManager;
import com.l7tech.gateway.common.service.ServiceDocument;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.reflect.Method;

/**
 * Handles the PublishedService -> ServiceDocument pseudo-dependency: null mapping is returned to reflect this,
 * but the dependency header (of the service documents) is discovered through this resolver.
 *
 * @author jbufu
 */
public class ServiceDocumentResolver extends AbstractPropertyResolver {
    private static final Logger logger = Logger.getLogger(AbstractOidPropertyResolver.class.getName());

    private ServiceDocumentManager documentManager;

    public ServiceDocumentResolver(PropertyResolverFactory factory, ServiceDocumentManager documentManager) {
        super(factory);
        this.documentManager = documentManager;
    }

    public final Map<EntityHeader, Set<MigrationDependency>> getDependencies(final EntityHeader source, Object entity, Method property, String propertyName) throws PropertyResolverException {
        logger.log(Level.FINEST, "Getting dependencies for property {0} of entity with header {1}.", new Object[]{property.getName(),source});

        final Long serviceOid;
        try {
            serviceOid = Long.parseLong((String) property.invoke(entity));
        } catch (Exception e) {
            throw new PropertyResolverException("Error getting property value for entity: " + entity, e);
        }

        Map<EntityHeader,Set<MigrationDependency>> result = new HashMap<EntityHeader, Set<MigrationDependency>>();
        try {
            for (ServiceDocument doc : documentManager.findByServiceId(serviceOid)) {
                    EntityHeader docHeader = EntityHeaderUtils.fromEntity(doc);
                    result.put(docHeader, Collections.<MigrationDependency>singleton(null));
                }
        } catch (FindException e) {
            logger.log(Level.FINE, "No service documents found for service: {0}.", serviceOid);
        }
        return result;
    }

    public void applyMapping(Object sourceEntity, String propName, EntityHeader targetHeader, Object targetValue, EntityHeader originalHeader) throws PropertyResolverException {
        // nothing to do here; this is an inverse dependency: the service entity is applied as a property of the service document
    }
}

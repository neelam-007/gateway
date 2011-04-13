package com.l7tech.server.entity;

import com.l7tech.gateway.common.entity.EntityAdmin;
import com.l7tech.server.PersistenceEventInterceptor;
import org.hibernate.SessionFactory;
import org.hibernate.metadata.ClassMetadata;

import java.util.*;

/**
 * The implementation of the Entity Admin API, EntityAdmin.
 *
 * @author ghuang
 */
public class EntityAdminImpl implements EntityAdmin {

    private final SessionFactory sessionFactory;
    private PersistenceEventInterceptor persistenceEventInterceptor;


    public EntityAdminImpl(final SessionFactory sessionFactory, final PersistenceEventInterceptor persistenceEventInterceptor) {
        this.sessionFactory = sessionFactory;
        this.persistenceEventInterceptor = persistenceEventInterceptor;
    }

    /**
     * Get all entity class names.
     *
     * @param includeIgnoredAndNotAudited: an indicator to decide if the list includes those ignored and not-audited entities, which are
     *               defined in PersistenceEventInterceptor.
     * @return: the list of entity class names.
     */
    @Override
    public Collection<String> getAllEntityClassNames(boolean includeIgnoredAndNotAudited) {
        Collection<String> entityClassNamesList = new ArrayList<String>();
        Map<String, ClassMetadata> classMetaDataMap = sessionFactory.getAllClassMetadata();

        if (includeIgnoredAndNotAudited) {
            entityClassNamesList.addAll(classMetaDataMap.keySet());
        } else {
            final Set<String> ignoredClassNames = persistenceEventInterceptor.getIgnoredClassNames();
            final Set<String> noAuditClassNames = persistenceEventInterceptor.getNoAuditClassNames();

            for (String className: classMetaDataMap.keySet()) {
                if (!ignoredClassNames.contains(className) && !noAuditClassNames.contains(className)) {
                    entityClassNamesList.add(className);
                }
            }
        }

        return entityClassNamesList;
    }
}
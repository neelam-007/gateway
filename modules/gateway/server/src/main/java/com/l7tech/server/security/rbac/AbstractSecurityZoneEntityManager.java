package com.l7tech.server.security.rbac;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.server.HibernateEntityManager;
import org.apache.commons.lang.Validate;

import java.util.Collection;

/**
 * An Entity Manager which can retrieve entities by SecurityZone.
 *
 * @param <ET> the type of PersistentEntity which can be retrieved.
 * @param <HT> the type of EntityHeader which can be retrieved.
 */
public abstract class AbstractSecurityZoneEntityManager<ET extends PersistentEntity, HT extends EntityHeader> extends HibernateEntityManager<ET, HT> implements SecurityZoneEntityManager<ET> {
    public AbstractSecurityZoneEntityManager() {
        validateEntityType();
    }

    public Collection<ET> findBySecurityZoneOid(final long securityZoneOid) {
        final String query = "from " + getTableName() +
                " in class " + getEntityType().getEntityClass().getName() +
                " where " + getTableName() + ".securityZone.oid = ?";
        return (Collection<ET>) getHibernateTemplate().find(query, securityZoneOid);
    }

    /**
     * Overridden in unit tests.
     */
    void validateEntityType() {
        Validate.isTrue(getEntityType().isSecurityZoneable(), "Entity Type must be zoneable.");
    }
}

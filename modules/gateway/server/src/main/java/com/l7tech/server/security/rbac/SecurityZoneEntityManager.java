package com.l7tech.server.security.rbac;

import com.l7tech.objectmodel.PersistentEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * An Entity Manager which can retrieve entities by SecurityZone.
 *
 * @param <ET> the type of PersistentEntity which can be retrieved.
 */
public interface SecurityZoneEntityManager<ET extends PersistentEntity> {
    /**
     * Retrieves a collection of ET by SecurityZone.
     *
     * @param securityZoneOid the oid of the SecurityZone.
     * @return a collection of ET which have the given SecurityZone identified by oid.
     */
    @NotNull
    public Collection<ET> findBySecurityZoneOid(final long securityZoneOid);
}

package com.l7tech.gateway.common.admin;

import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.PersistentEntity;
import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

/**
 * Remote admin interface which retrieves entities by SecurityZone.
 *
 * @param <ET> the type of PersistentEntity which can be retrieved.
 */
public interface SecurityZoneEntityAdmin<ET extends PersistentEntity> {
    /**
     * Retrieves a collection of ET by SecurityZone.
     *
     * @param securityZoneOid the oid of the SecurityZone.
     * @return a collection of ET which have the given SecurityZone identified by oid.
     */
    @Secured(stereotype= MethodStereotype.FIND_ENTITIES)
    @Transactional(readOnly = true)
    @Administrative
    @NotNull
    public Collection<ET> findBySecurityZoneOid(final long securityZoneOid);
}

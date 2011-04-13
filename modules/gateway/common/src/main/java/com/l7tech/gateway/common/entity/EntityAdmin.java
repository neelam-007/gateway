package com.l7tech.gateway.common.entity;

import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.EntityType;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

import static org.springframework.transaction.annotation.Propagation.REQUIRED;

/**
 * The Admin API manages entities.
 *
 * @author ghuang
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
@Secured(types= EntityType.ANY)
@Administrative
public interface EntityAdmin {
    /**
     * Get all entity class names.
     *
     * @param includeIgnoredAndNotAudited: an indicator to decide if the list includes those ignored and not-audited entities, which are
     *               defined in PersistenceEventInterceptor.
     * @return: the list of entity class names.
     */
    @Transactional(readOnly=true)
    Collection<String> getAllEntityClassNames(boolean includeIgnoredAndNotAudited);
}

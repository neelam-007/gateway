package com.l7tech.gateway.common.security.rbac;

import com.l7tech.objectmodel.Entity;

public interface ScopeEvaluator {
    /**
     * @param entity the Entity to evaluate against the predicate
     * @return true if this predicate matches the given entity.
     */
    boolean matches(Entity entity);
}

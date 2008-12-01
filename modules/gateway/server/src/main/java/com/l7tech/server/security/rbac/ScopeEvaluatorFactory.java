package com.l7tech.server.security.rbac;

import com.l7tech.gateway.common.security.rbac.ScopePredicate;
import com.l7tech.gateway.common.security.rbac.ScopeEvaluator;

public interface ScopeEvaluatorFactory<PT extends ScopePredicate> {
    ScopeEvaluator makeEvaluator(PT predicate);
}

/**
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy;

import com.l7tech.common.policy.Policy;
import com.l7tech.common.policy.PolicyType;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;

import java.util.Collection;

/**
 * @author alex
 */
public interface PolicyManager extends EntityManager<Policy, EntityHeader> {
    Collection<EntityHeader> findHeadersByType(PolicyType type) throws FindException;
}

/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion;

import com.l7tech.objectmodel.EntityHeader;

/**
 * Attached to an {@link Assertion} to indicate that it relies on one or more {@link com.l7tech.objectmodel.Entity}s to
 * exist in the database at policy enforcement time.
 * 
 * @author alex
 */
public interface UsesEntities {
    EntityHeader[] getEntitiesUsed();

    void replaceEntity(EntityHeader oldEntityHeader, EntityHeader newEntityHeader);
}

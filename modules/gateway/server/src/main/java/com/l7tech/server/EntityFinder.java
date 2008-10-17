/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;

/**
 * @author alex
 */
public interface EntityFinder {
    @Transactional(readOnly=true)
    EntityHeader[] findAll(Class<? extends Entity> entityClass) throws FindException;

    @Transactional(readOnly=true)
    Entity find(EntityHeader header) throws FindException;

    @Transactional(readOnly=true)
    <ET extends Entity> ET find(Class<ET> clazz, Serializable pk) throws FindException;

    @Transactional(readOnly=true)
    EntityHeader findHeader(EntityType etype, Serializable pk) throws FindException;
}

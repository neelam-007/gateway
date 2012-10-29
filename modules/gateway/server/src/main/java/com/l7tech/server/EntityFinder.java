/**
 * Copyright (C) 2006-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import com.l7tech.objectmodel.*;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;

/**
 * Generic read-only access to persistent entities.  
 *
 * @see EntityCrud
 * 
 * @author alex
 */
public interface EntityFinder {
    @Transactional(readOnly=true)
    EntityHeaderSet<EntityHeader> findAll(Class<? extends Entity> entityClass) throws FindException;

    @Transactional(readOnly=true)
    Entity find(EntityHeader header) throws FindException;

    @Transactional(readOnly=true)
    <ET extends Entity> ET find(Class<ET> clazz, Serializable pk) throws FindException;

    @Transactional(readOnly=true)
    EntityHeader findHeader(EntityType etype, Serializable pk) throws FindException;

    public class UnsupportedEntityTypeException extends RuntimeException {
        public UnsupportedEntityTypeException(final String s) {
            super(s);
        }
    }
}

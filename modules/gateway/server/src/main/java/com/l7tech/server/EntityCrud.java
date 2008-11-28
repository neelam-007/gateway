/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;

import java.io.Serializable;

/**
 * Generic CRUD access to persistent entities
 *
 * @author alex
 */
public interface EntityCrud extends EntityFinder {
    Serializable save(Entity e) throws SaveException;
    void update(Entity e) throws UpdateException;
    void delete(Entity e) throws DeleteException;
}

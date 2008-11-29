package com.l7tech.objectmodel;

import com.l7tech.objectmodel.folder.HasFolder;


/**
 * Implemented by any class which represents an alias instance of a real entity
 * Instances of this class are kept in the alias property of the real entity
 * Instance of this interface will be persisted. Each implementation will be persisted
 * in it's own db table.
 *
 * @author darmstrong
 */
public interface EntityAlias extends HasFolder {
    /*
    * entityOid is the entity oid of the real entity an instance of this interface
    * is aliasing
    * */
    public long getEntityOid();
}

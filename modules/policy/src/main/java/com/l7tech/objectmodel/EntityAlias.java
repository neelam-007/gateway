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

    /**
     * Get the type for the entity that is the target of the alias.
     *
     * @return The entity type.
     */
    EntityType getEntityType();

    /*
    * entityGoid is the entity goid of the real entity an instance of this interface
    * is aliasing
    * */
    Goid getEntityGoid();
}

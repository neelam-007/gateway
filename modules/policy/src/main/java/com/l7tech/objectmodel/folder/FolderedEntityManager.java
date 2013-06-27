package com.l7tech.objectmodel.folder;

import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.UpdateException;

/**
 * Extension of EntityManager interface with Folder specific methods.
 */
@Deprecated //to be replaced by com.l7tech.objectmodel.folder.FolderedGoidEntityManager
public interface FolderedEntityManager<ET extends PersistentEntity, HT extends EntityHeader> extends EntityManager<ET,HT> {

    void updateFolder( long entityId, Folder folder ) throws UpdateException;

    void updateFolder( ET entity, Folder folder ) throws UpdateException;

    void updateWithFolder( final ET entity ) throws UpdateException;
}

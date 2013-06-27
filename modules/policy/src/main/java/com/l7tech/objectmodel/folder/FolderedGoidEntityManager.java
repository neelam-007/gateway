package com.l7tech.objectmodel.folder;

import com.l7tech.objectmodel.*;

/**
 * Extension of EntityManager interface with Folder specific methods.
 */
public interface FolderedGoidEntityManager<ET extends GoidEntity, HT extends EntityHeader> extends GoidEntityManager<ET,HT> {

    void updateFolder(Goid goid, Folder folder) throws UpdateException;

    void updateFolder(ET entity, Folder folder) throws UpdateException;

    void updateWithFolder(final ET entity) throws UpdateException;
}

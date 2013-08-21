package com.l7tech.objectmodel.folder;

import com.l7tech.objectmodel.*;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Extension of EntityManager interface with Folder specific methods.
 */
public interface FolderedEntityManager<ET extends PersistentEntity, HT extends EntityHeader> extends EntityManager<ET,HT> {

    void updateFolder(Goid goid, Folder folder) throws UpdateException;

    void updateFolder(ET entity, Folder folder) throws UpdateException;

    void updateWithFolder(final ET entity) throws UpdateException;

    Collection<ET> findByFolder(@NotNull final Goid folderGoid) throws FindException;
}

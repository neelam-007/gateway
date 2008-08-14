package com.l7tech.console.tree;

import com.l7tech.objectmodel.folder.Folder;

/**
 * Interface for classes that can create service/policy folder tree nodes.
 */
public interface PolicyServiceTreeNodeCreator {
    public AbstractTreeNode createFolderNode(Folder folder);
}
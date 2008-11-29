package com.l7tech.console.tree.servicesAndPolicies;

import com.l7tech.objectmodel.folder.Folder;

import javax.swing.tree.MutableTreeNode;

public interface FolderNodeBase {
    long getOid();

    void insert(MutableTreeNode node, int insertPosition);

    int getInsertPosition(MutableTreeNode node);

    void remove(MutableTreeNode node);

    Folder getFolder();
}

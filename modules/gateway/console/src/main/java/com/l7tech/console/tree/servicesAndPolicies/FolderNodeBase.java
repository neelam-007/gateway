package com.l7tech.console.tree.servicesAndPolicies;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;

import javax.swing.tree.MutableTreeNode;

public interface FolderNodeBase {
    Goid getGoid();

    void insert(MutableTreeNode node, int insertPosition);

    int getInsertPosition(MutableTreeNode node);

    void remove(MutableTreeNode node);

    Folder getFolder();
}

package com.l7tech.console.tree.servicesAndPolicies;

import javax.swing.tree.MutableTreeNode;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 1-Aug-2008
 * Time: 10:30:39 PM
 * To change this template use File | Settings | File Templates.
 */
public interface FolderNodeBase {
    public long getOid();

    public void insert(MutableTreeNode node, int insertPosition);

    public int getInsertPosition(MutableTreeNode node);

    public void remove(MutableTreeNode node);
}

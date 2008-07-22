/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree;

import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.console.action.CreatePolicyAction;

import javax.swing.*;
import javax.swing.tree.MutableTreeNode;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;

/**
 * This class represents the lower-left CRUD folder for policy fragments.
 */
public class PoliciesFolderNode extends AbstractTreeNode {
    static Logger log = Logger.getLogger(PoliciesFolderNode.class.getName());

    public static final String NAME = "policiesFolderNode";

    private PolicyAdmin policyAdmin;
    private String title;

    private final Action[] allActions = new Action[]{
        new CreatePolicyAction(),
        new RefreshTreeNodeAction(this)
    };

    /**
     * construct the <CODE>ServicesFolderNode</CODE> instance for
     * a given service manager with the name.
     */
    public PoliciesFolderNode(PolicyAdmin pa, String name) {
        super(null, EntityHeaderNode.IGNORE_CASE_NAME_COMPARATOR);
        policyAdmin = pa;
        title = name;
    }

    /**
     * Returns true if the receiver is a leaf.
     *
     * @return true if leaf, false otherwise
     */
    public boolean isLeaf() {
        return false;
    }

    /**
     * Returns true if the receiver allows children.
     */
    public boolean getAllowsChildren() {
        return true;
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        // Filter unlicensed actions
        List<Action> actions = new ArrayList<Action>();
        for (Action action : allActions) {
            if (action.isEnabled())
                actions.add(action);
        }
        return actions.toArray(new Action[0]);
    }

    /**
     * load the service folder children
     */
    protected void loadChildren() {
        EntitiesEnumeration en = new EntitiesEnumeration(new PolicyEntitiesCollection(policyAdmin));
        Enumeration e = TreeNodeFactory.getTreeNodeEnumeration(en);
        children = null;
        for (; e.hasMoreElements();) {
            MutableTreeNode mn = (MutableTreeNode)e.nextElement();
            insert(mn, getInsertPosition(mn));
        }
    }

    /**
     * @return true as this node children can be refreshed
     */
    public boolean canRefresh() {
        return true;
    }

    /**
     * Returns the node name.
     * Gui nodes have name to facilitate handling in
     * components such as JTree.
     *
     * @return the name as a String
     */
    public String getName() {
        return title;
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/ServerRegistry.gif";
    }
}

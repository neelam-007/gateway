package com.l7tech.console.tree;

import com.l7tech.console.action.CreateServiceWsdlAction;
import com.l7tech.console.action.PublishServiceAction;
import com.l7tech.service.ServiceAdmin;

import javax.swing.*;
import javax.swing.tree.MutableTreeNode;
import java.util.Enumeration;
import java.util.logging.Logger;


/**
 * The class represents a node element in the TreeModel.
 * It represents the folder with services.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 */
public class ServicesFolderNode extends AbstractTreeNode {
    static Logger log = Logger.getLogger(ServicesFolderNode.class.getName());

    private ServiceAdmin serviceManager;
    private String title;

    /**
     * construct the <CODE>ServicesFolderNode</CODE> instance for
     * a given service manager with the name.
     */
    public ServicesFolderNode(ServiceAdmin sm, String name) {
        super(null, EntityHeaderNode.IGNORE_CASE_NAME_COMPARATOR);
        serviceManager = sm;
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
        return new Action[]{
            new PublishServiceAction(),
            new CreateServiceWsdlAction(),
            new RefreshTreeNodeAction(this)
        };
    }

    /**
     * load the service folder children
     */
    protected void loadChildren() {
        EntitiesEnumeration en = new EntitiesEnumeration(new ServiceEntitiesCollection(serviceManager));
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

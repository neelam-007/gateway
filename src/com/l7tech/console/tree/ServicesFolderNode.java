package com.l7tech.console.tree;

import com.l7tech.service.ServiceManager;
import com.l7tech.console.action.NewUserAction;
import com.l7tech.console.action.PublishServiceAction;

import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.*;
import java.util.Enumeration;


/**
 * The class represents a node element in the TreeModel.
 * It represents the folder with services.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.1
 */
public class ServicesFolderNode extends AbstractTreeNode {
    private ServiceManager serviceManager;
    private String title;
    private DefaultTreeModel model;


    /**
     * construct the <CODE>ServicesFolderNode</CODE> instance for
     * a given servcie manager with the name.
     */
    public ServicesFolderNode(ServiceManager sm, String name, DefaultTreeModel model) {
        super(null);
        serviceManager = sm;
        title = name;
        this.model = model;
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
        return new Action[]{new PublishServiceAction(this, model)};
    }

    /**
     * load the service folder children
     */
    protected void loadChildren() {
        Enumeration e =
          TreeNodeFactory.
          getTreeNodeEnumeration(
            new EntitiesEnumeration(new ServiceEntitiesCollection(serviceManager)));
        int index = 0;
        for (; e.hasMoreElements();) {
            insert((MutableTreeNode) e.nextElement(), index++);
        }
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
        if (open)
            return "com/l7tech/console/resources/folderOpen.gif";

        return "com/l7tech/console/resources/folder.gif";
    }

}

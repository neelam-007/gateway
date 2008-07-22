package com.l7tech.console.tree;

import com.l7tech.console.action.CreateServiceWsdlAction;
import com.l7tech.console.action.PublishServiceAction;
import com.l7tech.console.action.PublishNonSoapServiceAction;
import com.l7tech.console.action.PublishInternalServiceAction;
import com.l7tech.gateway.common.service.ServiceAdmin;

import javax.swing.*;
import javax.swing.tree.MutableTreeNode;
import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;
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

    public static final String NAME = "servicesFolderNode";
    
    private ServiceAdmin serviceManager;
    private String title;

    private final Action[] allActions = new Action[]{
        new PublishServiceAction(),
        new CreateServiceWsdlAction(),
        new PublishNonSoapServiceAction(),
        new PublishInternalServiceAction(),
        new RefreshTreeNodeAction(this)
    };

    /**
     * construct the <CODE>ServicesFolderNode</CODE> instance for
     * a given service manager with the name.
     */
    public ServicesFolderNode(ServiceAdmin sm, String name) {
        super(null, EntityHeaderNode.DEFAULT_COMPARATOR);
        serviceManager = sm;
        title = name;
    }

    /**
     * Returns true if the receiver is a leaf.
     *
     * @return true if leaf, false otherwise
     */
    @Override
    public boolean isLeaf() {
        return false;
    }

    /**
     * Returns true if the receiver allows children.
     */
    @Override
    public boolean getAllowsChildren() {
        return true;
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    @Override
    public Action[] getActions() {
        // Filter unlicensed actions
        List<Action> actions = new ArrayList<Action>();
        for (Action action : allActions) {
            if (action.isEnabled())
                actions.add(action);
        }
        return actions.toArray(new Action[actions.size()]);
    }

    /**
     * load the service folder children
     */
    @Override
    protected void loadChildren() {
        EntitiesEnumeration en = new EntitiesEnumeration(new ServiceEntitiesCollection(serviceManager));
        Enumeration e = TreeNodeFactory.getTreeNodeEnumeration(en);
        children = null;
        while (e.hasMoreElements()) {
            MutableTreeNode mn = (MutableTreeNode)e.nextElement();
            insert(mn, getInsertPosition(mn));
        }
    }

    /**
     * @return true as this node children can be refreshed
     */
    @Override
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

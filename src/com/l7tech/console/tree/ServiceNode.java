package com.l7tech.console.tree;

import com.l7tech.console.action.DeleteServiceAction;
import com.l7tech.console.action.ServicePolicyPropertiesAction;
import com.l7tech.console.tree.wsdl.WsdlTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.service.PublishedService;
import com.l7tech.service.Wsdl;

import javax.swing.*;
import javax.swing.tree.MutableTreeNode;
import java.io.StringReader;
import java.util.Enumeration;
import java.util.logging.Level;


/**
 * The class represents a node element in the TreeModel.
 * It represents the published service.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class ServiceNode extends EntityHeaderNode {
    private PublishedService svc;

    /**
     * construct the <CODE>ServiceNode</CODE> instance for
     * a given entity header.
     *
     * @param e  the EntityHeader instance, must represent published service
     * @exception IllegalArgumentException
     *                   thrown if unexpected type
     */
    public ServiceNode(EntityHeader e)
      throws IllegalArgumentException {
        super(e);
    }

    public PublishedService getPublishedService() throws FindException {
        if (svc == null) {
            EntityHeader eh = getEntityHeader();
            svc = Registry.getDefault().
              getServiceManager().findByPrimaryKey(eh.getOid());
        }
        return svc;
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        return new Action[]{
            new ServicePolicyPropertiesAction(this),
            new DeleteServiceAction(this)};
    }

    /**
     *Test if the node can be deleted. Default is <code>true</code>
     *
     * @return true if the node can be deleted, false otherwise
     */
    public boolean canDelete() {
        return true;
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
     * subclasses override this method
     */
    protected void loadChildren() {
        try {
            PublishedService s = getPublishedService();
            if (s != null) {
                Wsdl wsdl = Wsdl.newInstance(null, new StringReader(svc.getWsdlXml()));
                WsdlTreeNode node = WsdlTreeNode.newInstance(wsdl);
                node.loadChildren();
                int index = 0;
                for (Enumeration e = node.children(); e.hasMoreElements();) {
                    insert((MutableTreeNode) e.nextElement(), index++);
                }
            }
        } catch (Exception e) {
            ErrorManager.getDefault().
              notify(Level.WARNING, e,
              "Error loading service elements"+getEntityHeader().getOid());
        }
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        try {
            return getPublishedService().getName();
        } catch (FindException e) {
            ErrorManager.getDefault().notify(Level.WARNING, e, "Unable to find the service "+getEntityHeader().getOid());
        }
        return "Error Retreiving the service";
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/services16.png";
    }
}

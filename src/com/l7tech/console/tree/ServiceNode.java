package com.l7tech.console.tree;

import com.l7tech.common.xml.Wsdl;
import com.l7tech.console.action.*;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.tree.wsdl.WsdlTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.service.PublishedService;

import javax.swing.*;
import javax.swing.tree.MutableTreeNode;
import java.io.StringReader;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * The class represents a node element in the TreeModel.
 * It represents the published service.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class ServiceNode extends EntityHeaderNode {
    static final Logger log = Logger.getLogger(ServiceNode.class.getName());
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
        setAllowsChildren(true);
    }

    public PublishedService getPublishedService() throws FindException, RemoteException {
        if (svc == null) {
            EntityHeader eh = getEntityHeader();
            svc = Registry.getDefault().
              getServiceManager().findServiceByPrimaryKey(eh.getOid());
        }
        return svc;
    }

    /**
     * Nullify service,  will cause service reload next time.
     */
    public void clearServiceHolder() {
        svc = null;
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        DisableServiceAction da = new DisableServiceAction(this);
        da.setEnabled(false);
        EnableServiceAction ea = new EnableServiceAction(this);
        ea.setEnabled(false);

        try {
            boolean disabled = getPublishedService().isDisabled();
            da.setEnabled(!disabled);
            ea.setEnabled(disabled);
        } catch (Exception e) {
            log.log(Level.WARNING, "Error retrieving service", e);

        }

        return new Action[]{
            new EditServicePolicyAction(this),
            new ViewServiceWsdlAction(this),
            new EditServiceNameAction(this),
            ea,
            da,
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
     * subclasses override this method
     */
    protected void loadChildren() {
        try {
            PublishedService s = getPublishedService();
            if (s != null) {
                Wsdl wsdl = Wsdl.newInstance(null, new StringReader(svc.getWsdlXml()));
                WsdlTreeNode node = WsdlTreeNode.newInstance(wsdl);
                children = null;
                node.getChildCount();
                List nodes = Collections.list(node.children());
                int index = 0;
                for (Iterator iterator = nodes.iterator(); iterator.hasNext();) {
                    MutableTreeNode n = (MutableTreeNode)iterator.next();
                    insert(n, index++);
                }
            }
        } catch (Exception e) {
            ErrorManager.getDefault().
              notify(Level.SEVERE, e,
              "Error loading service elements"+getEntityHeader().getOid());
        }
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        try {
            return getPublishedService().getName();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Unable to find the service", e);
            // dont call the error manager here because this node is being refreshed constantly and it
            // causes some sort of error loop
            //ErrorManager.getDefault().notify(Level.SEVERE, e, "Unable to find the service "+getEntityHeader().getOid());
        }
        return "*invalid*";
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        if (svc == null || svc.isDisabled())
            return "com/l7tech/console/resources/services_disabled16.png";
        else
            return "com/l7tech/console/resources/services16.png";
    }
}

package com.l7tech.console.tree;

import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.console.action.*;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.tree.wsdl.WsdlTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.service.PublishedService;

import javax.swing.*;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
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
    private String serviceName = null;

    /**
     * construct the <CODE>ServiceNode</CODE> instance for
     * a given entity header.
     *
     * @param e the EntityHeader instance, must represent published service
     * @throws IllegalArgumentException thrown if unexpected type
     */
    public ServiceNode(EntityHeader e)
      throws IllegalArgumentException {
        super(e);
        setAllowsChildren(true);
    }

    public PublishedService getPublishedService() throws FindException, RemoteException {
        if (svc == null) {
            EntityHeader eh = getEntityHeader();
            svc = Registry.getDefault().getServiceManager().findServiceByID(eh.getOid());
            // throw something if null, the service may have been deleted
            if (svc == null) {
                TopComponents creg = TopComponents.getInstance();
                JTree tree = (JTree)creg.getComponent(ServicesTree.NAME);
                DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                model.removeNodeFromParent(this);
                throw new FindException("The service '"+eh.getName()+"' does not exist any more.");
            }
        }
        return svc;
    }

    /**
     * Nullify service,  will cause service reload next time.
     */
    public void clearServiceHolder() {
        svc = null;
        serviceName = null;
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        PublishedService ps = null;
        try {
            ps = getPublishedService();
        }
        catch (Exception e) {
            log.log(Level.WARNING, "Error retrieving service", e);
            return new Action[0];
        }
        if (ps == null) {
            log.warning("Cannot retrieve service");
            return new Action[0];
        }
        DisableServiceAction da = new DisableServiceAction(this);
        da.setEnabled(false);
        EnableServiceAction ea = new EnableServiceAction(this);
        ea.setEnabled(false);

        try {
            boolean disabled = ps.isDisabled();
            da.setEnabled(!disabled);
            ea.setEnabled(disabled);
        } catch (Exception e) {
            log.log(Level.WARNING, "Error retrieving service", e);

        }

        if (!svc.isSoap()) {
            return new Action[]{
                new EditServicePolicyAction(this),
                new EditServiceRoutingURIAction(this),
                new EditServiceNameAction(this),
                ea,
                da,
                new DeleteServiceAction(this)};
        } else {
            return new Action[]{
                new EditServicePolicyAction(this),
                new ViewServiceWsdlAction(this),
                new FeedNewWSDLToPublishedServiceAction(this),
                new EditServiceNameAction(this),
                ea,
                da,
                new DeleteServiceAction(this)};
        }
    }

    /**
     * Test if the node can be deleted. Default is <code>true</code>
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
            if (s != null && s.isSoap()) {
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
                "Error loading service elements" + getEntityHeader().getOid());
        }
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        return getServiceName();
    }

    private String getServiceName() {
        try {
            if (serviceName == null) {
                PublishedService ps = getPublishedService();
                if (ps != null) {
                    serviceName = getPublishedService().getName();
                } else {
                    return "deleted?";
                }
            }
            return serviceName;
        } catch (Exception e) {
            throw ExceptionUtils.wrap(e);
        }
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        if (svc == null || svc.isDisabled()) {
            if(svc.isSoap()) {
                return "com/l7tech/console/resources/services_disabled16.png";
            } else {
                return "com/l7tech/console/resources/xmlObject_disabled16.png";
            }
        }
        else {
            if(svc.isSoap()) {
                return "com/l7tech/console/resources/services16.png";
            } else {
                return "com/l7tech/console/resources/xmlObject16.gif";
            }
        }
    }
}

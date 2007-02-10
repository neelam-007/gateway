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
import javax.swing.tree.TreeNode;
import java.io.StringReader;
import java.rmi.RemoteException;
import java.util.*;
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
        try {
            setAllowsChildren(getPublishedService().isSoap());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public PublishedService getPublishedService() throws FindException, RemoteException {
        if (svc == null) {
            EntityHeader eh = getEntityHeader();
            svc = Registry.getDefault().getServiceManager().findServiceByID(eh.getStrId());
            // throw something if null, the service may have been deleted
            if (svc == null) {
                TopComponents creg = TopComponents.getInstance();
                JTree tree = (JTree)creg.getComponent(ServicesTree.NAME);
                if (tree !=null) {
                    DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                    Enumeration kids = this.getParent().children();
                    while (kids.hasMoreElements()) {
                        TreeNode node = (TreeNode) kids.nextElement();
                        if (node == this) {
                    model.removeNodeFromParent(this);
                            break;
                }
                    }
                }
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
        PublishedService ps;
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

        boolean s = svc.isSoap();
        boolean a = TopComponents.getInstance().isApplet();
        Collection<Action> actions = new ArrayList<Action>();

        actions.add(new EditServicePolicyAction(this));
        actions.add(new EditServiceProperties(this));
        if (s && !a) actions.add(new PublishPolicyToSystinetRegistry(this));
        actions.add(new DeleteServiceAction(this));

        return actions.toArray(new Action[0]);
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
        return !allowsChildren;
    }

    /**
     * subclasses override this method
     */
    protected void loadChildren() {
        try {
            PublishedService s = getPublishedService();
            if (s != null && s.isSoap()) {
                Wsdl wsdl = Wsdl.newInstance(Wsdl.extractBaseURI(s.getWsdlUrl()), new StringReader(svc.getWsdlXml()));
                WsdlTreeNode.Options opts = new WsdlTreeNode.Options();
                opts.setShowMessages(false);
                opts.setShowPortTypes(false);
                WsdlTreeNode node = WsdlTreeNode.newInstance(wsdl, opts);
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
                "Error accessing service id=" + getEntityHeader().getOid());
        }
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        String nodeName = getServiceName();
        try {
            PublishedService ps = getPublishedService();
            if (ps != null) {
                nodeName = ps.displayName();
            }
        } catch (Exception e) {
            ErrorManager.getDefault().
              notify(Level.SEVERE, e,
                "Error accessing service id=" + getEntityHeader().getOid());
        }
        return nodeName;
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

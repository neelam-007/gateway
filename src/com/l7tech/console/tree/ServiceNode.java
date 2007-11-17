package com.l7tech.console.tree;

import com.l7tech.common.policy.Policy;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.console.action.DeleteServiceAction;
import com.l7tech.console.action.EditServiceProperties;
import com.l7tech.console.action.PublishPolicyToUDDIRegistry;
import com.l7tech.console.action.EditPolicyAction;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.tree.wsdl.WsdlTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.service.PublishedService;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import java.io.StringReader;
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
public class ServiceNode extends PolicyEntityNode {
    static final Logger log = Logger.getLogger(ServiceNode.class.getName());
    private PublishedService svc;

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

    public PublishedService getPublishedService() throws FindException {
        return svc != null ? svc : (svc = refreshPublishedService());
    }

    @Override
    public Policy getPolicy() throws FindException {
        return getPublishedService().getPolicy();
    }

    @Override
    public Entity getEntity() throws FindException {
        return getPublishedService();
    }

    /**
     * Refresh info from server, including checking for deleted service.
     * If the service has been deleted, this will prune it from the services tree before returning.
     *
     * @throws com.l7tech.objectmodel.FindException  if unable to find service, possibly because it was deleted
     * @return the published service.  Never null.
     */
    public PublishedService refreshPublishedService() throws FindException {
        EntityHeader eh = getEntityHeader();
        svc = Registry.getDefault().getServiceManager().findServiceByID(eh.getStrId());
        // throw something if null, the service may have been deleted
        if (svc == null) {
            TopComponents creg = TopComponents.getInstance();
            JTree tree = (JTree)creg.getComponent(ServicesAndPoliciesTree.NAME);
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
        EntityHeader newEh = new EntityHeader(svc.getId(), eh.getType(), svc.getName(), svc.getName());
        setUserObject(newEh);
        firePropertyChange(this, "UserObject", eh, newEh);
        return svc;
    }

    /**
     * Nullify service,  will cause service reload next time.
     */
    public void clearCachedEntities() {
        super.clearCachedEntities();
        svc = null;
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        final Collection<Action> actions = new ArrayList<Action>();

        PublishedService ps;
        try {
            ps = getPublishedService();
        } catch (Exception e) {
            log.log(Level.WARNING, "Error retrieving service", e);
            return new Action[0];
        }

        if (ps == null) {
            log.warning("Cannot retrieve service");
            return new Action[0];
        }

        actions.add(new EditPolicyAction(this));
        actions.add(new DeleteServiceAction(this));
        actions.add(new EditServiceProperties(this));
        if (svc.isSoap() && !TopComponents.getInstance().isApplet()) actions.add(new PublishPolicyToUDDIRegistry(this));
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
                //noinspection unchecked
                List<MutableTreeNode> nodes = Collections.list(node.children());
                int index = 0;
                for (MutableTreeNode n : nodes) {
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
        String nodeName = getEntityName();
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

    protected String getEntityName() {
        return getEntityHeader().getName();
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        if (svc == null) {
            return "com/l7tech/console/resources/services_disabled16.png";
        }
        else if (svc.isDisabled()) {
            if (svc == null || !svc.isSoap()) {
                return "com/l7tech/console/resources/xmlObject_disabled16.png";
            } else {
                return "com/l7tech/console/resources/services_disabled16.png";
            }
        } else {
            if(svc.isSoap()) {
                return "com/l7tech/console/resources/services16.png";
            } else {
                return "com/l7tech/console/resources/xmlObject16.gif";
            }
        }
    }
}

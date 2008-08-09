package com.l7tech.console.tree;

import com.l7tech.console.action.*;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.tree.wsdl.WsdlTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.Policy;
import com.l7tech.wsdl.Wsdl;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
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
public class ServiceNode extends EntityWithPolicyNode<PublishedService, ServiceHeader> implements Comparable<ServiceNode> {
    static final Logger log = Logger.getLogger(ServiceNode.class.getName());
    private PublishedService svc;

    /**
     * construct the <CODE>ServiceNode</CODE> instance for
     * a given entity header.
     *
     * @param e the EntityHeader instance, must represent published service
     * @throws IllegalArgumentException thrown if unexpected type
     */
    public ServiceNode(ServiceHeader e)
      throws IllegalArgumentException {
        super(e);
        setAllowsChildren(e.isSoap());
    }

    public PublishedService getPublishedService() throws FindException {
        if (svc != null) return svc;

        ServiceHeader serviceHeader = getEntityHeader();
        PublishedService svc = Registry.getDefault().getServiceManager().findServiceByID(serviceHeader.getStrId());
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
            throw new FindException("The service '"+serviceHeader.getName()+"' does not exist any more.");
        }

        this.svc = svc;

        ServiceHeader newEh = new ServiceHeader(svc);
        setUserObject(newEh);
        firePropertyChange(this, "UserObject", serviceHeader, newEh);
        return svc;
    }

    @Override
    public Policy getPolicy() throws FindException {
        return getPublishedService().getPolicy();
    }

    @Override
    public PublishedService getEntity() throws FindException {
        return getPublishedService();
    }

    /**
     * Nullify service,  will cause service reload next time.
     */
    @Override
    public void clearCachedEntities() {
        svc = null;
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    @Override
    public Action[] getActions() {
        final Collection<Action> actions = new ArrayList<Action>();

        actions.add(new EditPolicyAction(this));
        actions.add(new EditServiceProperties(this));
        if (getEntityHeader().isSoap() && !TopComponents.getInstance().isApplet()) actions.add(new PublishPolicyToUDDIRegistry(this));
        actions.add(new DeleteServiceAction(this));
        actions.add(new PolicyRevisionsAction(this));

        return actions.toArray(new Action[actions.size()]);
    }

    /**
     * Returns true if the receiver is a leaf.
     *
     * @return true if leaf, false otherwise
     */
    @Override
    public boolean isLeaf() {
        return !allowsChildren;
    }

    /**
     * subclasses override this method
     */
    @Override
    protected void loadChildren() {
        try {
            PublishedService s = getPublishedService();
            if (s != null && s.isSoap()) {
                Wsdl wsdl = svc.parsedWsdl();
//                Wsdl wsdl = Wsdl.newInstance(Wsdl.extractBaseURI(s.getWsdlUrl()), new StringReader(svc.getWsdlXml()));
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
    @Override
    public String getName() {
        return getEntityHeader().getDisplayName();
    }

    @Override
    protected String getEntityName() {
        return getEntityHeader().getName();
    }

    /**
     *
     */
    public int compareTo( ServiceNode serviceNode ) {
        return getName().toLowerCase().compareTo( serviceNode.getName().toLowerCase() );
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    @Override
    protected String iconResource(boolean open) {
        ServiceHeader header = getEntityHeader();
        if (header == null) {
            return "com/l7tech/console/resources/services_disabled16.png";
        }
        else if (header.isDisabled()) {
            if (!header.isSoap()) {
                return "com/l7tech/console/resources/xmlObject_disabled16.png";
            } else {
                return "com/l7tech/console/resources/services_disabled16.png";
            }
        } else {
            if(header.isSoap()) {
                return "com/l7tech/console/resources/services16.png";
            } else {
                return "com/l7tech/console/resources/xmlObject16.gif";
            }
        }
    }

}

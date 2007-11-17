/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree;

import com.l7tech.common.policy.Policy;
import com.l7tech.console.action.EditPolicyAction;
import com.l7tech.console.action.EditPolicyProperties;
import com.l7tech.console.action.DeletePolicyAction;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class represents Policy nodes in the lower-left policy CRUD tree
 */
public class PolicyEntityNode extends EntityHeaderNode {
    static final Logger log = Logger.getLogger(PolicyEntityNode.class.getName());
    protected Policy policy;

    /**
     * construct the <CODE>PolicyEntityNode</CODE> instance for
     * a given entity header.
     *
     * @param e the EntityHeader instance, must represent published service
     * @throws IllegalArgumentException thrown if unexpected type
     */
    public PolicyEntityNode(EntityHeader e)
      throws IllegalArgumentException {
        super(e);
    }

    public Policy getPolicy() throws FindException {
        return policy != null ? policy : (policy = refreshPolicy());
    }

    public Entity getEntity() throws FindException, RemoteException {
        return getPolicy();
    }

    /**
     * Refresh info from server, including checking for deleted service.
     * If the service has been deleted, this will prune it from the services tree before returning.
     *
     * @throws com.l7tech.objectmodel.FindException  if unable to find service, possibly because it was deleted
     * @throws java.rmi.RemoteException  on remote communication error
     * @return the published service.  Never null.
     */
    public Policy refreshPolicy() throws FindException {
        EntityHeader eh = getEntityHeader();
        policy = Registry.getDefault().getPolicyAdmin().findPolicyByPrimaryKey(eh.getOid());
        // throw something if null, the service may have been deleted
        if (policy == null) {
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
            throw new FindException("The policy for '"+eh.getName()+"' does not exist any more.");
        }

        EntityHeader newEh = new EntityHeader(policy.getId(), eh.getType(), policy.getName(), policy.getName());
        setUserObject(newEh);
        firePropertyChange(this, "UserObject", eh, newEh);
        return policy;
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        Collection<Action> actions = new ArrayList<Action>();

        actions.add(new EditPolicyAction(this));
        actions.add(new EditPolicyProperties(this));
        actions.add(new DeletePolicyAction(this));
        // TODO find usages...

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
     * @return the node name that is displayed
     */
    public String getName() {
        try {
            String nodeName = getPolicy().getName();
            if (nodeName == null) nodeName = getEntityName();
            return nodeName;
        } catch (Exception e) {
            ErrorManager.getDefault().
              notify(Level.SEVERE, e,
                "Error accessing policy entity");
            return null;
        }
    }

    protected String getEntityName() {
        return policy.getName();
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/include16.png";
    }

    public void clearCachedEntities() {
        policy = null;
    }
}

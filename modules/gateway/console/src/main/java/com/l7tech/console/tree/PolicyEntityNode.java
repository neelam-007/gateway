/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree;

import com.l7tech.console.action.DeletePolicyAction;
import com.l7tech.console.action.EditPolicyAction;
import com.l7tech.console.action.EditPolicyProperties;
import com.l7tech.console.action.PolicyRevisionsAction;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.logging.Level;

/** @author alex */
public class PolicyEntityNode extends EntityWithPolicyNode<Policy, PolicyHeader> {
    protected volatile Policy policy;

    public PolicyEntityNode(PolicyHeader e) {
        super(e);
    }

    public Policy getPolicy() throws FindException {
        if (policy != null) return policy;

        PolicyHeader eh = getEntityHeader();
        Policy policy = Registry.getDefault().getPolicyAdmin().findPolicyByGuid(eh.getGuid());
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

        this.policy = policy;

        EntityHeader newEh = new PolicyHeader(policy);
        setUserObject(newEh);
        firePropertyChange(this, "UserObject", eh, newEh);

        return policy;
    }

    public Policy getEntity() throws FindException {
        return getPolicy();
    }

    @Override
    public Action[] getActions() {
        Collection<Action> actions = new ArrayList<Action>();
        actions.add(new EditPolicyAction(this));
        actions.add(new EditPolicyProperties(this));
        actions.add(new DeletePolicyAction(this));
        actions.add(new PolicyRevisionsAction(this));
        return actions.toArray(new Action[0]);
    }

    protected String getEntityName() {
        return policy.getName();
    }

    public void clearCachedEntities() {
        policy = null;
    }

    @Override
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

    @Override
    protected String iconResource(boolean open) {
        boolean isSoap;
        boolean isInternal;
        try {
            isSoap = getPolicy().isSoap();
            isInternal = getPolicy().getType() == PolicyType.INTERNAL;
        } catch (Exception e) {
            ErrorManager.getDefault().
              notify(
                  Level.SEVERE, e,
                "Error accessing policy entity");
            return "com/l7tech/console/resources/include16.png";
        }

        if (isInternal) {
            if (isSoap) return "com/l7tech/console/resources/include_internalsoap16.png";
            else return "com/l7tech/console/resources/include_internal16.png";
        } else {
            if (isSoap) return "com/l7tech/console/resources/include_soap16.png";
            else return "com/l7tech/console/resources/include16.png";
        }
    }
}

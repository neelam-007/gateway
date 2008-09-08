/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.util.Functions;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.console.panels.WorkSpacePanel;
import com.l7tech.console.panels.HomePagePanel;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.tree.PolicyEntityNode;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.EntityWithPolicyNode;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyDeletionForbiddenException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.gui.util.DialogDisplayer;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Set;

/**
 * The <code>DeletePolicyAction</code> action deletes a {@link com.l7tech.policy.Policy}.
 */
public final class DeletePolicyAction extends DeleteEntityNodeAction<PolicyEntityNode> {
    static final Logger log = Logger.getLogger(DeletePolicyAction.class.getName());

    /**
     * create the action that deletes the policy
     *
     * @param en the node to delete
     */
    public DeletePolicyAction(PolicyEntityNode en) {
        super(en);
    }

    protected OperationType getOperation() {
        return OperationType.DELETE;
    }

    /**
     * @return the action name
     */
    @Override
    public String getName() {
        return "Delete Policy";
    }

    /**
     * @return the aciton description
     */
    @Override
    public String getDescription() {
        return "Delete the Policy";
    }

    /**
     * subclasses override this method specifying the resource name
     */
    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/delete.gif";
    }

    public boolean deleteEntity(){
        final PolicyAdmin policyAdmin = Registry.getDefault().getPolicyAdmin();
        Object userObj = node.getUserObject();
        if(!(userObj instanceof PolicyHeader)) return false;

        PolicyHeader pH = (PolicyHeader) userObj;
        try {
            policyAdmin.deletePolicy(pH.getOid());
            return true;
        }catch (ObjectModelException ome) {
            PolicyDeletionForbiddenException pdfe = ExceptionUtils.getCauseIfCausedBy(ome, PolicyDeletionForbiddenException.class);
            String msg;
            if (pdfe != null) {
                msg = node.getName() + " cannot be deleted at this time; it is still in use by another policy";
            } else {
                msg = "Error encountered while deleting " +
                        node.getName() +
                        ". Please try again later.";
            }
            log.log(Level.WARNING, "Error deleting policy", ome);
            DialogDisplayer.showMessageDialog(Actions.getTopParent(),
                    msg,
                    "Delete Policy",
                    JOptionPane.ERROR_MESSAGE, null);
        } catch (Throwable throwable) {
            ErrorManager.getDefault().notify(Level.WARNING, throwable, "Error deleting policy");
        }
        return false;
    }

    public String getUserConfirmationMessage() {
        return "Are you sure you want to delete the " + node.getName() + " policy?";
    }

    public String getUserConfirmationTitle() {
        return "Delete Policy";
    }
}

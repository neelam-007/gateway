/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.console.tree.PolicyEntityNodeAlias;
import com.l7tech.console.util.Registry;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyAlias;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.gui.util.DialogDisplayer;

import javax.swing.*;
import java.util.logging.Logger;
import java.util.logging.Level;

public class DeletePolicyAliasAction extends DeleteEntityNodeAction<PolicyEntityNodeAlias> {
    static final Logger log = Logger.getLogger(DeletePolicyAction.class.getName());

    /**
     * create the action that deletes the policy
     *
     * @param en the node to delete
     */
    public DeletePolicyAliasAction(PolicyEntityNodeAlias en) {
        super(en);
    }

    /**
     * Create the action that deletes the policy
     * @param en the node to delete
     * @param confirmationEnabled: to check if a deletion confirmation is needed or not.
     */
    public DeletePolicyAliasAction(PolicyEntityNodeAlias en, boolean confirmationEnabled) {
        super(en, confirmationEnabled);
    }

    protected OperationType getOperation() {
        return OperationType.DELETE;
    }

    /**
     * @return the action name
     */
    @Override
    public String getName() {
        return "Delete Policy Alias";
    }

    /**
     * @return the aciton description
     */
    @Override
    public String getDescription() {
        return "Delete Policy Alias";
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
            //delete the alias, leaving the original service alone
            //what alias does this node represent? Need it's policy id and folder id to find out
            //pH's folder id has been modified to point at the folder containing the alias
            PolicyAlias pa = policyAdmin.findAliasByEntityAndFolder(pH.getGoid(), pH.getFolderGoid());
            if(pa != null){
                policyAdmin.deleteEntityAlias((Goid.toString(pa.getGoid())));
            }else{
                DialogDisplayer.showMessageDialog(Actions.getTopParent(),
                  "Cannot find alias to delete",
                  "Delete Policy Alias",
                  JOptionPane.ERROR_MESSAGE, null);
            }
            return true;
        }catch (ObjectModelException ome) {
            String msg = "Error encountered while deleting " +
                        node.getName() +
                        ". Please try again later.";
            log.log(Level.WARNING, "Error deleting policy alias", ome);
            DialogDisplayer.showMessageDialog(Actions.getTopParent(),
                    msg,
                    "Delete Policy Alias",
                    JOptionPane.ERROR_MESSAGE, null);
        } catch (Throwable throwable) {
            ErrorManager.getDefault().notify(Level.WARNING, throwable, "Error deleting policy alias");
        }
        return false;
    }

    public String getUserConfirmationTitle() {
        return "Delete Policy Alias";
    }
}


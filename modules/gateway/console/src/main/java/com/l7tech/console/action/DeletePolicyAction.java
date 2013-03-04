/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.tree.PolicyEntityNode;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.NamedEntity;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.policy.PolicyDeletionForbiddenException;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    /**
     * Create the action that deletes the policy
     * @param en the node to delete
     * @param confirmationEnabled: to check if a deletion confirmation is needed or not.
     */
    public DeletePolicyAction(PolicyEntityNode en, boolean confirmationEnabled) {
        super(en, confirmationEnabled);
    }

    @Override
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

    @Override
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
                msg = pdfeMessage(pdfe);
            } else {
                msg = "Error encountered while deleting " +
                        node.getName() +
                        ". Please try again later.";
            }
            showDeleteError(ome, msg);
        } catch (PolicyDeletionForbiddenException e) {
            showDeleteError(e, pdfeMessage(e));
        } catch (Throwable throwable) {
            ErrorManager.getDefault().notify(Level.WARNING, throwable, "Error deleting policy");
        }
        return false;
    }

    private void showDeleteError(Throwable e, String msg) {
        log.log(Level.WARNING, "Error deleting policy: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        DialogDisplayer.showMessageDialog(Actions.getTopParent(),
                    msg,
                    "Delete Policy",
                JOptionPane.ERROR_MESSAGE, null);
    }

    private String pdfeMessage(PolicyDeletionForbiddenException pdfe) {
        if (EntityType.POLICY.equals(pdfe.getReferringEntityType()))
            return node.getName() + " cannot be deleted at this time; it is still in use by another policy";
        if (EntityType.ENCAPSULATED_ASSERTION.equals(pdfe.getReferringEntityType())) {
            String encassMsg = node.getName() + " cannot be deleted at this time; it is still in use as the underlying policy fragment for an encapsulated assertion";
            final Entity referringEntity = pdfe.getReferringEntity();
            if (referringEntity != null && referringEntity instanceof NamedEntity) {
                final NamedEntity named = (NamedEntity) referringEntity;
                encassMsg = encassMsg + " (" + named.getName() + ")";
            }
            return encassMsg;
        }
        return pdfe.getMessage() != null && pdfe.getMessage().contains(" trace ") ?
                node.getName() + " cannot be deleted at this time: debug tracing is still enabled on at least one service" :
                node.getName() + " cannot be deleted at this time; it is still in use as the audit sink policy";
    }

    @Override
    public String getUserConfirmationTitle() {
        return "Delete Policy";
    }
}

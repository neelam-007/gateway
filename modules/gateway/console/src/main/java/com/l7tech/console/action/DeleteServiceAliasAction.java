/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.console.tree.ServiceNodeAlias;
import com.l7tech.console.util.Registry;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.ObjectModelException;

import javax.swing.*;
import java.util.logging.Logger;
import java.util.logging.Level;

public class DeleteServiceAliasAction extends DeleteEntityNodeAction<ServiceNodeAlias> {
    static final Logger log = Logger.getLogger(DeletePolicyAction.class.getName());

    /**
     * create the action that deletes the policy
     *
     * @param en the node to delete
     */
    public DeleteServiceAliasAction(ServiceNodeAlias en) {
        super(en);
    }

    /**
     * Create the action that deletes the policy
     * @param en the node to delete
     * @param confirmationEnabled: to check if a deletion confirmation is needed or not.
     */
    public DeleteServiceAliasAction(ServiceNodeAlias en, boolean confirmationEnabled) {
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
        return "Delete Service Alias";
    }

    /**
     * @return the aciton description
     */
    @Override
    public String getDescription() {
        return "Delete Service Alias";
    }

    /**
     * subclasses override this method specifying the resource name
     */
    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/delete.gif";
    }

    public boolean deleteEntity(){
        final ServiceAdmin serviceAdmin = Registry.getDefault().getServiceManager();
        Object userObj = node.getUserObject();
        if(!(userObj instanceof ServiceHeader)) return false;

        ServiceHeader sH = (ServiceHeader) userObj;
        try {
            //delete the alias, leaving the original service alone
            //what alias does this node represent? Need it's policy id and folder id to find out
            //sH's folder id has been modified to point at the folder containing the alias
            PublishedServiceAlias pa = serviceAdmin.findAliasByEntityAndFolder(sH.getGoid(), sH.getFolderId());
            if(pa != null){
                serviceAdmin.deleteEntityAlias((Goid.toString(pa.getGoid())));
            }else{
                DialogDisplayer.showMessageDialog(Actions.getTopParent(),
                  "Cannot find alias to delete",
                  "Delete Service Alias",
                  JOptionPane.ERROR_MESSAGE, null);
            }
            return true;
        }catch (ObjectModelException ome) {
            String msg = "Error encountered while deleting " +
                        node.getName() +
                        ". Please try again later.";
            log.log(Level.WARNING, "Error deleting service alias", ome);
            DialogDisplayer.showMessageDialog(Actions.getTopParent(),
                    msg,
                    "Delete Service Alias",
                    JOptionPane.ERROR_MESSAGE, null);
        } catch (Throwable throwable) {
            ErrorManager.getDefault().notify(Level.WARNING, throwable, "Error deleting service alias");
        }
        return false;
    }

    public String getUserConfirmationTitle() {
        return "Delete Service Alias";
    }
}


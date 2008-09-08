package com.l7tech.console.action;

import com.l7tech.console.panels.HomePagePanel;
import com.l7tech.console.panels.WorkSpacePanel;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.tree.EntityWithPolicyNode;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.util.Functions;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.OrganizationHeader;
import com.l7tech.objectmodel.DeleteException;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Set;


/**
 * The <code>DeleteServiceAction</code> action deletes the service
 */
public final class DeleteServiceAction extends DeleteEntityNodeAction<ServiceNode> {
    static final Logger log = Logger.getLogger(DeleteServiceAction.class.getName());

    /**
     * create the acction that deletes the service
     *
     * @param en the node to delete
     */
    public DeleteServiceAction(ServiceNode en) {
        super(en);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Delete Service";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Delete the Web service";
    }

    public String getUserConfirmationMessage() {
        return "Are you sure you want to delete the " + node.getName() + " service?";
    }

    public String getUserConfirmationTitle() {
        return "Delete Service";
    }

    public boolean deleteEntity(){
        final ServiceAdmin serviceManager = Registry.getDefault().getServiceManager();
        Object userObj = node.getUserObject();

        if(!(userObj instanceof ServiceHeader)) return false;
        try{
            ServiceHeader sH = (ServiceHeader) userObj;
            serviceManager.deletePublishedService(Long.toString(sH.getOid()));
            return true;
        }
        catch (ObjectModelException ome) {
            log.log(Level.WARNING, "Error deleting service", ome);
            DialogDisplayer.showMessageDialog(Actions.getTopParent(),
              "Error encountered while deleting " +
              node.getName() +
              ". Please try again later.",
              "Delete Service",
              JOptionPane.ERROR_MESSAGE, null);
        } catch (Throwable throwable) {
            ErrorManager.getDefault().notify(Level.WARNING, throwable, "Error deleting service");
        }
        return false;
    }
}

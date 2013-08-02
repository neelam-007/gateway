package com.l7tech.console.action;

import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.ObjectModelException;

import javax.swing.*;
import java.util.logging.Logger;
import java.util.logging.Level;


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
     * Create the action that deletes the policy
     * @param en the node to delete
     * @param confirmationEnabled: to check if a deletion confirmation is needed or not.
     */
    public DeleteServiceAction(ServiceNode en, boolean confirmationEnabled) {
        super(en, confirmationEnabled);
    }

    /**
     * @return the action name
     */
    @Override
    public String getName() {
        return "Delete Service";
    }

    /**
     * @return the aciton description
     */
    @Override
    public String getDescription() {
        return "Delete the Web service";
    }

    @Override
    public String getUserConfirmationTitle() {
        return "Delete Service";
    }

    @Override
    public boolean deleteEntity(){
        final ServiceAdmin serviceManager = Registry.getDefault().getServiceManager();
        Object userObj = node.getUserObject();

        if(!(userObj instanceof ServiceHeader)) return false;
        try{
            ServiceHeader sH = (ServiceHeader) userObj;
            serviceManager.deletePublishedService(Goid.toString(sH.getGoid()));
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

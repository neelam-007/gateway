package com.l7tech.console.action;

import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.security.rbac.OperationType;
import com.l7tech.common.util.Functions;
import com.l7tech.console.panels.ServicePropertiesDialog;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.ServicesTree;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.service.PublishedService;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.rmi.RemoteException;
import java.util.logging.Level;

/**
 * Action to edit the published service properties
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 24, 2007<br/>
 */
public class EditServiceProperties extends ServiceNodeAction {
    public EditServiceProperties(ServiceNode node) {
        super(node);
    }

    protected OperationType getOperation() {
        return OperationType.UPDATE;
    }

    public String getName() {
        return "Service Properties";
    }

    public String getDescription() {
        return "View/Edit the properties of the published service";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/Edit16.gif";
    }

    protected void performAction() {
        final ServiceNode serviceNode = ((ServiceNode)node);
        PublishedService svc;
        try {
            svc = serviceNode.getPublishedService();
        } catch (FindException e) {
            logger.log(Level.WARNING, "Cannot get service", e);
            throw new RuntimeException(e);
        } catch (RemoteException e) {
            logger.log(Level.WARNING, "Cannot get service", e);
            throw new RuntimeException(e);
        }

        Functions.UnaryVoid<Boolean> callback = new Functions.UnaryVoid<Boolean>() {
            public void call(Boolean changed) {
                if (changed) {
                    serviceNode.clearServiceHolder();
                    JTree tree = (JTree) TopComponents.getInstance().getComponent(ServicesTree.NAME);
                    if (tree != null) {
                        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                        model.nodeChanged(node);
                    }
                }
            }
        };
        editServiceProperties(svc, callback);
    }

    private void editServiceProperties(final PublishedService svc, final Functions.UnaryVoid<Boolean> resultCallback) {
        final Frame mw = TopComponents.getInstance().getTopParent();
        final ServicePropertiesDialog dlg = new ServicePropertiesDialog(mw, svc);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                if (dlg.wasOKed()) {
                    try {
                        Registry.getDefault().getServiceManager().savePublishedService(svc);
                    } catch (DuplicateObjectException e) {
                        JOptionPane.showMessageDialog(mw,
                              "Unable to save the service '" + svc.getName() + "'\n" +
                              "because an existing service is already using the URI " + svc.getRoutingUri(),
                              "Service already exists",
                              JOptionPane.ERROR_MESSAGE);
                    } catch (Exception e) {
                        String msg = "Error while changing service properties";
                        logger.log(Level.INFO, msg, e);
                        JOptionPane.showMessageDialog(mw, msg + e.getMessage());
                    }
                    resultCallback.call(true);
                    return;
                }
                resultCallback.call(false);
            }
        });
    }
}

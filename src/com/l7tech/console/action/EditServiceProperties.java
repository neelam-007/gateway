package com.l7tech.console.action;

import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.security.rbac.OperationType;
import com.l7tech.common.security.rbac.AttemptedUpdate;
import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.common.util.Functions;
import com.l7tech.console.panels.ServicePropertiesDialog;
import com.l7tech.console.panels.WorkSpacePanel;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.ServicesTree;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceDocument;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.logging.Level;
import java.util.Collection;

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
        return OperationType.READ;
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
        boolean canUpdate;
        PublishedService svc;
        try {
            svc = serviceNode.getPublishedService();
            canUpdate = Registry.getDefault().getSecurityProvider().hasPermission(new AttemptedUpdate(EntityType.SERVICE, svc));
        } catch (FindException e) {
            logger.log(Level.WARNING, "Cannot get service", e);
            throw new RuntimeException(e);
        }

        Functions.UnaryVoid<Boolean> callback = new Functions.UnaryVoid<Boolean>() {
            public void call(Boolean changed) {
                if (changed) {
                    serviceNode.clearServiceHolder();
                    serviceNode.reloadChildren();
                    JTree tree = (JTree) TopComponents.getInstance().getComponent(ServicesTree.NAME);
                    if (tree != null) {
                        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                        model.nodeChanged(node);
                        model.reload(node); // WSDL may have changed
                    }

                    // update name on top of editor if that service is being edited
                    final WorkSpacePanel cws = TopComponents.getInstance().getCurrentWorkspace();
                    JComponent jc = cws.getComponent();
                    if (jc == null || !(jc instanceof PolicyEditorPanel)) {
                        return;
                    }
                    PolicyEditorPanel pe = (PolicyEditorPanel)jc;
                    try {
                        PublishedService editedSvc = pe.getServiceNode().getPublishedService();
                        // if currently edited service was deleted
                        if (serviceNode.getPublishedService().getOid() == editedSvc.getOid()) {
                            // update name on top of editor
                            pe.changeSubjectName(serviceNode.getName());
                            pe.updateHeadings();
                        }
                    } catch (FindException e) {
                        logger.log(Level.WARNING, "problem modifying policy editor title");
                    }
                }
            }
        };
        editServiceProperties(svc, callback, canUpdate);
    }

    private void editServiceProperties(final PublishedService svc, final Functions.UnaryVoid<Boolean> resultCallback, boolean canUpdate) {
        final Frame mw = TopComponents.getInstance().getTopParent();
        final ServicePropertiesDialog dlg = new ServicePropertiesDialog(mw, svc, canUpdate);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                if (dlg.wasOKed()) {
                    try {
                        Collection<ServiceDocument> documents = dlg.getServiceDocuments();
                        if (documents == null)
                            Registry.getDefault().getServiceManager().savePublishedService(svc);
                        else
                            Registry.getDefault().getServiceManager().savePublishedServiceWithDocuments(svc, documents);                        
                    } catch (DuplicateObjectException e) {
                        JOptionPane.showMessageDialog(mw,
                              "Unable to save the service '" + svc.getName() + "'\n" +
                              "because an existing service is already using the URI " + svc.getRoutingUri(),
                              "Service already exists",
                              JOptionPane.ERROR_MESSAGE);
                    } catch (Exception e) {
                        String msg = "Error while changing service properties";
                        logger.log(Level.INFO, msg, e);
                        String errorMessage = e.getMessage();
                        if (errorMessage != null) msg += ":\n" + errorMessage;
                        JOptionPane.showMessageDialog(mw, msg);
                    }
                    resultCallback.call(true);
                    return;
                }
                resultCallback.call(false);
            }
        });
    }
}

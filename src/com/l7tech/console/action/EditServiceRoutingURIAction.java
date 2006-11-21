package com.l7tech.console.action;

import com.l7tech.console.MainWindow;
import com.l7tech.console.panels.SoapServiceRoutingURIEditor;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.ServicesTree;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.*;
import com.l7tech.service.PublishedService;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.security.rbac.OperationType;
import com.l7tech.common.util.Functions;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.awt.*;

/**
 * This action is to view/edit the HTTP URI resolution parameter of a non-soap service.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Sep 15, 2004<br/>
 * $Id$<br/>
 */
public class EditServiceRoutingURIAction extends ServiceNodeAction {
    public EditServiceRoutingURIAction(ServiceNode node) {
        super(node);
    }

    protected OperationType getOperation() {
        return OperationType.UPDATE;
    }

    public String getName() {
        return "View Gateway URL";
    }

    public String getDescription() {
        return "View/Edit the HTTP URI resolution parameter of a non-soap service";
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
                    JTree tree = (JTree)TopComponents.getInstance().getComponent(ServicesTree.NAME);
                    if (tree != null) {
                        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                        model.nodeChanged(node);
                    }
                }
            }
        };

        if (svc.isSoap()) {
            editSoapServiceRoutingURI(svc, callback);
        }
        else {
            editXMLServiceRoutingURI(svc, callback);
        }
    }

    private void editSoapServiceRoutingURI(final PublishedService svc, final Functions.UnaryVoid<Boolean> resultCallback) {
        final Frame mw = TopComponents.getInstance().getTopParent();
        final SoapServiceRoutingURIEditor dlg = new SoapServiceRoutingURIEditor(mw, svc);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                if (dlg.wasSubjectAffected()) {
                    try {
                        Registry.getDefault().getServiceManager().savePublishedService(svc);
                    } catch (DuplicateObjectException e) {
                        JOptionPane.showMessageDialog(mw,
                              "Unable to save the service '" + svc.getName() + "'\n" +
                              "because an existing service is already using the URI " + svc.getRoutingUri(),
                              "Service already exists",
                              JOptionPane.ERROR_MESSAGE);
                    } catch (Exception e) {
                        String msg = "Error while changing routing URI ";
                        logger.log(Level.INFO, msg, e);
                        JOptionPane.showMessageDialog(mw, msg + e.getMessage());
                    }
                    resultCallback.call(true);
                    return;
                }
                resultCallback.call(false);
                return;
            }
        });
    }

    private void editXMLServiceRoutingURI(PublishedService svc, final Functions.UnaryVoid<Boolean> result) {
        final Frame parent = TopComponents.getInstance().getTopParent();
        String ssgUrl = TopComponents.getInstance().ssgURL();
        if (!ssgUrl.startsWith("http://")) {
            ssgUrl = "http://" + ssgUrl;
        }
        int pos = ssgUrl.lastIndexOf(':');
        if (pos > 4) {
            ssgUrl = ssgUrl.substring(0, pos);
            ssgUrl = ssgUrl + ":8080";
        }
        String prefix = ssgUrl;
        String newURI = null;
        String previousRoutingURI = null;
        boolean updated = false;
        try {
            String existingRoutingURI = svc.getRoutingUri();
            previousRoutingURI = existingRoutingURI;
            if (existingRoutingURI == null) existingRoutingURI = "";
            newURI = (String)JOptionPane.showInputDialog(parent,
              "View or edit the SecureSpan Gateway URL that receives service requests:\n" + prefix,
              "View Gateway URL",
              JOptionPane.PLAIN_MESSAGE,
              null,
              null,
              existingRoutingURI);

            if (newURI != null && !newURI.equals(existingRoutingURI)) {
                if (newURI.length() <= 0 || newURI.equals("/")) { // non-soap service cannot have null routing uri
                    JOptionPane.showMessageDialog(parent, "Cannot set empty uri on non-soap service");
                    result.call(false);
                    return;
                } else if (newURI.startsWith(SecureSpanConstants.SSG_RESERVEDURI_PREFIX)) {
                    JOptionPane.showMessageDialog(parent, "URI cannot start with " + SecureSpanConstants.SSG_RESERVEDURI_PREFIX);
                    result.call(false);
                    return;
                } else {
                    if (newURI.length() <= 0) {
                        svc.setRoutingUri(null);
                    } else {
                        if (!newURI.startsWith("/")) newURI = "/" + newURI;
                        new URL(ssgUrl + newURI);
                        svc.setRoutingUri(newURI);
                    }
                    Registry.getDefault().getServiceManager().savePublishedService(svc);
                    updated = true;
                    result.call(true);
                    return;
                }
            }
        } catch (DuplicateObjectException e) {
            JOptionPane.showMessageDialog(parent,
              "Unable to save the service '" + svc.getName() + "'\n" +
              "because an existing service is already using the URI " + svc.getRoutingUri(),
              "Service already exists",
              JOptionPane.ERROR_MESSAGE);
        } catch (MalformedURLException e) {
            JOptionPane.showMessageDialog(parent, "Invalid URL " + prefix + newURI);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(parent, "Error while changing routing URI " + e.getMessage());
        } finally {
            // go back to previous value if something was aborted
            if (!updated && svc != null) {
                if (previousRoutingURI != null && previousRoutingURI.length() <= 0) {
                    previousRoutingURI = null;
                }
                svc.setRoutingUri(previousRoutingURI);
            }
        }
        result.call(false);
    }
}

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

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.logging.Level;

/**
 * This action is to view/edit the HTTP URI resolution parameter of a non-soap service.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Sep 15, 2004<br/>
 * $Id$<br/>
 */
public class EditServiceRoutingURIAction extends NodeAction {
    public EditServiceRoutingURIAction(ServiceNode node) {
        super(node);
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

        boolean changed;
        if (svc.isSoap()) {
            changed = editSoapServiceRoutingURI(svc);
        }
        else {
            changed = editXMLServiceRoutingURI(svc);
        }
        if (changed) {
            serviceNode.clearServiceHolder();
            JTree tree = (JTree)TopComponents.getInstance().getComponent(ServicesTree.NAME);
            if (tree != null) {
                DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                model.nodeChanged(node);
            }
        }
    }

    private boolean editSoapServiceRoutingURI(PublishedService svc) {
        final MainWindow mw = TopComponents.getInstance().getMainWindow();
        SoapServiceRoutingURIEditor dlg = new SoapServiceRoutingURIEditor(mw, svc);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.show();
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
                String msg = "Error while changing routinG URI ";
                logger.log(Level.INFO, msg, e);
                JOptionPane.showMessageDialog(mw, msg + e.getMessage());
            }
            return true;
        }
        return false;
    }

    private boolean editXMLServiceRoutingURI(PublishedService svc) {
        final MainWindow mw = TopComponents.getInstance().getMainWindow();
        String ssgUrl = mw.ssgURL();
        if (!ssgUrl.startsWith("http://")) {
            ssgUrl = "http://" + ssgUrl;
        }
        int pos = ssgUrl.lastIndexOf(':');
        if (pos > 0) {
            ssgUrl = ssgUrl.substring(0, pos);
            ssgUrl = ssgUrl + ":8080";
        }
        String prefix = ssgUrl + PublishedService.ROUTINGURI_PREFIX;
        String newURI = null;
        String previousRoutingURI = null;
        boolean updated = false;
        try {
            String existingRoutingURI = svc.getRoutingUri();
            previousRoutingURI = existingRoutingURI;
            if (existingRoutingURI == null) existingRoutingURI = ""; //  should only happen for soap services
            if (existingRoutingURI.length() > PublishedService.ROUTINGURI_PREFIX.length()) {
                existingRoutingURI = existingRoutingURI.substring(PublishedService.ROUTINGURI_PREFIX.length());
            }

            newURI = (String)JOptionPane.showInputDialog(mw,
              "View or edit the SecureSpan Gateway URL that receives service requests:\n" + prefix,
              "View Gateway URL",
              JOptionPane.PLAIN_MESSAGE,
              null,
              null,
              existingRoutingURI);

            if (newURI != null && !newURI.equals(existingRoutingURI)) {
                if (newURI.length() <= 0 && !svc.isSoap()) { // non-soap service cannot have null routing uri
                    JOptionPane.showMessageDialog(mw, "Cannot set empty uri on non-soap service");
                    return false;
                } else {
                    if (newURI.length() <= 0) {
                        svc.setRoutingUri(null);
                    } else {
                        if (newURI.startsWith("/")) newURI = newURI.substring(1);
                        new URL(ssgUrl + PublishedService.ROUTINGURI_PREFIX + newURI);
                        svc.setRoutingUri(PublishedService.ROUTINGURI_PREFIX + newURI);
                    }
                    Registry.getDefault().getServiceManager().savePublishedService(svc);
                    updated = true;
                    return true;
                }
            }
        } catch (DuplicateObjectException e) {
            JOptionPane.showMessageDialog(mw,
              "Unable to save the service '" + svc.getName() + "'\n" +
              "because an existing service is already using the URI " + svc.getRoutingUri(),
              "Service already exists",
              JOptionPane.ERROR_MESSAGE);
        } catch (MalformedURLException e) {
            JOptionPane.showMessageDialog(mw, "Invalid URL " + prefix + newURI);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(mw, "Error while changing routinG URI " + e.getMessage());
        } finally {
            // go back to previous value if something was aborted
            if (!updated && svc != null) {
                if (previousRoutingURI != null && previousRoutingURI.length() <= 0) {
                    previousRoutingURI = null;
                }
                svc.setRoutingUri(previousRoutingURI);
            }
        }
        return false;
    }

}

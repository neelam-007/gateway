package com.l7tech.console.action;

import com.l7tech.console.MainWindow;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.ServicesTree;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.service.PublishedService;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.net.MalformedURLException;
import java.net.URL;

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
        PublishedService svc = null;
        boolean updated = false;
        try {
            final ServiceNode serviceNode = ((ServiceNode)node);
            svc = serviceNode.getPublishedService();
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
                    return;
                } else {
                    if (newURI.length() <= 0) {
                        svc.setRoutingUri(null);
                    } else {
                        if (newURI.startsWith("/")) newURI = newURI.substring(1);
                        new URL(ssgUrl + PublishedService.ROUTINGURI_PREFIX+ newURI);
                        svc.setRoutingUri(PublishedService.ROUTINGURI_PREFIX + newURI);
                    }
                    Registry.getDefault().getServiceManager().savePublishedService(svc);
                    serviceNode.clearServiceHolder();
                    updated = true;
                    JTree tree = (JTree)TopComponents.getInstance().getComponent(ServicesTree.NAME);
                    if (tree != null) {
                        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                        model.nodeChanged(node);
                    }
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
            JOptionPane.showMessageDialog(mw, "Error while changing routin URI " + e.getMessage());
        } finally {
            // go back to previous value if something was aborted
            if (!updated && svc != null) {
                if (previousRoutingURI != null && previousRoutingURI.length() <= 0) {
                    previousRoutingURI = null;
                }
                svc.setRoutingUri(previousRoutingURI);
            }
        }
    }

}

package com.l7tech.console.action;

import com.l7tech.console.MainWindow;
import com.l7tech.console.panels.NonSoapServicePanel;
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
        String prefix = mw.ssgURL() + NonSoapServicePanel.DEF_PREFIX;
        String newURI = null;
        String existingRoutingURI = null;
        PublishedService svc = null;
        boolean updated = false;
        try {
            final ServiceNode serviceNode = ((ServiceNode)node);
            svc = serviceNode.getPublishedService();
            existingRoutingURI = svc.getRoutingUri();
            if (existingRoutingURI == null) existingRoutingURI = ""; //  should not happen
            if (existingRoutingURI.length() > NonSoapServicePanel.DEF_PREFIX.length()) {
                existingRoutingURI = existingRoutingURI.substring(NonSoapServicePanel.DEF_PREFIX.length());
            }

            newURI = (String)JOptionPane.showInputDialog(mw,
              "View or edit the SecureSpan Gateway URL that receives service requests:\n" + prefix,
              "View Gateway URL",
              JOptionPane.PLAIN_MESSAGE,
              null,
              null,
              existingRoutingURI);

            if (newURI != null && !newURI.equals(existingRoutingURI)) {
                if (newURI.length() > 0) {
                    if (newURI.startsWith("/")) newURI = newURI.substring(1);
                    new URL(mw.ssgURL() + NonSoapServicePanel.DEF_PREFIX + newURI);
                    svc.setRoutingUri(NonSoapServicePanel.DEF_PREFIX + newURI);
                    Registry.getDefault().getServiceManager().savePublishedService(svc);
                    serviceNode.clearServiceHolder();
                    updated = true;
                    JTree tree = (JTree)TopComponents.getInstance().getComponent(ServicesTree.NAME);
                    if (tree != null) {
                        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                        model.nodeChanged(node);
                    }
                } else {
                    JOptionPane.showMessageDialog(mw, "Cannot set empty uri");
                    return;
                }
            }
        } catch (DuplicateObjectException e) {
            JOptionPane.showMessageDialog(mw,
              "Unable to save the service '" + svc.getName() + "'\n" +
              "because an existing service is already using the URI " + svc.getRoutingUri(),
              "Service already exists",
              JOptionPane.ERROR_MESSAGE);
        } catch (MalformedURLException e) {
            JOptionPane.showMessageDialog(mw, "Invalid URL " + mw.ssgURL() + prefix + newURI);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(mw, "Error while changing name " + e.getMessage());
        } finally {
            if (!updated && svc != null) {
                svc.setRoutingUri(existingRoutingURI);
            }
        }
    }

}

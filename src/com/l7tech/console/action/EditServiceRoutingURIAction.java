package com.l7tech.console.action;

import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.ServicesTree;
import com.l7tech.console.MainWindow;
import com.l7tech.console.panels.NonSoapServicePanel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.Registry;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ResolutionParameterTooLongException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.VersionException;
import com.l7tech.objectmodel.SaveException;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.net.URL;
import java.net.MalformedURLException;
import java.rmi.RemoteException;

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
        return "View Routing URL";
    }

    public String getDescription() {
        return "View/Edit the HTTP URI resolution parameter of a non-soap service";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/Edit16.gif";
    }

    protected void performAction() {
        final MainWindow mw = TopComponents.getInstance().getMainWindow();
        try {
            PublishedService svc = ((ServiceNode)node).getPublishedService();
            String existingRoutingURI = svc.getRoutingUri();
            if (existingRoutingURI == null) existingRoutingURI = ""; //  should not happen
            if (existingRoutingURI.length() > NonSoapServicePanel.DEF_PREFIX.length()) {
                existingRoutingURI = existingRoutingURI.substring(NonSoapServicePanel.DEF_PREFIX.length());
            }
            String prefix = mw.ssgURL() + NonSoapServicePanel.DEF_PREFIX;

            String res = (String) JOptionPane.showInputDialog(mw,
                    "View or edit the Gateway URL that will receive service requests:\n" + prefix,
                    "View Routing URL",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    existingRoutingURI);
            
            if (res != null && !res.equals(existingRoutingURI)) {
                if (res.length() > 0) {
                    if (res.startsWith("/")) res = res.substring(1);
                    try {
                        new URL(mw.ssgURL() + NonSoapServicePanel.DEF_PREFIX + res);
                        svc.setRoutingUri(NonSoapServicePanel.DEF_PREFIX + res);
                        Registry.getDefault().getServiceManager().savePublishedService(svc);

                        JTree tree = (JTree)TopComponents.getInstance().getComponent(ServicesTree.NAME);
                        if (tree != null) {
                            DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                            model.nodeChanged(node);
                        }
                        
                    } catch (MalformedURLException e) {
                        JOptionPane.showMessageDialog(mw, "Invalid URL " + mw.ssgURL() + prefix + res);
                    }
                } else {
                    JOptionPane.showMessageDialog(mw, "Cannot set empty uri");
                }
            }
        } catch (RemoteException e) {
            JOptionPane.showMessageDialog(mw, "Error while changing name " + e.getMessage());
        } catch (FindException e) {
            JOptionPane.showMessageDialog(mw, "Error while changing name " + e.getMessage());
        } catch (UpdateException e) {
            JOptionPane.showMessageDialog(mw, "Error while changing name " + e.getMessage());
        } catch (SaveException e) {
            JOptionPane.showMessageDialog(mw, "Error while changing name " + e.getMessage());
        } catch (VersionException e) {
            JOptionPane.showMessageDialog(mw, "This service is not up to date.");
        } catch (ResolutionParameterTooLongException e) {
            JOptionPane.showMessageDialog(mw, "Error while changing name " + e.getMessage());
            // should not happen
        }
    }
}

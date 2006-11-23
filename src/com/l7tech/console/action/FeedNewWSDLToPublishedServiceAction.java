package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.common.security.rbac.OperationType;
import com.l7tech.console.MainWindow;
import com.l7tech.console.panels.SelectWsdlDialog;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.ServicesTree;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.VersionException;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.service.PublishedService;
import org.w3c.dom.Document;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.awt.*;

/**
 * This action resets the wsdl of an already published web service (for example if the downstream
 * service's wsdl has changed since publication).
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * <p/>
 * User: flascell<br/>
 * Date: Oct 18, 2004<br/>
 */
public class FeedNewWSDLToPublishedServiceAction extends ServiceNodeAction {

    public FeedNewWSDLToPublishedServiceAction(ServiceNode node) {
        super(node);
    }

    protected OperationType getOperation() {
        return OperationType.UPDATE;
    }

    public String getName() {
        return "Reset WSDL";
    }

    public String getDescription() {
        return "Reset the WSDL property of this service.";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/Edit16.gif";
    }

    protected void performAction() {
        final Frame mw = TopComponents.getInstance().getTopParent();
        final PublishedService svc;
        try {
            svc = ((ServiceNode)node).getPublishedService();
        } catch (FindException e) {
            logger.log(Level.WARNING, "error retrieving service", e);
            DialogDisplayer.showMessageDialog(mw, "Cannot retrieve published service.", null);
            return;
        } catch (RemoteException e) {
            logger.log(Level.WARNING, "error retrieving service", e);
            DialogDisplayer.showMessageDialog(mw, "Cannot retrieve published service.", null);
            return;
        }
        String existingURL = svc.getWsdlUrl();
        if (existingURL == null) existingURL = "";

        final SelectWsdlDialog rwd = new SelectWsdlDialog(mw, "Reset WSDL");
        rwd.setWsdlUrl(existingURL);
        Utilities.centerOnScreen(rwd);
        DialogDisplayer.display(rwd, new Runnable() {
            public void run() {
                Wsdl wsdl = rwd.getWsdl();
                if (wsdl != null) {
                    String response = rwd.getWsdlUrl();
                    Document document = rwd.getWsdlDocument();
                    try {
                        svc.setWsdlUrl(response.startsWith("http") ? response : null);
                        svc.setWsdlXml(XmlUtil.nodeToString(document));
                        Registry.getDefault().getServiceManager().savePublishedService(svc);
                        ((ServiceNode)node).clearServiceHolder();
                        JTree tree = (JTree)TopComponents.getInstance().getComponent(ServicesTree.NAME);
                        if (tree != null) {
                            DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                            model.nodeChanged(node);
                        }
                    } catch (MalformedURLException e) {
                        logger.log(Level.WARNING, "invalid url", e);
                        throw new RuntimeException("Invalid URL", e);
                    } catch (RemoteException e) {
                        logger.log(Level.WARNING, "cannot change wsdl", e);
                        throw new RuntimeException("Error Changing WSDL. Consult log for more information.", e);
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "cannot change wsdl", e);
                        throw new RuntimeException("Error Changing WSDL. Consult log for more information.", e);
                    } catch (UpdateException e) {
                        logger.log(Level.WARNING, "cannot change wsdl", e);
                        throw new RuntimeException("Error Changing WSDL. Consult log for more information.", e);
                    } catch (SaveException e) {
                        logger.log(Level.WARNING, "cannot change wsdl", e);
                        throw new RuntimeException("Error Changing WSDL. Consult log for more information.", e);
                    } catch (VersionException e) {
                        logger.log(Level.WARNING, "version mismatch", e);
                        throw new RuntimeException("The service's version number is no longer valid. Perhaps " +
                                                   "another administrator has changed the service since you loaded it?", e);
                    } catch (PolicyAssertionException e) {
                        logger.log(Level.WARNING, "policy invalid", e);
                        throw new RuntimeException("The server policy cannot be created: " + ExceptionUtils.getMessage(e), e);
                    }
                }
            }
        });
    }
}

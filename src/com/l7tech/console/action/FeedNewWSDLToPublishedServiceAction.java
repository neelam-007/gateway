package com.l7tech.console.action;

import com.l7tech.service.PublishedService;
import com.l7tech.service.ResolutionParameterTooLongException;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.ServicesTree;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.MainWindow;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.VersionException;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.wsdl.WSDLException;
import java.io.StringReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.logging.Level;

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
public class FeedNewWSDLToPublishedServiceAction extends NodeAction {

    public FeedNewWSDLToPublishedServiceAction(ServiceNode node) {
        super(node);
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
        final MainWindow mw = TopComponents.getInstance().getMainWindow();
        PublishedService svc = null;
        try {
            svc = ((ServiceNode)node).getPublishedService();
        } catch (FindException e) {
            logger.log(Level.WARNING, "error retrieving service", e);
            JOptionPane.showMessageDialog(mw, "Cannot retrieve published service.");
            return;
        } catch (RemoteException e) {
            logger.log(Level.WARNING, "error retrieving service", e);
            JOptionPane.showMessageDialog(mw, "Cannot retrieve published service.");
            return;
        }
        String existingURL = svc.getWsdlUrl();
        if (existingURL == null) existingURL = "";
        String response = (String)JOptionPane.showInputDialog(mw, "Enter the URL for a new WSDL:", "Reset WSDL",
                                                              JOptionPane.QUESTION_MESSAGE, null, null, existingURL);
        if (response == null) return;
        String newWSDL = null;
        try {
            try {
                new URL(response);
            } catch (MalformedURLException e) {
                logger.log(Level.WARNING, "bad url " + response, e);
                JOptionPane.showMessageDialog(mw, response + " is not a valid url.");
                return;
            }
            newWSDL = Registry.getDefault().getServiceManager().resolveWsdlTarget(response);
            if (newWSDL != null && newWSDL.length() > 0) {
                Wsdl.newInstance(null, new StringReader(newWSDL));
            }
        } catch (WSDLException e) {
            logger.log(Level.WARNING, "invalid wsdl", e);
            throw new RuntimeException("Invalid WSDL. Consult log for more information.", e);
        } catch (RemoteException e) {
            logger.log(Level.WARNING, "cannot resolve wsdl", e);
            throw new RuntimeException("Error resolving WSDL. Consult log for more information.", e);
        } catch (MalformedURLException e) {
            logger.log(Level.WARNING, "Malformed URL, cannot resolve wsdl", e);
            JOptionPane.showMessageDialog(mw, response + " is not a valid url.");
            return;
        } catch (IOException e) {
            logger.log(Level.WARNING, "cannot access wsdl", e);
            JOptionPane.showMessageDialog(mw, "Error accessing WSDL at "+response);
            return;
        }
        try {
            svc.setWsdlUrl(response);
            svc.setWsdlXml(newWSDL);
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
        } catch (ResolutionParameterTooLongException e) {
            logger.log(Level.WARNING, "resolution parameter too long", e);
            throw new RuntimeException("The WSDL contains resolution parameters that are too long. " +
                                       "Consult log for more information.", e);
        }
    }
}

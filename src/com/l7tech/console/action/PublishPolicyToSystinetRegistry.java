package com.l7tech.console.action;

import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.PublishPolicyToUDDIWizard;
import com.l7tech.service.PublishedService;
import com.l7tech.objectmodel.FindException;
import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;
import java.util.logging.Level;
import java.rmi.RemoteException;

/**
 * Action corresponding to publishing a policy on a systinet registry.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Mar 8, 2006<br/>
 */
public class PublishPolicyToSystinetRegistry extends NodeAction {
    public PublishPolicyToSystinetRegistry(ServiceNode node) {
        super(node);
    }

    public String getName() {
        return "Publish to Systinet Registry";
    }

    public String getDescription() {
        return "Publish the service's Policy to a UDDI Registry";
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
        String policyURL = null;
        try {
            policyURL = Registry.getDefault().getServiceManager().getPolicyURL(""+svc.getOid());
        } catch (RemoteException e) {
            logger.log(Level.WARNING, "Error publishing service on Systinet registry", e);
            // todo nicer error message here
            throw new RuntimeException(e);
        } catch (FindException e) {
            logger.log(Level.WARNING, "Error publishing service on Systinet registry", e);
            // todo nicer error message here
            throw new RuntimeException(e);
        }
        assert(policyURL != null);
        JFrame f = TopComponents.getInstance().getMainWindow();
        PublishPolicyToUDDIWizard wizard = PublishPolicyToUDDIWizard.getInstance(f, policyURL, svc.getName());
        wizard.setSize(850, 500);
        Utilities.centerOnScreen(wizard);
        wizard.setVisible(true);
    }
}

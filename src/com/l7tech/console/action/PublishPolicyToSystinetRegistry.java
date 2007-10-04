package com.l7tech.console.action;

import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.PublishPolicyToUDDIWizard;
import com.l7tech.service.PublishedService;
import com.l7tech.objectmodel.FindException;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;

import javax.swing.*;
import java.util.logging.Level;
import java.awt.*;

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
        super(node, LIC_AUTH_ASSERTIONS, null);
    }

    public String getName() {
        return "Publish to UDDI Registry";
    }

    public String getDescription() {
        return "Publish the service's Policy to a UDDI Registry";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/Edit16.gif";
    }

    protected void performAction() {
        Frame f = TopComponents.getInstance().getTopParent();

        // check that systinet (only supported uddi implementation so far) is in class path
        try {
            Class.forName("org.systinet.uddi.client.v3.struct.Save_tModel");
        } catch (Throwable e) {
            logger.log(Level.INFO,  "UDDI client implementation not in class path", e);
            JOptionPane.showMessageDialog(f, "No UDDI client implementation installed on this installation",
                                             "Missing Component", JOptionPane.ERROR_MESSAGE);
            return;
        }

        final ServiceNode serviceNode = ((ServiceNode)node);
        PublishedService svc;
        try {
            svc = serviceNode.getPublishedService();
        } catch (FindException e) {
            logger.log(Level.WARNING, "Cannot get service", e);
            throw new RuntimeException(e);
        }
        String policyURL;
        String serviceConsumptionURL;
        try {
            policyURL = Registry.getDefault().getServiceManager().getPolicyURL(""+svc.getOid());
            serviceConsumptionURL = Registry.getDefault().getServiceManager().getConsumptionURL(""+svc.getOid());
        } catch (FindException e) {
            logger.log(Level.WARNING, "Cannot get service detail from SSG", e);
            JOptionPane.showMessageDialog(f, "Error getting service details from SecureSpan\nGateway " +
                                             "consult log for more information", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        assert(policyURL != null);
        PublishPolicyToUDDIWizard wizard = PublishPolicyToUDDIWizard.getInstance(f, policyURL, serviceConsumptionURL, svc.getName());
        wizard.pack();
        Utilities.centerOnScreen(wizard);
        DialogDisplayer.display(wizard);
    }
}

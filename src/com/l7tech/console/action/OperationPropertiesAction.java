package com.l7tech.console.action;

import com.l7tech.console.tree.policy.OperationTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.WSDLOperationPropertiesDialog;
import com.l7tech.policy.assertion.Operation;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.service.PublishedService;
import com.l7tech.objectmodel.FindException;

import javax.wsdl.WSDLException;
import javax.swing.*;
import java.awt.*;
import java.rmi.RemoteException;
import java.util.logging.Level;

/**
 * Action for editing the properties of the Operaion assertion.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: May 17, 2006<br/>
 */
public class OperationPropertiesAction extends SecureAction {
    private final OperationTreeNode subject;

    public OperationPropertiesAction(OperationTreeNode subject) {
        this.subject = subject;
    }

    public String getName() {
        return "WSDL Operation Properties";
    }

    public String getDescription() {
        return "Change the properties of the Fault Level assertion.";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    protected void performAction() {
        Frame f = TopComponents.getInstance().getMainWindow();

        PublishedService svc = null;
        try {
            svc = subject.getService();
        } catch (RemoteException e) {
            String msg = "error retrieving service";
            log.log(Level.WARNING, msg, e);
            JOptionPane.showMessageDialog(f, msg, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        } catch (FindException e) {
            String msg = "error retrieving service";
            log.log(Level.WARNING, msg, e);
            JOptionPane.showMessageDialog(f, msg, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!svc.isSoap()) {
            String msg = "This assertion is not applicable to non-SOAP services.";
            JOptionPane.showMessageDialog(f, msg, "Not applicable", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String[] operations;
        try {
            operations = (String[])SoapUtil.getOperationNames(svc.parsedWsdl()).toArray(new String[0]);
        } catch (WSDLException e) {
            String msg = "Error retrieving wsdl details";
            log.log(Level.WARNING, msg, e);
            JOptionPane.showMessageDialog(f, msg, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        WSDLOperationPropertiesDialog dlg = new WSDLOperationPropertiesDialog(f, (Operation)subject.asAssertion(), operations);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.setVisible(true);
        if (dlg.oked) {
            JTree tree = TopComponents.getInstance().getPolicyTree();
            if (tree != null) {
                PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
                model.assertionTreeNodeChanged(subject);
            } else {
                log.log(Level.WARNING, "Unable to reach the policy tree.");
            }
        }
    }
}

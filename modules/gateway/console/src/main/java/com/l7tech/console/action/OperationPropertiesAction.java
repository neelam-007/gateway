package com.l7tech.console.action;

import com.l7tech.console.tree.policy.OperationTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.WSDLOperationPropertiesDialog;
import com.l7tech.policy.assertion.Operation;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.FindException;

import javax.wsdl.WSDLException;
import javax.swing.*;
import java.awt.*;
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
        super(null, Operation.class);
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
        Frame f = TopComponents.getInstance().getTopParent();

        PublishedService svc = null;
        try {
            svc = subject.getService();
        } catch (FindException e) {
            String msg = "error retrieving service";
            log.log(Level.WARNING, msg, e);
            DialogDisplayer.showMessageDialog(f, msg, "Error", JOptionPane.ERROR_MESSAGE, null);
            return;
        }
        if (!svc.isSoap()) {
            String msg = "This assertion is not applicable to non-SOAP services.";
            DialogDisplayer.showMessageDialog(f, msg, "Not applicable", JOptionPane.ERROR_MESSAGE, null);
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

        final WSDLOperationPropertiesDialog dlg = new WSDLOperationPropertiesDialog(f, (Operation)subject.asAssertion(), operations);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
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
        });
    }
}

package com.l7tech.console.action;

import com.l7tech.console.tree.policy.OperationTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.WSDLOperationPropertiesDialog;
import com.l7tech.policy.assertion.Operation;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.service.PublishedService;
import com.l7tech.objectmodel.FindException;

import javax.wsdl.WSDLException;
import java.awt.*;
import java.rmi.RemoteException;

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
        PublishedService svc = null;
        try {
            svc = subject.getService();
        } catch (RemoteException e) {
            // todo
            svc = null;
        } catch (FindException e) {
            // todo
            svc = null;
        }
        if (svc == null || !svc.isSoap()) {
            // todo, show error
            return;
        }

        String[] operations = new String[0];
        try {
            operations = (String[])SoapUtil.getOperationNames(svc.parsedWsdl()).toArray(new String[0]);
        } catch (WSDLException e) {
            // todo, show error
            return;
        }
        Frame f = TopComponents.getInstance().getMainWindow();
        WSDLOperationPropertiesDialog dlg = new WSDLOperationPropertiesDialog(f, (Operation)subject.asAssertion(), operations);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.setVisible(true);
        // todo check oked and tell policy
    }
}

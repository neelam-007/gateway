package com.l7tech.console.tree.policy.advice;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.console.MainWindow;
import com.l7tech.console.panels.WSDLOperationPropertiesDialog;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.Operation;
import com.l7tech.service.PublishedService;

import javax.swing.*;
import javax.wsdl.WSDLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.*;

/**
 * Invoked when a WSDL Operation assertion is dropped in a policy tree.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: May 17, 2006<br/>
 */
public class AddWSDLOperationAssertionAdvice implements Advice {
    private static final Logger logger = Logger.getLogger(AddWSDLOperationAssertionAdvice.class.getName());

    public void proceed(final PolicyChange pc) {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof Operation)) {
            throw new IllegalArgumentException();
        }
        Operation assertion = (Operation)assertions[0];
        final Frame f = TopComponents.getInstance().getTopParent();


        PublishedService svc = pc.getService();
        if (!svc.isSoap()) {
            String msg = "This assertion is not applicable to non-SOAP services.";
            JOptionPane.showMessageDialog(f, msg, "Not applicable", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String[] operations;
        try {
            operations = (String[]) SoapUtil.getOperationNames(svc.parsedWsdl()).toArray(new String[0]);
        } catch (WSDLException e) {
            String msg = "Error retrieving wsdl details";
            logger.log(Level.WARNING, msg, e);
            JOptionPane.showMessageDialog(f, msg, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        final WSDLOperationPropertiesDialog dlg = new WSDLOperationPropertiesDialog(f, assertion, operations);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                if (dlg.oked) {
                    pc.proceed();
                }
            }
        });
    }
}

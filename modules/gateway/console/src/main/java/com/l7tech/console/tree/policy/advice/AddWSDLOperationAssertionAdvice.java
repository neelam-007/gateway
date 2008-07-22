package com.l7tech.console.tree.policy.advice;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.console.panels.WSDLOperationPropertiesDialog;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.Operation;
import com.l7tech.gateway.common.service.PublishedService;

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
        if (svc == null || !(svc.isSoap())) {
            DialogDisplayer.showMessageDialog(f, null,
                    "The 'WSDL Operation' assertion is not supported by non-SOAP services or policies not attached to a WSDL.", null);
            return;
        }

        String[] operations;
        try {
            //noinspection unchecked,ToArrayCallWithZeroLengthArrayArgument
            operations = (String[]) SoapUtil.getOperationNames(svc.parsedWsdl()).toArray(new String[0]);
        } catch (WSDLException e) {
            String msg = "Error retrieving wsdl details";
            logger.log(Level.WARNING, msg, e);
            DialogDisplayer.showMessageDialog(f, null, msg, null);
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

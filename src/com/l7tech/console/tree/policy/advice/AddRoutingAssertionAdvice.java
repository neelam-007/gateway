package com.l7tech.console.tree.policy.advice;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.console.MainWindow;
import com.l7tech.console.panels.JmsRoutingAssertionDialog;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.*;
import com.l7tech.service.PublishedService;

import javax.wsdl.WSDLException;
import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The class <code>AddRoutingAssertionAdvice</code> intercepts policy
 * routing assertion add. It sets security defaults such as default envelope signing.
 * <p/>
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class AddRoutingAssertionAdvice implements Advice {
    private static final Logger log = Logger.getLogger(AddRoutingAssertionAdvice.class.getName());

    public AddRoutingAssertionAdvice() {
    }

    /**
     * Intercepts a policy change.
     * 
     * @param pc The policy change.
     */
    public void proceed(PolicyChange pc) throws PolicyException {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 ||
          !(assertions[0] instanceof RoutingAssertion)) {
            throw new IllegalArgumentException();
        }

        if (assertions[0] instanceof HttpRoutingAssertion) {
            HttpRoutingAssertion ra = (HttpRoutingAssertion) assertions[0];
            try {
                ra.setSamlAssertionVersion(2); // default for new routing assertions
                if (null == ra.getProtectedServiceUrl()) {
                    String url = null;
                    PublishedService service = pc.getService();
                    if (service != null && service.isSoap()) {
                        Wsdl wsdl = service.parsedWsdl();
                        if (wsdl != null) {
                            url = wsdl.getServiceURI();
                        }
                    }

                    if (url == null /*&& service != null*/) {
                        url = JOptionPane.showInputDialog(
                                TopComponents.getInstance().getPolicyTree(),
                                "The Protected Service URL cannot be determined automatically.\nPlease enter the URL:",
                                "Unable to Determine Service URL", JOptionPane.WARNING_MESSAGE);
                        if (url == null) return;
                    }

                    ra.setProtectedServiceUrl(url);
                }
            } catch (WSDLException e) {
                log.log(Level.WARNING, "WSDL error", e);
            }
            pc.proceed();
        } else if (assertions[0] instanceof JmsRoutingAssertion) {
            JmsRoutingAssertion ra = (JmsRoutingAssertion) assertions[0];
            if (ra.getEndpointOid() == null) {
                final MainWindow mainWindow = TopComponents.getInstance().getMainWindow();
                JmsRoutingAssertionDialog dialog = new JmsRoutingAssertionDialog(mainWindow, ra);
                dialog.setModal(true);
                dialog.pack();
                Utilities.centerOnScreen(dialog);
                dialog.setVisible(true);
                if (!dialog.isCanceled())
                    pc.proceed();
            } else
                pc.proceed();
        } else if (assertions[0] instanceof EchoRoutingAssertion) {
            pc.proceed();
        } else if (assertions[0] instanceof HardcodedResponseAssertion) {
            pc.proceed();
        } else {
            throw new IllegalArgumentException("Can't handle " + assertions[0].getClass().getName());
        }
    }
}
package com.l7tech.console.tree.policy.advice;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.MainWindow;
import com.l7tech.console.panels.JmsRoutingAssertionDialog;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.JmsRoutingAssertion;
import com.l7tech.policy.assertion.RoutingAssertion;

import javax.wsdl.WSDLException;
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
            String url = "Unable to determine the service url. Please edit";
            try {
                if (null == ra.getProtectedServiceUrl()) {
                    url = pc.getService().parsedWsdl().getServiceURI();
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
                dialog.show();
                if (!dialog.isCanceled())
                    pc.proceed();
            } else
                pc.proceed();
        }
    }
}

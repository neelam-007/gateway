package com.l7tech.console.tree.policy.advice;

import com.l7tech.common.xml.Wsdl;
import com.l7tech.console.MainWindow;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RequestXpathAssertion;
import com.l7tech.service.PublishedService;

import javax.swing.*;
import javax.wsdl.WSDLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The class <code>AddXpathAssertionAdvice</code> intercepts policy
 * Xpath assertion add. It invokes the xpath assertion edit dialog.
 * <p/>
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class AddXpathAssertionAdvice implements Advice {
    /**
     * Intercepts a policy change.
     * 
     * @param pc The policy change.
     */
    public void proceed(PolicyChange pc) throws PolicyException {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 ||
          !(assertions[0] instanceof RequestXpathAssertion)) {
            throw new IllegalArgumentException();
        }
        RequestXpathAssertion a = (RequestXpathAssertion)assertions[0];
        if (edit(a, pc.getService())) {
            pc.proceed();
        }
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * <p/>
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    public boolean edit(RequestXpathAssertion xpathAssertion, PublishedService service) {
        try {
            Wsdl serviceWsdl = service.parsedWsdl();
            Map namespaceMap = new HashMap();
            Map assertionNSMap = xpathAssertion.getNamespaceMap();
            if (assertionNSMap != null) namespaceMap.putAll(xpathAssertion.getNamespaceMap());
            Map wsdlNamespaces = serviceWsdl.getNamespaces();

            if (namespaceMap == null || namespaceMap.isEmpty()) {
                namespaceMap = wsdlNamespaces;
            } else {
                for (Iterator i = wsdlNamespaces.keySet().iterator(); i.hasNext();) {
                    String key = (String)i.next();
                    String value = (String)wsdlNamespaces.get(key);
                    namespaceMap.put(key, value);
                }
            }

            String help;
            if (namespaceMap.isEmpty()) {
                help = "Please enter an XPath pattern:";
            } else {
                StringBuffer helpBuffer = new StringBuffer("Please enter an XPath pattern using only the following namespaces:\n\n");

                for (Iterator i = namespaceMap.keySet().iterator(); i.hasNext();) {
                    String prefix = (String)i.next();
                    String uri = (String)namespaceMap.get(prefix);
                    if (prefix == null || prefix.length() == 0) prefix = "<default>";
                    helpBuffer.append(prefix);
                    helpBuffer.append("=");
                    helpBuffer.append(uri);
                    helpBuffer.append("\n");
                }

                helpBuffer.append("\n");

                help = helpBuffer.toString();
            }

            final MainWindow mw = TopComponents.getInstance().getMainWindow();
            String s =
              (String)JOptionPane.showInputDialog(mw,
                help,
                "XPath Assertion properties",
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                xpathAssertion.getPattern());
            final boolean ok = (s != null) && (s.length() > 0);
            if (ok) {
                xpathAssertion.setPattern(s);
                xpathAssertion.setNamespaceMap(namespaceMap);
            }
            return ok;
        } catch (WSDLException e) {
            throw new RuntimeException("Couldn't parse WSDL from Published Service!", e);
        }
    }
}

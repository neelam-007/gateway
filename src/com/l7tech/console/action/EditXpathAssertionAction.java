/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.action;

import com.l7tech.common.xml.Wsdl;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.RequestXpathPolicyTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.Cookie;
import com.l7tech.console.MainWindow;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.RequestXpathAssertion;
import com.l7tech.service.PublishedService;

import javax.swing.*;
import javax.wsdl.WSDLException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;

/**
 * @author alex
 * @version $Revision$
 */
public class EditXpathAssertionAction extends BaseAction {
    public EditXpathAssertionAction(RequestXpathPolicyTreeNode xpathAssertionTreeNode) {
        _node = xpathAssertionTreeNode;
    }

    public String getName() {
        return "Edit Request XPath Assertion";
    }

    public String getDescription() {
        return "Edit a Request XPath Assertion";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/xmlsignature.gif";
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * <p/>
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    public void performAction() {
        RequestXpathAssertion xpathAssertion = (RequestXpathAssertion)_node.asAssertion();

        if (_cachedWsdl == null) {
            ServiceNode serviceNode = getServiceNodeCookie();
            try {
                PublishedService service = serviceNode.getPublishedService();
                _cachedWsdl = service.parsedWsdl();
            } catch (FindException e) {
                throw new RuntimeException("Couldn't resolve Published Service!", e);
            } catch (RemoteException e) {
                throw new RuntimeException("Couldn't resolve Published Service!", e);
            } catch (WSDLException e) {
                throw new RuntimeException("Couldn't parse WSDL from Published Service!", e);
            }
        }

        Map namespaceMap = new HashMap();
        Map assertionNSMap = xpathAssertion.namespaceMap();
        if (assertionNSMap != null) namespaceMap.putAll(xpathAssertion.namespaceMap());
        Map wsdlNamespaces = _cachedWsdl.getNamespaces();

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
            "XPath Assertion Properties",
            JOptionPane.PLAIN_MESSAGE,
            new ImageIcon(_node.getIcon()),
            null,
            xpathAssertion.pattern());
        if (s != null) {
            if (s.length() > 0) {
                xpathAssertion.setXpathExpression(new XpathExpression(s, namespaceMap));
                assertionChanged();
            } else {
                JOptionPane.showMessageDialog(mw, "No XPath pattern specified.", "XPath Assertion Properties", JOptionPane.ERROR_MESSAGE);

                // re-prompt the user
                performAction();
            }
        }
    }

    public void assertionChanged() {
        JTree tree =
          TopComponents.getInstance().getPolicyTree();
        if (tree != null) {
            PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
            model.assertionTreeNodeChanged(_node);
        } else {
            log.log(Level.WARNING, "Unable to reach the palette tree.");
        }
    }

    /**
     * @return the published service cookie or null if not founds
     */
    private ServiceNode getServiceNodeCookie() {
        for (Iterator i = ((AbstractTreeNode)_node.getRoot()).cookies(); i.hasNext();) {
            Object value = ((Cookie)i.next()).getValue();
            if (value instanceof ServiceNode) return (ServiceNode)value;
        }
        return null;
    }

    private RequestXpathPolicyTreeNode _node;
    private transient Wsdl _cachedWsdl;
}


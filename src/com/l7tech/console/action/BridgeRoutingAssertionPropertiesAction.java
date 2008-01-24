/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.policy.Policy;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.console.event.PolicyEvent;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.console.event.PolicyListenerAdapter;
import com.l7tech.console.panels.BridgeRoutingAssertionPropertiesDialog;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.BridgeRoutingAssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.BridgeRoutingAssertion;
import com.l7tech.service.PublishedService;

import javax.swing.*;
import javax.wsdl.WSDLException;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Action for showing {@link com.l7tech.policy.assertion.BridgeRoutingAssertion} properties dialog.
 */
public class BridgeRoutingAssertionPropertiesAction extends NodeAction {
    private static final Logger log = Logger.getLogger(BridgeRoutingAssertionPropertiesAction.class.getName());

    public BridgeRoutingAssertionPropertiesAction(BridgeRoutingAssertionTreeNode node) {
        super(node, BridgeRoutingAssertion.class);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Bridge Routing Properties";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "View and edit bridge routing properties";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    protected void performAction() {
                Frame f = TopComponents.getInstance().getTopParent();
                final BridgeRoutingAssertionPropertiesDialog d;
                try {
                    final Policy policy;
                    final Wsdl wsdl;
                    final ServiceNode snc = getServiceNodeCookie();
                    if (snc != null) {
                        PublishedService svc = snc.getPublishedService();
                        policy = svc.getPolicy();
                        wsdl = svc.parsedWsdl();
                    } else {
                        policy = getPolicyNodeCookie().getPolicy();
                        wsdl = null;
                    }
                    d = new BridgeRoutingAssertionPropertiesDialog(f, (BridgeRoutingAssertion)node.asAssertion(), policy, wsdl, !node.canEdit());
                } catch (FindException e) {
                    log.log(Level.WARNING, e.getMessage(), e);
                    throw new RuntimeException(e);
                } catch (WSDLException e) {
                    log.log(Level.WARNING, e.getMessage(), e);
                    throw new RuntimeException(e);
                }
                d.setModal(true);
                d.pack();
                Utilities.centerOnScreen(d);
                d.addPolicyListener(listener);
        DialogDisplayer.display(d, TopComponents.getInstance().getRootSheetHolder(), null);
    }

    private final PolicyListener listener = new PolicyListenerAdapter() {
        public void assertionsChanged(PolicyEvent e) {
            JTree tree = TopComponents.getInstance().getPolicyTree();
            if (tree != null) {
                PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
                model.assertionTreeNodeChanged((AssertionTreeNode)node);
            } else {
                log.log(Level.WARNING, "Unable to reach the palette tree.");
            }
        }
    };
}

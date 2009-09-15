package com.l7tech.console.action;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.policy.Policy;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.console.event.PolicyEvent;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.console.event.PolicyListenerAdapter;
import com.l7tech.console.panels.HttpRoutingAssertionDialog;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.gateway.common.service.PublishedService;

import javax.swing.*;
import javax.wsdl.WSDLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.*;

/**
 * The <code>HttpRoutingAssertionPropertiesAction</code> edits the
 * protected service properties.
 */
public class HttpRoutingAssertionPropertiesAction extends NodeActionWithMetaSupport {
    static final Logger log = Logger.getLogger(HttpRoutingAssertionPropertiesAction.class.getName());

    public HttpRoutingAssertionPropertiesAction(AssertionTreeNode node) {
        super(node, HttpRoutingAssertion.class, node.asAssertion());
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    @Override
    protected void performAction() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Frame f = TopComponents.getInstance().getTopParent();
                HttpRoutingAssertionDialog d;
                try {
                    final Policy policy;
                    final Wsdl wsdl;
                    ServiceNode snc = getServiceNodeCookie();
                    if (snc != null) {
                        PublishedService svc = snc.getEntity();
                        policy = svc.getPolicy();
                        wsdl = svc.parsedWsdl();
                    } else {
                        policy = getEntityWithPolicyNodeCookie().getPolicy();
                        wsdl = null;
                    }
                    d = new HttpRoutingAssertionDialog(f, (HttpRoutingAssertion)node.asAssertion(), policy, wsdl, !node.canEdit());
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
                DialogDisplayer.display(d);
            }
        });
    }

    private final PolicyListener listener = new PolicyListenerAdapter() {
        @Override
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

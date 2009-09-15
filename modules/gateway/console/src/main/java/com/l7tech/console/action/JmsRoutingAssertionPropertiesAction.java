package com.l7tech.console.action;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.console.event.PolicyEvent;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.console.event.PolicyListenerAdapter;
import com.l7tech.console.panels.JmsRoutingAssertionDialog;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.JmsRoutingAssertion;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.*;

/**
 * The <code>JmsRoutingAssertionPropertiesAction</code> edits the
 * protected service properties when JMS is used as the outbound transport.
 *
 * @author <a href="mailto:mlyons@layer7-tech.com">Mike Lyons</a>
 * @version 1.0
 */
public class JmsRoutingAssertionPropertiesAction extends NodeActionWithMetaSupport {
    static final Logger log = Logger.getLogger(JmsRoutingAssertionPropertiesAction.class.getName());

    public JmsRoutingAssertionPropertiesAction(AssertionTreeNode node) {
        super(node, JmsRoutingAssertion.class, node.asAssertion());
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
                JmsRoutingAssertionDialog d =
                  new JmsRoutingAssertionDialog(f, (JmsRoutingAssertion)node.asAssertion(), !node.canEdit());
                d.addPolicyListener(listener);
                d.pack();
                Utilities.centerOnScreen(d);
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

package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.event.PolicyEvent;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.console.event.PolicyListenerAdapter;
import com.l7tech.console.panels.HttpRoutingAssertionDialog;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.HttpRoutingAssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.Registry;
import com.l7tech.policy.assertion.HttpRoutingAssertion;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <code>HttpRoutingAssertionPropertiesAction</code> edits the
 * protected service properties.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class HttpRoutingAssertionPropertiesAction extends NodeAction {
    static final Logger log = Logger.getLogger(HttpRoutingAssertionPropertiesAction.class.getName());

    public HttpRoutingAssertionPropertiesAction(HttpRoutingAssertionTreeNode node) {
        super(node);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Routing Properties";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "View and edit routing properties";
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
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame f = TopComponents.getInstance().getMainWindow();
                HttpRoutingAssertionDialog d =
                  new HttpRoutingAssertionDialog(f, (HttpRoutingAssertion)node.asAssertion(), getServiceNodeCookie());
                d.setModal(true);
                d.pack();
                Utilities.centerOnScreen(d);
                d.addPolicyListener(listener);
                d.show();
            }
        });
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

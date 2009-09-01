package com.l7tech.console.action;

import com.l7tech.console.panels.EditSslAssertionPropertiesDialog;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.SslAssertionTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.gui.util.DialogDisplayer;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <code>SslPropertiesAction</code> edits the SSL assertion
 * properties.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class SslPropertiesAction extends NodeAction {
    static final Logger log = Logger.getLogger(SslPropertiesAction.class.getName());

    public SslPropertiesAction(SslAssertionTreeNode node) {
        super(node, SslAssertion.class);
    }

    /**
     * @return the action name
     */
    @Override
    public String getName() {
        return "SSL or TLS Transport Properties";
    }

    /**
     * @return the aciton description
     */
    @Override
    public String getDescription() {
        return "View/Edit SSL Properties";
    }

    /**
     * specify the resource name for this action
     */
    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * <p/>
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    @Override
    protected void performAction() {
        SslAssertion sslAssertion = (SslAssertion)node.asAssertion();
        DialogDisplayer.display(new EditSslAssertionPropertiesDialog(TopComponents.getInstance().getTopParent(), sslAssertion, !node.canEdit()),
                                new Runnable() {
                                    public void run() {
                                        assertionChanged();
                                    }
                                });
    }

    public void assertionChanged() {
        JTree tree = TopComponents.getInstance().getPolicyTree();
        if (tree != null) {
            PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
            model.assertionTreeNodeChanged((AssertionTreeNode)node);
        } else {
            log.log(Level.WARNING, "Unable to reach the palette tree.");
        }
    }
}

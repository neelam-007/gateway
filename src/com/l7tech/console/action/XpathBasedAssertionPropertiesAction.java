package com.l7tech.console.action;

import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.panels.XpathBasedAssertionPropertiesDialog;
import com.l7tech.console.tree.policy.*;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RequestXpathAssertion;
import com.l7tech.policy.assertion.ResponseXpathAssertion;
import com.l7tech.policy.assertion.XpathBasedAssertion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Action for editing XML security assertion properties
 */
public abstract class XpathBasedAssertionPropertiesAction extends NodeAction {
    static final Logger log = Logger.getLogger(XpathBasedAssertionPropertiesAction.class.getName());

    public static XpathBasedAssertionPropertiesAction actionForNode(XpathBasedAssertionTreeNode node) {
        if (node instanceof RequestWssConfidentialityTreeNode) {
            RequestWssConfidentialityTreeNode n = (RequestWssConfidentialityTreeNode)node;
            return new RequestConfidentialityPropertiesAction(n);
        } else if (node instanceof RequestWssIntegrityTreeNode) {
            RequestWssIntegrityTreeNode n = (RequestWssIntegrityTreeNode)node;
            return new RequestIntegrityPropertiesAction(n);
        } else if (node instanceof ResponseWssConfidentialityTreeNode) {
            ResponseWssConfidentialityTreeNode n = (ResponseWssConfidentialityTreeNode)node;
            return new ResponseConfidentialityPropertiesAction(n);
        } else if (node instanceof ResponseWssIntegrityTreeNode) {
            ResponseWssIntegrityTreeNode n = (ResponseWssIntegrityTreeNode)node;
            return new ResponseIntegrityPropertiesAction(n);
        } else if (node instanceof RequestXpathPolicyTreeNode) {
            RequestXpathPolicyTreeNode n = (RequestXpathPolicyTreeNode)node;
            return new RequestXpathPropertiesAction(n);
        } else if (node instanceof ResponseXpathPolicyTreeNode) {
            ResponseXpathPolicyTreeNode n = (ResponseXpathPolicyTreeNode)node;
            return new ResponseXpathPropertiesAction(n);
        }
        throw new RuntimeException("Type not supported " + node.getClass().getName());
    }

    protected XpathBasedAssertionPropertiesAction(XpathBasedAssertionTreeNode node, Class<? extends XpathBasedAssertion> concreteAssertionClass) {
        super(node, concreteAssertionClass);
    }

    /**
     * @return the aciton description
     */
    @Override
    public String getDescription() {
        return getName();
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
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     *
     * We moved checking if the node is a service node or a policy node into the method, 
     * {@link XpathBasedAssertionPropertiesDialog#construct(com.l7tech.console.tree.policy.XpathBasedAssertionTreeNode, java.awt.event.ActionListener, boolean)}.
     */
    @Override
    protected void performAction() {
        XpathBasedAssertionTreeNode n = (XpathBasedAssertionTreeNode)node;
        final Frame mw = TopComponents.getInstance().getTopParent();
        boolean showAccelStatus = shouldShowHardwareAccelStatus(n);
        XpathBasedAssertionPropertiesDialog dialog = new XpathBasedAssertionPropertiesDialog(mw, false, n, okListener, showAccelStatus, !n.canEdit());
        dialog.pack();
        Utilities.centerOnScreen(dialog);
        DialogDisplayer.display(dialog);
    }

    private boolean shouldShowHardwareAccelStatus(XpathBasedAssertionTreeNode n) {
        Assertion ass = n.asAssertion();
        return ass instanceof RequestXpathAssertion || ass instanceof ResponseXpathAssertion;
    }


    public ActionListener okListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    JTree tree = TopComponents.getInstance().getPolicyTree();
                    if (tree != null) {
                        PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
                        model.assertionTreeNodeChanged((AssertionTreeNode)node);
                    } else {
                        log.log(Level.WARNING, "Unable to reach the palette tree.");
                    }
                }
            });
        }
    };
}

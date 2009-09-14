package com.l7tech.console.action;

import com.l7tech.policy.assertion.transport.PreemptiveCompression;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.PreemptiveCompressionDialog;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;

/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * @author flascell<br/>
 */
public class PreemptiveCompressionAction extends NodeActionWithMetaSupport {
    private AssertionTreeNode treeNode;

    public PreemptiveCompressionAction(AssertionTreeNode node) {
        super(node, PreemptiveCompression.class, node.asAssertion());
        treeNode = node;
    }

    @Override
    protected void performAction() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Frame f = TopComponents.getInstance().getTopParent();
                final PreemptiveCompressionDialog d = new PreemptiveCompressionDialog(f, (PreemptiveCompression)node.asAssertion(), !node.canEdit());
                d.pack();
                Utilities.centerOnScreen(d);
                //d.addPolicyListener(listener);
                DialogDisplayer.display(d, new Runnable() {
                    @Override
                    public void run() {
                        if (d.wasOKed()) {
                            //treeNode.setUserObject(d.getAssertion());
                            fireAssertionChanged();
                        }
                    }
                });
            }
        });
    }

    private void fireAssertionChanged() {
        JTree tree = TopComponents.getInstance().getPolicyTree();
        if (tree != null) {
            PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
            model.assertionTreeNodeChanged(treeNode);
        } else {
            log.log(Level.WARNING, "Unable to reach the palette tree.");
        }
    }
}

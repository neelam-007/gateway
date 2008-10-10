package com.l7tech.console.action;

import com.l7tech.policy.assertion.transport.PreemptiveCompression;
import com.l7tech.console.tree.policy.PreemptiveCompressionPolicyNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.PreemptiveCompressionDialog;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;

/**
 * [todo jdoc this class]
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jun 9, 2008<br/>
 */
public class PreemptiveCompressionAction extends NodeAction {
    private PreemptiveCompressionPolicyNode treeNode;
    public PreemptiveCompressionAction(PreemptiveCompressionPolicyNode node) {
        super(node, PreemptiveCompression.class);
        treeNode = node;
    }
    /**
     * @return the action name
     */
    @Override
    public String getName() {
        return "Preemptive Compression Properties";
    }

    /**
     * @return the aciton description
     */
    @Override
    public String getDescription() {
        return "View and edit Preemptive Compression properties";
    }

    /**
     * specify the resource name for this action
     */
    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    @Override
    protected void performAction() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Frame f = TopComponents.getInstance().getTopParent();
                final PreemptiveCompressionDialog d = new PreemptiveCompressionDialog(f, (PreemptiveCompression)node.asAssertion(), !node.canEdit());
                d.pack();
                Utilities.centerOnScreen(d);
                //d.addPolicyListener(listener);
                DialogDisplayer.display(d, new Runnable() {
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

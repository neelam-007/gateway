package com.l7tech.console.action;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.console.panels.AssertionMessageTargetSelector;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.util.BeanUtils;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.*;

/**
 * Action for selecting the target for MessageTargetable assertions.
 *
 */
public class SelectMessageTargetAction extends NodeAction {
    private final MessageTargetable messageTargetable;
    private final boolean readOnly;
    private final AssertionTreeNode[] nodes;

    public SelectMessageTargetAction( final AssertionTreeNode node ) {
        super(node);
        messageTargetable = (MessageTargetable)node.asAssertion();
        readOnly = !node.canEdit();
        nodes = new AssertionTreeNode[]{node};
    }

    public SelectMessageTargetAction( final AssertionTreeNode[] nodes, final MessageTargetable[] messageTargetables) {
        super(nodes[0]);
        this. messageTargetable = BeanUtils.collectionBackedInstance( MessageTargetable.class, Arrays.asList(messageTargetables) );
        boolean readOnly = false;
        for ( AssertionTreeNode node : nodes ) {
            if ( !node.canEdit() ) {
                readOnly = true;
                break;
            }
        }
        this.readOnly = readOnly;
        this.nodes = nodes;
    }

    @Override
    public String getName() {
        return "Select Target Message";
    }

    @Override
    public String getDescription() {
        return getName();
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    @Override
    protected void performAction() {
        final Frame mw = TopComponents.getInstance().getTopParent();
        final AssertionMessageTargetSelector dlg = new AssertionMessageTargetSelector(mw, messageTargetable, readOnly);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.hasAssertionChanged()) {
                    JTree tree = TopComponents.getInstance().getPolicyTree();
                    if (tree != null) {
                        PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
                        for ( AssertionTreeNode node : nodes ) {
                            model.assertionTreeNodeChanged(node);                            
                        }
                    } else {
                        log.log(Level.WARNING, "Unable to reach the palette tree.");
                    }
                }
            }
        });
    }
}

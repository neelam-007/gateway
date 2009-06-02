package com.l7tech.console.action;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.console.panels.AssertionIdentityTagSelector;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.IdentityTagable;
import com.l7tech.util.BeanUtils;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.*;

/**
 * Action for selecting the tag for IdentityTagable assertions.
 *
 */
public class SelectIdentityTagAction extends NodeAction {
    private final IdentityTagable identityTagable;
    private final boolean readOnly;
    private final AssertionTreeNode[] nodes;

    public SelectIdentityTagAction( final AssertionTreeNode node ) {
        super(node);
        this.identityTagable = (IdentityTagable) node.asAssertion();
        this.readOnly = !node.canEdit();
        this.nodes = new AssertionTreeNode[]{node};
    }

    public SelectIdentityTagAction( final AssertionTreeNode[] nodes, final IdentityTagable[] IdentityTagables ) {
        super(nodes[0]);
        this.identityTagable = BeanUtils.collectionBackedInstance( IdentityTagable.class, Arrays.asList(IdentityTagables) );
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
        return "Identity Tag";
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
        final AssertionIdentityTagSelector dlg = new AssertionIdentityTagSelector(mw, identityTagable, readOnly);
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

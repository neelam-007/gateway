package com.l7tech.console.action;

import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.panels.AssertionKeyAliasEditor;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.PrivateKeyable;

import java.awt.*;

/**
 * Action for editing the keypair property of assertions.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: May 22, 2007<br/>
 */
public class EditKeyAliasForAssertion extends NodeAction {
    private final PrivateKeyable assertion;
    public EditKeyAliasForAssertion(AssertionTreeNode node) {
        super(node);
        assertion = (PrivateKeyable)node.asAssertion();
    }

    public String getName() {
        return "Set Private Key Alias";
    }

    public String getDescription() {
        return getName();
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    protected void performAction() {
        final Frame mw = TopComponents.getInstance().getTopParent();
        final AssertionKeyAliasEditor dlg = new AssertionKeyAliasEditor(mw, assertion);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                // post dlg code if necessary
            }
        });
    }
}

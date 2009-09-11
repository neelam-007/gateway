/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.console.panels.AddWssTimestampDialog;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.xmlsec.AddWssTimestamp;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.*;

/**
 * Edits the {@link com.l7tech.policy.assertion.xmlsec.AddWssTimestamp} properties.
 */
public class AddWssTimestampPropertiesAction extends NodeActionWithMetaSupport {
    static final Logger log = Logger.getLogger(AddWssTimestampPropertiesAction.class.getName());

    public AddWssTimestampPropertiesAction(AbstractTreeNode node) {
        super(node, AddWssTimestamp.class, node.asAssertion());
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
        final AddWssTimestamp ass = (AddWssTimestamp)node.asAssertion();
        Frame f = TopComponents.getInstance().getTopParent();
        final AddWssTimestampDialog dlg = new AddWssTimestampDialog(f, true, (AddWssTimestamp)(ass.clone()), !node.canEdit());
        Utilities.setEscKeyStrokeDisposes(dlg);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.wasOKed()) {
                    AddWssTimestamp newAss = dlg.getValue();
                    if (newAss == null) return;
                    ass.copyFrom(newAss);
                    JTree tree = TopComponents.getInstance().getPolicyTree();
                    if (tree != null) {
                        PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
                        model.assertionTreeNodeChanged((AssertionTreeNode)node);
                    } else {
                        log.log(Level.WARNING, "Unable to reach the palette tree.");
                    }
                }
            }
        });
    }

}

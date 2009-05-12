/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.console.panels.ResponseWssTimestampDialog;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.xmlsec.ResponseWssTimestamp;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.*;

/**
 * Edits the {@link com.l7tech.policy.assertion.xmlsec.ResponseWssTimestamp} properties.
 */
public class ResponseWssTimestampPropertiesAction extends NodeAction {
    static final Logger log = Logger.getLogger(ResponseWssTimestampPropertiesAction.class.getName());

    public ResponseWssTimestampPropertiesAction(AbstractTreeNode node) {
        super(node, ResponseWssTimestamp.class);
    }

    /**
     * @return the action name
     */
    @Override
    public String getName() {
        return "Response Timestamp Assertion Properties";
    }

    /**
     * @return the aciton description
     */
    @Override
    public String getDescription() {
        return "View/Edit Response Timestamp Assertion Properties";
    }

    /**
     * specify the resource name for this action
     */
    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/About16.gif";
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
        final ResponseWssTimestamp ass = (ResponseWssTimestamp)node.asAssertion();
        Frame f = TopComponents.getInstance().getTopParent();
        final ResponseWssTimestampDialog dlg = new ResponseWssTimestampDialog(f, true, (ResponseWssTimestamp)(ass.clone()), !node.canEdit());
        Utilities.setEscKeyStrokeDisposes(dlg);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.wasOKed()) {
                    ResponseWssTimestamp newAss = dlg.getValue();
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

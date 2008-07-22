/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.console.panels.RequestWssTimestampDialog;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.RequestWssTimestampPolicyNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.xmlsec.RequestWssTimestamp;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Edits the {@link com.l7tech.policy.assertion.xmlsec.RequestWssTimestamp} properties.
 */
public class RequestWssTimestampPropertiesAction extends NodeAction {
    static final Logger log = Logger.getLogger(RequestWssTimestampPropertiesAction.class.getName());

    public RequestWssTimestampPropertiesAction(RequestWssTimestampPolicyNode node) {
        super(node, RequestWssTimestamp.class);
    }

    /**
     * @return the action name
     */
    @Override
    public String getName() {
        return "Timestamp Properties";
    }

    /**
     * @return the aciton description
     */
    @Override
    public String getDescription() {
        return "View/Edit Timestamp Assertion Properties";
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
        final RequestWssTimestamp ass = (RequestWssTimestamp)node.asAssertion();
        Frame f = TopComponents.getInstance().getTopParent();
        final RequestWssTimestampDialog dlg = new RequestWssTimestampDialog(f, true, (RequestWssTimestamp)(ass.clone()), !node.canEdit());
        Utilities.setEscKeyStrokeDisposes(dlg);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                if (dlg.wasOKed()) {
                    RequestWssTimestamp newAss = dlg.getValue();
                    if (newAss == null) return;

                    ass.copyFrom(newAss);
                    JTree tree = TopComponents.getInstance().getPolicyTree();
                    if (tree != null) {
                        PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
                        model.assertionTreeNodeChanged((AssertionTreeNode)node);
                    } else {
                        RequestWssTimestampPropertiesAction.log.log(Level.WARNING, "Unable to reach the palette tree.");
                    }
                }
            }
        });
    }

}

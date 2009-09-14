/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.console.event.PolicyEvent;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.console.event.PolicyListenerAdapter;
import com.l7tech.console.panels.RequestSwAAssertionDialog;
import com.l7tech.console.panels.WorkSpacePanel;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.tree.EntityWithPolicyNode;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.RequestSwAAssertion;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RequestSwAAssertionPropertiesAction extends NodeActionWithMetaSupport {
    static final Logger log = Logger.getLogger(RequestSwAAssertionPropertiesAction.class.getName());

    public RequestSwAAssertionPropertiesAction(AssertionTreeNode node) {
        super(node, RequestSwAAssertion.class, node.asAssertion());
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    @Override
    protected void performAction() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Frame f = TopComponents.getInstance().getTopParent();

                WorkSpacePanel currentWorkSpace = TopComponents.getInstance().getCurrentWorkspace();
                JComponent currentPanel = currentWorkSpace.getComponent();
                if(currentPanel == null || !(currentPanel instanceof PolicyEditorPanel)) {
                    logger.warning("Internal error: current workspace is not a PolicyEditorPanel instance");
                } else {
                    final EntityWithPolicyNode pn = ((PolicyEditorPanel) currentPanel).getPolicyNode();
                    if (pn instanceof ServiceNode) {
                        ServiceNode sn = (ServiceNode) pn;
                        try {
                            if (!(sn.getEntity().isSoap())) {
                                JOptionPane.showMessageDialog(null, "This assertion is not supported by non-soap services.");
                            } else {
                                RequestSwAAssertionDialog d = new RequestSwAAssertionDialog(f, (RequestSwAAssertion)node.asAssertion());
                                d.setModal(true);
                                d.pack();
                                Utilities.centerOnScreen(d);
                                d.addPolicyListener(listener);
                                DialogDisplayer.display(d);
                            }
                        } catch (FindException e) {
                            log.log(Level.INFO, "Error getting Policy", e);
                        }
                    }
                }
            }
        });
    }

    private final PolicyListener listener = new PolicyListenerAdapter() {
        @Override
        public void assertionsChanged(PolicyEvent e) {
            JTree tree = TopComponents.getInstance().getPolicyTree();
            if (tree != null) {
                PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
                model.assertionTreeNodeChanged((AssertionTreeNode)node);
            } else {
                log.log(Level.WARNING, "Unable to reach the palette tree.");
            }
        }

    };
}

package com.l7tech.console.action;

import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.RequestSwAAssertionPolicyTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.console.event.PolicyListenerAdapter;
import com.l7tech.console.event.PolicyEvent;
import com.l7tech.console.panels.RequestSwAAssertionDialog;
import com.l7tech.console.panels.WorkSpacePanel;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.policy.assertion.RequestSwAAssertion;
import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class RequestSwAAssertionPropertiesAction extends NodeAction {
    static final Logger log = Logger.getLogger(RequestSwAAssertionPropertiesAction.class.getName());

    public RequestSwAAssertionPropertiesAction(RequestSwAAssertionPolicyTreeNode nodePolicy) {
        super(nodePolicy);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "SOAP Attachment Properties";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "View and edit SOAP attachment properties";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    protected void performAction() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame f = TopComponents.getInstance().getMainWindow();

                WorkSpacePanel currentWorkSpace = TopComponents.getInstance().getCurrentWorkspace();
                JComponent currentPanel = currentWorkSpace.getComponent();
                if(currentPanel == null || !(currentPanel instanceof PolicyEditorPanel)) {
                    logger.warning("Internal error: current workspace is not a PolicyEditorPanel instance");
                } else {
                     RequestSwAAssertionDialog d =
                            new RequestSwAAssertionDialog(f, (RequestSwAAssertion)node.asAssertion(), ((PolicyEditorPanel) currentPanel).getServiceNode());
                    d.setModal(true);
                    d.pack();
                    Utilities.centerOnScreen(d);
                    d.addPolicyListener(listener);
                    d.show();
                }
            }
        });
    }

    private final PolicyListener listener = new PolicyListenerAdapter() {
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
package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.event.PolicyEvent;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.console.event.PolicyListenerAdapter;
import com.l7tech.console.panels.RemoteIpRangePropertiesDialog;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.RemoteIpRangeTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.RemoteIpRange;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Action for viewing or editing the properties of a RemoteIpRange assertion.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 23, 2004<br/>
 * $Id$<br/>
 */
public class RemoteIpRangePropertiesAction extends SecureAction {

    public RemoteIpRangePropertiesAction(RemoteIpRangeTreeNode subject) {
        super(null, RemoteIpRange.class);
        this.subject = subject;
    }

    public String getName() {
        return "IP Address Range Properties";
    }

    public String getDescription() {
        return "View / Edit properties of an IP Address Range Assertion";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    protected void performAction() {
        Frame f = TopComponents.getInstance().getTopParent();
        RemoteIpRangePropertiesDialog dlg = new RemoteIpRangePropertiesDialog(f, false, subject.getAssertion());
        dlg.addPolicyListener(listener);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.setVisible(true);
    }

    private final PolicyListener listener = new PolicyListenerAdapter() {
        public void assertionsChanged(PolicyEvent e) {
            JTree tree = TopComponents.getInstance().getPolicyTree();
            if (tree != null) {
                PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
                model.assertionTreeNodeChanged(subject);
                log.finest("model invalidated");
            } else {
                log.log(Level.WARNING, "Unable to reach the palette tree.");
            }
        }
    };

    private final Logger log = Logger.getLogger(getClass().getName());
    private RemoteIpRangeTreeNode subject;
}

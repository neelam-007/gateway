package com.l7tech.console.action;

import com.l7tech.console.util.Registry;
import com.l7tech.console.util.ComponentRegistry;
import com.l7tech.console.panels.RemoteIpRangePropertiesDialog;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.console.event.PolicyListenerAdapter;
import com.l7tech.console.event.PolicyEvent;
import com.l7tech.console.tree.policy.RemoteIpRangeTreeNode;
import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Action for viewing or editing the properties of a RemoteIpRange assertion.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 23, 2004<br/>
 * $Id$<br/>
 *
 */
public class RemoteIpRangePropertiesAction extends BaseAction {

    public RemoteIpRangePropertiesAction(RemoteIpRangeTreeNode subject) {
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

    public void performAction() {
        Frame f = Registry.getDefault().getComponentRegistry().getMainWindow();
        RemoteIpRangePropertiesDialog dlg = new RemoteIpRangePropertiesDialog(f, false, subject.getAssertion());
        dlg.addPolicyListener(listener);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.show();
    }

    private final PolicyListener listener = new PolicyListenerAdapter() {
        public void assertionsChanged(PolicyEvent e) {
            JTree tree = ComponentRegistry.getInstance().getPolicyTree();
            if (tree != null) {
                DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                model.nodeChanged(subject);
                log.finest("model invalidated");
            } else {
                log.log(Level.WARNING, "Unable to reach the palette tree.");
            }
        }
    };

    private final Logger log = Logger.getLogger(getClass().getName());
    private RemoteIpRangeTreeNode subject;
}

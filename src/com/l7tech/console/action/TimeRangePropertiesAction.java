package com.l7tech.console.action;

import com.l7tech.console.tree.policy.TimeRangeTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.ComponentRegistry;
import com.l7tech.console.panels.TimeRangePropertiesDialog;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.console.event.PolicyListenerAdapter;
import com.l7tech.console.event.PolicyEvent;
import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.logging.Level;

/**
 * Action for viewing or editing the properties of a TimeRange assertion.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 19, 2004<br/>
 * $Id$
 * 
 */
public class TimeRangePropertiesAction extends BaseAction {
    public TimeRangePropertiesAction(TimeRangeTreeNode subject) {
        this.subject = subject;
    }

    public String getName() {
        return "Time And Day Availability Properties";
    }

    public String getDescription() {
        return "Change the properties of the time and day availability assertion.";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    public void performAction() {
        Frame f = Registry.getDefault().getComponentRegistry().getMainWindow();
        TimeRangePropertiesDialog dlg = new TimeRangePropertiesDialog(f, false, subject.getTimeRange());
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

    private TimeRangeTreeNode subject;
}

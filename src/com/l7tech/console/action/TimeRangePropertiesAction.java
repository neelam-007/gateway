package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.console.event.PolicyEvent;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.console.event.PolicyListenerAdapter;
import com.l7tech.console.panels.TimeRangePropertiesDialog;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.TimeRangeTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.TimeRange;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;

/**
 * Action for viewing or editing the properties of a TimeRange assertion.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 19, 2004<br/>
 * $Id$
 */
public class TimeRangePropertiesAction extends SecureAction {
    public TimeRangePropertiesAction(TimeRangeTreeNode subject) {
        super(null, TimeRange.class);
        this.subject = subject;
    }

    public String getName() {
        return "Time/Day Availability Properties";
    }

    public String getDescription() {
        return "Change the properties of the time and day availability assertion.";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    protected void performAction() {
        Frame f = TopComponents.getInstance().getTopParent();
        TimeRangePropertiesDialog dlg = new TimeRangePropertiesDialog(f, false, subject.getTimeRange());
        dlg.addPolicyListener(listener);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg);
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

    private TimeRangeTreeNode subject;
}

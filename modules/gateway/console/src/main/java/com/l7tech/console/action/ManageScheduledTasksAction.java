package com.l7tech.console.action;

import com.l7tech.console.panels.JdbcConnectionManagerWindow;
import com.l7tech.console.panels.ScheduledTaskWindow;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedAnyOperation;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;

/**
 */
public class ManageScheduledTasksAction extends SecureAction {
    public ManageScheduledTasksAction() {
        super(new AttemptedAnyOperation(EntityType.SCHEDULED_TASK), "service:Admin");
    }

    @Override
    public String getName() {
        return "Manage Scheduled Tasks";
    }

    @Override
    public String getDescription() {
        return "Create, edit, and remove scheduled tasks";
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/time.gif";
    }

    @Override
    protected void performAction() {
        ScheduledTaskWindow mgrWindow = new ScheduledTaskWindow(TopComponents.getInstance().getTopParent());
        mgrWindow.pack();
        Utilities.centerOnScreen(mgrWindow);
        DialogDisplayer.display(mgrWindow);
    }
}

package com.l7tech.console.action;

import com.l7tech.console.panels.WorkQueueManagerDialog;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedAnyOperation;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;

import java.util.ResourceBundle;

public class ManageWorkQueuesAction extends SecureAction {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.panels.resources.WorkQueueManagerDialog");

    public ManageWorkQueuesAction() {
        super(new AttemptedAnyOperation(EntityType.CASSANDRA_CONFIGURATION), UI_MANAGE_CASSANDRA_CONNECTIONS);
    }

    @Override
    public String getName() {
        return resources.getString("dialog.title.manage.work.queues");
    }

    @Override
    public String getDescription() {
        return resources.getString("action.description.manage.work.queues");
    }

    @Override
    protected String iconResource() {
        //TODO: add new icon
        return "com/l7tech/console/resources/testAction.gif";
    }

    @Override
    protected void performAction() {
        WorkQueueManagerDialog workQueueManagerDialog = new WorkQueueManagerDialog(TopComponents.getInstance().getTopParent());
        workQueueManagerDialog.pack();
        Utilities.centerOnScreen(workQueueManagerDialog);
        DialogDisplayer.display(workQueueManagerDialog);
    }
}

package com.l7tech.console.action;

import com.l7tech.console.panels.CassandraConnectionManagerDialog;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedAnyOperation;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;

import java.util.ResourceBundle;

/**
 * Copyright: Layer 7 Technologies, 2014
 * User: ymoiseyenko
 * Date: 11/3/14
 */
public class ManageCassandraConnectionAction extends SecureAction {

    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.panels.resources.CassandraConnectionManagerDialog");

    public ManageCassandraConnectionAction() {
        super(new AttemptedAnyOperation(EntityType.CASSANDRA_CONFIGURATION), UI_MANAGE_CASSANDRA_CONNECTIONS);
    }

    @Override
    public String getName() {
        return resources.getString("dialog.title.manage.cassandra.connections");
    }

    @Override
    public String getDescription() {
        return resources.getString("action.description.manage.cassandra.connections");
    }

    @Override
    protected String iconResource() {
        //TODO: add new icon
        return "com/l7tech/console/resources/PerformJdbcQuery16x16.gif";
    }

    @Override
    protected void performAction() {
        CassandraConnectionManagerDialog connectionManagerDialog = new CassandraConnectionManagerDialog(TopComponents.getInstance().getTopParent());
        connectionManagerDialog.pack();
        Utilities.centerOnScreen(connectionManagerDialog);
        DialogDisplayer.display(connectionManagerDialog);
    }
}

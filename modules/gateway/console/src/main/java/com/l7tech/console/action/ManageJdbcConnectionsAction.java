package com.l7tech.console.action;

import com.l7tech.gateway.common.security.rbac.AttemptedAnyOperation;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.console.panels.JdbcConnectionManagerWindow;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;

/**
 * @author: ghuang
 */
public class ManageJdbcConnectionsAction extends SecureAction {
    public ManageJdbcConnectionsAction() {
        super(new AttemptedAnyOperation(EntityType.JDBC_CONNECTION), "service:Admin");
    }

    @Override
    public String getName() {
        return "Manage JDBC Connections";
    }

    @Override
    public String getDescription() {
        return "Create, edit, and remove JDBC connections";
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/CreateIdentityProvider16x16.gif";
    }

    @Override
    protected void performAction() {
        JdbcConnectionManagerWindow connMgrWindow = new JdbcConnectionManagerWindow(TopComponents.getInstance().getTopParent());
        connMgrWindow.pack();
        Utilities.centerOnScreen(connMgrWindow);
        DialogDisplayer.display(connMgrWindow);
    }
}

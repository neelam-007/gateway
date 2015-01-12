package com.l7tech.console.action;

import com.l7tech.console.panels.ServerModuleFileManagerWindow;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedAnyOperation;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;

import java.util.ResourceBundle;

/**
 * Action that opens the Manage Encapsulated Assertions dialog.
 */
public class ManageServerModuleFilesAction extends SecureAction {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.panels.ServerModuleFileManagerWindow");

    public ManageServerModuleFilesAction() {
        super(new AttemptedAnyOperation( EntityType.SERVER_MODULE_FILE), "service:Admin" );
    }

    @Override
    public String getName() {
        return resources.getString("action.title.manage.server.module.files");
    }

    @Override
    public String getDescription() {
        return resources.getString("action.description.manage.server.module.files");
    }

    @Override
    protected String iconResource() {
        //TODO: Consider new icon
        return "com/l7tech/console/resources/Bean16.gif";
    }

    @Override
    protected void performAction() {
        final ServerModuleFileManagerWindow dlg = new ServerModuleFileManagerWindow( TopComponents.getInstance().getTopParent() );
        dlg.pack();
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg);
    }
}

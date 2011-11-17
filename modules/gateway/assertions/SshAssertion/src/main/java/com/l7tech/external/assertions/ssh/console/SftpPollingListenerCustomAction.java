package com.l7tech.external.assertions.ssh.console;

import com.l7tech.console.action.SecureAction;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedAnyOperation;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;

public class SftpPollingListenerCustomAction extends SecureAction {

    public SftpPollingListenerCustomAction() {
        super(new AttemptedAnyOperation(EntityType.SSG_ACTIVE_CONNECTOR));
    }

    @Override
    public String getName() {
        return "Manage SFTP Polling Listeners";
    }

    @Override
    public String getDescription() {
        return "Create, edit and remove configurations for SFTP polling listeners.";
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/interface.gif";
    }

    @Override
    protected void performAction() {
        final SftpPollingListenersWindow splw = new SftpPollingListenersWindow( TopComponents.getInstance().getTopParent());
        splw.pack();
        Utilities.centerOnParentWindow( splw );
        DialogDisplayer.display( splw );
    }
}

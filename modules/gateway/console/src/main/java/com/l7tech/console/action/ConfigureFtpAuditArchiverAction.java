package com.l7tech.console.action;

import com.l7tech.console.util.TopComponents;

import java.util.logging.Logger;

import com.l7tech.console.panels.FtpAuditArchiverPropertiesDialog;
import com.l7tech.gateway.common.security.rbac.AttemptedAnyOperation;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;


/**
 * Menu action for configuring the Audit Archiver FTP destination.
 *
 * @author jbufu
 */
public class ConfigureFtpAuditArchiverAction extends SecureAction {
    static final Logger log = Logger.getLogger(ManageEmailListenersAction.class.getName());

    public ConfigureFtpAuditArchiverAction() {
        super(new AttemptedAnyOperation(EntityType.ANY));
    }

    @Override
    public String getName() {
        return "Configure FTP Audit Archiver";
    }

    @Override
    public String getDescription() {
        return "Configure the FTP destination for archiving audit records.";
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/interface.gif";
    }

    @Override
    protected void performAction() {
        FtpAuditArchiverPropertiesDialog dialog = new FtpAuditArchiverPropertiesDialog(TopComponents.getInstance().getTopParent());
        dialog.pack();
        Utilities.centerOnScreen(dialog);
        DialogDisplayer.display(dialog);
    }
}


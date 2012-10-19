package com.l7tech.console.action;

import com.l7tech.console.panels.FtpAuditArchiverPropertiesDialog;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.security.rbac.AttemptedReadSpecific;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;

import java.util.logging.Logger;


/**
 * Menu action for configuring the Audit Archiver FTP destination.
 *
 * @author jbufu
 */
public class ConfigureFtpAuditArchiverAction extends SecureAction {
    static final Logger log = Logger.getLogger(ManageEmailListenersAction.class.getName());

    public ConfigureFtpAuditArchiverAction() {
        super(new AttemptedReadSpecific(EntityType.CLUSTER_PROPERTY, new ClusterProperty(FtpAuditArchiverPropertiesDialog.AUDIT_ARCHIVER_CONFIG_CLUSTER_PROPERTY_NAME, "")), "service:Admin");
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


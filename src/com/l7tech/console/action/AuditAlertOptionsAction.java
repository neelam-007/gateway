package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.security.rbac.AttemptedReadAny;
import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.console.panels.AuditAlertOptionsDialog;
import com.l7tech.console.panels.AuditAlertConfigBean;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.SsmPreferences;
import com.l7tech.console.AuditWatcher;
import com.l7tech.console.MainWindow;

import java.awt.*;
import java.util.logging.Level;

/**
 * User: megery
 * Date: Nov 7, 2006
 * Time: 10:54:58 AM
 */
public class AuditAlertOptionsAction extends SecureAction {
    private AuditAlertConfigBean configBean = null;

    public AuditAlertOptionsAction() {
        super(new AttemptedReadAny(EntityType.AUDIT_ADMIN));
    }

    public String getName() {
        return "View Audit Alert Options";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    protected void performAction() {
        AuditAlertOptionsDialog dlg = new AuditAlertOptionsDialog(TopComponents.getInstance().getTopParent());
        Utilities.centerOnScreen(dlg);
        dlg.pack();
        dlg.setVisible(true);
    }

    public AuditAlertConfigBean getConfigBean() {
        return configBean;
    }
}

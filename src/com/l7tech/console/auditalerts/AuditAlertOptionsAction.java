package com.l7tech.console.auditalerts;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.security.rbac.AttemptedReadAny;
import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.console.action.SecureAction;
import com.l7tech.console.util.TopComponents;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: Nov 7, 2006
 * Time: 10:54:58 AM
 */
public class AuditAlertOptionsAction extends SecureAction {
    private static final Logger logger = Logger.getLogger(AuditAlertOptionsAction.class.getName());
    private static AuditAlertOptionsAction instance;

    private java.util.List<AuditWatcher> auditWatchers;

    public static AuditAlertOptionsAction getInstance() {
        if (instance == null)
            instance = new AuditAlertOptionsAction();

        return instance;
    }

    private AuditAlertOptionsAction() {
        super(new AttemptedReadAny(EntityType.AUDIT_ADMIN));
        auditWatchers = new ArrayList<AuditWatcher>();
    }

    public void addAuditWatcher(AuditWatcher watcher) {
        if (watcher != null)
            auditWatchers.add(watcher);
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
        if (!dlg.wasCancelled()) {
            try {
                dlg.getConfigBean().savePreferences();
            } catch (IOException e) {
                logger.warning("Couldn't save preferences for audit alerts options: " + e.getMessage());
            }
            updateAuditWatchers(dlg.getConfigBean());
        }
    }

    private void updateAuditWatchers(AuditAlertConfigBean bean) {
        for (AuditWatcher auditWatcher : auditWatchers) {
            auditWatcher.alertSettingsChanged(bean);
        }
    }
}

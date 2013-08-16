package com.l7tech.console.auditalerts;

import com.l7tech.console.util.ConsoleLicenseManager;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.console.action.SecureAction;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.Registry;
import com.l7tech.console.panels.PermissionFlags;

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

    private final java.util.List<AuditWatcher> auditWatchers;

    public static AuditAlertOptionsAction getInstance() {
        if (instance == null)
            instance = new AuditAlertOptionsAction();

        return instance;
    }

    private AuditAlertOptionsAction() {
        super(null);
        auditWatchers = new ArrayList<>();
    }

    public void addAuditWatcher(AuditWatcher watcher) {
        if (watcher != null)
            auditWatchers.add(watcher);
    }

    public String getName() {
        return "Configure Audit Alert Options";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    @Override
    protected boolean isPrerequisiteFeatureSetLicensed(ConsoleLicenseManager lm) {
        // this action does not require service:Admin to be licensed
        return true;
    }

    public boolean isAuthorized() {
        return  Registry.getDefault().isAdminContextPresent() && (
                PermissionFlags.get(EntityType.AUDIT_ADMIN).canReadSome() ||
                PermissionFlags.get(EntityType.AUDIT_MESSAGE).canReadSome() ||
                PermissionFlags.get(EntityType.AUDIT_RECORD).canReadSome() ||
                PermissionFlags.get(EntityType.AUDIT_SYSTEM).canReadSome());
    }

    protected void performAction() {
        final AuditAlertOptionsDialog dlg = new AuditAlertOptionsDialog(TopComponents.getInstance().getTopParent());
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                if (!dlg.wasCancelled()) {
                    try {
                        dlg.getConfigBean().savePreferences();
                    } catch (IOException e) {
                        logger.warning("Couldn't save preferences for audit alerts options: " + e.getMessage());
                    }
                    updateAuditWatchers(dlg.getConfigBean());
                }
            }
        });
    }

    private void updateAuditWatchers(AuditAlertConfigBean bean) {
        for (AuditWatcher auditWatcher : auditWatchers) {
            auditWatcher.alertSettingsChanged(bean);
        }
    }
}

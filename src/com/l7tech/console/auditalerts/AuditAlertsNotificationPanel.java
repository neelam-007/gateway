package com.l7tech.console.auditalerts;

import com.l7tech.common.audit.LogonEvent;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.console.security.LogonListener;
import com.l7tech.console.util.Registry;
import com.l7tech.console.panels.PermissionFlags;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Level;

/**
 * User: megery
* Date: Nov 2, 2006
* Time: 2:57:33 PM
*/
public class AuditAlertsNotificationPanel extends JPanel implements AuditWatcher, LogonListener {
    Frame parentFrame;

    private JLabel alertBar;
    private JPanel mainPanel;

    private AuditAlertsDialog alertsDialog;
    private AuditAlertChecker checker;
    private boolean hasClusterPropertyPermissions = false;
    private boolean hasAuditPermissions = false;

    public AuditAlertsNotificationPanel(Frame parent, AuditAlertChecker auditChecker) {
        super();
        this.parentFrame = parent;
        this.checker = auditChecker;

        if (checker != null)
            checker.addWatcher(this);
        
        initComponents();
    }

    private void initComponents() {
        setupAlertBar();
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    private void setupAlertBar() {
            alertBar.setVisible(false);
            alertBar.setToolTipText("There are audit alerts that require your attention.");
            alertBar.setOpaque(true);
            alertBar.setForeground(Color.BLACK);
            alertBar.setBackground(Color.RED);
            alertBar.setRequestFocusEnabled(true);

            alertBar.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 1)
                        showPopup();
                }
            });
    }

    private void showPopup() {
        checker.stop();
        Utilities.centerOnScreen(getAlertsDialog());
        getAlertsDialog().setVisible(true);
    }

    private AuditAlertsDialog getAlertsDialog() {
        if (alertsDialog == null)
            alertsDialog = new AuditAlertsDialog(parentFrame, this);

        return alertsDialog;
    }

    private void setAlertsReady(boolean areAlertsReady) {
        if (areAlertsReady)
            alertBar.setVisible(true);
        else
            alertBar.setVisible(false);
    }

    public void auditsViewed() {
        setAlertsReady(false);
        checker.updateAuditsAcknowledgedTime();
        checker.start();
    }

    public void alertsAvailable(boolean alertsAreAvailable) {
        setAlertsReady(alertsAreAvailable);
    }

    public void alertSettingsChanged(AuditAlertConfigBean bean) {
        if (bean != null) {
            boolean isEnabled = bean.isEnabled();
            int checkInterval = bean.getAuditCheckInterval();
            Level checkLevel = bean.getAuditAlertLevel();
            if (hasPermissions()) checker.updateSettings(isEnabled, checkInterval, checkLevel);
        }
    }

    public void onLogon(LogonEvent e) {
        updatePermissions();
        if (hasPermissions() && checker != null) {
            checker.setClusterAdmin(Registry.getDefault().getClusterStatusAdmin());
            checker.setAuditAdmin(Registry.getDefault().getAuditAdmin());
            checker.checkForNewAlerts();
        }
    }

    private boolean hasPermissions() {
        return hasAuditPermissions && hasClusterPropertyPermissions;
    }

    public void onLogoff(LogonEvent e) {
        if (checker != null) {
            checker.setClusterAdmin(null);
            checker.setAuditAdmin(null);
            checker.stop();
        }
        setAlertsReady(false);
    }

    private void updatePermissions() {
        PermissionFlags perms = PermissionFlags.get(EntityType.CLUSTER_PROPERTY);
        hasClusterPropertyPermissions = perms.canReadSome() && perms.canUpdateSome();

        hasAuditPermissions =   PermissionFlags.get(EntityType.AUDIT_ADMIN).canReadSome() &&
                                PermissionFlags.get(EntityType.AUDIT_MESSAGE).canReadSome() &&
                                PermissionFlags.get(EntityType.AUDIT_RECORD).canReadSome() &&
                                PermissionFlags.get(EntityType.AUDIT_SYSTEM).canReadSome();
    }
}

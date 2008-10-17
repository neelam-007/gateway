package com.l7tech.console.auditalerts;

import com.l7tech.gateway.common.audit.LogonEvent;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.console.security.LogonListener;
import com.l7tech.console.util.Registry;
import com.l7tech.console.panels.PermissionFlags;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.logging.Level;

/**
 * User: megery
* Date: Nov 2, 2006
* Time: 2:57:33 PM
*/
public class AuditAlertsNotificationPanel extends JPanel implements AuditWatcher, LogonListener {
    private JPanel mainPanel;
    private JButton auditAlertsButton;

    private AuditAlertChecker checker;
    private long alertTime;
    private boolean hasAuditPermissions = false;

    public AuditAlertsNotificationPanel(AuditAlertChecker auditChecker) {
        super();
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
            mainPanel.setVisible(false);
            Icon icon = new ImageIcon(
                    AuditAlertsNotificationPanel.class.getResource("/com/l7tech/console/resources/Alert16x16.gif"));
            auditAlertsButton.setIcon(icon);
            auditAlertsButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                        showPopup();
                }
            });
    }

    private void showPopup() {
        checker.stop();

        AuditAlertsDialog auditAlertDialog = getAlertsDialog(alertTime);
        auditAlertDialog.pack();
        Utilities.centerOnScreen(auditAlertDialog);
        DialogDisplayer.display(auditAlertDialog);
    }

    private AuditAlertsDialog getAlertsDialog(long time) {
        AuditAlertsDialog auditAlertsDialog;
        Window ancestor = SwingUtilities.getWindowAncestor(this);
        if (ancestor instanceof Frame) {
            auditAlertsDialog = new AuditAlertsDialog((Frame)ancestor, this, time);
        } else if (ancestor instanceof Dialog) {
            auditAlertsDialog = new AuditAlertsDialog((Dialog)ancestor, this, time);
        } else {
            auditAlertsDialog = new AuditAlertsDialog((Frame)null, this, time);
        }
        return auditAlertsDialog;
    }

    private void setAlertsReady(boolean areAlertsReady) {
        if (areAlertsReady)
            mainPanel.setVisible(true);
        else
            mainPanel.setVisible(false);
    }

    public void auditsViewed() {
        setAlertsReady(false);
        checker.updateAuditsAcknowledgedTime();
        checker.start();
    }

    public void alertsAvailable(boolean alertsAreAvailable, long alertTime) {
        this.alertTime = alertTime;
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
            checker.setAuditAdmin(Registry.getDefault().getAuditAdmin());
            checker.start();
        }
    }

    private boolean hasPermissions() {
        return hasAuditPermissions;
    }

    public void onLogoff(LogonEvent e) {
        if (checker != null) {
            checker.stop();
            checker.setAuditAdmin(null);
        }
        setAlertsReady(false);
    }

    private void updatePermissions() {
        hasAuditPermissions = PermissionFlags.get(EntityType.AUDIT_ADMIN).canReadSome() ||
                              PermissionFlags.get(EntityType.AUDIT_MESSAGE).canReadSome() ||
                              PermissionFlags.get(EntityType.AUDIT_RECORD).canReadSome() ||
                              PermissionFlags.get(EntityType.AUDIT_SYSTEM).canReadSome();
    }
}

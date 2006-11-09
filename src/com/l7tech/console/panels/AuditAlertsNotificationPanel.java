package com.l7tech.console.panels;

import com.l7tech.common.audit.LogonEvent;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.AuditAlertsDialog;
import com.l7tech.console.AuditWatcher;
import com.l7tech.console.security.LogonListener;
import com.l7tech.console.util.Registry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Logger;

/**
 * User: megery
* Date: Nov 2, 2006
* Time: 2:57:33 PM
*/
public class AuditAlertsNotificationPanel extends JPanel implements AuditWatcher, LogonListener {
    private static final Logger logger = Logger.getLogger(AuditAlertsNotificationPanel.class.getName());

    Frame parentFrame;
//    private JLabel alertLabel;

    private JLabel alertBar;
    private JPanel mainPanel;

    private AuditAlertsDialog alertsDialog;
    private AuditAlertChecker checker;

    public AuditAlertsNotificationPanel(Frame parent, AuditAlertChecker auditChecker) {
        super();
        this.parentFrame = parent;
        this.checker = auditChecker;
        if (checker != null) checker.addWatcher(this);
        
        initComponents();
    }

    private void initComponents() {
        setupAlertBar();
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    private void setupAlertBar() {
            alertBar.setVisible(false);
            alertBar.setToolTipText("There are important audit records to view. Click here to view them.");
            alertBar.setOpaque(true);
            alertBar.setForeground(Color.BLACK);
            alertBar.setBackground(Color.RED);
            alertBar.setRequestFocusEnabled(true);

            alertBar.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 1) {
                        showPopup();
                    }
                }
            });
    }

    private void showPopup() {
        checker.stop();
        Utilities.centerOnScreen(getAlertsDialog());
        getAlertsDialog().setVisible(true);
    }

    private AuditAlertsDialog getAlertsDialog() {
        if (alertsDialog == null) {
            alertsDialog = new AuditAlertsDialog(parentFrame, this);
        }
        return alertsDialog;
    }

    private void setAlertsReady(boolean areAlertsReady) {
        if (areAlertsReady) {
//            alertBar.setText("Audit Alerts Waiting");
            alertBar.setVisible(true);
        }
        else {
            alertBar.setVisible(false);
        }
    }

    public void auditsViewed() {
        setAlertsReady(false);
        checker.updateAuditsAcknowledgedTime();
        checker.start();
    }

    public void alertsAvailable(boolean alertsAreAvailable) {
        setAlertsReady(alertsAreAvailable);
    }

    public void onLogon(LogonEvent e) {
        checker.setClusterAdmin(Registry.getDefault().getClusterStatusAdmin());
        checker.setAuditAdmin(Registry.getDefault().getAuditAdmin());
        checker.checkForNewAlerts();
    }

    public void onLogoff(LogonEvent e) {
        checker.setClusterAdmin(null);
        checker.setAuditAdmin(null);
        checker.stop();
        setAlertsReady(false);
    }

    public void updateSettings(String enabled, String intervalSeconds, String warningLevel) {
    }

}

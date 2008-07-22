package com.l7tech.console.auditalerts;

import com.l7tech.console.action.ViewGatewayAuditsAction;
import com.l7tech.console.action.BaseAction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class AuditAlertsDialog extends JDialog {
    private JPanel contentPane;
    private JButton viewAuditsButton;
    private JButton ignoreAuditsButton;
    private JButton viewOptionsButton;

    private final AuditWatcher watcher;
    private final long auditTime;

    public AuditAlertsDialog(Frame owner, AuditWatcher watcher, long time) throws HeadlessException {
        super(owner, "Audit Alerts");
        this.watcher = watcher;
        this.auditTime = time;
        init();
    }

    public AuditAlertsDialog(Dialog owner, AuditWatcher watcher, long time) throws HeadlessException {
        super(owner, "Audit Alerts");
        this.watcher = watcher;
        this.auditTime = time;
        init();
    }

    public void init() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(viewAuditsButton);

        viewAuditsButton.setAction(
                new ViewGatewayAuditsAction(auditTime){
                    protected void performAction() {
                        dispose();
                        if (watcher != null) watcher.auditsViewed();
                        super.performAction();
                    }

                    public String getName() {
                        return "View Audits";
                    }
                });

        ignoreAuditsButton.setAction(
                new BaseAction(){
                    public String getName() {
                        return "Acknowledge Audits";
                    }

                    protected String iconResource() {
                        return "com/l7tech/console/resources/AnalyzeGatewayLog_disabled16x16.gif";
                    }

                    protected void performAction() {
                        dispose();
                        if (watcher != null) watcher.auditsViewed();
                    }
                });

        viewOptionsButton.setAction(AuditAlertOptionsAction.getInstance());

// call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

// call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        pack();
    }

    private void onCancel() {
// add your code here if necessary
        dispose();
    }
}

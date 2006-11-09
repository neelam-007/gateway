package com.l7tech.console;

import com.l7tech.console.action.ViewGatewayAuditsAction;
import com.l7tech.console.action.AuditAlertOptionsAction;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;

public class AuditAlertsDialog extends JDialog {
    private JPanel contentPane;
    private JButton viewOptionsButton;
    private JButton buttonCancel;
    private JButton viewAuditsButton;

    private AuditWatcher watcher;
    private AuditAlertOptionsAction auditAlertOptionsAction;


    public AuditAlertsDialog(Frame owner, AuditWatcher watcher) throws HeadlessException {
        super(owner);
        this.watcher = watcher;
        init();
    }

    public void init() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(viewOptionsButton);

        viewAuditsButton.setAction(
                new ViewGatewayAuditsAction(){
                    protected void performAction() {
                        dispose();
                        if (watcher != null) watcher.auditsViewed();
                        super.performAction();
                    }
                });

        viewOptionsButton.setAction(getAuditAlertOptionsAction());

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

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

    private Action getAuditAlertOptionsAction() {
        if (auditAlertOptionsAction == null) {
            auditAlertOptionsAction = new AuditAlertOptionsAction();
        }
        return auditAlertOptionsAction;
    }

    private void viewAlertOptions() {
        dispose();
    }

    private void onCancel() {
// add your code here if necessary
        dispose();
    }
}

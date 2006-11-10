package com.l7tech.console.auditalerts;

import com.l7tech.console.action.ViewGatewayAuditsAction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class AuditAlertsDialog extends JDialog {
    private JPanel contentPane;
    private JButton viewOptionsButton;
    private JButton buttonCancel;
    private JButton viewAuditsButton;

    private AuditWatcher watcher;

    public AuditAlertsDialog(Frame owner, AuditWatcher watcher) throws HeadlessException {
        super(owner);
        this.watcher = watcher;
        init();
    }

    public void init() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(viewAuditsButton);

        viewAuditsButton.setAction(
                new ViewGatewayAuditsAction(){
                    protected void performAction() {
                        dispose();
                        if (watcher != null) watcher.auditsViewed();
                        super.performAction();
                    }
                });

        viewOptionsButton.setAction(AuditAlertOptionsAction.getInstance());

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

    private void onCancel() {
// add your code here if necessary
        dispose();
    }
}

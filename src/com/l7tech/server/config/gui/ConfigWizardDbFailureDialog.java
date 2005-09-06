package com.l7tech.server.config.gui;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.action.Actions;
import com.l7tech.console.panels.Wizard;
import com.l7tech.server.config.DBActions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 17, 2005
 * Time: 10:39:46 AM
 * To change this template use File | Settings | File Templates.
 */
public class ConfigWizardDbFailureDialog extends JDialog {
    private JPanel mainPanel;
    private JPanel inputPanel;

    private JPanel reasonPanel;
    private JButton okButton;
    private JButton cancelButton;

    public ConfigWizardDbFailureDialog(JDialog owner, String title, int reason) throws HeadlessException {
        super(owner, title, true);
        doInit(reason);
    }

    private void doInit(final int reason) {
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
        inputPanel.setLayout(new BorderLayout());
        switch (reason) {
            case DBActions.DB_UNKNOWNDB_FAILURE:
                reasonPanel = new DBMissingPanel();
                break;
            case DBActions.DB_AUTHORIZATION_FAILURE:
                reasonPanel = new DBAuthenticationFailedPanel();
                break;
            default:
                reasonPanel = new DBAuthenticationFailedPanel();
                break;
        }
        inputPanel.add(reasonPanel, BorderLayout.CENTER);
        Actions.setEscKeyStrokeDisposes(this);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                cancel(new ActionEvent(ConfigWizardDbFailureDialog.this, 0, ""));
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancel(new ActionEvent(ConfigWizardDbFailureDialog.this, 0, ""));
            }
        });

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean success = retry(reason);
                while(!success) {
                    retry(reason);
                }
                cancel(new ActionEvent(ConfigWizardDbFailureDialog.this, 0, ""));
            }
        });

        pack();
        Utilities.centerOnScreen(this);
    }

    private boolean retry(int reason) {
        boolean retrySuccess = false;
        if (reason == DBActions.DB_AUTHORIZATION_FAILURE) {
            System.out.println("Retry DB auth failure with following creds: ");
            DBAuthenticationFailedPanel dbafp = (DBAuthenticationFailedPanel) reasonPanel;
            System.out.println("username = " + dbafp.getUsername());
            System.out.println("pasword = " + dbafp.getPassword());
            retrySuccess = true;
        }
        if (reason == DBActions.DB_UNKNOWNDB_FAILURE) {
            System.out.println("Retry DB missing failure with following creds: ");
            DBMissingPanel dbmp = (DBMissingPanel) reasonPanel;
            System.out.println("username = " + dbmp.getUsername());
            System.out.println("password = " + dbmp.getPassword());
            retrySuccess = true;
        }
        return retrySuccess;
    }

    private void cancel(ActionEvent event) {
        setVisible(false);
        dispose();
    }
}

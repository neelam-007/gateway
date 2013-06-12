package com.l7tech.console.api;

import com.l7tech.console.panels.SecurePasswordComboBox;
import com.l7tech.console.panels.SecurePasswordManagerWindow;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.policy.assertion.ext.commonui.CustomSecurePasswordPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;

/**
 * Implementation of CustomSecurePasswordPanel interface.
 */
public class CustomSecurePasswordPanelImpl implements CustomSecurePasswordPanel {

    private static final int COMBO_BOX_MIN_WIDTH = 50;
    private static final int COMBO_BOX_PREFERRED_WIDTH = 100;

    private final SecurePasswordComboBox securePasswordComboBox;
    private final SecurePassword.SecurePasswordType typeFilter;
    private final JDialog parent;
    private final JPanel mainPanel;
    private final JButton managePasswordsButton;
    private boolean isManagePasswordsButtonDisplayed;
    private List<ManagePasswordsDialogClosedListener> managePasswordsDialogClosedListeners;

    public CustomSecurePasswordPanelImpl(SecurePassword.SecurePasswordType typeFilter, JDialog owner) {
        securePasswordComboBox = new SecurePasswordComboBox(typeFilter);
        securePasswordComboBox.setRenderer(TextListCellRenderer.<SecurePasswordComboBox>basicComboBoxRenderer());
        securePasswordComboBox.setMinimumSize(new Dimension(COMBO_BOX_MIN_WIDTH, -1));
        securePasswordComboBox.setPreferredSize(new Dimension(COMBO_BOX_PREFERRED_WIDTH, -1));
        this.typeFilter = typeFilter;
        parent = owner;

        managePasswordsButton = new JButton("Manage Passwords");
        managePasswordsButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {

                Window parentWindow = parent;
                if (parentWindow == null) {
                    parentWindow = TopComponents.getInstance().getTopParent();
                }
                final SecurePasswordManagerWindow dialog = new SecurePasswordManagerWindow(parentWindow);

                dialog.pack();
                Utilities.centerOnScreen(dialog);
                DialogDisplayer.display(dialog, new Runnable() {
                    @Override
                    public void run() {
                        onManagePasswordsDialogClosed();
                    }
                });
            }
        });

        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
        mainPanel.add(securePasswordComboBox);
        mainPanel.add(Box.createHorizontalStrut(10)); // Add a spacer between combo box and button.
        mainPanel.add(managePasswordsButton);
        isManagePasswordsButtonDisplayed = true;
    }

    @Override
    public void setEnabled (boolean enabled) {
        securePasswordComboBox.setEnabled(enabled);
        managePasswordsButton.setEnabled(enabled);
    }

    @Override
    public long getSelectedItem() {
        SecurePassword securePassword = securePasswordComboBox.getSelectedSecurePassword();
        if (securePassword == null) {
            return -1L;
        } else {
            return securePasswordComboBox.getSelectedSecurePassword().getOid();
        }
    }

    @Override
    public void setSelectedItem (long oid) {
        securePasswordComboBox.setSelectedSecurePassword(oid);
    }

    @Override
    public  boolean isItemSelected() {
        return (this.getSelectedItem() != -1L);
    }

    @Override
    public boolean containsItem (long oid) {
        return securePasswordComboBox.containsItem(oid);
    }

    @Override
    public void reloadComboBox() {
        securePasswordComboBox.reloadPasswordList(typeFilter);
    }

    @Override
    public void addListener (ManagePasswordsDialogClosedListener listener) {
        if (managePasswordsDialogClosedListeners == null) {
            managePasswordsDialogClosedListeners = new LinkedList<>();
        }
        managePasswordsDialogClosedListeners.add(listener);
    }

    @Override
    public void setDisplayManagePasswordsButton (boolean display) {
        if (display && !isManagePasswordsButtonDisplayed) {
            mainPanel.add(managePasswordsButton);
            mainPanel.validate();
            isManagePasswordsButtonDisplayed = true;
        } else if (!display && isManagePasswordsButtonDisplayed) {
            mainPanel.remove(managePasswordsButton);
            mainPanel.validate();
            isManagePasswordsButtonDisplayed = false;
        }
    }

    @Override
    public JPanel getPanel() {
        return mainPanel;
    }

    @Override
    public JComboBox getPasswordComboBox() {
        return securePasswordComboBox;
    }

    private void onManagePasswordsDialogClosed() {
        // Reload secure password combo box.
        //
        reloadComboBox();

        // Call registered callback methods.
        //
        for (ManagePasswordsDialogClosedListener listener : managePasswordsDialogClosedListeners) {
            listener.onClosed();
        }
    }
}
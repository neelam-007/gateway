package com.l7tech.console.api;

import com.l7tech.console.panels.SecurePasswordComboBox;
import com.l7tech.console.panels.SecurePasswordManagerWindow;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.Goid;
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

    private static final int MANAGE_PASSWORD_BUTTON_PANEL_FIXED_WIDTH = 125;
    private static final int MANAGE_PASSWORD_BUTTON_PANEL_FIXED_HEIGHT = 23;

    private final SecurePasswordComboBox securePasswordComboBox;
    private final SecurePassword.SecurePasswordType typeFilter;
    private final JDialog parent;
    private final JPanel mainPanel;
    private final JButton managePasswordsButton;
    private List<ManagePasswordsDialogClosedListener> managePasswordsDialogClosedListeners;

    public CustomSecurePasswordPanelImpl(SecurePassword.SecurePasswordType typeFilter, JDialog owner) {
        securePasswordComboBox = new SecurePasswordComboBox(typeFilter);
        securePasswordComboBox.setRenderer(TextListCellRenderer.basicComboBoxRenderer());
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
                Utilities.centerOnParentWindow(dialog);
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

        // Create a panel that contains manage passwords button.
        // This panel is fixed size, so that if manage passwords button is hidden,
        // the password combo box remains consistent as though manage password button is
        // still displayed.
        JPanel managePasswordsButtonPanel = new JPanel();
        Dimension dimension = new Dimension(MANAGE_PASSWORD_BUTTON_PANEL_FIXED_WIDTH, MANAGE_PASSWORD_BUTTON_PANEL_FIXED_HEIGHT);
        managePasswordsButtonPanel.setPreferredSize(dimension);
        managePasswordsButtonPanel.setMaximumSize(dimension);
        managePasswordsButtonPanel.setMinimumSize(dimension);
        managePasswordsButtonPanel.setLayout(new BorderLayout());
        managePasswordsButtonPanel.add(managePasswordsButton, BorderLayout.CENTER);

        mainPanel.add(managePasswordsButtonPanel);
    }

    @Override
    public void setEnabled (boolean enabled) {
        securePasswordComboBox.setEnabled(enabled);
        managePasswordsButton.setEnabled(enabled);
    }

    @Override
    public String getSelectedItem() {
        SecurePassword securePassword = securePasswordComboBox.getSelectedSecurePassword();
        if (securePassword == null) {
            return null;
        } else {
            return securePasswordComboBox.getSelectedSecurePassword().getGoid().toString();
        }
    }

    @Override
    public void setSelectedItem (String id) {
        securePasswordComboBox.setSelectedSecurePassword(new Goid(id));
    }

    @Override
    public boolean isItemSelected() {
        return securePasswordComboBox.getSelectedSecurePassword() != null;
    }

    @Override
    public boolean containsItem (String id) {
        return securePasswordComboBox.containsItem(new Goid(id));
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
        managePasswordsButton.setVisible(display);
        managePasswordsButton.setEnabled(display);
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

        // Repack the parent when the password dialog disposes, just in case the size of the parent dialog is changed.
        DialogDisplayer.pack(parent);
    }
}
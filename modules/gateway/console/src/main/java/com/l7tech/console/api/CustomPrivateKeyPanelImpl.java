package com.l7tech.console.api;

import com.l7tech.console.panels.PrivateKeyManagerWindow;
import com.l7tech.console.panels.PrivateKeysComboBox;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.ext.commonui.CustomPrivateKeyPanel;
import com.l7tech.policy.assertion.ext.security.SignerServices;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;

/**
 * Implementation of CustomPrivateKeyPanel interface.
 */
public class CustomPrivateKeyPanelImpl implements CustomPrivateKeyPanel {
    private static final boolean DEFAULT_INCLUDE_HARDWARE_KEYSTORE = true;
    private static final boolean DEFAULT_INCLUDE_DEFAULT_SSL_KEY = true;
    private static final boolean DEFAULT_INCLUDE_DEFAULT_RESTRICTED_ACCESS_KEYS = true;

    private final JDialog parent;
    private final JPanel mainPanel;
    private final PrivateKeysComboBox privateKeysComboBox;
    private final JButton managePrivateKeysButton;
    private final List<ManagePrivateKeysDialogClosedListener> listeners;

    public CustomPrivateKeyPanelImpl(JDialog owner) {
        parent = owner;

        privateKeysComboBox = new PrivateKeysComboBox(
            DEFAULT_INCLUDE_HARDWARE_KEYSTORE,
            DEFAULT_INCLUDE_DEFAULT_SSL_KEY,
            DEFAULT_INCLUDE_DEFAULT_RESTRICTED_ACCESS_KEYS);
        //noinspection unchecked
        privateKeysComboBox.setRenderer(TextListCellRenderer.basicComboBoxRenderer());

        managePrivateKeysButton = new JButton("Manage Private Keys");
        managePrivateKeysButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Window parentWindow = parent;
                if (parentWindow == null) {
                    parentWindow = TopComponents.getInstance().getTopParent();
                }
                PrivateKeyManagerWindow dialog = new PrivateKeyManagerWindow(parentWindow);
                dialog.pack();
                Utilities.centerOnParentWindow(dialog);
                DialogDisplayer.display(dialog, new Runnable() {
                        @Override
                        public void run() {
                            onManagePrivateKeysDialogClosed();
                        }
                    }
                );
            }
        });

        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
        mainPanel.add(privateKeysComboBox);
        mainPanel.add(Box.createHorizontalStrut(10)); // Add a spacer between combo box and button.

        // Create a panel that contains manage private keys button.
        // This panel is fixed size, so that if manage passwords button is hidden,
        // the password combo box remains consistent as though manage private keys button is
        // still displayed.
        JPanel managePrivateKeysButtonPanel = new JPanel();
        Dimension dimension = managePrivateKeysButton.getPreferredSize();
        managePrivateKeysButtonPanel.setPreferredSize(dimension);
        managePrivateKeysButtonPanel.setMaximumSize(dimension);
        managePrivateKeysButtonPanel.setMinimumSize(dimension);
        managePrivateKeysButtonPanel.setLayout(new BorderLayout());
        managePrivateKeysButtonPanel.add(managePrivateKeysButton, BorderLayout.CENTER);

        mainPanel.add(managePrivateKeysButtonPanel);

        listeners = new LinkedList<>();
    }

    @Override
    public void setEnabled(boolean enabled) {
        privateKeysComboBox.setEnabled(enabled);
        managePrivateKeysButton.setEnabled(enabled);
    }

    @Override
    public String getSelectedItem() {
        if (this.isItemSelected()) {
            if (privateKeysComboBox.isSelectedDefaultSsl()) {
                return SignerServices.KEY_ID_SSL;
            } else {
                Goid keyStoreId = privateKeysComboBox.getSelectedKeystoreId();
                String keyAlias = privateKeysComboBox.getSelectedKeyAlias();
                return keyStoreId.toString() + ":" + keyAlias;
            }
        } else {
            return null;
        }
    }

    @Override
    public void setSelectedItem(String keyId) {
        if (keyId == null || keyId.trim().length() == 0) {
            privateKeysComboBox.selectDefaultSsl();
        } else if (SignerServices.KEY_ID_SSL.equals(keyId)) {
            privateKeysComboBox.selectDefaultSsl();
        } else {
            String[] keyIdSplit = keyId.split(":");
            if (keyIdSplit.length != 2) {
                privateKeysComboBox.setSelectedIndex(-1);
            } else {
                try {
                    privateKeysComboBox.select(Goid.parseGoid(keyIdSplit[0]), keyIdSplit[1]);
                } catch (IllegalArgumentException e) {
                    privateKeysComboBox.setSelectedIndex(-1);
                }
            }
        }
    }

    @Override
    public boolean isItemSelected() {
        return (privateKeysComboBox.getSelectedIndex() != -1);
    }

    @Override
    public void reloadComboBox() {
        privateKeysComboBox.repopulate();
    }

    @Override
    public void setIncludeDefaultSslKey(boolean includeDefaultSslKey) {
        privateKeysComboBox.setIncludeDefaultSslKey(includeDefaultSslKey);
    }

    @Override
    public void setDisplayManagePrivateKeysButton(boolean display) {
        managePrivateKeysButton.setVisible(display);
        managePrivateKeysButton.setEnabled(display);
    }

    @Override
    public void addListener(ManagePrivateKeysDialogClosedListener listener) {
        listeners.add(listener);
    }

    @Override
    public JPanel getPanel() {
        return mainPanel;
    }

    private void onManagePrivateKeysDialogClosed() {
        // Reload private keys combo box.
        //
        this.reloadComboBox();

        // Call registered callback methods.
        //
        for (ManagePrivateKeysDialogClosedListener listener : listeners) {
            listener.onClosed();
        }

        // Repack the parent when the manage private keys dialog disposes, just in case the size of the parent dialog is changed.
        if (parent != null) {
            DialogDisplayer.pack(parent);
        }
    }
}

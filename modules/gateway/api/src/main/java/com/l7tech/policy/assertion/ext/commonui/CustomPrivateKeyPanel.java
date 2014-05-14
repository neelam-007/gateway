package com.l7tech.policy.assertion.ext.commonui;

import javax.swing.*;

/**
 * The private key panel. The private key panel contains a combo box populated with Gateway private keys,
 * and a button to open the Manage Private Keys dialog. The Manage Private Keys dialog can be used to add, edit,
 * delete Gateway private keys. See {@link CommonUIServices#createPrivateKeyComboBoxPanel(javax.swing.JDialog)}.
 */
public interface CustomPrivateKeyPanel {

    /**
     * The listener interface. When the Manage Private Keys dialog is closed,
     * the onClosed method is invoked.
     */
    interface ManagePrivateKeysDialogClosedListener {
        /**
         * Invoked when the Manage Private Keys dialog is closed.
         */
        void onClosed();
    }

    /**
     *  Sets whether or not this component is enabled.
     *
     * @param enabled true if this component should be enabled, false otherwise
     */
    void setEnabled(boolean enabled);

    /**
     * Gets the selected private key in the combo box.
     *
     * @return the key ID of the selected private key. Null if a private key is not selected.
     */
    String getSelectedItem();

    /**
     * Selects the specified private key in the combo box.
     *
     * @param keyId the key ID of the private key to select
     */
    void setSelectedItem(String keyId);

    /**
     * Checks whether or not a private key is selected in the combo box.
     *
     * @return true if a private key is selected, false otherwise.
     */
    boolean isItemSelected();

    /**
     * Reloads the combo box.
     */
    void reloadComboBox();

    /**
     * Sets whether or not to include the default SSL key. Must call {@link #reloadComboBox()} after
     * calling this method to reload the private keys combo box.
     * <p>Default: true</p>
     *
     * @param includeDefaultSslKey true to include the default SSL key, false otherwise
     */
    void setIncludeDefaultSslKey(boolean includeDefaultSslKey);

    /**
     * Sets whether or not to display the "Manage Private Keys" button.
     *
     * @param display true to display the "Manage Private Keys" button, false otherwise
     */
    void setDisplayManagePrivateKeysButton(boolean display);

    /**
     * Adds a listener to be invoked when the Manage Private Keys dialog is closed.
     *
     * @param listener the listener to invoke
     */
    void addListener (ManagePrivateKeysDialogClosedListener listener);

    /**
     * Gets the private key panel.
     *
     * @return the panel
     */
    JPanel getPanel();
}
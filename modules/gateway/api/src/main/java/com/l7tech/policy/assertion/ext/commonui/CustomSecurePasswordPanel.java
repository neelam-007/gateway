package com.l7tech.policy.assertion.ext.commonui;

import javax.swing.*;

/**
 * The secure password panel. The secure password panel contains a combo box populated with passwords or PEM private keys,
 * and a button to open the Manage Passwords dialog. The Manage Password dialog can be used to add, edit, delete passwords
 * and PEM private keys in the SSG.
 */
public interface CustomSecurePasswordPanel {

    /**
     * The listener interface. When the Manage Passwords dialog is closed,
     * the onClosed method is invoked.
     */
    public interface ManagePasswordsDialogClosedListener {
        /**
         * Invoked when the Manage Passwords dialog is closed.
         */
        void onClosed();
    }

    /**
     *  Sets whether or not this component is enabled.
     *
     * @param enabled true if this component should be enabled, false otherwise
     */
    void setEnabled (boolean enabled);

    /**
     * Gets the selected password in the combo box.
     *
     * @return the ID of the selected password. Null if a password is not selected.
     */
    String getSelectedItem();

    /**
     * Selects the specified password in the combo box.
     *
     * @param id the ID of the password to select
     */
    void setSelectedItem (String id);

    /**
     * Checks whether or not a password is selected in the combo box.
     *
     * @return true if a password is selected, false otherwise.
     */
    boolean isItemSelected();

    /**
     * Checks whether or not the specified password is in the combo box.
     *
     * @param id the ID of the password
     * @return true if the password is in the combo box, false otherwise.
     */
    boolean containsItem (String id);

    /**
     * Reloads the combo box.
     */
    void reloadComboBox();

    /**
     * Sets whether or not to display the "Manage Passwords" button.
     *
     * @param display true to display the "Manage Passwords" button, false otherwise
     */
    void setDisplayManagePasswordsButton (boolean display);

    /**
     * Adds an listener to be invoked when the Manage Passwords dialog is closed.
     *
     * @param listener the listener to invoke
     */
    void addListener (ManagePasswordsDialogClosedListener listener);

    /**
     * Gets the secure password panel.
     * @return the panel
     */
    JPanel getPanel();

    /**
     * Gets the password combo box.
     *
     * @return the password combo box
     */
    JComboBox getPasswordComboBox();
}
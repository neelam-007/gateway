package com.l7tech.policy.assertion.ext.commonui;

import javax.swing.*;

/**
 * Use this service to create common UI components.
 */
public interface CommonUIServices {

    /**
     * The key for retrieving {@link CommonUIServices} from the Console Context.<p>
     * See {@link com.l7tech.policy.assertion.ext.cei.UsesConsoleContext UsesConsoleContext} for more details.
     */
    static final String CONSOLE_CONTEXT_KEY = "commonUIServices";

    /**
     * Creates a target variable panel.
     *
     * @return the target variable panel
     */
    CustomTargetVariablePanel createTargetVariablePanel();

    /**
     * Creates a secure password panel with password combo box.
     *
     * @param owner the owner dialog
     * @return the secure password panel
     */
    CustomSecurePasswordPanel createPasswordComboBoxPanel (JDialog owner);

    /**
     * Create a secure password panel with PEM private key combo box.
     *
     * @param owner the owner dialog
     * @return the secure password panel
     */
    CustomSecurePasswordPanel createPEMPrivateKeyComboBoxPanel (JDialog owner);

    /**
     * Create a private key panel with private keys combo box.
     *
     * @param owner the owner dialog
     * @return the private key panel
     */
    CustomPrivateKeyPanel createPrivateKeyComboBoxPanel (JDialog owner);
}
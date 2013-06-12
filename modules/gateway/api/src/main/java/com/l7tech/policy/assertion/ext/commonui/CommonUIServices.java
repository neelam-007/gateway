package com.l7tech.policy.assertion.ext.commonui;

import javax.swing.*;

/*
* Use this service to create common UI components.
* This service can be retrieved from the Console Context using "commonUIServices" as the key.
*/
public interface CommonUIServices {

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
}
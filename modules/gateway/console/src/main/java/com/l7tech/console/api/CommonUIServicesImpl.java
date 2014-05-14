package com.l7tech.console.api;

import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.ext.commonui.CommonUIServices;
import com.l7tech.policy.assertion.ext.commonui.CustomPrivateKeyPanel;
import com.l7tech.policy.assertion.ext.commonui.CustomSecurePasswordPanel;
import com.l7tech.policy.assertion.ext.commonui.CustomTargetVariablePanel;

import javax.swing.*;

/**
 * Implementation of CommonUIServices interface.
 */
public class CommonUIServicesImpl implements CommonUIServices {
    private final Assertion assertion;
    private final Assertion previousAssertion;

    public CommonUIServicesImpl (final Assertion assertion, final Assertion previousAssertion) {
        this.assertion = assertion;
        this.previousAssertion = previousAssertion;
    }

    @Override
    public CustomTargetVariablePanel createTargetVariablePanel () {
        return new CustomTargetVariablePanelImpl(assertion, previousAssertion);
    }

    @Override
    public CustomSecurePasswordPanel createPasswordComboBoxPanel (JDialog owner) {
        return new CustomSecurePasswordPanelImpl(SecurePassword.SecurePasswordType.PASSWORD, owner);
    }

    @Override
    public CustomSecurePasswordPanel createPEMPrivateKeyComboBoxPanel (JDialog owner) {
        return new CustomSecurePasswordPanelImpl(SecurePassword.SecurePasswordType.PEM_PRIVATE_KEY, owner);
    }

    @Override
    public CustomPrivateKeyPanel createPrivateKeyComboBoxPanel(JDialog owner) {
        return new CustomPrivateKeyPanelImpl(owner);
    }
}
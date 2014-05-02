package com.l7tech.console.api;

import com.l7tech.console.policy.SsmPolicyVariableUtils;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.ext.commonui.CommonUIServices;
import com.l7tech.policy.assertion.ext.commonui.CustomSecurePasswordPanel;
import com.l7tech.policy.assertion.ext.commonui.CustomTargetVariablePanel;
import com.l7tech.policy.variable.VariableMetadata;

import javax.swing.*;
import java.util.Collections;
import java.util.Map;

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
    public Map<String, VariableMetadata> getVariablesSetByPredecessors() {
        if (assertion.getParent() != null) {
            // Editing an assertion that has already been added in policy.
            return SsmPolicyVariableUtils.getVariablesSetByPredecessors(assertion);
        } else if (previousAssertion != null) {
            // Adding a new assertion to policy.
            return SsmPolicyVariableUtils.getVariablesSetByPredecessorsAndSelf(previousAssertion);
        } else {
            return Collections.emptyMap();
        }
    }
}
package com.l7tech.external.assertions.generatepassword.server;

import com.l7tech.external.assertions.generatepassword.GeneratePasswordAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ConfigFactory;

import java.io.IOException;

/**
 * Server implementation for Assertion to generate random characters for password
 *
 * @author rraquepo, 4/9/14
 */
public class ServerGeneratePasswordAssertion extends AbstractServerAssertion<GeneratePasswordAssertion> {

    public ServerGeneratePasswordAssertion(final GeneratePasswordAssertion assertion) {
        super(assertion);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final String numberParam = ExpandVariables.process("${" + GeneratePasswordAssertion.PARAM_NUMBERS_CHARACTERS_LENGTH + "}", context.getVariableMap(assertion.getVariablesUsed(), getAudit()), getAudit());
        final String lowerCaseParam = ExpandVariables.process("${" + GeneratePasswordAssertion.PARAM_LOWERCASE_CHARACTERS_LENGTH + "}", context.getVariableMap(assertion.getVariablesUsed(), getAudit()), getAudit());
        final String upperCaseParam = ExpandVariables.process("${" + GeneratePasswordAssertion.PARAM_UPPERCASE__CHARACTERS_LENGTH + "}", context.getVariableMap(assertion.getVariablesUsed(), getAudit()), getAudit());
        final String specialCharParam = ExpandVariables.process("${" + GeneratePasswordAssertion.PARAM_SPECIAL_CHARACTERS + "}", context.getVariableMap(assertion.getVariablesUsed(), getAudit()), getAudit());
        int numberLength = getIntValue(GeneratePasswordAssertion.PARAM_NUMBERS_CHARACTERS_LENGTH, numberParam);
        int lowerCaseLength = getIntValue(GeneratePasswordAssertion.PARAM_LOWERCASE_CHARACTERS_LENGTH, lowerCaseParam);
        int upperCaseLength = getIntValue(GeneratePasswordAssertion.PARAM_UPPERCASE__CHARACTERS_LENGTH, upperCaseParam);
        int specialCharLength = getIntValue(GeneratePasswordAssertion.PARAM_SPECIAL_CHARACTERS, specialCharParam);
        int maxLength = ConfigFactory.getIntProperty(GeneratePasswordAssertion.MAX_LENGTH_PASSWORD_CONFIG, GeneratePassword.MAX_PASSWORD_LENGTH);
        context.setVariable(GeneratePasswordAssertion.RESPONSE_PASSWORD, GeneratePassword.generatePassword(numberLength, lowerCaseLength, specialCharLength, upperCaseLength, maxLength));
        return AssertionStatus.NONE;
    }

    int getIntValue(String paramName, String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            String errorMsg = "Invalid value {" + value + "} for " + paramName + " : " + e.getMessage();
            logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, new String[]{errorMsg});
        }
        return 0;
    }
}


package com.l7tech.external.assertions.jsonwebtoken.console;

import com.l7tech.console.util.VariablePrefixUtil;
import com.l7tech.external.assertions.jsonwebtoken.JwtDecodeAssertion;
import com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.JwtUtilities;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.validator.AssertionValidator;
import com.l7tech.policy.validator.PolicyValidationContext;
import com.l7tech.util.GoidUpgradeMapper;

/**
 * User: rseminoff
 * Date: 20/12/12
 */
public class JwtDecodeValidator implements AssertionValidator {

    private final JwtDecodeAssertion myAssertion;

    public JwtDecodeValidator(JwtDecodeAssertion assertion) {
        this.myAssertion = assertion;
    }

    private void validateReadVariable(String variableTitle, String variable, PolicyValidatorResult result) {
        if (variable.isEmpty()) {
            result.addError(new PolicyValidatorResult.Error(myAssertion, "The " + variableTitle + " variable is not set.", null));
        } else {
            VariableValidation varValidator = new VariableValidation(myAssertion);
            varValidator.allowEmptyVariables(false);  // No empty vars allowed.
            String varCheck = VariablePrefixUtil.fixVariableName(variable);
            VariableValidation.VariableStatus currentStatus = varValidator.readVariableIsValid(varCheck);

            switch (currentStatus) {
                case INVALID_SYNTAX: {
                    result.addError(new PolicyValidatorResult.Error(myAssertion, "The variable '" + variable + "' is an invalid variable name.", null));
                    break;
                }
            }
        }
    }

    @Override
    public void validate(AssertionPath path, PolicyValidationContext pvc, PolicyValidatorResult result) {

        // This confirms the following information for the JWT Encode Assertion:
        // 1. The JSON Payload variable is specified and exists
        // 2. The Signature Algorithm, if it's a variable, is specified and exists.
        // 3. The Secret, if its a variable, is specified and exists
        // 4. The Output variable is specified.

        // Start with the JSON Payload Variable.
        validateReadVariable("Token", myAssertion.getIncomingToken(), result);
        // Next is the Signature Algorithm variable, if it's defined.
        if ((myAssertion.getAlgorithmSecretLocation() == JwtUtilities.SELECTED_SECRET_VARIABLE) || (myAssertion.getAlgorithmSecretLocation() == JwtUtilities.SELECTED_SECRET_VARIABLE_BASE64)) {
            validateReadVariable("Secret", myAssertion.getAlgorithmSecretValue(), result);
        }

        // Check the selected secrets if they are keys or passwords, ensure they exist in the SSG
        if (myAssertion.getAlgorithmSecretLocation() == JwtUtilities.SELECTED_SECRET_KEY) {
            // Check to see if the saved OID is still available on the SSG
            Goid keyStoreGOID = null;
            long keyStoreId = Long.MIN_VALUE;
            String keyStoreName;

            String privateKeyReference[] = myAssertion.getAlgorithmSecretValue().split("[.]");
            if (privateKeyReference.length != 2) {
                // It's an invalid reference.
                result.addError(new PolicyValidatorResult.Error(myAssertion, "The Private Key reference is invalid. Policy will fail until a new Key is selected and saved.", null));
            } else {
                try {
                    keyStoreGOID = Goid.parseGoid(privateKeyReference[0]);
                } catch (IllegalArgumentException iae) {
                    try {
                        keyStoreId = Long.parseLong(privateKeyReference[0]);
                    } catch (NumberFormatException nfe) {
                        result.addError(new PolicyValidatorResult.Error(myAssertion, "The selected Private Key for this assertion cannot be parsed.  Policy will fail until a new Private Key is selected and saved.", null));
                        keyStoreGOID = PersistentEntity.DEFAULT_GOID;
                    }
                }

                if (keyStoreGOID == null) {
                    // No GOID was found.
                    keyStoreGOID = GoidUpgradeMapper.mapOid(EntityType.SSG_KEY_ENTRY, keyStoreId);
                }
                keyStoreName = privateKeyReference[1];

                if (!ResourceValidation.validatePrivateKey(keyStoreGOID, keyStoreName)) {
                    result.addError(new PolicyValidatorResult.Error(myAssertion, "The selected Private Key for this assertion does not exist on the SSG. Policy will fail until a new Private Key is selected and saved.", null));
                }
            }
        } else if (myAssertion.getAlgorithmSecretLocation() == JwtUtilities.SELECTED_SECRET_PASSWORD) {
            try {
                if (!ResourceValidation.validatePassword(Goid.parseGoid(myAssertion.getAlgorithmSecretValue()))) {
                    result.addError(new PolicyValidatorResult.Error(myAssertion, "The selected Password for this assertion does not exist on the SSG. Policy will fail until a new Password is selected and saved.", null));
                }
            } catch (IllegalArgumentException e) {
                result.addWarning(new PolicyValidatorResult.Warning(myAssertion, "The selected Password for this assertion may be invalid, and should be reselected.  Policy may fail until reselection is performed and policy is saved.", null));
            }
        }

        // Validate the write variable (the output variable) here.
        String outputVariable = myAssertion.getOutputVariable();

        if (outputVariable.isEmpty()) {
            result.addError(new PolicyValidatorResult.Error(myAssertion, "The Output variable is not set.", null));
        } else {
            VariableValidation varValidator = new VariableValidation(myAssertion);
            varValidator.allowEmptyVariables(false);  // No empty vars allowed.
            String varCheck = VariablePrefixUtil.fixVariableName(outputVariable);
            VariableValidation.VariableStatus currentStatus = varValidator.writeVariableIsValid(varCheck);

            switch (currentStatus) {
                case EXISTS_IMMUTABLE: {
                    result.addError(new PolicyValidatorResult.Error(myAssertion, "The variable '" + outputVariable + "' is immutable and cannot be written to.", null));
                    break;
                }
                case INVALID_SYNTAX: {
                    result.addError(new PolicyValidatorResult.Error(myAssertion, "The variable '" + outputVariable + "' is an invalid variable name.", null));
                    break;
                }
                case BUILT_IN_IMMUTABLE: {
                    result.addError(new PolicyValidatorResult.Error(myAssertion, "The variable '" + outputVariable + "' is immutable and cannot be written to.", null));
                    break;
                }
            }

        }
    }
}

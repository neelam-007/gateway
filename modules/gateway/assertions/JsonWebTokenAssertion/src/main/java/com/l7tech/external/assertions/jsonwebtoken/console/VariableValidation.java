package com.l7tech.external.assertions.jsonwebtoken.console;

import com.l7tech.console.policy.SsmPolicyVariableUtils;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.variable.BuiltinVariables;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * This is the Variable Validation core, blatantly stolen from TargetVariablePanel.java.
 * This was done so the functionality could be used standalone, with no widgets attached to it.
 * <p/>
 * This code can be used standalone, and doesn't require any UI interaction.  In fact, the UI part is
 * implemented as a separate JLabel that can be attached anywhere on a form, and calls this code.
 * <p/>
 * User: rseminoff
 * Date: 4/17/12
 */
public class VariableValidation {

    public enum VariableStatus {
        OK,                 // Entry is OK and passes validation.
        OK_USES_DEFAULT,    // Entry is OK and will use a default.
        EXISTS_MUTABLE,     // Already Exists, variable can be overwritten.
        EXISTS_IMMUTABLE,   // Already Exists, but preferred not to be overwritten
        INVALID_SYNTAX,     // Variable is named incorrectly
        BUILT_IN_MUTABLE,   // Gateway variable, can be overwritten
        BUILT_IN_IMMUTABLE, // Gateway variable, cannot be overwritten
        NOT_FOUND           // Variable doesn't exist, and can be used. Used when returnNotFound flag is set.
    }

    private Set<String> predecessorVariables = new TreeSet<>();
    private Map<String, VariableMetadata> predecessorMap;
    private boolean allowEmptyVariable = false;  // Makes empty fields invalid by default
    private boolean allowVariableOverwrite = true;  // Allow existing vars to be overwritten.
    private final Assertion parentAssertion;

    public VariableValidation(Assertion parentAssertion) {
        this.parentAssertion = parentAssertion;
    }

    private void updateVariables() {
        // This updates the used variable list within the policy.
        // It requires the current assertion, as well as the previous ones.
        // Is it worth traversing the path in this case?
        predecessorMap = SsmPolicyVariableUtils.getVariablesSetByPredecessors(parentAssertion);
        Set<String> vars = predecessorMap.keySet();

        // convert all vars to lower
        predecessorVariables = new TreeSet<>();
        for (String var : vars) {
            predecessorVariables.add(var.toLowerCase());
        }

    }

    private VariableStatus basicVariableCheck(String variable) {
        // Check to see if the variable is null or empty (ie: if a default variable is used if empty)
        if ((variable == null) || (variable.trim().isEmpty())) {
            if (allowEmptyVariable) {
                return VariableStatus.OK_USES_DEFAULT;
            } else {
                // This variable is invalid while it is empty.
                return VariableStatus.INVALID_SYNTAX;
            }
        }

        // The variable is not empty, so let's validate the text we've received.
        // Is the variable in proper format?
        if (!VariableMetadata.isNameValid(variable)) {
            return VariableStatus.INVALID_SYNTAX;
        }

        return VariableStatus.OK;
    }

    private VariableStatus validateReadVariable(String variable) {

        // This checks READ variables, as in they're only read from, not written to.
        VariableStatus basicStatus = basicVariableCheck(variable);
        if (basicStatus != VariableStatus.OK) {
            return basicStatus;
        }

        VariableMetadata meta = BuiltinVariables.getMetadata(variable);

        if (meta != null) {
            // This name exists as a built-in variable.
            return VariableStatus.OK;
        }

        // It's not a built in variable.
        final String name = Syntax.parse(variable, Syntax.DEFAULT_MV_DELIMITER).remainingName;
        String exists = Syntax.getMatchingName(name, predecessorVariables);

        if (exists == null) {
            // The variable doesn't exist in the policy.
            return VariableStatus.NOT_FOUND;
        } else {
            // The variable does exist in the policy...but can it be overwritten?
            // It's not a built in variable - it's in the predecessorMap metadata for that variable.
            try {
                meta = predecessorMap.get(exists);
            } catch (Exception e) {
                // Any exception means we bail.
                return VariableStatus.NOT_FOUND;
            }

            if (meta != null) {
                if (meta.isSettable()) {
                    return VariableStatus.OK;
                }
            }
        }

        return VariableStatus.OK;
    }

    private VariableStatus validateWriteVariable(String variable) {

        // Confirms the writability of WRITE variables.
        VariableStatus basicStatus = basicVariableCheck(variable);
        if (basicStatus != VariableStatus.OK) {
            return basicStatus;
        }

        VariableMetadata meta = BuiltinVariables.getMetadata(variable);

        if (meta != null) {
            // This name exists as a built-in variable.
            // Is it settable?
            if (meta.isSettable()) {
                // It can be set
                return VariableStatus.BUILT_IN_MUTABLE;
            } else {
                return VariableStatus.BUILT_IN_IMMUTABLE;
            }
        }

        // It's not a built in variable.
        final String name = Syntax.parse(variable, Syntax.DEFAULT_MV_DELIMITER).remainingName;
        String exists = Syntax.getMatchingName(name, predecessorVariables);

        if (exists == null) {
            // The variable doesn't exist in the policy.
            return VariableStatus.OK;
        } else {
            // The variable does exist in the policy...but can it be overwritten?
            // It's not a built in variable - it's in the predecessorMap metadata for that variable.
            try {
                meta = predecessorMap.get(exists);
            } catch (Exception e) {
                // Any exception means we bail.
                return VariableStatus.OK;
            }

            if (meta != null) {
                if (meta.isSettable()) {
                    return VariableStatus.EXISTS_MUTABLE;
                } else {
                    return VariableStatus.EXISTS_IMMUTABLE;
                }
            }
        }

        return VariableStatus.OK;
    }


    /**
     * This checks to see if a specified variable already exists in policy.  Like the writeVariableIsValid() method, the
     * variable is validated and INVALID_SYNTAX returned if it is not.
     * If the variable exists, it will return OK value.  If the variable is not found, it returns NOT_FOUND.
     *
     * @param variable The variable to check.  If decorated, it will be stripped.
     * @return Any value in the VariableStatus enum EXCEPT for OK and OK_USES_DEFAULT.
     */
    public VariableStatus variableExistsInPolicy(String variable) {
        updateVariables();
        return validateReadVariable(variable);
    }

    /**
     * Checks to see if the specified write variable is valid.  If it is valid and already exists in policy, it will return
     * EXISTS_MUTABLE, EXISTS_IMMUTABLE, BUILT_IN_MUTABLE, or BUILT_IN_IMMUTABLE based on if it can be overwritten.
     * Otherwise, an OK or OK_USES_DEFAULT will be returned based on allowEmptyVariables flag.
     * If the variable does not validate, INVALID_SYNTAX is returned.
     *
     * @param variable The variable to check.  If decorated, it will be stripped.
     * @return Any value in the VariableStatus enum EXCEPT for the NOT_FOUND status.
     */
    public VariableStatus writeVariableIsValid(String variable) {
        updateVariables();
        return validateWriteVariable(variable);
    }

    /**
     * Checks to see if the specified read variable is valid.  If it is valid and already exists in policy, it will return OK.
     * Otherwise, NOT_FOUND is returned.
     * If the variable does not validate, INVALID_SYNTAX is returned.
     *
     * @param variable The variable to check.  If decorated, it will be stripped.
     * @return OK, NOT_FOUND or INVALID_SYNTAX.
     */
    public VariableStatus readVariableIsValid(String variable) {
        updateVariables();
        return validateReadVariable(variable);
    }

    /**
     * Makes empty variable names legal or illegal based on need.  Making an empty variable name legal implies that
     * you'll be using a default instead.
     *
     * @param status True if empty variable names are allowed, which returns OK_USES_DEFAULT on validation,
     *               False if they're disallowed and returns INVALID_SYNTAX when validation is attempted.
     */
    public void allowEmptyVariables(boolean status) {
        allowEmptyVariable = status;
    }

    /**
     * Returns whether or not this instance allows empty variable names.
     *
     * @return True if empty variable names are allowed, false otherwise.
     */
    public boolean doesAllowEmptyVariables() {
        return allowEmptyVariable;
    }

    /**
     * If set to true, then any validated variable that already exists in policy and is settable will return one of the
     * *_MUTABLE statuses.  Otherwise, *_IMMUTABLE is always returned if the variable exists, regardless of it can be set.
     * Note: This does not affect the actual mutability of a variable.  This only makes it easier in code to check
     * for an existing variable and "set" it as read-only.
     *
     * @param status True to return one of EXISTS_MUTABLE or BUILT_IN_MUTABLE if the variable validates and is settable.
     *               False to always return EXISTS_IMMUTABLE or BUILT_IN_IMMUTABLE.
     */
    public void allowVariableOverwrite(boolean status) {
        allowVariableOverwrite = status;
    }

    /**
     * This returns whether or not this instance will indicate that existing variables can be overwritten if they are
     * settable.
     *
     * @return True if it returns any of the *_MUTABLE statuses, false if it always returns *_IMMUTABLE.
     */
    public boolean doesAllowVariableOverwrite() {
        return allowVariableOverwrite;
    }


}

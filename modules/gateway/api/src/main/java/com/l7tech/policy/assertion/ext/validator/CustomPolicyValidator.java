package com.l7tech.policy.assertion.ext.validator;

import java.util.List;
import java.util.Map;

/**
 * Policy validator for Custom Assertion.
 * Use this interface to validate Custom Assertion during design time.
 */
public interface CustomPolicyValidator {

    /**
     * ***** NOTES *****
     * Decided to create this interface if you want to implement policy validation for your custom assertion.
     * Custom assertion must implement the getWarningMessages(...) and getErrorMessages(...) methods
     * specified in this interface, and can use consoleContext input parameter to retrieve extension interface finder.
     *
     * Instead of passing in consoleContext in these two methods, I tried to have the custom assertion implement
     * UsesConsoleContext interface, and not pass in consoleContext, but this was not possible because of following:
     * - ConsoleContext must be saved as part of custom assertion, but it could not be serialized. I tried to make it
     * transient, but that caused the more problems b/c it was not copied when doing clone().
     */

    /**
     * Gets a list of warning messages.
     *
     * @param consoleContext a map of custom objects from the Gateway
     * @return a list of warning messages. Can be null.
     */
    List<String> getWarningMessages (Map<String, Object> consoleContext);

    /**
     * Gets a list of error messages.
     *
     * @param consoleContext a map of custom objects from the Gateway
     * @return a list of error messages. Can be null.
     */
    List<String> getErrorMessages (Map<String, Object> consoleContext);
}
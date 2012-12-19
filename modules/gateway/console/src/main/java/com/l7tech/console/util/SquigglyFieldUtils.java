package com.l7tech.console.util;

import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableNameSyntaxException;
import com.l7tech.util.Functions;
import com.l7tech.util.TextUtils;
import com.l7tech.util.ValidationUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.l7tech.util.Functions.grep;

public class SquigglyFieldUtils {

    private static Pattern leadingTrailingWhiteSpacePattern = Pattern.compile("^\\s+|\\s+$");

    /**
     * Validate the contents of a squiggly text field and update the UI.
     *
     * @param squigglyTextField field to validate and manage UI state for.
     * @param textSplitPattern if not null, the pattern will be applied to the contents of the field.
     * @param trimText Set to true to trim the text in the field. Set to false to leave it untrimmed
     * @param validateFunction function to validate contents. If textSplitPattern was supplied, then this function will be
     * called once for each value after contents of text field are split. validateFunction should return null when there
     * are no errors with the resolved string value.
     * @return true if the field contains no validation errors.
     */
    public static boolean validateSquigglyTextFieldState(@NotNull final SquigglyTextField squigglyTextField,
                                                         @Nullable final Pattern textSplitPattern,
                                                         final boolean trimText,
                                                         @NotNull final Functions.Unary<String, String> validateFunction) {
        String customText = squigglyTextField.getText();
        if(trimText){
            customText = customText.trim();
        }
        final boolean hasText = !customText.isEmpty();
        boolean invalid = false;
        if (hasText) {
            final List<String> values = grep((textSplitPattern == null) ? Arrays.asList(customText) : Arrays.asList(textSplitPattern.split(customText)), new Functions.Unary<Boolean, String>() {
                @Override
                public Boolean call(String s) {
                    return TextUtils.isNotEmpty().call(s);
                }
            });
            for (String s : values) {
                final String errorMsg = validateFunction.call(s);
                if (errorMsg != null) {
                    squigglyTextField.setSquiggly();
                    //Don't set a range as there may be more than one error. Squiggly does not yet support multiple values.
                    //Set the entire text field to red squiggly
                    squigglyTextField.setModelessFeedback(errorMsg);
                    invalid = true;
                    break;//first invalid value is the pop up message.
                }
            }
        }
        if (!invalid) {
            squigglyTextField.setNone();
            squigglyTextField.setModelessFeedback(null);
        }

        return !invalid;
    }

    public static boolean validateSquigglyTextFieldState(@NotNull final SquigglyTextField squigglyTextField,
                                                         @Nullable final Pattern textSplitPattern,
                                                         @NotNull final Functions.Unary<String, String> validateFunction) {
        return validateSquigglyTextFieldState(squigglyTextField, textSplitPattern, true, validateFunction);
    }

    public static boolean validateSquigglyTextFieldState(@NotNull final SquigglyTextField squigglyTextField,
                                                         @NotNull final Functions.Unary<String, String> validateFunction) {
        return validateSquigglyTextFieldState(squigglyTextField, null, true, validateFunction);
    }

    /**
     * See {@link #validateSquigglyFieldForUris(com.l7tech.gui.widgets.SquigglyTextField, boolean)} which is passed
     * a value of true.
     * @param squigglyTextField squiggly field to validate
     * @return String if any error is in the field, null otherwise.
     */
    @Nullable
    public static String validateSquigglyFieldForUris(@NotNull final SquigglyTextField squigglyTextField) {
        return validateSquigglyFieldForUris(squigglyTextField, true);
    }

    /**
     * Convenience method to validate URIs contained in a text field. Variable references are allowed and are validated.
     *
     * @param squigglyTextField squiggly text field to validate.
     * @param allowMoreThanOne true if the field may reference more than a single URI directly or indirectly via a
     * variable reference.
     * @return null String when text field only contains valid URI values or contains valid variable references,
     * otherwise an error String is returned.
     */
    @Nullable
    public static String validateSquigglyFieldForUris(@NotNull final SquigglyTextField squigglyTextField,
                                                      final boolean allowMoreThanOne) {
        final String [] errorCollected = new String[1];
        SquigglyFieldUtils.validateSquigglyTextFieldState(squigglyTextField,
                (allowMoreThanOne) ? TextUtils.URI_STRING_SPLIT_PATTERN : null,
                new Functions.Unary<String, String>() {
                    @Override
                    public String call(String s) {
                        try {
                            final String[] referencedNames = Syntax.getReferencedNames(s);
                            if (referencedNames.length == 0) {
                                if (!ValidationUtils.isValidUri(s)) {
                                    final String error = "Invalid URI: '" + s + "'";
                                    errorCollected[0] = error;
                                    return error;
                                }
                            }
                        } catch (VariableNameSyntaxException e) {
                            final String error = "Invalid variable reference '" + s + "'";
                            errorCollected[0] = error;
                            return error;
                        }
                        return null;
                    }
                });
        return errorCollected[0];
    }

    /**
     * Convenience method to validate a SquigglyTextField for variable reference.
     *
     * @param squigglyTextField field to validate
     * @return null if the field contains no invalid variable references, otherwise a String with an error message
     */
    @Nullable
    public static String validateSquigglyFieldForVariableReference(@NotNull final SquigglyTextField squigglyTextField) {
        final String [] errorCollected = new String[1];
        SquigglyFieldUtils.validateSquigglyTextFieldState(squigglyTextField,
                new Functions.Unary<String, String>() {
                    @Override
                    public String call(String s) {
                        try {
                            Syntax.getReferencedNames(s);
                        } catch (VariableNameSyntaxException e) {
                            final String error = "Invalid variable reference '" + s + "'";
                            errorCollected[0] = error;
                            return error;
                        }
                        return null;
                    }
                });

        return errorCollected[0];
    }

    /**
     * Equivalent to calling {@link #validateSquigglyFieldForRegex(com.l7tech.gui.widgets.SquigglyTextField, boolean, int, boolean)} with squigglyTextField, allowContextVariables, 0, false
     *
     * @param squigglyTextField     field to validate
     * @param allowContextVariables Set to true if context variables are allowed in the regex
     * @return True if the field is a valid regex, or if it contains only warnings and no errors. False if it is invalid.
     */
    public static boolean validateSquigglyFieldForRegex(@NotNull final SquigglyTextField squigglyTextField, final boolean allowContextVariables) {
        return validateSquigglyFieldForRegex(squigglyTextField, allowContextVariables, 0, false);
    }

    /**
     * A convenience method for validating a squiggly text field for a regex.
     *
     * @param squigglyTextField     field to validate
     * @param allowContextVariables Set to true if context variables are allowed in the regex
     * @param flags                 the flags to use with the regex
     * @param allowEmptyPattern     set to true if a null or empty regex is allowed
     * @return True if the field is a valid regex, or if it contains only warnings and no errors. False if it is invalid.
     */
    public static boolean validateSquigglyFieldForRegex(@NotNull final SquigglyTextField squigglyTextField, final boolean allowContextVariables, final int flags, final boolean allowEmptyPattern) {
        //use a pessimistic approach
        final AtomicBoolean regexValid = new AtomicBoolean(false);
        SquigglyFieldUtils.validateSquigglyTextFieldState(squigglyTextField, null, false,
                new Functions.Unary<String, String>() {
                    @Override
                    public String call(final String text) {
                        if (text != null && !text.isEmpty()) {
                            try {
                                String parsedText = text;
                                if (allowContextVariables && Syntax.getReferencedNames(text, false).length > 0) {
                                    parsedText = Pattern.quote(Syntax.regexPattern.matcher(text).replaceAll("\\${$1}"));
                                }
                                Pattern.compile(parsedText, flags);

                                // Warn on leading or trailing whitespace
                                Matcher whiteSpaceMatcher = leadingTrailingWhiteSpacePattern.matcher(text);
                                if (whiteSpaceMatcher.find()) {
                                    squigglyTextField.setColor(Color.GREEN);
                                    regexValid.set(true);
                                    return "Pattern contains leading or trailing whitespace";
                                }
                            } catch (PatternSyntaxException patternSyntaxException) {
                                //There is no consistent way to fine the range for the exception. So set range to null and the entire field will be red.
                                squigglyTextField.setColor(Color.RED);
                                return patternSyntaxException.getDescription() + " index: " + patternSyntaxException.getIndex();
                            }
                        } else if (!allowEmptyPattern) {
                            return "Empty pattern not allowed";
                        }
                        regexValid.set(true);
                        return null;
                    }
                });
        return regexValid.get();
    }
}

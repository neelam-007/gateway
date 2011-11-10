package com.l7tech.console.util;

import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableNameSyntaxException;
import com.l7tech.util.Functions;
import com.l7tech.util.TextUtils;
import com.l7tech.util.ValidationUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static com.l7tech.util.Functions.grep;

public class SquigglyFieldUtils {
    /**
     * Validate the contents of a squiggly text field and update the UI.
     *
     * @param squigglyTextField field to validate and manage UI state for.
     * @param textSplitPattern if not null, the pattern will be applied to the contents of the field.
     * @param validateFunction function to validate contents. If splitPatter was supplied, then this function will be
     * called once for each value after conents of text field are split.
     * @return true if the field contains no validation errors.
     */
    public static boolean validateSquigglyTextFieldState(@NotNull final SquigglyTextField squigglyTextField,
                                                         @Nullable final Pattern textSplitPattern,
                                                         @NotNull final Functions.Unary<String, String> validateFunction) {
        final String customText = squigglyTextField.getText().trim();
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

    /**
     * Convenience method to validate URIs contained in a text field. Variable references are allowed and are validated.
     *
     * @param squigglyTextField squiggly text field to validate.
     * @return true if text field only contains valid URI values or contains valid variable references.
     */
    public static boolean validateSquigglyFieldForUris(@NotNull final SquigglyTextField squigglyTextField) {
        return SquigglyFieldUtils.validateSquigglyTextFieldState(squigglyTextField,
                TextUtils.URI_STRING_SPLIT_PATTERN,
                new Functions.Unary<String, String>() {
                    @Override
                    public String call(String s) {
                        try {
                            final String[] referencedNames = Syntax.getReferencedNames(s);
                            if (referencedNames.length == 0) {
                                if (!ValidationUtils.isValidUri(s)) {
                                    return "Invalid URI: '" + s + "'";
                                }
                            }
                        } catch (VariableNameSyntaxException e) {
                            return "Invalid variable reference '" + s + "'";
                        }
                        return null;
                    }
                });
    }
}

package com.l7tech.console.util;

import com.l7tech.gui.util.InputValidator;
import com.l7tech.policy.variable.VariableMetadata;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.text.JTextComponent;

/**
 * A validation rule for a JTextComponent that requires the value to be a valid context variable name.
 * Optionally, the field may be permitted to be completely blank.
 * Optionally, syntax may be permitted within the field (eg, a "[0]" at the end).
 */
public class ContextVariableTextComponentValidationRule extends InputValidator.ComponentValidationRule {
    private final String fieldName;
    private final JTextComponent component;
    private final boolean permitSyntax;
    private final boolean allowBlank;

    /**
     * Create a validation rule for the specified text component.
     *
     * @param fieldName name of field, for building the error message.
     * @param component component to validate.
     * @param permitSyntax true if extra syntax should be permitted within the name.
     * @param allowBlank true if an empty value should be permitted.
     */
    public ContextVariableTextComponentValidationRule(@NotNull String fieldName, @NotNull JTextComponent component, boolean permitSyntax, boolean allowBlank) {
        super(component);
        this.fieldName = fieldName;
        this.component = component;
        this.permitSyntax = permitSyntax;
        this.allowBlank = allowBlank;
    }

    @Override
    public String getValidationError() {
        if ( StringUtils.isBlank( component.getText() ) ) {
            return allowBlank
                ? null
                : fieldName + " must not be empty";
        }

        if ( !VariableMetadata.isNameValid( component.getText(), permitSyntax ) ) {
            return fieldName + " must begin with a letter or underscore and may contain only letters, digits, underscores, dashes, and periods";
        }

        return null;
    }
}

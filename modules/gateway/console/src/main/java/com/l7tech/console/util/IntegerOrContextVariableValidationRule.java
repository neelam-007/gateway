package com.l7tech.console.util;

import com.l7tech.gui.util.InputValidator;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableNameSyntaxException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ValidationUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;

/**
 * Validation Rule for fields that must either be an integer or a context variable expression.
 * @author alee
 */
public class IntegerOrContextVariableValidationRule implements InputValidator.ValidationRule {
    private final int minimum;
    private int maximum;
    private final String fieldName;
    private String textToValidate;

    public IntegerOrContextVariableValidationRule(final int minimum, final int maximum, final String fieldName){
        this.minimum = minimum;
        this.maximum = maximum;
        this.fieldName = fieldName;
        this.textToValidate = StringUtils.EMPTY;
    }

    public void setTextToValidate(@Nullable final String textToValidate) {
        this.textToValidate = (textToValidate == null)? null : textToValidate.trim();
    }

    public void setMaximum(final int maximum) {
        this.maximum = maximum;
    }

    @Override
    public String getValidationError() {
        String errorMessage = null;
        if(textToValidate != null && !textToValidate.isEmpty()){
            final String[] referencedNames;
            try {
                referencedNames = Syntax.getReferencedNames(textToValidate, true);
                if (referencedNames.length == 0) {
                    //it's not a variable expression
                    if(!ValidationUtils.isValidInteger(textToValidate, false, minimum, maximum)){
                        errorMessage = MessageFormat.format(InputValidator.MUST_BE_INTEGER, fieldName, minimum, maximum);
                    }
                }
            } catch (VariableNameSyntaxException e) {
                errorMessage = MessageFormat.format("Invalid variable referenced for {0} field: {1}.", fieldName, ExceptionUtils.getMessage(e));
            }
        }else{
            errorMessage = MessageFormat.format("The {0} must not be empty.", fieldName);
        }
        return errorMessage;
    }
}

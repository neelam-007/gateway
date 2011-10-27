package com.l7tech.console.util;

import com.l7tech.gui.util.InputValidator;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.ValidationUtils;
import org.apache.commons.lang.StringUtils;

import java.text.MessageFormat;

/**
 * Validation Rule for fields that must either be an integer or a context variable.
 * @author alee
 */
public class IntegerOrContextVariableValidationRule implements InputValidator.ValidationRule {
    private final int minimum;
    private final int maximum;
    private final String fieldName;
    private String textToValidate;

    public IntegerOrContextVariableValidationRule(final int minimum, final int maximum, final String fieldName){
        this.minimum = minimum;
        this.maximum = maximum;
        this.fieldName = fieldName;
        this.textToValidate = StringUtils.EMPTY;
    }

    public void setTextToValidate(final String textToValidate) {
        this.textToValidate = (textToValidate == null)? null : textToValidate.trim();
    }

    @Override
    public String getValidationError() {
        String errorMessage = null;
        if(textToValidate != null && !textToValidate.isEmpty()){
            if(StringUtils.isNumeric(textToValidate) && !ValidationUtils.isValidInteger(textToValidate, false, minimum, maximum)){
                errorMessage = MessageFormat.format("The {0} must be between {1} and {2}.", fieldName, minimum, maximum);
            } else if(!StringUtils.isNumeric(textToValidate) && !Syntax.validateStringOnlyReferencesVariables(textToValidate)){
                errorMessage = MessageFormat.format("Invalid syntax used for {0}.", fieldName);
            }
        }else{
            errorMessage = MessageFormat.format("The {0} must not be empty.", fieldName);
        }
        return errorMessage;
    }
}

package com.l7tech.console.util;

import org.junit.Before;
import org.junit.Test;

import javax.swing.*;

import static org.junit.Assert.*;

public class IntegerOrContextVariableValidationRuleTest {
    private static final int MIN = 1;
    private static final int MAX = 100;
    private static final String FIELD_NAME = "myField";
    private IntegerOrContextVariableValidationRule rule;
    private JTextField textComponent;

    @Before
    public void setup() {
        textComponent = new JTextField();
        rule = new IntegerOrContextVariableValidationRule(MIN, MAX, FIELD_NAME, textComponent);
    }

    @Test
    public void numericBelowMin() {
        textComponent.setText(String.valueOf(MIN - 1));

        final String error = rule.getValidationError();

        validateNonIntegerError(error);
    }

    @Test
    public void nullString() {
        textComponent.setText(null);

        final String error = rule.getValidationError();

        assertEquals("The " + FIELD_NAME + " must not be empty.", error);
    }

    @Test
    public void emptyString() {
        textComponent.setText("");

        final String error = rule.getValidationError();

        assertEquals("The " + FIELD_NAME + " must not be empty.", error);
    }

    @Test
    public void numericAboveMax() {
        textComponent.setText(String.valueOf(MAX + 1));

        final String error = rule.getValidationError();

        assertEquals("The " + FIELD_NAME + " field must be an integer between " + MIN + " and " + MAX + ".", error);
    }

    @Test
    public void numericEqualToMin() {
        textComponent.setText(String.valueOf(MIN));
        assertNull(rule.getValidationError());
    }

    @Test
    public void numericEqualToMax() {
        textComponent.setText(String.valueOf(MAX));

        assertNull(rule.getValidationError());
    }

    @Test
    public void validContextVariable() {
        textComponent.setText("${myVar}");

        assertNull(rule.getValidationError());
    }

    @Test
    public void invalidContextVariable() {
        textComponent.setText("${myVar");

        final String error = rule.getValidationError();

        validateNonIntegerError(error);
    }

    @Test
    public void invalidContextVariableReference() {
        textComponent.setText("${myVar[}");

        final String error = rule.getValidationError();

        assertTrue(error.startsWith("Invalid variable referenced for " + FIELD_NAME + " field"));
    }

    @Test
    public void plainString() {
        textComponent.setText("asdf");

        final String error = rule.getValidationError();

        validateNonIntegerError(error);
    }

    @Test
    public void decimalInvalid() {
        textComponent.setText("1.1");

        final String error = rule.getValidationError();

        validateNonIntegerError(error);
    }

    private void validateNonIntegerError(String error) {
        assertEquals("The " + FIELD_NAME + " field must be an integer between " + MIN + " and " + MAX + ".", error);
    }

    @Test
    public void spacesTrimmed() {
        textComponent.setText("   1   ");
        assertNull(rule.getValidationError());
    }
}

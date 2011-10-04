package com.l7tech.console.util;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;

public class IntegerOrContextVariableValidationRuleTest {
    private static final int MIN = 1;
    private static final int MAX = 100;
    private static final String FIELD_NAME = "myField";
    private IntegerOrContextVariableValidationRule rule;

    @Before
    public void setup(){
        rule = new IntegerOrContextVariableValidationRule(MIN, MAX, FIELD_NAME);
    }

    @Test
    public void numericBelowMin(){
        rule.setTextToValidate(String.valueOf(MIN - 1));

        final String error = rule.getValidationError();

        assertEquals("The " + FIELD_NAME + " must be between " + MIN + " and " + MAX + ".", error);
    }

    @Test
    public void nullString(){
        rule.setTextToValidate(null);

        final String error = rule.getValidationError();

        assertEquals("The " + FIELD_NAME + " must not be empty.", error);
    }

    @Test
    public void emptyString(){
        rule.setTextToValidate("");

        final String error = rule.getValidationError();

        assertEquals("The " + FIELD_NAME + " must not be empty.", error);
    }

    @Test
    public void numericAboveMax(){
        rule.setTextToValidate(String.valueOf(MAX + 1));

        final String error = rule.getValidationError();

        assertEquals("The " + FIELD_NAME + " must be between " + MIN + " and " + MAX + ".", error);
    }

    @Test
    public void numericEqualToMin(){
        rule.setTextToValidate(String.valueOf(MIN));
        assertNull(rule.getValidationError());
    }

    @Test
    public void numericEqualToMax(){
        rule.setTextToValidate(String.valueOf(MAX));

        assertNull(rule.getValidationError());
    }

    @Test
    public void validContextVariable(){
        rule.setTextToValidate("${myVar}");

        assertNull(rule.getValidationError());
    }

    @Test
    public void invalidContextVariable(){
        rule.setTextToValidate("${myVar");

        final String error = rule.getValidationError();

        assertEquals("Invalid syntax used for " + FIELD_NAME + ".", error);
    }

    @Test
    public void plainString(){
        rule.setTextToValidate("asdf");

        final String error = rule.getValidationError();

        assertEquals("Invalid syntax used for " + FIELD_NAME + ".", error);
    }
}

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Mar 19, 2009
 * Time: 7:07:04 PM
 */
package com.l7tech.gateway.standardreports;

import org.junit.Test;
import org.junit.Assert;

public class JasperValidLongExpressionTests {

    /**
     * Tests for SimpleJavaLongExpression
     */
    @Test
    public void testSimpleJavaLongExpression() {
        String expected = "$V{TOTAL}";
        String actual = new SimpleJavaLongExpression(
                SimpleJavaLongExpression.PLAIN_STRING_VARIABLE.TOTAL).getVariableString();
        Assert.assertEquals("Expected: " + expected + " actual: " + actual, actual, expected);
    }

    /**
     * Tests for SimpleIndexJavaLongExpression
     */
    @Test
    public void testSimpleIndexJavaLongExpression() {
        String expected = "$V{COLUMN_REPORT_1}";
        String actual =
                new SimpleIndexJavaLongExpression(
                        SimpleIndexJavaLongExpression.INDEX_MISSING_VARIABLE.COLUMN_REPORT_, 1).getVariableString();

        Assert.assertEquals("Expected: " + expected + " actual: " + actual, actual, expected);
    }

    /**
     * Tests for TertiaryJavaLongExpression
     * This class has two cases, one which has a single condition in the ternary condition and the other which
     * has two, with OR semantics
     */
    @Test
    public void testTertiaryJavaLongExpression() {
        //"($V{COLUMN_" + (i + 1) + "} == null)?new Long(0):$V{COLUMN_" + (i + 1) + "}"
        String expected = "($V{COLUMN_12} == null)?new Long(0):$V{COLUMN_12}";
        String actual = new TertiaryJavaLongExpression(
                TertiaryJavaLongExpression.TERNARY_STRING_VARIABLE.COLUMN_, 12).getVariableString();
        Assert.assertEquals("Expected: " + expected + " actual: " + actual, actual, expected);

        //"($V{COLUMN_SERVICE_TOTAL_" + (i + 1) + "} == null || $V{COLUMN_SERVICE_TOTAL_" + (i + 1) + "}.intValue() == 0)?new Long(0):$V{COLUMN_SERVICE_TOTAL_" + (i + 1) + "}"
        expected = "($V{COLUMN_SERVICE_TOTAL_5} == null || $V{COLUMN_SERVICE_TOTAL_5}.intValue() == 0)?new Long(0):$V{COLUMN_SERVICE_TOTAL_5}";
        actual = new TertiaryJavaLongExpression(
                TertiaryJavaLongExpression.TERNARY_STRING_VARIABLE.COLUMN_SERVICE_TOTAL_, 5).getVariableString();
        Assert.assertEquals("Expected: " + expected + " actual: " + actual, actual, expected);
    }
}

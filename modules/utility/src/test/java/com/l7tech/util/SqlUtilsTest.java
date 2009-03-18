/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Mar 17, 2009
 * Time: 2:09:45 PM
 */
package com.l7tech.util;

import org.junit.Test;
import org.junit.Assert;

public class SqlUtilsTest {

    @Test
    public void testNullChar() {
        String s = "\u0000";
        String expectedVal = "";
        String actualValue = SqlUtils.mySqlEscapeIllegalSqlChars(s);
        Assert.assertEquals("Expected: " + expectedVal + " actual: " + actualValue, actualValue, expectedVal);
    }

    @Test
    public void testBackSlashRChar() {
        String s = "\r";
        String expectedVal = "\\\\r";
        String actualValue = SqlUtils.mySqlEscapeIllegalSqlChars(s);
        Assert.assertEquals("Expected: " + expectedVal + " actual: " + actualValue, actualValue, expectedVal);
    }

    @Test
    public void testBackSlashNChar() {
        String s = "\n";
        String expectedVal = "\\\\n";
        String actualValue = SqlUtils.mySqlEscapeIllegalSqlChars(s);
        Assert.assertEquals("Expected: " + expectedVal + " actual: " + actualValue, actualValue, expectedVal);
    }

    @Test
    public void testSubChar() {
        String s = "\u001a";
        String expectedVal = "";
        String actualValue = SqlUtils.mySqlEscapeIllegalSqlChars(s);
        Assert.assertEquals("Expected: " + expectedVal + " actual: " + actualValue, actualValue, expectedVal);
    }

    @Test
    public void testDoubleQuoteChar() {
        String s = "\"";
        String expectedVal = "\\\"";
        String actualValue = SqlUtils.mySqlEscapeIllegalSqlChars(s);
        Assert.assertEquals("Expected: " + expectedVal + " actual: " + actualValue, actualValue, expectedVal);
    }

    @Test
    public void testSingleQuoteChar() {
        String s = "'";
        String expectedVal = "\\'";
        String actualValue = SqlUtils.mySqlEscapeIllegalSqlChars(s);
        Assert.assertEquals("Expected: " + expectedVal + " actual: " + actualValue, actualValue, expectedVal);
    }

    @Test
    public void testBackSlashChar() {
        String s = "\\";
        String expectedVal = "\\\\";
        String actualValue = SqlUtils.mySqlEscapeIllegalSqlChars(s);
        Assert.assertEquals("Expected: " + expectedVal + " actual: " + actualValue, actualValue, expectedVal);
    }

}

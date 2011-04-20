package com.l7tech.console.util;

import junit.framework.TestCase;

/**
 * Tests for VariablePrefixUtil
 */
public class VariablePrefixUtilTest extends TestCase {
    public void testHasValidDollarCurlyOpenStart() {

        // invalid
        assertFalse(VariablePrefixUtil.hasValidDollarCurlyOpenStart(""));
        assertFalse(VariablePrefixUtil.hasValidDollarCurlyOpenStart("$"));
        assertFalse(VariablePrefixUtil.hasValidDollarCurlyOpenStart("{"));
        assertFalse(VariablePrefixUtil.hasValidDollarCurlyOpenStart("}"));
        assertFalse(VariablePrefixUtil.hasValidDollarCurlyOpenStart("variableName"));
        assertFalse(VariablePrefixUtil.hasValidDollarCurlyOpenStart("${{"));
        assertFalse(VariablePrefixUtil.hasValidDollarCurlyOpenStart("${{variableName"));
        assertFalse(VariablePrefixUtil.hasValidDollarCurlyOpenStart("${{}}"));
        assertFalse(VariablePrefixUtil.hasValidDollarCurlyOpenStart("${{variableName}}"));
        assertFalse(VariablePrefixUtil.hasValidDollarCurlyOpenStart("${$"));
        assertFalse(VariablePrefixUtil.hasValidDollarCurlyOpenStart("${$variableName"));
        assertFalse(VariablePrefixUtil.hasValidDollarCurlyOpenStart("${variable$Name}"));
        assertFalse(VariablePrefixUtil.hasValidDollarCurlyOpenStart("${${"));
        assertFalse(VariablePrefixUtil.hasValidDollarCurlyOpenStart("${${}}"));
        assertFalse(VariablePrefixUtil.hasValidDollarCurlyOpenStart("${${variableName}}"));

        // valid
        assertTrue(VariablePrefixUtil.hasValidDollarCurlyOpenStart("${"));
        assertTrue(VariablePrefixUtil.hasValidDollarCurlyOpenStart("${}"));
        assertTrue(VariablePrefixUtil.hasValidDollarCurlyOpenStart("${variableName"));
        assertTrue(VariablePrefixUtil.hasValidDollarCurlyOpenStart(" ${ variableName }  "));
        assertTrue(VariablePrefixUtil.hasValidDollarCurlyOpenStart("${variable88Name}"));
    }

    public void testHasValidCurlyCloseEnd() {

        // invalid
        assertFalse(VariablePrefixUtil.hasValidCurlyCloseEnd(""));
        assertFalse(VariablePrefixUtil.hasValidCurlyCloseEnd("$"));
        assertFalse(VariablePrefixUtil.hasValidCurlyCloseEnd("{"));
        assertFalse(VariablePrefixUtil.hasValidCurlyCloseEnd("variableName"));
        assertFalse(VariablePrefixUtil.hasValidCurlyCloseEnd("{{}}"));
        assertFalse(VariablePrefixUtil.hasValidCurlyCloseEnd("{{variableName}}"));
        assertFalse(VariablePrefixUtil.hasValidCurlyCloseEnd("}}"));
        assertFalse(VariablePrefixUtil.hasValidCurlyCloseEnd("${}}"));
        assertFalse(VariablePrefixUtil.hasValidCurlyCloseEnd("${${}}"));
        assertFalse(VariablePrefixUtil.hasValidCurlyCloseEnd("${variable}Name}"));
        assertFalse(VariablePrefixUtil.hasValidCurlyCloseEnd("${{variableName}}"));

        // valid
        assertTrue(VariablePrefixUtil.hasValidCurlyCloseEnd("}"));
        assertTrue(VariablePrefixUtil.hasValidCurlyCloseEnd("${}"));
        assertTrue(VariablePrefixUtil.hasValidCurlyCloseEnd("${variableName}"));
        assertTrue(VariablePrefixUtil.hasValidCurlyCloseEnd(" ${ variableName }  "));
    }

    public void testHasDollarOrCurlyOpen() {

        // false
        assertFalse(VariablePrefixUtil.hasDollarOrCurlyOpen(""));
        assertFalse(VariablePrefixUtil.hasDollarOrCurlyOpen("variableName"));

        // true
        assertTrue(VariablePrefixUtil.hasDollarOrCurlyOpen("$"));
        assertTrue(VariablePrefixUtil.hasDollarOrCurlyOpen("{"));
        assertTrue(VariablePrefixUtil.hasDollarOrCurlyOpen("${"));
        assertTrue(VariablePrefixUtil.hasDollarOrCurlyOpen("variable${Name"));
    }
}

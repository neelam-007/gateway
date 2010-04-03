/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 */
package com.l7tech.policy.variable;

import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.util.Set;
import java.util.HashSet;

public class SyntaxTest {

    @Test
    public void testGetReferencedNamesWithIndex() throws SAXException {
        final String varName = "${IDS[0]} ${TEST} ${IDS[1]}";
        final String [] referencedNames = Syntax.getReferencedNamesIndexedVarsNotOmitted(varName);

        Assert.assertEquals("Three values expected", 3, referencedNames.length);

        Assert.assertEquals("Correct referenced name found", "IDS[0]", referencedNames[0]);
        Assert.assertEquals("Correct referenced name found", "TEST", referencedNames[1]);
        Assert.assertEquals("Correct referenced name found", "IDS[1]", referencedNames[2]);
    }
    
    @Test
    public void testGetReferencedNamesWithoutIndex() throws SAXException {
        final String varName = "${IDS[0]} ${TEST} ${IDS[1]}";
        final String [] referencedNames = Syntax.getReferencedNamesIndexedVarsOmitted(varName);

        Assert.assertEquals("Only 1 value expected", 1, referencedNames.length);

        Assert.assertEquals("Incorrect referenced name found", "TEST", referencedNames[0]);
    }

    @Test
    public void testGetReferencedNames() throws SAXException {
        final String varName = "${IDS[0]} ${TEST} ${IDS[1]}";
        final String [] referencedNames = Syntax.getReferencedNames(varName);

        Assert.assertEquals("Three values expected", 3, referencedNames.length);

        Assert.assertEquals("Correct referenced name found", "IDS", referencedNames[0]);
        Assert.assertEquals("Correct referenced name found", "TEST", referencedNames[1]);
        Assert.assertEquals("Correct referenced name found", "IDS", referencedNames[2]);
    }

    @Test
    public void testGetReferencedNames3d() throws SAXException {
        final String varName = "${IDS[0][0][0]} ${TEST} ${IDS[1]}";
        final String [] referencedNames = Syntax.getReferencedNames(varName);

        Assert.assertEquals("Three values expected", 3, referencedNames.length);

        Assert.assertEquals("Correct referenced name found", "IDS", referencedNames[0]);
        Assert.assertEquals("Correct referenced name found", "TEST", referencedNames[1]);
        Assert.assertEquals("Correct referenced name found", "IDS", referencedNames[2]);
    }

    /**
     * This test is capturing the behaviour of getReferencedNames, where it applies no logic for selectors
     */
    @Test
    public void testGetMainPartFromVariable() throws Exception{
        final String varName = "${TEST.mainpart}";
        final String [] referencedNames = Syntax.getReferencedNames(varName);

        Assert.assertEquals("Only 1 value expected", 1, referencedNames.length);

        Assert.assertEquals("Selector information should be intact", "TEST.mainpart", referencedNames[0]);
    }

    /**
     * Tests that any selector information is removed from a variable name
     */
    @Test
    public void testGetMatchingNames(){
        final Set<String> contextVarNames = new HashSet<String>();
        contextVarNames.add("var1");
        final String referencedName = Syntax.getMatchingName("var1.mainpart", contextVarNames);
        Assert.assertEquals("Invalid name found", "var1", referencedName);
    }

    @Test(expected=VariableNameSyntaxException.class)
    public void testArraySyntaxSubscriptNotInt() {
        Syntax.parse( "blah[i]", "," );
    }

    @Test(expected=VariableNameSyntaxException.class)
    public void testArraySyntaxSubscriptEmpty() {
        Syntax.parse( "blah[0][]", "," );
    }

    @Test(expected=VariableNameSyntaxException.class)
    public void testArraySyntaxSubscriptNoClose() {
        Syntax.parse( "blah[", "," );
    }

    @Test(expected=VariableNameSyntaxException.class)
    public void testArraySyntaxSubscriptNegative() {
        Syntax.parse( "blah[-34]", "," );
    }
}

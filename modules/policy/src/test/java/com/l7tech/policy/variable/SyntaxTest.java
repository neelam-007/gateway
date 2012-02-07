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
        Syntax.parse( "blah[]", "," );
    }

    @Test(expected=VariableNameSyntaxException.class)
    public void testArraySyntaxSubscriptNoClose() {
        Syntax.parse( "blah[", "," );
    }

    @Test(expected=VariableNameSyntaxException.class)
    public void testArraySyntaxSubscriptNegative() {
        Syntax.parse( "blah[-34]", "," );
    }

    @Test
    public void testOnlyVariablesReferenced(){
        String var = "${var1} ${var[2]} ${request.mainpart}";
        Assert.assertTrue("Only variables are referenced", Syntax.validateStringOnlyReferencesVariables(var));

        var = "${var1}${var}";
        Assert.assertTrue("Only variables are referenced", Syntax.validateStringOnlyReferencesVariables(var));

        var = "${var1}           ,  ${var} \n \f \r \t  ";
        Assert.assertTrue("Only variables are referenced", Syntax.validateStringOnlyReferencesVariables(var));

        var = "${var1} ${var[2]} request.mainpart";
        Assert.assertFalse("Not only variables are referenced", Syntax.validateStringOnlyReferencesVariables(var));

        var = "${issuedSamlAssertion saml2}";
        Assert.assertFalse(Syntax.validateStringOnlyReferencesVariables(var));

        var = "${issuedSamlAssertionsaml2}test";
        Assert.assertFalse(Syntax.validateStringOnlyReferencesVariables(var));

        var = "test${issuedSamlAssertionsaml2}";
        Assert.assertFalse(Syntax.validateStringOnlyReferencesVariables(var));

        var = "test test";
        Assert.assertFalse(Syntax.validateStringOnlyReferencesVariables(var));
    }
    
    @Test
    public void testVariableExpression() throws Exception {
        final String varName = Syntax.getVariableExpression("varName");
        Assert.assertEquals("Incorrect variable name generated.", "${varName}", varName);
    }
    
    @Test
    public void testOnlySingleVariableReferenced() throws Exception {
        String var = "${host}";
        Assert.assertTrue(Syntax.isOnlyASingleVariableReferenced(var));

        var = "${host[0]}";
        Assert.assertTrue(Syntax.isOnlyASingleVariableReferenced(var));

        var = "${host}one";
        Assert.assertFalse(Syntax.isOnlyASingleVariableReferenced(var));

        var = "no ${host}";
        Assert.assertFalse(Syntax.isOnlyASingleVariableReferenced(var));

        var = "host,${host}";
        Assert.assertFalse(Syntax.isOnlyASingleVariableReferenced(var));

        var = "host, ${host}";
        Assert.assertFalse(Syntax.isOnlyASingleVariableReferenced(var));

        var = "host host";
        Assert.assertFalse(Syntax.isOnlyASingleVariableReferenced(var));

        var = "${host}, ${host}";
        Assert.assertFalse(Syntax.isOnlyASingleVariableReferenced(var));

        var = "${host[0][1]}";
        Assert.assertTrue(Syntax.isOnlyASingleVariableReferenced(var));

        var = "${host[0][1]} no";
        Assert.assertFalse(Syntax.isOnlyASingleVariableReferenced(var));

        var = "${host.1} no";
        Assert.assertFalse(Syntax.isOnlyASingleVariableReferenced(var));

        var = "${host.1}";
        Assert.assertTrue(Syntax.isOnlyASingleVariableReferenced(var));

        try {
            var = "${host[0}";
            Syntax.isOnlyASingleVariableReferenced(var);
            Assert.fail("Should have thrown");
        } catch (Exception e) {
            //success
        }
    }
}

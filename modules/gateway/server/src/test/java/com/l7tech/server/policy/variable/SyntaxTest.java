/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.variable;

import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;
import com.l7tech.policy.variable.Syntax;

import java.util.Set;
import java.util.HashSet;

public class SyntaxTest {

    @Test
    public void testGetReferencedNamesWithIndex() throws SAXException {
        final String varName = "${IDS[1]} ${TEST}";
        final String [] referencedNames = Syntax.getReferencedNamesIndexedVarsOmitted(varName);

        Assert.assertEquals("Only 1 value expected", 1, referencedNames.length);

        Assert.assertEquals("Incorrect referenced name found", "TEST", referencedNames[0]);
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
}

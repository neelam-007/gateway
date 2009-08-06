/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Aug 5, 2009
 * Time: 6:03:49 PM
 */
package com.l7tech.server.policy.variable;

import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;
import com.l7tech.policy.variable.Syntax;

public class SyntaxTest {

    @Test
    public void testGetReferencedNamesWithIndex() throws SAXException {
        final String varName = "${IDS[1]} ${TEST}";
        final String [] referencedNames = Syntax.getReferencedNamesIndexedVarsOmitted(varName);

        Assert.assertEquals("Only 1 value expected", 1, referencedNames.length);

        Assert.assertEquals("Incorrect referenced name found", "TEST", referencedNames[0]);
    }
}

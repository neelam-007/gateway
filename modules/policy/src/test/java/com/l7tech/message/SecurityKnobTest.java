/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.message;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.test.BugNumber;
import junit.framework.Assert;
import org.junit.Test;

/**
 * Unit tests fo SecurityKnob
 */
public class SecurityKnobTest {

    @Test
    @BugNumber(9661)
    public void testHasAndSizeDecorationRequirements() throws Exception{
        Message request = new Message(XmlUtil.parse("<xml></xml>"));
        final SecurityKnob securityKnob = request.getSecurityKnob();
        Assert.assertNotNull(securityKnob);

        Assert.assertFalse("No decorations have been added yet", securityKnob.hasDecorationRequirements());
        Assert.assertFalse("There should be no side affect from previous check", securityKnob.hasDecorationRequirements());

        securityKnob.getOrMakeDecorationRequirements();
        Assert.assertTrue("Decorations should have been added", securityKnob.hasDecorationRequirements());

        DecorationRequirements[] decReqs = securityKnob.getDecorationRequirements();
        Assert.assertEquals("Only 1 decoration requirement should exist", 1, decReqs.length);
        Assert.assertNotNull("Decoration requirement should not be null", decReqs[0]);

        //test recipients
        final XmlSecurityRecipientContext recipient1 = new XmlSecurityRecipientContext("actor1", null);
        Assert.assertFalse("No decorations have been added yet", securityKnob.hasAlternateDecorationRequirements(recipient1));
        Assert.assertFalse("There should be no side affect from previous check", securityKnob.hasAlternateDecorationRequirements(recipient1));

        securityKnob.getAlternateDecorationRequirements(recipient1);
        Assert.assertTrue("Decoration for the recipient should have been added.", securityKnob.hasAlternateDecorationRequirements(recipient1));

        final XmlSecurityRecipientContext recipient2 = new XmlSecurityRecipientContext("actor2", null);
        Assert.assertFalse("No decorations have been added yet", securityKnob.hasAlternateDecorationRequirements(recipient2));
        Assert.assertFalse("There should be no side affect from previous check", securityKnob.hasAlternateDecorationRequirements(recipient2));

        securityKnob.getAlternateDecorationRequirements(recipient2);
        Assert.assertTrue("Decoration for the recipient should have been added.", securityKnob.hasAlternateDecorationRequirements(recipient2));

        decReqs = securityKnob.getDecorationRequirements();
        Assert.assertEquals("Only 3 decoration requirements should exist", 3, decReqs.length);
        for (DecorationRequirements decReq : decReqs) {
            Assert.assertNotNull("Decoration requirement should not be null", decReqs);    
        }
    }
}

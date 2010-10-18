package com.l7tech.policy.assertion;

import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.xmlsec.RequireWssSignedElement;
import com.l7tech.xml.xpath.XpathExpression;
import org.junit.Test;

import javax.xml.soap.SOAPConstants;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test case for {@link XpathBasedAssertionNamespaceMigrator}.
 */
public class XpathBasedAssertionNamespaceMigratorTest {

    @Test
    public void testMigrate() throws Exception {
        Map<String, String> xlatMap = new HashMap<String, String>();
        xlatMap.put("urn:source", "urn:dest");
        xlatMap.put("urn:othersource", "urn:otherdest");
        XpathBasedAssertionNamespaceMigrator migrator = new XpathBasedAssertionNamespaceMigrator(xlatMap);

        Map<String, String> nsmap = new HashMap<String, String>();
        nsmap.put("s", SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE);
        nsmap.put("a", "urn:source");
        XpathBasedAssertion xba;
        Assertion root = new AllAssertion(Arrays.asList(
                xba = new RequireWssSignedElement(new XpathExpression("/s:Envelope/s:Body/a:Payload", nsmap))
        ));

        migrator.migrateDescendantsAndSelf(root, null);

        assertTrue(xba.getXpathExpression().getNamespaces().values().contains("urn:dest"));
        assertFalse(xba.getXpathExpression().getNamespaces().values().contains("urn:source"));
    }
}

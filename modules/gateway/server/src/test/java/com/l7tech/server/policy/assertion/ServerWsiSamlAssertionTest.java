package com.l7tech.server.policy.assertion;

import java.util.Map;
import java.util.Iterator;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathConstants;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.w3c.dom.Document;

import com.l7tech.util.ResourceUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.xml.DomElementCursor;
import com.l7tech.common.io.XmlUtil;

/**
 * Test the rules defined for WS-I SAML Token Profile validation.
 *
 * @author $Author$
 * @version $Revision$
 */
public class ServerWsiSamlAssertionTest extends TestCase {

    //- PUBLIC

    public static Test suite() {
        TestSuite suite = new TestSuite(ServerWsiSamlAssertionTest.class);
        return suite;
    }

    public void testSuccesses() throws Exception {
        System.out.println("Running testSuccesses()");
        Map ruleMap = swsa.getRules();
        for(Iterator iterator = ruleMap.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iterator.next();
            XPathExpression xpe = (XPathExpression) entry.getKey();
            String description = (String) entry.getValue();
            String rule = description.substring(0, description.indexOf(':'));

            if(rule.startsWith("R")) {
                System.out.println("Running rule: " + description);
                String messageText = readMessage(rule + "_success_01.xml");
                Document document = XmlUtil.stringToDocument(messageText);
                DomElementCursor dec = new DomElementCursor(document);

                Boolean result = (Boolean) xpe.evaluate(dec, XPathConstants.BOOLEAN);
                assertTrue("XPath should be true for rule '"+description+"'.", result.booleanValue());
            }
        }
    }

    public void testNonSuccesses() throws Exception {
        System.out.println("Running testNonSuccesses()");
        Map ruleMap = swsa.getRules();
        for(Iterator iterator = ruleMap.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iterator.next();
            XPathExpression xpe = (XPathExpression) entry.getKey();
            String description = (String) entry.getValue();
            String rule = description.substring(0, description.indexOf(':'));

            if(rule.startsWith("R")) {
                System.out.println("Running rule: " + description);
                String messageText = readMessage(rule + "_failure_01.xml");
                Document document = XmlUtil.stringToDocument(messageText);
                DomElementCursor dec = new DomElementCursor(document);

                Boolean result = (Boolean) xpe.evaluate(dec, XPathConstants.BOOLEAN);
                assertFalse("XPath should be false [message is invalid] for rule '"+description+"'.", result.booleanValue());
            }
        }
    }

    //- PRIVATE

    private ServerWsiSamlAssertion swsa = new ServerWsiSamlAssertion();

    private String readMessage(String name) throws IOException {
        String result = null;
        InputStream in = null;
        try {
            String resPath = "/com/l7tech/policy/resources/samltokenprofile/" + name;
            in = ServerWsiSamlAssertionTest.class.getResourceAsStream(resPath);
            if(in==null) throw new IllegalStateException("Cannot find resource '" + resPath + "'.");
            result = new String( IOUtils.slurpStream(in));
        }
        finally {
            ResourceUtils.closeQuietly(in);
        }
        return result;
    }
}

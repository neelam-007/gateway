package com.l7tech.server.policy.assertion;

import java.util.Map;
import java.util.Iterator;
import java.util.MissingResourceException;
import java.io.InputStream;
import java.io.IOException;

import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathConstants;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.DomElementCursor;

import org.w3c.dom.Document;

/**
 * Test the rules defined for WS-I BSP validation.
 *
 * @author $Author$
 * @version $Revision$
 */
public class ServerWsiBspAssertionTest extends TestCase {

    //- PUBLIC

    public static Test suite() {
        TestSuite suite = new TestSuite(ServerWsiBspAssertionTest.class);
        return suite;
    }


    public void testSuccesses() throws Exception {
        System.out.println("Running testSuccesses()");
        Map ruleMap = swba.getRules();
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

    public void testFailures() throws Exception {
        System.out.println("Running testSuccesses()");
        Map ruleMap = swba.getRules();
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

    private ServerWsiBspAssertion swba = new ServerWsiBspAssertion();

    private String readMessage(String name) throws IOException {
        String result = null;
        InputStream in = null;
        try {
            String resPath = "/com/l7tech/policy/resources/basicsecurityprofile/" + name;
            in = ServerWsiBspAssertionTest.class.getResourceAsStream(resPath);
            if(in==null) throw new IllegalStateException("Cannot find resource '" + resPath + "'.");
            result = new String(HexUtils.slurpStream(in));
        }
        finally {
            ResourceUtils.closeQuietly(in);
        }
        return result;
    }
}

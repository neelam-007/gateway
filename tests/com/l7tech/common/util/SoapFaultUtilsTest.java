package com.l7tech.common.util;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import org.w3c.dom.Document;

import java.util.logging.Logger;

/**
 * Tests for SoapFaultUtils class.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Sep 8, 2004<br/>
 * $Id$<br/>
 */
public class SoapFaultUtilsTest extends TestCase {
    public static Test suite() {
        return new TestSuite(SoapFaultUtilsTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testNormalFault() throws Exception {
        Document parsedBack = tryThis("CODE", "String", "Details", "Actor");
    }

    public void testSpecialCharacters() throws Exception {
        Document parsedBack = tryThis("<fault>", "[string]", "\"details\"", ">actor=");
    }

    public Document tryThis(String faultCode, String faultString, String faultDetails, String faultActor) throws Exception {
        String fault = SoapFaultUtils.generateRawSoapFault(faultCode, faultString, faultDetails, faultActor);
        logger.info("Raw result:\n" + fault);
        Document output = XmlUtil.stringToDocument(fault);
        logger.info("Parsed result:\n" + XmlUtil.nodeToFormattedString(output));
        return output;
    }

    private final Logger logger = Logger.getLogger(SoapFaultUtilsTest.class.getName());
}

package com.l7tech.common.util;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
        Document parsedBack = tryThis("SpaceCrocodile", "Leakage detected in space suit", "Dont forget to floss", "John");
    }

    public void testSpecialCharacters() throws Exception {
        Document parsedBack = tryThis("<fault>", "![string]?", "\"details\"", ">actor=");
        parsedBack = tryThis("<fault>", "![string]?", "<>blah", ">actor=");
    }

    public void testEmptyStuff() throws Exception {
        Document parsedBack = tryThis("Le Code", "La String", "", null);
    }

    public Document tryThis(String faultCode, String faultString, String faultDetails, String faultActor) throws Exception {

        Element exceptiondetails = null;
        if (faultDetails != null && faultDetails.length() > 0) {
            exceptiondetails = SoapFaultUtils.makeFaultDetailsSubElement("more", faultDetails);
        }

        String fault = SoapFaultUtils.generateSoapFaultXml(faultCode, faultString, exceptiondetails, faultActor);
        logger.info("Raw result:\n" + fault);
        Document output = XmlUtil.stringToDocument(fault);
        logger.info("Parsed result:\n" + XmlUtil.nodeToFormattedString(output));
        return output;
    }

    private final Logger logger = Logger.getLogger(SoapFaultUtilsTest.class.getName());
}

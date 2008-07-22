/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion.xml;

import com.l7tech.common.io.XmlUtil;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JdkSchemaValidationTest extends TestCase {
    private static final Logger logger = Logger.getLogger(JdkSchemaValidationTest.class.getName());
    private final String REUTERS_SCHEMA_URL = "http://locutus/reuters/schemas1/ReutersResearchAPI.xsd";
    private final String REUTERS_REQUEST_URL = "http://locutus/reuters/request1.xml";

    public JdkSchemaValidationTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(JdkSchemaValidationTest.class);
    }

    public static void main(String[] args) throws Throwable {
        junit.textui.TestRunner.run(JdkSchemaValidationTest.suite());
        System.out.println("Test complete: " + JdkSchemaValidationTest.class);
    }

    protected void setUp() throws Exception {
    }

    public void testReutersUrlSchemaJaxp() throws Exception {
        SchemaFactory sfac = SchemaFactory.newInstance(XmlUtil.W3C_XML_SCHEMA);
        sfac.setResourceResolver(new LSResourceResolver() {
            public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
                logger.fine("Resolving resource: " + type + ", " + namespaceURI + ", " + publicId + ", " + systemId + ", " + baseURI);
                return null;
            }
        });

        Schema s = sfac.newSchema(getSchemaSource());
        Validator val = s.newValidator();
        val.setErrorHandler(new ErrorHandler() {
            public void warning(SAXParseException exception) throws SAXException {
                logger.log(Level.INFO, exception.getMessage(), exception);
            }

            public void error(SAXParseException exception) throws SAXException {
                logger.log(Level.WARNING, exception.getMessage(), exception);
            }

            public void fatalError(SAXParseException exception) throws SAXException {
                logger.log(Level.SEVERE, exception.getMessage(), exception);
            }
        });

        URL request = new URL(REUTERS_REQUEST_URL);
        val.validate(getMessageSource(request));
    }

    private StreamSource getMessageSource(URL request) throws IOException {
        return new StreamSource(request.openStream(), REUTERS_REQUEST_URL);
    }

    private StreamSource getSchemaSource() throws IOException {
        return new StreamSource(new URL(REUTERS_SCHEMA_URL).openStream(), REUTERS_SCHEMA_URL);
    }
}

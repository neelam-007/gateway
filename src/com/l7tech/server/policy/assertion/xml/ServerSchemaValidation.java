package com.l7tech.server.policy.assertion.xml;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Validates the soap body's contents of a soap request or soap response against
 * a schema provided by the SchemaValidation assertion.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 4, 2004<br/>
 * $Id$<br/>
 *
 */
public class ServerSchemaValidation implements ServerAssertion {
    public ServerSchemaValidation(SchemaValidation data) {
        this.data = data;
    }

    /**
     * validates the soap envelope's body's child against the schema
     * @param context
     */
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException,
                                                                              PolicyAssertionException {

        // decide which document to act upon based on routing status
        RoutingStatus routing = context.getRoutingStatus();
        if (routing == RoutingStatus.ROUTED || routing == RoutingStatus.ATTEMPTED) {
            // try to validate response
            try {
                logger.finest("validating response document");
                if (!context.getResponse().isXml()) {
                    logger.info("Response not XML; cannot validate schema");
                    return AssertionStatus.NOT_APPLICABLE;
                }

                return checkRequest(context.getResponse().getXmlKnob().getDocument(false));
            } catch (SAXException e) {
                throw new PolicyAssertionException("could not parse response document", e);
            }
        } else {
            // try to validate request
            try {
                logger.finest("validating request document");
                if (!context.getRequest().isXml()) {
                    logger.info("Request not XML; cannot validate schema");
                    return AssertionStatus.NOT_APPLICABLE;
                }

                return checkRequest(context.getRequest().getXmlKnob().getDocument(false));
            } catch (SAXException e) {
                throw new PolicyAssertionException("could not parse request document", e);
            }
        }
    }

    /**
     * validates the soap envelope's body's child against the schema passed in constructor
     * @param soapmsg the full soap envelope.
     */
    AssertionStatus checkRequest(Document soapmsg) throws IOException {
        String[] bodystr = null;
        try {
            bodystr = getXMLElementsToValidate(soapmsg);
        } catch (ParserConfigurationException e) {
            String msg = "parser configuration exception";
            logger.log(Level.WARNING, msg, e);
            throw new IOException(msg + "-" + e.getMessage());
        }
        if (bodystr == null || bodystr.length < 1) {
            logger.fine("empty body. nothing to validate");
            return AssertionStatus.FAILED;
        }
        ByteArrayInputStream schemaIS = new ByteArrayInputStream(data.getSchema().getBytes());
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	    dbf.setNamespaceAware(true);
	    dbf.setValidating(true);
        dbf.setAttribute(XmlUtil.JAXP_SCHEMA_LANGUAGE, XmlUtil.W3C_XML_SCHEMA);
	    // Specify other factory configuration settings
	    dbf.setAttribute(XmlUtil.JAXP_SCHEMA_SOURCE, schemaIS);
        DocumentBuilder db = null;
        try {
            db = dbf.newDocumentBuilder();
            db.setEntityResolver(XmlUtil.getSafeEntityResolver());
        } catch (ParserConfigurationException e) {
            String msg = "parser configuration exception";
            logger.log(Level.WARNING, msg, e);
            throw new IOException(msg + "-" + e.getMessage());
        }
        SchameValidationErrorHandler reporter = new SchameValidationErrorHandler();
        db.setErrorHandler(reporter);
        for (int i = 0; i < bodystr.length; i++) {
            InputSource source = new InputSource(new ByteArrayInputStream(bodystr[i].getBytes()));
            try {
                db.parse(source);
            } catch (SAXException e) {
                String msg = "parsing exception";
                logger.log(Level.WARNING, msg, e);
                throw new IOException(msg + "-" + e.getMessage());
            }
            Collection errors = reporter.recordedErrors();
            if (!errors.isEmpty()) {
                for (Iterator it = errors.iterator(); it.hasNext();) {
                    String msg = "assertion failure: " + it.next().toString();
                    logger.fine(msg);
                }
                return AssertionStatus.FAILED;
            }
        }
        logger.finest("schema validation success");
        return AssertionStatus.NONE;
    }

    private String[] getXMLElementsToValidate(Document doc) throws IOException, ParserConfigurationException {
        if (SoapUtil.isSoapMessage(doc)) {
            return getRequestBodyChild(doc);
        } else {
            return new String[] {XmlUtil.nodeToString(doc.getDocumentElement())};
        }
    }

    private String[] getRequestBodyChild(Document soapenvelope) throws IOException, ParserConfigurationException {
        NodeList bodylist = soapenvelope.getElementsByTagNameNS(soapenvelope.getDocumentElement().getNamespaceURI(),
                                                                SoapUtil.BODY_EL_NAME);
        Element bodyel = null;
        switch (bodylist.getLength()) {
            case 1:
                bodyel = (Element)bodylist.item(0);
                break;
            default:
                return null;
        }
        NodeList bodychildren = bodyel.getChildNodes();
        ArrayList children = new ArrayList();
        for (int i = 0; i < bodychildren.getLength(); i++) {
            Node child = bodychildren.item(i);
            if (child instanceof Element) {
                children.add(child);
            }
        }
        String[] output = new String[children.size()];
        int cnt = 0;
        for (Iterator i = children.iterator(); i.hasNext(); cnt++) {
            output[cnt] = XmlUtil.elementToXml((Element)i.next());
        }
        return output;
    }

    private static class SchameValidationErrorHandler implements ErrorHandler {
        public void warning(SAXParseException exception) throws SAXException {
            // ignore warnings
        }
        public void error(SAXParseException exception) throws SAXException {
            errors.add(exception);
        }
        public void fatalError(SAXParseException exception) throws SAXException {
            errors.add(exception);
        }
        /**
         * prepare this object for another parse operation
         * (forget about previous errors)
         */
        public void reset() {
            errors.clear();
        }
        /**
         * get the errors recorded during parse operation
         * @return a collection of SAXParseException objects
         */
        public Collection recordedErrors() {
            return errors;
        }
        private final ArrayList errors = new ArrayList();
    }

    private SchemaValidation data;
    private final Logger logger = Logger.getLogger(getClass().getName());
}

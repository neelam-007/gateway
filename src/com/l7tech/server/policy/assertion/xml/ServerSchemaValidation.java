package com.l7tech.server.policy.assertion.xml;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.springframework.context.ApplicationContext;
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
    private final Auditor auditor;

    public ServerSchemaValidation(SchemaValidation data, ApplicationContext springContext) {
        this.data = data;
        auditor = new Auditor(this, springContext, logger);
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
                auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_VALIDATE_RESPONSE);
                if (!context.getResponse().isXml()) {
                    auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_RESPONSE_NOT_XML);
                    return AssertionStatus.NOT_APPLICABLE;
                }

                return checkRequest(context.getResponse().getXmlKnob().getDocumentReadOnly());
            } catch (SAXException e) {
                throw new PolicyAssertionException("could not parse response document", e);
            }
        } else {
            // try to validate request
            try {
                auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_VALIDATE_REQUEST);
                if (!context.getRequest().isXml()) {
                    auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_REQUEST_NOT_XML);
                    return AssertionStatus.NOT_APPLICABLE;
                }

                return checkRequest(context.getRequest().getXmlKnob().getDocumentReadOnly());
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
        bodystr = getXMLElementsToValidate(soapmsg);
        if (bodystr == null || bodystr.length < 1) {
            auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_EMPTY_BODY);
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
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {msg}, e);
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
                auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {msg}, e);
                throw new IOException(msg + "-" + e.getMessage());
            }
            Collection errors = reporter.recordedErrors();
            if (!errors.isEmpty()) {
                for (Iterator it = errors.iterator(); it.hasNext();) {
                    auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_FAILED, new String[] {it.next().toString()});
                }
                return AssertionStatus.FAILED;
            }
        }
        auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_SUCCEEDED);
        return AssertionStatus.NONE;
    }

    private String[] getXMLElementsToValidate(Document doc) throws IOException {
        if (SoapUtil.isSoapMessage(doc)) {
            return getRequestBodyChild(doc);
        } else {
            return new String[] {XmlUtil.nodeToString(doc.getDocumentElement())};
        }
    }

    private String[] getRequestBodyChild(Document soapenvelope) throws IOException {
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

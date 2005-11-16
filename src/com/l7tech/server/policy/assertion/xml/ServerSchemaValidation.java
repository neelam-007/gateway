package com.l7tech.server.policy.assertion.xml;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import com.tarari.xml.validation.ValidationException;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.impl.xb.xsdschema.SchemaDocument;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.TarariKnob;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.common.xml.tarari.GlobalTarariContext;
import com.l7tech.common.xml.tarari.TarariMessageContext;
import com.l7tech.common.xml.tarari.TarariMessageContextImpl;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.server.communityschemas.CommunitySchemaManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;

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

    //- PUBLIC

    /**
     *
     */
    public ServerSchemaValidation(SchemaValidation data, ApplicationContext springContext) {
        this.schemaValidationAssertion = data;
        this.auditor = new Auditor(this, springContext, logger);
        this.tarariContext = TarariLoader.getGlobalContext();
        this.springContext = springContext;

        if (tarariContext != null) {
            try {
                SchemaDocument sdoc = SchemaDocument.Factory.parse(new StringReader(data.getSchema()));
                tarariNamespaceUri = sdoc.getSchema().getTargetNamespace();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error getting tns from schema", e);
            } catch (XmlException e) {
                logger.log(Level.SEVERE, "Error getting tns from schema", e);
            }
        }
    }

    /**
     * Validates the soap envelope's body's child against the schema
     */
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException,
                                                                              PolicyAssertionException {
        // decide which document to act upon based on routing status
        RoutingStatus routing = context.getRoutingStatus();
        Message msg = null;
        try {
            msg = getMessageToValidate(routing, context);
        }
        catch(IllegalStateException ise) {
            return AssertionStatus.NOT_APPLICABLE;
        }

        return validateMessage(msg);
    }

    //- PACKAGE

    /**
     * Validates the given document (or parts of it) against schema as directed.
     *
     * @param soapmsg the full soap envelope.
     * @return the AssertionStatus
     */
    AssertionStatus validateDocument(Document soapmsg) throws IOException {
        Element[] elementsToValidate = null;
        try {
            elementsToValidate = getXMLElementsToValidate(soapmsg);
        } catch (InvalidDocumentFormatException e) {
            logger.log(Level.INFO, "The document to validate does not respect the expected format", e);
            return AssertionStatus.FAILED;
        }
        if (elementsToValidate == null || elementsToValidate.length < 1) {
            if (schemaValidationAssertion.isApplyToArguments()) {
                logger.fine("There is nothing to validate. This is legal because setting is set " +
                            "to validate arguments only and certain rpc operations do not have any " +
                            "argument elements.");
                return AssertionStatus.NONE;
            } else {
                auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_EMPTY_BODY);
                return AssertionStatus.FAILED;
            }
        }

        if(schema==null) {
            try {
                //CommunitySchemaManager manager = (CommunitySchemaManager) springContext.getBean("communitySchemaManager");
                SchemaFactory sf = SchemaFactory.newInstance(XmlUtil.W3C_XML_SCHEMA);
                //sf.setResourceResolver(manager.communityLSResourceResolver());
                schema = sf.newSchema(new StreamSource(new StringReader(schemaValidationAssertion.getSchema())));
            }
            catch(SAXException se) {
                String msg = "parsing exception";
                auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {msg}, se);
                throw new IOException(msg + "-" + se.getMessage());
            }
        }

        SchemaValidationErrorHandler reporter = new SchemaValidationErrorHandler();
        Validator v = schema.newValidator();
        v.setErrorHandler(reporter);
        v.setResourceResolver(XmlUtil.getSafeLSResourceResolver());

        for (int i = 0; i < elementsToValidate.length; i++) {
            try {
                v.validate(new DOMSource(elementsToValidate[i]));
            } catch (SAXException e) {
                // drop thru, get the error from the handler
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

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ServerSchemaValidation.class.getName());

    private final Auditor auditor;
    private final GlobalTarariContext tarariContext;
    private String tarariNamespaceUri;
    private ApplicationContext springContext;
    private Schema schema;
    private SchemaValidation schemaValidationAssertion;

    /**
     * Get the request or response message depending on routing state
     */
    private Message getMessageToValidate(RoutingStatus routing, PolicyEnforcementContext context) throws IOException {
        Message msg;

        if (routing == RoutingStatus.ROUTED || routing == RoutingStatus.ATTEMPTED) {
            // try to validate response
            auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_VALIDATE_RESPONSE);

            if (!context.getResponse().isXml()) {
                auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_RESPONSE_NOT_XML);
                throw new IllegalStateException();
            }

            msg = context.getResponse();
        } else {
            // try to validate request
            auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_VALIDATE_REQUEST);

            if (!context.getRequest().isXml()) {
                auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_REQUEST_NOT_XML);
                throw new IllegalStateException();
            }

            msg = context.getRequest();
        }

        return msg;
    }

    /**
     * Validate the given message (using hardware if available).
     */
    private AssertionStatus validateMessage(Message msg) throws IOException, PolicyAssertionException {
        try {
            if (tarariNamespaceUri != null) {
                if (TarariLoader.getGlobalContext().targetNamespaceLoadedMoreThanOnce(tarariNamespaceUri) != 1) {
                    logger.fine("Falling back to software validation because the tns is not used by exactly one schema");
                } else if (schemaValidationAssertion.isApplyToArguments()) {
                    logger.fine("Falling back to software validation because assertion requests " +
                                "that only arguments be validated");
                } else {
                    boolean msgisSoap = msg.isSoap(); // Prime the pump
                    TarariKnob tk = (TarariKnob) msg.getKnob(TarariKnob.class);
                    if (tk != null) {
                        TarariMessageContext tmc = tk.getContext();
                        if (tmc instanceof TarariMessageContextImpl) {
                            TarariMessageContextImpl tarariMessageContext = (TarariMessageContextImpl) tmc;
                            try {
                                if (tarariMessageContext.getStreamContext().isValid()) {
                                    logger.fine("Hardware schema validation success. Checking for right namespace.");
                                    // this only applies to soap messages
                                    if (msgisSoap) {
                                        // todo, there could be more than one element under the body, we need to check all of their ns
                                        if (!tk.getSoapInfo().getPayloadNsUri().equals(tarariNamespaceUri)) {
                                            logger.info("Hardware schema validation succeeded but the tns " +
                                                        "did not match the assertion at hand. Returning failure.");
                                            return AssertionStatus.FAILED;
                                        } else {
                                            logger.fine("Tns match. Returning success.");
                                            return AssertionStatus.NONE;
                                        }
                                    } else {
                                        // todo, need to check the ns of first element without parsing the document (?)
                                        logger.fine("Skipping tns check because not soap. Returning success.");
                                        return AssertionStatus.NONE;
                                    }
                                } else {
                                    logger.info("Hardware schema validation failed. The assertion will " +
                                                "fallback on software schema validation");
                                }
                            } catch (ValidationException e) {
                                auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_FALLBACK, null, e);
                            }
                        }
                    }
                }
            }
            return validateDocument(msg.getXmlKnob().getDocumentReadOnly());
        } catch (SAXException e) {
            throw new PolicyAssertionException("could not parse request or response document", e);
        } catch (NoSuchPartException e) {
            throw new RuntimeException(e); // Can't happen
        }
    }

    /**
     * Get the Elements to perform validation on
     */
    private Element[] getXMLElementsToValidate(Document doc) throws InvalidDocumentFormatException, IOException {
        if (SoapUtil.isSoapMessage(doc)) {
            if (schemaValidationAssertion.isApplyToArguments()) {
                logger.finest("validating against the body 'arguments'");
                return getBodyArguments(doc);
            } else {
                logger.finest("validating against the whole body");
                return getRequestBodyChild(doc);
            }
        } else {
            return new Element[]{doc.getDocumentElement()};
        }
    }

    /**
     * Goes one level deeper than getRequestBodyChild
     */
    private Element[] getBodyArguments(Document soapenvelope) throws InvalidDocumentFormatException, IOException {
        // first, get the body
        Element bodyel = SoapUtil.getBodyElement(soapenvelope);
        // then, get the body's first child element
        NodeList bodychildren = bodyel.getChildNodes();
        Element bodyFirstElement = null;
        for (int i = 0; i < bodychildren.getLength(); i++) {
            Node child = bodychildren.item(i);
            if (child instanceof Element) {
                bodyFirstElement = (Element)child;
                break;
            }
        }
        if (bodyFirstElement == null) {
            throw new InvalidDocumentFormatException("The soap body does not have a child element as expected");
        }
        // construct a return output for each element under the body first child
        NodeList maybearguments = bodyFirstElement.getChildNodes();
        ArrayList argumentList = new ArrayList();
        for (int i = 0; i < maybearguments.getLength(); i++) {
            Node child = maybearguments.item(i);
            if (child instanceof Element) {
                argumentList.add(child);
            }
        }
        Element[] output = new Element[argumentList.size()];
        int cnt = 0;
        for (Iterator i = argumentList.iterator(); i.hasNext(); cnt++) {
            output[cnt] = (Element)i.next();
        }
        return output;
    }

    /**
     *
     */
    private Element[] getRequestBodyChild(Document soapenvelope) throws InvalidDocumentFormatException, IOException {
        Element bodyel = SoapUtil.getBodyElement(soapenvelope);
        NodeList bodychildren = bodyel.getChildNodes();
        ArrayList children = new ArrayList();
        for (int i = 0; i < bodychildren.getLength(); i++) {
            Node child = bodychildren.item(i);
            if (child instanceof Element) {
                children.add(child);
            }
        }
        Element[] output = new Element[children.size()];
        int cnt = 0;
        for (Iterator i = children.iterator(); i.hasNext(); cnt++) {
            output[cnt] = (Element)i.next();
        }
        return output;
    }

    /**
     * Error handler to keep track of any errors
     */
    private static class SchemaValidationErrorHandler implements ErrorHandler {
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
}

package com.l7tech.server.policy.assertion.xml;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.TarariKnob;
import com.l7tech.common.message.XmlKnob;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.xml.ElementCursor;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.common.xml.schema.SchemaEntry;
import com.l7tech.common.xml.tarari.GlobalTarariContext;
import com.l7tech.common.xml.tarari.TarariMessageContext;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.MessageUrlResourceInfo;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.server.communityschemas.CommunitySchemaManager;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.spring.util.WeakReferenceApplicationListener;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.impl.xb.xsdschema.SchemaDocument;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.StringReader;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.text.ParseException;
import java.security.GeneralSecurityException;

/**
 * Validates the soap body's contents of a soap request or soap response against
 * a schema provided by the SchemaValidation assertion.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 4, 2004<br/>
 */
public class ServerSchemaValidation extends AbstractServerAssertion implements ServerAssertion, ApplicationListener {
    private static final Logger logger = Logger.getLogger(ServerSchemaValidation.class.getName());
    public static final String PROP_SUPPRESS_FALLBACK_IF_TARARI_FAILS =
            "com.l7tech.server.schema.suppressSoftwareRecheckIfHardwareFlagsInvalidXml";
    public static final boolean SUPPRESS_FALLBACK_IF_TARARI_FAILS = Boolean.getBoolean(PROP_SUPPRESS_FALLBACK_IF_TARARI_FAILS);


    private static final String MSG_PAYLOAD_NS = "Hardware schema validation succeeded but the tns " +
            "did not match the assertion at hand. Returning failure.";


    private final Auditor auditor;
    private final ApplicationContext springContext;
    private final ResourceGetter resourceGetter;
    private final String tarariNamespaceUri;
    private final SchemaValidation schemaValidationAssertion;
    private PreparedSchema lastPreparedSchema = null;

    private static class PreparedSchema {
        final private Schema schema;
        final private boolean schemaHasDependencies;

        public PreparedSchema(Schema schema, boolean schemaHasDependencies) {
            this.schema = schema;
            this.schemaHasDependencies = schemaHasDependencies;
        }
    }

    public void onApplicationEvent(ApplicationEvent event) {
        if(lastPreparedSchema != null && lastPreparedSchema.schemaHasDependencies) {
            if(event instanceof EntityInvalidationEvent) {
                EntityInvalidationEvent eie = (EntityInvalidationEvent) event;
                if(SchemaEntry.class.isAssignableFrom(eie.getEntityClass())) {
                    logger.info("Invalidating cached validation schema.");
                    lastPreparedSchema = null; // flush cached schema
                }
            }
        }
    }

    /**
     *
     */
    public ServerSchemaValidation(SchemaValidation data, ApplicationContext springContext) throws ServerPolicyException {
        super(data);
        this.springContext = springContext;
        this.auditor = new Auditor(this, springContext, logger);
        this.schemaValidationAssertion = data;

        if (data.getResourceInfo() instanceof MessageUrlResourceInfo)
            throw new ServerPolicyException(data, "MessageUrlResourceInfo is not yet supported.");

        ResourceGetter.ResourceObjectFactory rof = new ResourceGetter.ResourceObjectFactory() {
            public Object createResourceObject(byte[] resourceBytes) throws ParseException {
                logger.info("Loading schema for message validation.");
                try {
                    final boolean[] hasDependencies = new boolean[] { false };
                    CommunitySchemaManager manager = (CommunitySchemaManager)
                            ServerSchemaValidation.this.springContext.getBean("communitySchemaManager");
                    SchemaFactory sf = SchemaFactory.newInstance(XmlUtil.W3C_XML_SCHEMA);
                    final LSResourceResolver delegate = manager.communityLSResourceResolver();
                    sf.setResourceResolver(new LSResourceResolver(){
                        public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
                            hasDependencies[0] = true;
                            return delegate.resolveResource(type, namespaceURI, publicId, systemId, baseURI);
                        }
                    });
                    Schema schema = sf.newSchema(new StreamSource(new ByteArrayInputStream(resourceBytes)));
                    return new PreparedSchema(schema, hasDependencies[0]);
                }
                catch(SAXException se) {
                    String msg = "parsing exception";
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {msg}, se);
                    throw new ParseException(msg + "-" + se.getMessage(), 0);
                }
            }
        };

        this.resourceGetter = ResourceGetter.createResourceGetter(data, rof, null, springContext, auditor);

        // Configure tarari only if it's a static schema.  TODO: find a way to accelerate dynamic schemas
        String tarariNamespaceUri = null;
        if (data.getResourceInfo() instanceof StaticResourceInfo) {
            StaticResourceInfo sri = (StaticResourceInfo)data.getResourceInfo();

            // Listen for community schema updates
            WeakReferenceApplicationListener.addApplicationListener(springContext, this);

            // Tarari
            GlobalTarariContext tarariContext = TarariLoader.getGlobalContext();
            if (tarariContext != null) {
                try {
                    SchemaDocument sdoc = SchemaDocument.Factory.parse(new StringReader(sri.getDocument()));
                    tarariNamespaceUri = sdoc.getSchema().getTargetNamespace();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Error getting tns from schema", e);
                } catch (XmlException e) {
                    logger.log(Level.SEVERE, "Error getting tns from schema", e);
                }
            }
        }
        this.tarariNamespaceUri = tarariNamespaceUri;
    }

    /**
     * Validates the soap envelope's body's child against the schema
     */
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        // decide which document to act upon based on routing status
        RoutingStatus routing = context.getRoutingStatus();
        final Message msg;
        try {
            msg = getMessageToValidate(routing, context);
        }
        catch(IllegalStateException ise) {
            return AssertionStatus.NOT_APPLICABLE;
        }

        AssertionStatus status = validateMessage(msg);
        // not pretty, but functional ...
        if(!isRequest(routing) && status==AssertionStatus.BAD_REQUEST) {
            status = AssertionStatus.BAD_RESPONSE;
        }

        return status;
    }

    //- PACKAGE

    /**
     * Validates the given document (or parts of it) against schema as directed.
     *
     * @param message the message to validate.  Must not be null.
     * @return the AssertionStatus
     * @throws IOException if there is a problem reading the document to validate
     * @throws SAXException if the first part's content type is not text/xml.
     * @throws SAXException if the document to validate is not well-formed
     */
    AssertionStatus validateMessageInSoftware(Message message) throws IOException, SAXException {
        final Document soapmsg;
        XmlKnob xmlKnob = message.getXmlKnob();
        soapmsg = xmlKnob.getDocumentReadOnly();

        final Element[] elementsToValidate;
        try {
            elementsToValidate = getXMLElementsToValidate(soapmsg);
        } catch (InvalidDocumentFormatException e) {
            auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_FAILED, new String[]{"The document to validate does not respect the expected format"});
            return AssertionStatus.BAD_REQUEST; // Note if this is not the request this gets changed later ...
        }
        if (elementsToValidate == null || elementsToValidate.length < 1) {
            if (schemaValidationAssertion.isApplyToArguments()) {
                logger.fine("There is nothing to validate. This is legal because setting is set " +
                            "to validate arguments only and certain rpc operations do not have any " +
                            "argument elements.");
                return AssertionStatus.NONE;
            } else {
                auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_EMPTY_BODY);
                return AssertionStatus.BAD_REQUEST; // Note if this is not the request this gets changed later ...
            }
        }

        final PreparedSchema ps;
        try {
            Object got = resourceGetter.getResource(xmlKnob.getElementCursor());
            if (!(got instanceof PreparedSchema)) {
                // XXX This is a design flaw when cache shared with XSLT assertion.  Not yet fixed.  See Bug #2535
                auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                                    new String[] {"The specified Schema URL has recently been fetched and found to contain something other than a schema"});
                return AssertionStatus.SERVER_ERROR;
            }

            ps = (PreparedSchema)got;

        } catch (ResourceGetter.InvalidMessageException e) {
            auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_FAILED,
                                new String[]{"The document to validate was not well-formed XML"});
            return AssertionStatus.BAD_REQUEST; // Note if this is not the request this gets changed later ...
        } catch (ResourceGetter.UrlNotFoundException e) {
            auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_FAILED,
                                new String[]{"The document to validate made use of namespaces for which we have no schemas registered, " +
                                             "and did not include schema URLs for these namespaces"});
            return AssertionStatus.BAD_REQUEST; // Note if this is not the request this gets changed later ...
        } catch (ResourceGetter.MalformedResourceUrlException e) {
            auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_FAILED,
                                new String[]{"The document to validate included a schema declaration pointing at an invalid URL"});
            return AssertionStatus.BAD_REQUEST; // Note if this is not the request this gets changed later ...
        } catch (ResourceGetter.UrlNotPermittedException e) {
            auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_FAILED,
                                new String[]{"The document to validate included a schema declaration pointing at a URL that is not permitted by the whitelist"});
            return AssertionStatus.BAD_REQUEST; // Note if this is not the request this gets changed later ...
        } catch (ResourceGetter.ResourceIOException e) {
            auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_FAILED,
                                new String[]{"Unable to retrieve a schema document: " + ExceptionUtils.getMessage(e)});
            return AssertionStatus.SERVER_ERROR;
        } catch (ResourceGetter.ResourceParseException e) {
            auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_FAILED,
                                new String[]{"A remote schema document could not be parsed: " + ExceptionUtils.getMessage(e)});
            return AssertionStatus.SERVER_ERROR;
        } catch (GeneralSecurityException e) {
            auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_FAILED,
                                new String[]{"A remote schema document could not be downloaded because an SSL context could not be created: " + ExceptionUtils.getMessage(e)});
            return AssertionStatus.SERVER_ERROR;
        }

        SchemaValidationErrorHandler reporter = new SchemaValidationErrorHandler();
        Validator v = ps.schema.newValidator();
        v.setErrorHandler(reporter);
        v.setResourceResolver(XmlUtil.getSafeLSResourceResolver());

        for (Element element : elementsToValidate) {
            try {
                v.validate(new DOMSource(element));
            } catch (SAXException e) {
                // drop thru, get the error from the handler
            }
            Collection<SAXParseException> errors = reporter.recordedErrors();
            if (!errors.isEmpty()) {
                for (Object error : errors) {
                    auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_FAILED, new String[]{error.toString()});
                }
                return AssertionStatus.BAD_REQUEST; // Note if this is not the request this gets changed later ...
            }
        }

        auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_SUCCEEDED);

        return AssertionStatus.NONE;
    }


    /**
     * Get the request or response message depending on routing state
     */
    private Message getMessageToValidate(RoutingStatus routing, PolicyEnforcementContext context) throws IOException {
        Message msg;

        if (!isRequest(routing)) {
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

    private boolean isRequest(RoutingStatus routing) {
        boolean isRequest = true;

        if (routing == RoutingStatus.ROUTED || routing == RoutingStatus.ATTEMPTED) {
            isRequest = false;
        }

        return isRequest;
    }

    /**
     * Validate the given message (using hardware if available).
     */
    private AssertionStatus validateMessage(Message msg) throws IOException {
        try {
            if (tarariNamespaceUri == null)
                return validateMessageInSoftware(msg); // no hardware

            final GlobalTarariContext globalContext = TarariLoader.getGlobalContext();

            if (schemaValidationAssertion.isApplyToArguments()) {
                logger.fine("Falling back to software validation because assertion requests " +
                        "that only arguments be validated");
                return validateMessageInSoftware(msg);
            }

            boolean msgisSoap = msg.isSoap(); // Prime the pump
            TarariKnob tarariKnob = (TarariKnob) msg.getKnob(TarariKnob.class);
            if (tarariKnob == null) {
                // No hardware?  Invalidated knob already? weird. shouldn't be possible
                logger.fine("Falling back to software validation because there is no message hardware context");
                return validateMessageInSoftware(msg);
            }

            TarariMessageContext doc = tarariKnob.getContext();
            Boolean result = globalContext.validateDocument(doc, tarariNamespaceUri);
            if (result == null) {
                logger.fine("Falling back to software validation because the tns is not used by exactly one schema");
                return validateMessageInSoftware(msg);
            }

            if (Boolean.FALSE.equals(result)) {
                if (SUPPRESS_FALLBACK_IF_TARARI_FAILS) {
                    // Don't recheck with software -- just record the failure and be done
                    auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_FAILED, new String[]{"Hardware reports invalid XML"});
                    return AssertionStatus.FALSIFIED;
                }

                // Recheck failed validations with software, in case the Tarari failure was spurious
                logger.info("Hardware schema validation failed. The assertion will " +
                        "fallback on software schema validation");
                // TODO do we still need to do this at all? does Tarari schema val still have spurious failures?
                return validateMessageInSoftware(msg);
            }

            // Hardware schema val succeeded.
            if (msgisSoap)
                return checkPayloadNamespaces(tarariKnob);
            return checkRootNamespace(doc);

        } catch (SAXException e) {
            auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_FAILED, new String[]{"Invalid XML " + e.getMessage()}, e);
            return AssertionStatus.BAD_REQUEST; // Gets altered to BAD_RESPONSE if this is not the request ...
        } catch (NoSuchPartException e) {
            throw new RuntimeException(e); // Can't happen -- first part is currently never read destructively
        }
    }

    /** Ensure that all payload namespaces of this SOAP message match the tarariNamespaceUri. */
    private AssertionStatus checkPayloadNamespaces(TarariKnob tk) {
        // SOAP message -- ensure that the payload namespace URI matches up.
        String[] payloadUris = tk.getSoapInfo().getPayloadNsUris();
        if (payloadUris == null || payloadUris.length < 1) {
            logger.info(MSG_PAYLOAD_NS);
            return AssertionStatus.FAILED;
        }

        // They must all match up
        for (String payloadUri : payloadUris) {
            if (!tarariNamespaceUri.equals(payloadUri)) {
                logger.info(MSG_PAYLOAD_NS);
                return AssertionStatus.FAILED;
            }
        }

        // They all matched up.
        logger.fine("Tns match. Returning success.");
        return AssertionStatus.NONE;
    }

    /** Ensure that the root namespace of this XML message matches the tarariNamespaceUri. */
    private AssertionStatus checkRootNamespace(TarariMessageContext tmc) {
        // Non-SOAP message.  Ensure root namespace URI matches up.
        ElementCursor cursor = tmc.getElementCursor();
        cursor.moveToDocumentElement();
        String docNs = cursor.getNamespaceUri();
        if (!tarariNamespaceUri.equals(docNs)) {
            logger.fine("Hardware schema validation succeeded against non-SOAP message, " +
                    "but the document element namespace URI did not match the asseriton at hand.");
            return AssertionStatus.FAILED;
        }

        logger.fine("Non-SOAP message validated and root namespace URI matches.  Returning success.");
        return AssertionStatus.NONE;
    }

    /**
     * Get the Elements to perform validation on
     */
    private Element[] getXMLElementsToValidate(Document doc) throws InvalidDocumentFormatException {
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
    private Element[] getBodyArguments(Document soapenvelope) throws InvalidDocumentFormatException {
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
        ArrayList<Element> argumentList = new ArrayList<Element>();
        for (int i = 0; i < maybearguments.getLength(); i++) {
            Node child = maybearguments.item(i);
            if (child instanceof Element) {
                argumentList.add((Element)child);
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
    private Element[] getRequestBodyChild(Document soapenvelope) throws InvalidDocumentFormatException {
        Element bodyel = SoapUtil.getBodyElement(soapenvelope);
        NodeList bodychildren = bodyel.getChildNodes();
        ArrayList<Element> children = new ArrayList<Element>();
        for (int i = 0; i < bodychildren.getLength(); i++) {
            Node child = bodychildren.item(i);
            if (child instanceof Element) {
                children.add((Element)child);
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
        public Collection<SAXParseException> recordedErrors() {
            return errors;
        }
        private final ArrayList<SAXParseException> errors = new ArrayList<SAXParseException>();
    }
}

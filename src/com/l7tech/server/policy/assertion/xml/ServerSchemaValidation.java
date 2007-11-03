package com.l7tech.server.policy.assertion.xml;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.server.audit.Auditor;
import com.l7tech.common.urlcache.UrlResolver;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.XmlKnob;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.MessageUrlResourceInfo;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.assertion.GlobalResourceInfo;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.server.communityschemas.SchemaHandle;
import com.l7tech.server.communityschemas.SchemaManager;
import com.l7tech.server.communityschemas.SchemaValidationErrorHandler;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.util.res.ResourceGetter;
import com.l7tech.server.util.res.ResourceObjectFactory;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.MalformedURLException;

/**
 * Validates the soap body's contents of a soap request or soap response against
 * a schema provided by the SchemaValidation assertion.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 4, 2004<br/>
 */
public class ServerSchemaValidation
        extends AbstractServerAssertion<SchemaValidation>
        implements ServerAssertion
{
    private static final Logger logger = Logger.getLogger(ServerSchemaValidation.class.getName());

    private final Auditor auditor;
    private ResourceGetter<String> resourceGetter;
    private String registeredGlobalSchemaUrl;
    private final SchemaManager schemaManager;
    private final String[] varsUsed;
    private String globalSchemaID = null;

    public ServerSchemaValidation(SchemaValidation data, ApplicationContext springContext) throws ServerPolicyException {
        super(data);
        this.auditor = new Auditor(this, springContext, logger);
        this.schemaManager = (SchemaManager)springContext.getBean("schemaManager");
        this.varsUsed = data.getVariablesUsed();


        AssertionResourceInfo resourceInfo = assertion.getResourceInfo();

        if (resourceInfo instanceof GlobalResourceInfo) {
            // no voodoo necessary, the community schemas are automatically loaded by the schema manager
            globalSchemaID = ((GlobalResourceInfo)resourceInfo).getId();
            return;
        }

        if (resourceInfo instanceof MessageUrlResourceInfo)
            throw new ServerPolicyException(assertion, "MessageUrlResourceInfo is not yet supported.");

        if (resourceInfo instanceof StaticResourceInfo) {
            StaticResourceInfo ri = (StaticResourceInfo)resourceInfo;
            String schemaDoc = ri.getDocument();
            registeredGlobalSchemaUrl = "policy:assertion:schemaval:sa" +
                    System.identityHashCode(this) +
                    ":sd" + HexUtils.encodeBase64(HexUtils.getMd5Digest(schemaDoc.getBytes()), true);
            schemaManager.registerSchema(registeredGlobalSchemaUrl, schemaDoc);

            // Change the resource info -- the static resource is this statically-registered URL now, not the schema
            // document itself
            resourceInfo = new StaticResourceInfo(registeredGlobalSchemaUrl);

        } else
            registeredGlobalSchemaUrl = null;

        final ResourceObjectFactory<String> rof = new ResourceObjectFactory<String>() {
            public String createResourceObject(String resourceContent) {
                return resourceContent;
            }
        };

        final UrlResolver<String> urlResolver = new UrlResolver<String>() {
            public String resolveUrl(String url) {
                return url;
            }
        };

        this.resourceGetter = ResourceGetter.createResourceGetter(
                assertion, resourceInfo, rof, null, urlResolver, auditor); 
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
        catch (IllegalStateException ise) {
            return AssertionStatus.NOT_APPLICABLE;
        }

        AssertionStatus status = validateMessage(msg, context);
        // not pretty, but functional ...
        if (!isRequest(routing) && status == AssertionStatus.BAD_REQUEST) {
            status = AssertionStatus.BAD_RESPONSE;
        }

        return status;
    }

    //- PACKAGE

    /**
     * Validates the given document (or parts of it) against schema as directed.
     *
     * @param message the message to validate.  Must not be null.
     * @param context
     * @return the AssertionStatus
     * @throws IOException  if there is a problem reading the document to validate
     */
    AssertionStatus validateMessage(Message message, PolicyEnforcementContext context) throws IOException {
        SchemaHandle ps = null;
        try {
            XmlKnob xmlKnob = message.getXmlKnob();

            if (globalSchemaID != null) {
                try {
                    ps = schemaManager.getSchemaByUrl(globalSchemaID);
                } catch (MalformedURLException e) {
                    auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_GLOBALREF_BROKEN, new String[]{globalSchemaID});
                    return AssertionStatus.SERVER_ERROR;
                }
            } else {
                Map vars = context.getVariableMap(varsUsed, auditor);
                String schemaUrl = resourceGetter.getResource(xmlKnob.getElementCursor(), vars);
                ps = schemaManager.getSchemaByUrl(schemaUrl);
            }

            SchemaValidationErrorHandler reporter = new SchemaValidationErrorHandler();
            SAXException validationException = null;

            if (assertion.isApplyToArguments()) {
                final Document soapmsg;

                soapmsg = xmlKnob.getDocumentReadOnly();
                final Element[] elementsToValidate;
                try {
                    Element[] result;
                    if (SoapUtil.isSoapMessage(soapmsg)) {
                        logger.finest("validating against the body 'arguments'");
                        result = getBodyArguments(soapmsg);
                    } else {
                        result = new Element[]{soapmsg.getDocumentElement()};
                    }
                    elementsToValidate = result;
                } catch (InvalidDocumentFormatException e) {
                    auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_FAILED, new String[]{"The document to validate does not respect the expected format"});
                    return AssertionStatus.BAD_REQUEST; // Note if this is not the request this gets changed later ...
                }
                if (elementsToValidate == null || elementsToValidate.length < 1) {
                    if (assertion.isApplyToArguments()) {
                        logger.fine("There is nothing to validate. This is legal because setting is set " +
                                "to validate arguments only and certain rpc operations do not have any " +
                                "argument elements.");
                        return AssertionStatus.NONE;
                    } else {
                        auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_EMPTY_BODY);
                        return AssertionStatus.BAD_REQUEST; // Note if this is not the request this gets changed later ...
                    }
                }

                try {
                    ps.validateElements(elementsToValidate, reporter);
                } catch (SAXException e) {
                    validationException = e;
                }
            } else {
                // Validate entire message
                try {
                    ps.validateMessage(message, reporter);
                } catch (SAXException e) {
                    validationException = e;
                }
            }

            if (validationException != null) {
                Collection<SAXParseException> errors = reporter.recordedErrors();
                if (!errors.isEmpty()) {
                    for (Object error : errors) {
                        auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_FAILED, new String[]{error.toString()});
                    }
                    return AssertionStatus.BAD_REQUEST; // Note if this is not the request this gets changed later ...
                }
                // Tarari failure with no message
                auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_FAILED, new String[]{ExceptionUtils.getMessage(validationException)});
                return AssertionStatus.BAD_REQUEST;
            }


            auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_SUCCEEDED);
            return AssertionStatus.NONE;

        } catch (ResourceGetter.InvalidMessageException e) {
            logger.log(Level.INFO, "validation failed", e);
            auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_FAILED,
                    new String[]{"The document to validate was not well-formed XML"});
            return AssertionStatus.BAD_REQUEST; // Note if this is not the request this gets changed later ...
        } catch (ResourceGetter.UrlNotFoundException e) {
            logger.log(Level.INFO, "validation failed", e);
            auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_FAILED,
                    new String[]{"The document to validate made use of namespaces for which we have no schemas registered, " +
                            "and did not include schema URLs for these namespaces"});
            return AssertionStatus.BAD_REQUEST; // Note if this is not the request this gets changed later ...
        } catch (ResourceGetter.MalformedResourceUrlException e) {
            logger.log(Level.INFO, "validation failed", e);
            auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_FAILED,
                    new String[]{"The document to validate included a schema declaration pointing at an invalid URL"});
            return AssertionStatus.BAD_REQUEST; // Note if this is not the request this gets changed later ...
        } catch (ResourceGetter.UrlNotPermittedException e) {
            logger.log(Level.INFO, "validation failed", e);
            auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_FAILED,
                    new String[]{"The document to validate included a schema declaration pointing at a URL that is not permitted by the whitelist"});
            return AssertionStatus.BAD_REQUEST; // Note if this is not the request this gets changed later ...
        } catch (ResourceGetter.ResourceIOException e) {
            logger.log(Level.INFO, "validation failed", e);
            auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_FAILED,
                    new String[]{"Unable to retrieve a schema document: " + ExceptionUtils.getMessage(e)});
            return AssertionStatus.SERVER_ERROR;
        } catch (ResourceGetter.ResourceParseException e) {
            logger.log(Level.INFO, "validation failed", e);
            auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_FAILED,
                    new String[]{"A remote schema document could not be parsed: " + ExceptionUtils.getMessage(e)});
            return AssertionStatus.SERVER_ERROR;
        } catch (GeneralSecurityException e) {
            logger.log(Level.INFO, "validation failed", e);
            auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_FAILED,
                    new String[]{"A remote schema document could not be downloaded because an SSL context could not be created: " + ExceptionUtils.getMessage(e)});
            return AssertionStatus.SERVER_ERROR;
        } catch (NoSuchPartException e) {
            logger.log(Level.INFO, "validation failed", e);
            auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_FAILED,
                    new String[]{"A required MIME part was lost: " + ExceptionUtils.getMessage(e)});
            return AssertionStatus.SERVER_ERROR;
        } catch (SAXException e) {
            logger.log(Level.INFO, "validation failed", e);
            auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_FAILED,
                    new String[]{e.getMessage()});
            return AssertionStatus.BAD_REQUEST; // Note if this is not the request this gets changed later ...
        } finally {
            if (ps != null) ps.close();
        }
    }


    /**
     * Get the request or response message depending on routing state
     */
    private Message getMessageToValidate(RoutingStatus routing, PolicyEnforcementContext context) throws IOException {
        Message msg;

        if (isRequest(routing)) {
            // try to validate request
            auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_VALIDATE_REQUEST);

            if (!context.getRequest().isXml()) {
                auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_REQUEST_NOT_XML);
                throw new IllegalStateException();
            }

            msg = context.getRequest();
        } else {
            // try to validate response
            auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_VALIDATE_RESPONSE);

            if (!context.getResponse().isXml()) {
                auditor.logAndAudit(AssertionMessages.SCHEMA_VALIDATION_RESPONSE_NOT_XML);
                throw new IllegalStateException();
            }

            msg = context.getResponse();
        }

        return msg;
    }

    // TODO introduce a proper field for selecting validation of request or response.  this hack is past its best-before date
    private boolean isRequest(RoutingStatus routing) {
        boolean isRequest = true;

        if (routing == RoutingStatus.ROUTED || routing == RoutingStatus.ATTEMPTED) {
            isRequest = false;
        }

        return isRequest;
    }

    /**
     * Goes one level deeper than getRequestBodyChild
     */
    private static Element[] getBodyArguments(Document soapenvelope) throws InvalidDocumentFormatException {
        // first, get the body
        Element bodyel = SoapUtil.getBodyElement(soapenvelope);
        // then, get the body's first child element
        NodeList bodychildren = bodyel.getChildNodes();
        Element bodyFirstElement = null;
        for (int i = 0; i < bodychildren.getLength(); i++) {
            Node child = bodychildren.item(i);
            if (child instanceof Element) {
                bodyFirstElement = (Element) child;
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
                argumentList.add((Element) child);
            }
        }
        Element[] output = new Element[argumentList.size()];
        int cnt = 0;
        for (Iterator i = argumentList.iterator(); i.hasNext(); cnt++) {
            output[cnt] = (Element) i.next();
        }
        return output;
    }

    private boolean closed = false;

    /** Sets the {@link #closed} flag and returns the old value. */
    private synchronized boolean setClosed() {
        boolean old = closed;
        closed = true;
        return old;
    }

    public void close() {
        if (setClosed()) return;
        if (resourceGetter != null) resourceGetter.close();
        if (registeredGlobalSchemaUrl != null) schemaManager.unregisterSchema(registeredGlobalSchemaUrl);
    }
}

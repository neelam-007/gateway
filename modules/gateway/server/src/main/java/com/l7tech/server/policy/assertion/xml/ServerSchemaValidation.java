package com.l7tech.server.policy.assertion.xml;

import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.message.Message;
import com.l7tech.message.XmlKnob;
import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.MessageUrlResourceInfo;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.communityschemas.SchemaHandle;
import com.l7tech.server.communityschemas.SchemaManager;
import com.l7tech.server.communityschemas.SchemaValidationErrorHandler;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.url.UrlResolver;
import com.l7tech.server.util.res.ResourceGetter;
import com.l7tech.server.util.res.ResourceObjectFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.xml.ElementCursor;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.logging.Level;

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
{
    private final ResourceGetter<String, ElementCursor> resourceGetter;
    private final String registeredGlobalSchemaUrl;
    private final SchemaManager schemaManager;
    private final String[] varsUsed;
    private final String globalSchemaUri;

    public ServerSchemaValidation(SchemaValidation data, ApplicationContext springContext) throws ServerPolicyException {
        super(data);
        this.schemaManager = (SchemaManager)springContext.getBean("schemaManager");
        this.varsUsed = data.getVariablesUsed();

        AssertionResourceInfo resourceInfo = assertion.getResourceInfo();

        if (resourceInfo instanceof GlobalResourceInfo) {
            // no voodoo necessary, global resource schemas are automatically loaded by the schema manager
            globalSchemaUri = ((GlobalResourceInfo)resourceInfo).getId();
            schemaManager.registerUri(globalSchemaUri);
            resourceGetter = null;
            registeredGlobalSchemaUrl = null;
            return;
        } else {
            globalSchemaUri = null;
        }

        if (resourceInfo instanceof MessageUrlResourceInfo)
            throw new ServerPolicyException(assertion, "MessageUrlResourceInfo is not yet supported.");

        if (resourceInfo instanceof StaticResourceInfo) {
            StaticResourceInfo ri = (StaticResourceInfo)resourceInfo;
            String schemaDoc = ri.getDocument();
            String schemaUri = ri.getOriginalUrl();
            String registeredGlobalSchemaUniqueId = "policy:assertion:schemaval:sa" +
                    System.identityHashCode(this) +
                    ":sd" + HexUtils.encodeBase64(HexUtils.getMd5Digest(schemaDoc.getBytes()), true);
            if ( schemaUri == null ) {
                registeredGlobalSchemaUrl = registeredGlobalSchemaUniqueId;    
            } else { // add the unique id as a fragment or part of the fragment
                String sep = schemaUri.contains( "#" ) ? "_" : "#";
                registeredGlobalSchemaUrl = schemaUri + sep + registeredGlobalSchemaUniqueId;
            }

            schemaManager.registerSchema(registeredGlobalSchemaUrl, null, schemaDoc);

            // Change the resource info -- the static resource is this statically-registered URL now, not the schema
            // document itself
            resourceInfo = new StaticResourceInfo(registeredGlobalSchemaUrl);

        } else
            registeredGlobalSchemaUrl = null;

        final ResourceObjectFactory<String> rof = new ResourceObjectFactory<String>() {
            @Override
            public String createResourceObject( final String resourceContent ) {
                return resourceContent;
            }
            @Override
            public void closeResourceObject( final String resourceObject ) {
            }
        };

        final UrlResolver<String> urlResolver = new UrlResolver<String>() {
            @Override
            public String resolveUrl(Audit audit,String url) {
                return url;
            }
        };

        this.resourceGetter = ResourceGetter.createResourceGetter(
                assertion, resourceInfo, rof, null, urlResolver, getAudit());
    }

    /**
     * Validates the soap envelope's body's child against the schema
     */
    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        TargetMessageType target = assertion.getTarget();
        final Message msg;
        final String targetDescription;
        if (target == null) {
            // Backward compatibility: decide which document to act upon based on routing status
            final RoutingStatus routing = context.getRoutingStatus();
            final boolean isRequest = routing != RoutingStatus.ROUTED && routing != RoutingStatus.ATTEMPTED;

            if (isRequest) {
                msg = context.getRequest();
                target = TargetMessageType.REQUEST;
            } else {
                msg = context.getResponse();
                target = TargetMessageType.RESPONSE;
            }
            targetDescription = target.name().toLowerCase();
        } else {
            try {
                msg = context.getTargetMessage(assertion, true);
                targetDescription = assertion.getTargetName();
            } catch (NoSuchVariableException e) {
                logAndAudit(AssertionMessages.NO_SUCH_VARIABLE, assertion.getOtherTargetMessageVariable());
                return AssertionStatus.FAILED;
            }
        }

        if (!msg.isXml()) {
            logAndAudit(AssertionMessages.SCHEMA_VALIDATION_NOT_XML, targetDescription);
            return AssertionStatus.NOT_APPLICABLE;
        }

        logAndAudit(AssertionMessages.SCHEMA_VALIDATION_VALIDATING, targetDescription);
        AssertionStatus status = validateMessage(msg, context);
        if (status == AssertionStatus.BAD_REQUEST) {
            switch (target) {
                case RESPONSE:
                    status = AssertionStatus.BAD_RESPONSE;
                    break;
                case OTHER:
                    status = AssertionStatus.FAILED;
                    break;
                default:
                    // Leave it alone
                    break;
            }
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

            if ( globalSchemaUri != null) {
                try {
                    ps = schemaManager.getSchemaByUri( getAudit(), globalSchemaUri );
                } catch (MalformedURLException e) {
                    logAndAudit(AssertionMessages.SCHEMA_VALIDATION_GLOBALREF_BROKEN, globalSchemaUri );
                    return AssertionStatus.SERVER_ERROR;
                } catch (IOException e) {
                    logAndAudit(AssertionMessages.SCHEMA_VALIDATION_GLOBALREF_BROKEN, new String[] { globalSchemaUri  }, ExceptionUtils.getDebugException(e));
                    return AssertionStatus.SERVER_ERROR;
                }
            } else {
                final Map<String,Object> vars = context.getVariableMap(varsUsed, getAudit());
                String schemaUrl = null;
                try {
                    //fyi xmlKnob.getElementCursor() is in support of MessageUrlResourceGetter's which are not currently supported by this assertion. See constructor.
                    //xmlKnob.getElementCursor() is not needed, but will be used if support for MessageUrlResourceInfo's are allowed in this server assertion.
                    schemaUrl = resourceGetter.getResource( null , vars );
                    ps = schemaManager.getSchemaByUri( getAudit(), schemaUrl );
                } catch (IOException e) {
                    logAndAudit(AssertionMessages.SCHEMA_VALIDATION_IO_ERROR, new String[] { "schema URL: " + schemaUrl + ": " + ExceptionUtils.getMessage(e) }, ExceptionUtils.getDebugException(e));
                    return AssertionStatus.SERVER_ERROR;
                }
            }

            SchemaValidationErrorHandler reporter = new SchemaValidationErrorHandler();
            SAXException validationException = null;

            try {
                ps.validateMessage(message, assertion.getValidationTarget(), reporter);
            } catch (SAXException se) {
                validationException = se;
            }

            if (validationException != null) {
                Collection<SAXParseException> errors = reporter.recordedErrors();
                if (!errors.isEmpty()) {
                    List<String> messes = new ArrayList<String>();
                    for (Object error : errors) {
                        String mess = error.toString();
                        logAndAudit(AssertionMessages.SCHEMA_VALIDATION_FAILED, mess);
                        messes.add(mess);
                    }
                    context.setVariable(SchemaValidation.SCHEMA_FAILURE_VARIABLE, messes.toArray());
                    return AssertionStatus.BAD_REQUEST; // Note if this is not the request this gets changed later ...
                }
                // Tarari failure with no message
                schemaValidationFailed(context, ExceptionUtils.getMessage(validationException), ExceptionUtils.getDebugException(validationException));
                return AssertionStatus.BAD_REQUEST;
            }


            logAndAudit(AssertionMessages.SCHEMA_VALIDATION_SUCCEEDED);
            return AssertionStatus.NONE;

        } catch (ResourceGetter.InvalidMessageException e) {
            schemaValidationFailed(context, "The document to validate was not well-formed XML: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return AssertionStatus.BAD_REQUEST; // Note if this is not the request this gets changed later ...
        } catch (ResourceGetter.UrlNotFoundException e) {
            schemaValidationFailed(context, "The document to validate made use of namespaces for which we have no schemas registered, and did not include schema URLs for these namespaces", ExceptionUtils.getDebugException(e));
            return AssertionStatus.BAD_REQUEST; // Note if this is not the request this gets changed later ...
        } catch (ResourceGetter.MalformedResourceUrlException e) {
            schemaValidationFailed(context, "The document to validate included a schema declaration pointing at an invalid URL", ExceptionUtils.getDebugException(e));
            return AssertionStatus.BAD_REQUEST; // Note if this is not the request this gets changed later ...
        } catch (ResourceGetter.UrlNotPermittedException e) {
            schemaValidationFailed(context, "The document to validate included a schema declaration pointing at a URL that is not permitted by the whitelist", ExceptionUtils.getDebugException(e));
            return AssertionStatus.BAD_REQUEST; // Note if this is not the request this gets changed later ...
        } catch (ResourceGetter.ResourceIOException e) {
            schemaValidationFailed(context, "Unable to retrieve a schema document: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return AssertionStatus.SERVER_ERROR;
        } catch (ResourceGetter.ResourceParseException e) {
            schemaValidationFailed(context, "A remote schema document could not be parsed: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return AssertionStatus.SERVER_ERROR;
        } catch (GeneralSecurityException e) {
            schemaValidationFailed(context, "A remote schema document could not be downloaded because an SSL context could not be created: " + ExceptionUtils.getMessage(e), e);
            return AssertionStatus.SERVER_ERROR;
        } catch (NoSuchPartException e) {
            schemaValidationFailed(context, "A required MIME part was lost: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return AssertionStatus.SERVER_ERROR;
        } catch (SAXException e) {
            schemaValidationFailed(context, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return AssertionStatus.BAD_REQUEST; // Note if this is not the request this gets changed later ...
        } finally {
            if (ps != null) ps.close();
        }
    }

    private void schemaValidationFailed(PolicyEnforcementContext context, String msg, Throwable t) {
        logger.log(Level.INFO, "validation failed: " + msg);
        logAndAudit(AssertionMessages.SCHEMA_VALIDATION_FAILED, new String[] {msg}, t);
        context.setVariable(SchemaValidation.SCHEMA_FAILURE_VARIABLE, msg);
    }

    private boolean closed = false;

    /** Sets the {@link #closed} flag and returns the old value. */
    private synchronized boolean setClosed() {
        boolean old = closed;
        closed = true;
        return old;
    }

    @Override
    public void close() {
        if (setClosed()) return;
        if (resourceGetter != null) resourceGetter.close();
        if (globalSchemaUri != null) schemaManager.unregisterUri(globalSchemaUri);
        if (registeredGlobalSchemaUrl != null) schemaManager.unregisterSchema(registeredGlobalSchemaUrl);
    }
}

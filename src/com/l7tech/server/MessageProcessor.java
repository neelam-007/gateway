/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.common.message.Message;
import com.l7tech.common.message.XmlKnob;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.security.xml.decorator.WssDecorator;
import com.l7tech.common.security.xml.processor.*;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.MessageNotSoapException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.server.event.EventManager;
import com.l7tech.server.event.MessageProcessed;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.PolicyVersionException;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.secureconversation.SecureConversationContextManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.service.resolution.ServiceResolutionException;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceStatistics;
import org.springframework.context.support.ApplicationObjectSupport;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class MessageProcessor extends ApplicationObjectSupport {
    private final ServiceManager serviceManager;
    private final WssDecorator wssDecorator;
    private final PrivateKey serverPrivateKey;
    private final X509Certificate serverCertificate;
    private final EventManager eventManager;

    /**
     * Create the new <code>MessageProcessor</code> instance with the service
     * manager, Wss Decorator instance and the server private key.
     * All arguments are required
     * @param sm the service manager
     * @param wssd the Wss Decorator
     * @param pkey the server private key
     * @param pkey the server certificate
     * @throws IllegalArgumentException if any of the arguments is null
     */
    public MessageProcessor(ServiceManager sm, WssDecorator wssd, PrivateKey pkey, X509Certificate cert, EventManager em)
        throws IllegalArgumentException {
        if (sm == null) {
            throw new IllegalArgumentException("Service Manager is required");
        }
        if (wssd == null) {
            throw new IllegalArgumentException("Wss Decorator is required");
        }
        if (pkey == null) {
            throw new IllegalArgumentException("Server Private Key is required");
        }
        if (cert == null) {
            throw new IllegalArgumentException("Server Certificate is required");
        }
        if (em == null) {
            throw new IllegalArgumentException("Event Manager is required");
        }
        this.serviceManager = sm;
        this.wssDecorator = wssd;
        this.serverPrivateKey = pkey;
        this.serverCertificate = cert;
        this.eventManager = em;

        try {
            _xppf = XmlPullParserFactory.newInstance();
        } catch (XmlPullParserException e) {
            throw new RuntimeException( e );
        }
        _xppf.setNamespaceAware( true );
        _xppf.setValidating( false );
    }

    public AssertionStatus processMessage( PolicyEnforcementContext context )
            throws IOException, PolicyAssertionException, PolicyVersionException
    {
        try {
            currentContext.set(context);
            return reallyProcessMessage(context);
        } finally {
            currentContext.set(null);
        }
    }

    private AssertionStatus reallyProcessMessage( PolicyEnforcementContext context )
            throws IOException, PolicyAssertionException, PolicyVersionException
    {
        final Message request = context.getRequest();
        final Message response = context.getResponse();
        context.setAuditLevel(DEFAULT_MESSAGE_AUDIT_LEVEL);
        ProcessorResult wssOutput = null;

        // WSS-Processing Step
        boolean isSoap = false;
        boolean hasSecurity = false;

        try {
            isSoap = context.getRequest().isSoap();
            hasSecurity = context.getRequest().getSoapKnob().isSecurityHeaderPresent();
        } catch (SAXException e) {
            logger.log(Level.SEVERE, "Request XML is not well-formed", e);
            return AssertionStatus.BAD_REQUEST;
        } catch (MessageNotSoapException e) {
            logger.log(Level.INFO, "Message is not soap", e); // TODO remove this or downgrade to FINE
        }

        if (isSoap && hasSecurity) {
            WssProcessor trogdor = new WssProcessorImpl(); // no need for locator
            try {
                final XmlKnob reqXml = request.getXmlKnob();
                wssOutput = trogdor.undecorateMessage(reqXml.getDocument(false),
                                                      serverCertificate,
                                                      serverPrivateKey,
                                                      SecureConversationContextManager.getInstance());
                // todo, refactor SoapRequest so that it keeps a hold on the original message
                final Document message = wssOutput.getUndecoratedMessage();
                if (message != null)
                    reqXml.setDocument(message);
                reqXml.setProcessorResult(wssOutput);
            } catch (MessageNotSoapException e) {
                logger.log(Level.FINE, "Message is not SOAP; will not have any WSS results.");
                // this shouldn't be possible now
                // pass through, leaving wssOutput as null
            } catch (ProcessorException e) {
                logger.log(Level.SEVERE, "Error in WSS processing of request", e);
                return AssertionStatus.SERVER_ERROR;
            } catch (InvalidDocumentFormatException e) {
                logger.log(Level.SEVERE, "Error in WSS processing of request", e);
                return AssertionStatus.SERVER_ERROR;
            } catch (GeneralSecurityException e) {
                logger.log(Level.SEVERE, "Error in WSS processing of request", e);
                return AssertionStatus.SERVER_ERROR;
            } catch (SAXException e) {
                logger.log(Level.SEVERE, "Error getting xml document from request", e);
                return AssertionStatus.SERVER_ERROR;
            } catch (BadSecurityContextException e) {
                logger.log(Level.SEVERE, "Error in WSS processing of request", e);
                context.setFaultDetail(e);
                return AssertionStatus.FAILED;
            }
            logger.finest("WSS processing of request complete.");
        }
        
        // Policy Verification Step
        AssertionStatus status = AssertionStatus.UNDEFINED;
        try {
            PublishedService service = serviceManager.resolve(context.getRequest());

            if ( service == null ) {
                logger.warning( "Service not found" );
                status = AssertionStatus.SERVICE_NOT_FOUND;
            } else if ( service.isDisabled() ) {
                logger.warning( "Service disabled" );
                status = AssertionStatus.SERVICE_DISABLED;
            } else {
                logger.finer("Resolved service " + service.getName() + " #" + service.getOid());
                context.setService( service );

                // skip the http request header version checking if it is not a Http request
                if(context.getRequest().isHttpRequest()) {
                    // check if requestor provided a version number for published service
                    String requestorVersion = context.getRequest().getHttpRequestKnob().getHeaderSingleValue(SecureSpanConstants.HttpHeaders.POLICY_VERSION);
                    if (requestorVersion != null && requestorVersion.length() > 0) {
                        // format is policyId|policyVersion (seperated with char '|')
                        boolean wrongPolicyVersion = false;
                        int indexofbar = requestorVersion.indexOf('|');
                        if (indexofbar < 0) {
                            logger.finest("policy version passed has incorrect format");
                            wrongPolicyVersion = true;
                        } else {
                            try {
                                long reqPolicyId = Long.parseLong(requestorVersion.substring(0, indexofbar));
                                long reqPolicyVer = Long.parseLong(requestorVersion.substring(indexofbar+1));
                                if (reqPolicyVer != service.getVersion() || reqPolicyId != service.getOid()) {
                                    logger.finest("policy version passed is invalid " + requestorVersion + " instead of "
                                            + service.getOid() + "|" + service.getVersion());
                                    wrongPolicyVersion = true;
                                }
                            } catch (NumberFormatException e) {
                                wrongPolicyVersion = true;
                                logger.log(Level.FINE, "wrong format for policy version", e);
                            }
                        }
                        if (wrongPolicyVersion) {
                            context.setPolicyViolated(true);
                            throw new PolicyVersionException();
                        }
                    } else {
                        logger.fine("Requestor did not provide policy id.");
                    }
                }

                // Get the server policy
                ServerAssertion serverPolicy = null;
                try {
                    serverPolicy = serviceManager.getServerPolicy(service.getOid());
                } catch (FindException e) {
                    logger.log(Level.WARNING, "cannot get policy", e);
                    serverPolicy = null;
                }
                if (serverPolicy == null) {
                    throw new ServiceResolutionException("service is resolved but no corresponding policy available.");
                }

                // Run the policy
                logger.finest("Run the server policy");
                ServiceStatistics stats = null;
                try {
                    stats = serviceManager.getServiceStatistics(service.getOid());
                } catch (FindException e) {
                    logger.log(Level.WARNING, "cannot get a stats object", e);
                }
                if (stats != null) stats.attemptedRequest();
                status = serverPolicy.checkRequest(context);

                // Execute deferred actions for request, then response
                if (status == AssertionStatus.NONE)
                    status = doDeferredAssertions(context);

                // Run response through WssDecorator if indicated
                if (status == AssertionStatus.NONE &&
                        response.isSoap() &&
                        response.getXmlKnob().getDecorationRequirements() != null)
                {
                    Document doc = null;
                    try {
                        final XmlKnob respXml = response.getXmlKnob();
                        final DecorationRequirements responseDecoReq = respXml.getDecorationRequirements();
                        XmlKnob reqXml = request.getXmlKnob();
                        doc = respXml.getDocument(true); // writable, we are about to decorate it

                        if (request.isSoap()) {
                            final String messageId = SoapUtil.getL7aMessageId(reqXml.getDocument(false));
                            if (messageId != null) {
                                SoapUtil.setL7aRelatesTo(doc, messageId);
                            }
                        }

                        if (responseDecoReq != null) {
                            if (wssOutput != null && wssOutput.getSecurityNS() != null) {
                                responseDecoReq.setPreferredSecurityNamespace(wssOutput.getSecurityNS());
                            }
                            if (wssOutput != null && wssOutput.getWSUNS() != null) {
                                responseDecoReq.setPreferredWSUNamespace(wssOutput.getWSUNS());
                            }
                        }

                        wssDecorator.decorateMessage(doc, responseDecoReq);
                        respXml.setDocument(doc);
                    } catch (Exception e) {
                        throw new PolicyAssertionException("Failed to apply WSS decoration to response", e);
                    }
                }

                RoutingStatus rstat = context.getRoutingStatus();

                boolean authorized = false;
                if ( rstat == RoutingStatus.ATTEMPTED ) {
                    /* If policy execution got as far as the routing assertion,
                       we consider the request to have been authorized, whether
                       or not the routing itself was successful. */
                    authorized = true;
                }

                if ( status == AssertionStatus.NONE ) {
                    // Policy execution concluded successfully
                    authorized = true;
                    if ( rstat == RoutingStatus.ROUTED || rstat == RoutingStatus.NONE ) {
                        /* We include NONE because it's valid (albeit silly)
                        for a policy to contain no RoutingAssertion */
                        logger.fine("Request was completed with status " + " " + status.getNumeric() + " (" + status.getMessage() + ")");
                        if (stats != null) stats.completedRequest();
                    } else {
                        // This can only happen when a post-routing assertion fails
                        logger.severe( "Policy status was NONE but routing was attempted anyway!" );
                        status = AssertionStatus.SERVER_ERROR;
                    }
                } else {
                    // Policy execution concluded unsuccessfully
                    if (rstat == RoutingStatus.ATTEMPTED) {
                        // Most likely the failure was in the routing assertion
                        logger.warning("Request routing failed with status " + status.getNumeric() + " (" + status.getMessage() + ")");
                        status = AssertionStatus.FAILED;
                    } else {
                        // Most likely the failure was in some other assertion
                        logger.warning( "Policy evaluation resulted in status " + status.getNumeric() + " (" + status.getMessage() + ")" );
                    }
                }

                if ( authorized && stats != null ) stats.authorizedRequest();
            }

            return status;
        } catch ( ServiceResolutionException sre ) {
            logger.log(Level.SEVERE, sre.getMessage(), sre);
            return AssertionStatus.SERVER_ERROR;
        } catch (SAXException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return AssertionStatus.SERVER_ERROR;
        } finally {
            try {
                eventManager.fire(new MessageProcessed(context, status, this));
            } catch (Throwable t) {
                logger.log(Level.WARNING, "EventManager threw exception logging message processing result", t);
            }
        }
    }

    private AssertionStatus doDeferredAssertions(PolicyEnforcementContext context)
            throws PolicyAssertionException, IOException
    {
        AssertionStatus status = AssertionStatus.NONE;
        for (Iterator di = context.getDeferredAssertions().iterator(); di.hasNext();) {
            ServerAssertion assertion = (ServerAssertion)di.next();
            status = assertion.checkRequest(context);
            if (status != AssertionStatus.NONE)
                return status;
        }
        return status;
    }

    public XmlPullParser getPullParser() throws XmlPullParserException {
        return _xppf.newPullParser();
    }

    public static PolicyEnforcementContext getCurrentContext() {
        return (PolicyEnforcementContext)currentContext.get();
    }

    private static ThreadLocal currentContext = new ThreadLocal();

    private final Logger logger = Logger.getLogger(getClass().getName());

    private XmlPullParserFactory _xppf;
    private static final Level DEFAULT_MESSAGE_AUDIT_LEVEL = Level.INFO;
}
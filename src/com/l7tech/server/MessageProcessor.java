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
import com.l7tech.common.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.common.security.xml.processor.*;
import com.l7tech.common.util.KeystoreUtils;
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
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class MessageProcessor {

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
        try {
            isSoap = context.getRequest().isSoap();
        } catch (SAXException e) {
            logger.log(Level.SEVERE, "Request XML is not well-formed", e);
            return AssertionStatus.BAD_REQUEST;
        }

        if (isSoap) {
            WssProcessor trogdor = new WssProcessorImpl(); // no need for locator
            X509Certificate serverSSLcert = null;
            PrivateKey sslPrivateKey = null;
            try {
                serverSSLcert = KeystoreUtils.getInstance().getSslCert();
                sslPrivateKey = getServerKey();
            } catch (CertificateException e) {
                logger.log(Level.SEVERE, "Error getting server cert/private key", e);
                return AssertionStatus.SERVER_ERROR;
            } catch (KeyStoreException e) {
                logger.log(Level.SEVERE, "Error getting server cert/private key", e);
                return AssertionStatus.SERVER_ERROR;
            }
            try {
                final XmlKnob reqXml = request.getXmlKnob();
                wssOutput = trogdor.undecorateMessage(reqXml.getDocument(),
                                                      serverSSLcert,
                                                      sslPrivateKey,
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
            ServiceManager manager = (ServiceManager)context.getSpringContext().getBean("serviceManager");
            PublishedService service = manager.resolve(context.getRequest());

            if ( service == null ) {
                logger.warning( "Service not found" );
                status = AssertionStatus.SERVICE_NOT_FOUND;
            } else if ( service.isDisabled() ) {
                logger.warning( "Service disabled" );
                status = AssertionStatus.SERVICE_DISABLED;
            } else {
                logger.finer("Resolved service " + service.getName() + " #" + service.getOid());
                context.setService( service );

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

                // Get the server policy
                ServerAssertion serverPolicy = null;
                try {
                    serverPolicy = manager.getServerPolicy(service.getOid());
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
                    stats = manager.getServiceStatistics(service.getOid());
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
                        doc = respXml.getDocument();

                        if (request.isSoap()) {
                            final String messageId = SoapUtil.getL7aMessageId(reqXml.getDocument());
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

                        getWssDecorator().decorateMessage(doc, responseDecoReq);
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
                EventManager.fire(new MessageProcessed(context, status));
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

    private synchronized PrivateKey getServerKey() throws KeyStoreException {
        if (privateServerKey == null) {
            privateServerKey = KeystoreUtils.getInstance().getSSLPrivateKey();
        }
        return privateServerKey;
    }

    public static MessageProcessor getInstance() {
        return SingletonHolder.singleton;
    }

    public XmlPullParser getPullParser() throws XmlPullParserException {
        return _xppf.newPullParser();
    }

    private MessageProcessor() {
        try {
            _xppf = XmlPullParserFactory.newInstance();
        } catch (XmlPullParserException e) {
            throw new RuntimeException( e );
        }
        _xppf.setNamespaceAware( true );
        _xppf.setValidating( false );
    }

    public static PolicyEnforcementContext getCurrentContext() {
        return (PolicyEnforcementContext)currentContext.get();
    }

    private static class SingletonHolder {
        private static MessageProcessor singleton = new MessageProcessor();
    }

    private static synchronized WssDecorator getWssDecorator() {
        if (_wssDecorator != null) return _wssDecorator;
        return _wssDecorator = new WssDecoratorImpl();
    }

    private static ThreadLocal currentContext = new ThreadLocal();

    private final Logger logger = Logger.getLogger(getClass().getName());

    private XmlPullParserFactory _xppf;
    private static WssDecorator _wssDecorator = null;

    private PrivateKey privateServerKey = null;
    private static final Level DEFAULT_MESSAGE_AUDIT_LEVEL = Level.INFO;
}
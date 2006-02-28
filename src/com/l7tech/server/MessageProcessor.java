/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */
package com.l7tech.server;

import com.l7tech.common.Feature;
import com.l7tech.common.LicenseManager;
import com.l7tech.common.LicenseException;
import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.audit.MessageProcessingMessages;
import com.l7tech.common.audit.AuditContext;
import com.l7tech.common.audit.AuditDetailMessage;
import com.l7tech.common.message.*;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.security.xml.SecurityTokenResolver;
import com.l7tech.common.security.xml.SecurityActor;
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
import com.l7tech.server.event.MessageProcessed;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyContextCache;
import com.l7tech.server.message.HttpSessionPolicyContextCache;
import com.l7tech.server.policy.PolicyVersionException;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.secureconversation.SecureConversationContextManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.service.ServiceMetricsManager;
import com.l7tech.server.service.ServiceMetrics;
import com.l7tech.server.service.resolution.ServiceResolutionException;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceStatistics;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.support.ApplicationObjectSupport;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.text.MessageFormat;

/**
 * The server side component processing messages from any transport layer.
 *
 * @author alex
 */
public class MessageProcessor extends ApplicationObjectSupport implements InitializingBean {
    private final ServiceManager serviceManager;
    private final WssDecorator wssDecorator;
    private final PrivateKey serverPrivateKey;
    private final X509Certificate serverCertificate;
    private final SecurityTokenResolver securityTokenResolver;
    private final LicenseManager licenseManager;
    private final ServiceMetricsManager serviceMetricsManager;
    private final AuditContext auditContext;
    private final ServerConfig serverConfig;

    /**
     * Create the new <code>MessageProcessor</code> instance with the service
     * manager, Wss Decorator instance and the server private key.
     * All arguments are required
     *
     * @param sm             the service manager
     * @param wssd           the Wss Decorator
     * @param pkey           the server private key
     * @param pkey           the server certificate
     * @param licenseManager the SSG's Licence Manager
     * @param metricsManager the SSG's ServiceMetricsManager
     * @throws IllegalArgumentException if any of the arguments is null
     */
    public MessageProcessor(ServiceManager sm,
                            WssDecorator wssd,
                            PrivateKey pkey,
                            X509Certificate cert,
                            SecurityTokenResolver securityTokenResolver,
                            LicenseManager licenseManager,
                            ServiceMetricsManager metricsManager,
                            AuditContext auditContext,
                            ServerConfig serverConfig)
      throws IllegalArgumentException {
        if (sm == null) throw new IllegalArgumentException("Service Manager is required");
        if (wssd == null) throw new IllegalArgumentException("Wss Decorator is required");
        if (pkey == null) throw new IllegalArgumentException("Server Private Key is required");
        if (cert == null) throw new IllegalArgumentException("Server Certificate is required");
        if (licenseManager == null) throw new IllegalArgumentException("License Manager is required");
        if (metricsManager == null) throw new IllegalArgumentException("Service Metrics Manager is required");
        if (auditContext == null) throw new IllegalArgumentException("Audit Context is required");
        if (serverConfig == null) throw new IllegalArgumentException("Server Config is required");
        this.serviceManager = sm;
        this.wssDecorator = wssd;
        this.serverPrivateKey = pkey;
        this.serverCertificate = cert;
        this.securityTokenResolver = securityTokenResolver;
        this.licenseManager = licenseManager;
        this.serviceMetricsManager = metricsManager;
        this.auditContext = auditContext;
        this.serverConfig = serverConfig;
    }

    public AssertionStatus processMessage(PolicyEnforcementContext context)
            throws IOException, PolicyAssertionException, PolicyVersionException, LicenseException, MethodNotAllowedException {
        return reallyProcessMessage(context);
    }

    private AssertionStatus reallyProcessMessage(PolicyEnforcementContext context)
            throws IOException, PolicyAssertionException, PolicyVersionException, LicenseException, MethodNotAllowedException {
        context.setAuditLevel(DEFAULT_MESSAGE_AUDIT_LEVEL);
        // License check hook
        licenseManager.requireFeature(Feature.MESSAGEPROCESSOR);

        final Message request = context.getRequest();
        final Message response = context.getResponse();
        ProcessorResult wssOutput = null;
        AssertionStatus status = AssertionStatus.UNDEFINED;
        ServiceMetrics metrics = null;
        ServiceStatistics stats = null;

        // WSS-Processing Step
        try {
            boolean isSoap = false;
            boolean hasSecurity = false;

            try {
                isSoap = context.getRequest().isSoap();
                hasSecurity = isSoap && context.getRequest().getSoapKnob().isSecurityHeaderPresent();
            } catch (SAXException e) {
                auditor.logAndAudit(MessageProcessingMessages.REQUEST_INVALID_XML_FORMAT, null, e);
                return AssertionStatus.BAD_REQUEST;
            } catch (MessageNotSoapException e) {
                auditor.logAndAudit(MessageProcessingMessages.MESSAGE_NOT_SOAP, null, e);
            }

            if (isSoap && hasSecurity) {
                WssProcessor trogdor = new WssProcessorImpl(); // no need for locator
                try {
                    final SecurityKnob reqSec = request.getSecurityKnob();
                    wssOutput = trogdor.undecorateMessage(request,
                                                          null, serverCertificate,
                                                          serverPrivateKey,
                                                          SecureConversationContextManager.getInstance(),
                                                          securityTokenResolver);
                    reqSec.setProcessorResult(wssOutput);
                } catch (MessageNotSoapException e) {
                    auditor.logAndAudit(MessageProcessingMessages.MESSAGE_NOT_SOAP_NO_WSS, null, e);
                    // this shouldn't be possible now
                    // pass through, leaving wssOutput as null
                } catch (ProcessorException e) {
                    auditor.logAndAudit(MessageProcessingMessages.ERROR_WSS_PROCESSING, null, e);
                    return AssertionStatus.SERVER_ERROR;
                } catch (InvalidDocumentFormatException e) {
                    auditor.logAndAudit(MessageProcessingMessages.ERROR_WSS_PROCESSING, null, e);
                    context.setAuditLevel(Level.SEVERE);
                    return AssertionStatus.SERVER_ERROR;
                } catch (GeneralSecurityException e) {
                    auditor.logAndAudit(MessageProcessingMessages.ERROR_WSS_PROCESSING, null, e);
                    context.setAuditLevel(Level.SEVERE);
                    return AssertionStatus.SERVER_ERROR;
                } catch (SAXException e) {
                    auditor.logAndAudit(MessageProcessingMessages.ERROR_RETRIEVE_XML, null, e);
                    context.setAuditLevel(Level.SEVERE);
                    return AssertionStatus.SERVER_ERROR;
                } catch (BadSecurityContextException e) {
                    auditor.logAndAudit(MessageProcessingMessages.ERROR_WSS_PROCESSING, null, e);
                    context.setAuditLevel(Level.SEVERE);
                    context.setFaultDetail(e);
                    return AssertionStatus.FAILED;
                }
                auditor.logAndAudit(MessageProcessingMessages.WSS_PROCESSING_COMPLETE);
            }

            // Policy Verification Step
            PublishedService service = serviceManager.resolve(context.getRequest());
            if (service == null) {
                auditor.logAndAudit(MessageProcessingMessages.SERVICE_NOT_FOUND);
                return AssertionStatus.SERVICE_NOT_FOUND;
            }

            metrics = serviceMetricsManager.getServiceMetrics(service.getOid());

            if (service.isDisabled()) {
                auditor.logAndAudit(MessageProcessingMessages.SERVICE_DISABLED);
                if (metrics != null) metrics.addAttemptedRequest((int)(System.currentTimeMillis() - context.getStartTime()));
                return AssertionStatus.SERVICE_NOT_FOUND;
            }

            auditor.logAndAudit(MessageProcessingMessages.RESOLVED_SERVICE, new String[]{service.getName(), String.valueOf(service.getOid())});
            context.setService(service);

            // skip the http request header version checking if it is not a Http request
            if (context.getRequest().isHttpRequest()) {
                HttpRequestKnob httpRequestKnob = context.getRequest().getHttpRequestKnob();

                // Check the request method
                String requestMethod = httpRequestKnob.getMethod();
                if (!service.isMethodAllowed(requestMethod)) {
                    String[] args = new String[]{requestMethod, service.getName()};
                    auditor.logAndAudit(MessageProcessingMessages.METHOD_NOT_ALLOWED, args);
                    throw new MethodNotAllowedException(
                            MessageFormat.format(MessageProcessingMessages.METHOD_NOT_ALLOWED.getMessage(), args));
                }

                // initialize cache
                if(httpRequestKnob instanceof HttpServletRequestKnob) {
                    String cacheId = service.getOid() + "." + service.getVersion();
                    PolicyContextCache cache = new HttpSessionPolicyContextCache(((HttpServletRequestKnob)httpRequestKnob).getHttpServletRequest(), cacheId);
                    context.setCache(cache);
                }
                // check if requestor provided a version number for published service
                String requestorVersion = httpRequestKnob.getHeaderSingleValue(SecureSpanConstants.HttpHeaders.POLICY_VERSION);
                if (requestorVersion != null && requestorVersion.length() > 0) {
                    // format is policyId|policyVersion (seperated with char '|')
                    boolean wrongPolicyVersion = false;
                    int indexofbar = requestorVersion.indexOf('|');
                    if (indexofbar < 0) {
                        auditor.logAndAudit(MessageProcessingMessages.POLICY_VERSION_WRONG_FORMAT);
                        wrongPolicyVersion = true;
                    } else {
                        try {
                            long reqPolicyId = Long.parseLong(requestorVersion.substring(0, indexofbar));
                            long reqPolicyVer = Long.parseLong(requestorVersion.substring(indexofbar + 1));
                            if (reqPolicyVer != service.getVersion() || reqPolicyId != service.getOid()) {
                                auditor.logAndAudit(MessageProcessingMessages.POLICY_VERSION_INVALID, new String[]{requestorVersion, String.valueOf(service.getOid()), String.valueOf(service.getVersion())});
                                wrongPolicyVersion = true;
                            }
                        } catch (NumberFormatException e) {
                            wrongPolicyVersion = true;
                            auditor.logAndAudit(MessageProcessingMessages.POLICY_VERSION_WRONG_FORMAT, null, e);
                        }
                    }
                    if (wrongPolicyVersion) {
                        context.setRequestPolicyViolated();
                        throw new PolicyVersionException();
                    }
                } else {
                    auditor.logAndAudit(MessageProcessingMessages.POLICY_ID_NOT_PROVIDED);
                }
            }

            // Get the server policy
            ServerAssertion serverPolicy;
            try {
                serverPolicy = serviceManager.getServerPolicy(service.getOid());
            } catch (FindException e) {
                auditor.logAndAudit(MessageProcessingMessages.CANNOT_GET_POLICY, null, e);
                serverPolicy = null;
            }
            if (serverPolicy == null) {
                throw new ServiceResolutionException("service is resolved but no corresponding policy available.");
            }

            // Run the policy
            auditor.logAndAudit(MessageProcessingMessages.RUNNING_POLICY);
            try {
                stats = serviceManager.getServiceStatistics(service.getOid());
            } catch (FindException e) {
                auditor.logAndAudit(MessageProcessingMessages.CANNOT_GET_STATS_OBJECT, null, e);
            }
            if (stats != null) stats.attemptedRequest();
            status = serverPolicy.checkRequest(context);

            // Execute deferred actions for request, then response
            if (status == AssertionStatus.NONE)
                status = doDeferredAssertions(context);

            // Run response through WssDecorator if indicated
            if (status == AssertionStatus.NONE &&
                  response.isXml() &&
                  response.getSecurityKnob().getDecorationRequirements().length > 0 &&
                  response.isSoap())
            {
                Document doc;
                try {
                    final XmlKnob respXml = response.getXmlKnob();
                    DecorationRequirements[] allrequirements = response.getSecurityKnob().getDecorationRequirements();
                    XmlKnob reqXml = request.getXmlKnob();
                    SecurityKnob reqSec = request.getSecurityKnob();
                    doc = respXml.getDocumentWritable(); // writable, we are about to decorate it
                    if (request.isSoap()) {
                        final String messageId = SoapUtil.getL7aMessageId(reqXml.getDocumentReadOnly());
                        if (messageId != null) {
                            SoapUtil.setL7aRelatesTo(doc, messageId);
                        }
                    }

                    // if the request was processed on the noactor sec header instead of the l7 sec actor, then
                    // the response's decoration requirements should map this (if applicable)
                    if (reqSec.getProcessorResult() != null &&
                          reqSec.getProcessorResult().getProcessedActor() != null &&
                          reqSec.getProcessorResult().getProcessedActor() == SecurityActor.NOACTOR) {
                        // go find the l7 decoreq and adjust the actor
                        for (int i = 0; i < allrequirements.length; i++) {
                            if (SecurityActor.L7ACTOR.getValue().equals(allrequirements[i].getSecurityHeaderActor())) {
                                allrequirements[i].setSecurityHeaderActor(SecurityActor.NOACTOR.getValue());
                            }
                        }
                    }

                    // do the actual decoration
                    for (int i = 0; i < allrequirements.length; i++) {
                        final DecorationRequirements responseDecoReq = allrequirements[i];
                        if (responseDecoReq != null && wssOutput != null) {
                            if (wssOutput.getSecurityNS() != null)
                                responseDecoReq.getNamespaceFactory().setWsseNs(wssOutput.getSecurityNS());
                            if (wssOutput.getWSUNS() != null)
                                responseDecoReq.getNamespaceFactory().setWsuNs(wssOutput.getWSUNS());
                            if (wssOutput.isDerivedKeySeen())
                                responseDecoReq.setUseDerivedKeys(true);
                        }
                        wssDecorator.decorateMessage(doc, responseDecoReq);
                    }
                } catch (Exception e) {
                    throw new PolicyAssertionException("Failed to apply WSS decoration to response", e);
                }
            }

            return status;
        } catch (ServiceResolutionException sre) {
            auditor.logAndAudit(MessageProcessingMessages.EXCEPTION_SEVERE, new String[]{sre.getMessage()}, sre);
            return AssertionStatus.SERVER_ERROR;
        } catch (SAXException e) {
            auditor.logAndAudit(MessageProcessingMessages.EXCEPTION_SEVERE, new String[]{e.getMessage()}, e);
            return AssertionStatus.SERVER_ERROR;
        } finally {
            RoutingStatus rstat = context.getRoutingStatus();
            final int delta = (int)(System.currentTimeMillis() - context.getStartTime());
            if (metrics != null) metrics.addAttemptedRequest(delta);

            // Check auditing hints, position here since our "success" may be a back end service fault
            if(isAuditHintingEnabled()) {
                if(!context.isAuditSaveRequest() || !context.isAuditSaveResponse()) {
                    Set auditingHints = auditContext.getHints();
                    if(auditingHints.contains(HINT_SAVE_REQUEST)) context.setAuditSaveRequest(true);
                    if(auditingHints.contains(HINT_SAVE_RESPONSE)) context.setAuditSaveResponse(true);
                }
            }

            if (status == AssertionStatus.NONE) {
                // Policy execution concluded successfully
                if (metrics != null) metrics.addAuthorizedRequest();
                if (rstat == RoutingStatus.ROUTED || rstat == RoutingStatus.NONE) {
                    /* We include NONE because it's valid (albeit silly)
                    for a policy to contain no RoutingAssertion */
                    auditor.logAndAudit(MessageProcessingMessages.COMPLETION_STATUS, new String[]{String.valueOf(status.getNumeric()), status.getMessage()});
                    if (stats != null) stats.completedRequest();
                    if (metrics != null) metrics.addCompletedRequest(delta);
                } else {
                    // This can only happen when a post-routing assertion fails
                    auditor.logAndAudit(MessageProcessingMessages.SERVER_ERROR);
                    status = AssertionStatus.SERVER_ERROR;
                }
            } else {
                // Policy execution was not successful

                // Add audit details
                if (rstat == RoutingStatus.ATTEMPTED) {
                    // Most likely the failure was in the routing assertion
                    auditor.logAndAudit(MessageProcessingMessages.ROUTING_FAILED, new String[]{String.valueOf(status.getNumeric()), status.getMessage()});
                    status = AssertionStatus.FAILED;
                } else {
                    // Bump up the audit level to that of the highest AssertionStatus level
                    if(isAuditAssertionStatusEnabled()) {
                        Set statusSet = context.getSeenAssertionStatus();
                        Level highestLevel = getHighestAssertionStatusLevel(statusSet);
                        if(highestLevel.intValue() > context.getAuditLevel().intValue())
                            context.setAuditLevel(highestLevel);
                    }

                    // Most likely the failure was in some other assertion
                    auditor.logAndAudit(MessageProcessingMessages.POLICY_EVALUATION_RESULT, new String[]{String.valueOf(status.getNumeric()), status.getMessage()});
                }
            }

            try {
                getApplicationContext().publishEvent(new MessageProcessed(context, status, this));
            } catch (Throwable t) {
                auditor.logAndAudit(MessageProcessingMessages.EVENT_MANAGER_EXCEPTION, null, t);
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

    private static final Level DEFAULT_MESSAGE_AUDIT_LEVEL = Level.INFO;
    private static final AuditDetailMessage.Hint HINT_SAVE_REQUEST = AuditDetailMessage.Hint.getHint("MessageProcessor.saveRequest");
    private static final AuditDetailMessage.Hint HINT_SAVE_RESPONSE = AuditDetailMessage.Hint.getHint("MessageProcessor.saveRequest");

    private Auditor auditor;
    final Logger logger = Logger.getLogger(getClass().getName());

    public void afterPropertiesSet() throws Exception {
        this.auditor = new Auditor(this, getApplicationContext(), logger);
    }

    private Level getHighestAssertionStatusLevel(Set assertionStatusSet) {
        Level level = Level.ALL;
        for (Iterator iterator = assertionStatusSet.iterator(); iterator.hasNext();) {
            AssertionStatus assertionStatus = (AssertionStatus) iterator.next();
            if(assertionStatus.getLevel().intValue() > level.intValue()) {
                level = assertionStatus.getLevel();
            }
        }
        return level;
    }

    private boolean isAuditHintingEnabled() {
        String propHintingStr = serverConfig.getProperty(ServerConfig.PARAM_AUDIT_HINTING_ENABLED);
        boolean hintingEnabled = false;
        if(propHintingStr!=null) {
            hintingEnabled = Boolean.valueOf(propHintingStr).booleanValue();
        }
        return hintingEnabled;
    }

    private boolean isAuditAssertionStatusEnabled() {
        String propStatusStr = serverConfig.getProperty(ServerConfig.PARAM_AUDIT_ASSERTION_STATUS_ENABLED);
        boolean statusEnabled = false;
        if(propStatusStr!=null) {
            statusEnabled = Boolean.valueOf(propStatusStr).booleanValue();
        }
        return statusEnabled;
    }
}
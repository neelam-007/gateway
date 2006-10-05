/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */
package com.l7tech.server;

import com.l7tech.common.LicenseException;
import com.l7tech.common.LicenseManager;
import com.l7tech.common.audit.AuditContext;
import com.l7tech.common.audit.AuditDetailMessage;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.audit.MessageProcessingMessages;
import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.message.*;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.security.xml.SecurityActor;
import com.l7tech.common.security.xml.SecurityTokenResolver;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.security.xml.decorator.WssDecorator;
import com.l7tech.common.security.xml.processor.*;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.MessageNotSoapException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.server.event.MessageProcessed;
import com.l7tech.server.log.TrafficLogger;
import com.l7tech.server.message.HttpSessionPolicyContextCache;
import com.l7tech.server.message.PolicyContextCache;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.PolicyVersionException;
import com.l7tech.server.policy.ServerPolicyHandle;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.secureconversation.SecureConversationContextManager;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.server.service.ServiceMetrics;
import com.l7tech.server.service.ServiceMetricsManager;
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
import java.text.MessageFormat;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The server side component processing messages from any transport layer.
 *
 * @author alex
 */
public class MessageProcessor extends ApplicationObjectSupport implements InitializingBean {
    private final ServiceCache serviceCache;
    private final WssDecorator wssDecorator;
    private final PrivateKey serverPrivateKey;
    private final X509Certificate serverCertificate;
    private final SecurityTokenResolver securityTokenResolver;
    private final LicenseManager licenseManager;
    private final ServiceMetricsManager serviceMetricsManager;
    private final AuditContext auditContext;
    private final ServerConfig serverConfig;
    private final TrafficLogger trafficLogger;

    /**
     * Create the new <code>MessageProcessor</code> instance with the service
     * manager, Wss Decorator instance and the server private key.
     * All arguments are required
     *
     * @param sc             the service cache
     * @param wssd           the Wss Decorator
     * @param pkey           the server private key
     * @param pkey           the server certificate
     * @param licenseManager the SSG's Licence Manager
     * @param metricsManager the SSG's ServiceMetricsManager
     * @throws IllegalArgumentException if any of the arguments is null
     */
    public MessageProcessor(ServiceCache sc,
                            WssDecorator wssd,
                            PrivateKey pkey,
                            X509Certificate cert,
                            SecurityTokenResolver securityTokenResolver,
                            LicenseManager licenseManager,
                            ServiceMetricsManager metricsManager,
                            AuditContext auditContext,
                            ServerConfig serverConfig,
                            TrafficLogger trafficLogger)
      throws IllegalArgumentException {
        if (sc == null) throw new IllegalArgumentException("Service Cache is required");
        if (wssd == null) throw new IllegalArgumentException("Wss Decorator is required");
        if (pkey == null) throw new IllegalArgumentException("Server Private Key is required");
        if (cert == null) throw new IllegalArgumentException("Server Certificate is required");
        if (licenseManager == null) throw new IllegalArgumentException("License Manager is required");
        if (metricsManager == null) throw new IllegalArgumentException("Service Metrics Manager is required");
        if (auditContext == null) throw new IllegalArgumentException("Audit Context is required");
        if (serverConfig == null) throw new IllegalArgumentException("Server Config is required");
        this.serviceCache = sc;
        this.wssDecorator = wssd;
        this.serverPrivateKey = pkey;
        this.serverCertificate = cert;
        this.securityTokenResolver = securityTokenResolver;
        this.licenseManager = licenseManager;
        this.serviceMetricsManager = metricsManager;
        this.auditContext = auditContext;
        this.serverConfig = serverConfig;
        this.trafficLogger = trafficLogger;
    }

    public AssertionStatus processMessage(PolicyEnforcementContext context)
            throws IOException, PolicyAssertionException, PolicyVersionException, LicenseException, MethodNotAllowedException {
        return reallyProcessMessage(context);
    }

    private AssertionStatus reallyProcessMessage(PolicyEnforcementContext context)
            throws IOException, PolicyAssertionException, PolicyVersionException, LicenseException, MethodNotAllowedException {
        context.setAuditLevel(DEFAULT_MESSAGE_AUDIT_LEVEL);
        // License check hook
        licenseManager.requireFeature(GatewayFeatureSets.SERVICE_MESSAGEPROCESSOR);

        final Message request = context.getRequest();
        final Message response = context.getResponse();
        ProcessorResult wssOutput = null;
        AssertionStatus status = AssertionStatus.UNDEFINED;
        ServiceMetrics metrics = null;
        ServiceStatistics stats = null;
        boolean attemptedRequest = false;

        // WSS-Processing Step
        ServerPolicyHandle serverPolicy = null;
        try {
            boolean isSoap = false;
            boolean hasSecurity = false;

            try {
                isSoap = context.getRequest().isSoap();
                hasSecurity = isSoap && context.getRequest().getSoapKnob().isSecurityHeaderPresent();
            } catch (SAXException e) {
                auditor.logAndAudit(MessageProcessingMessages.REQUEST_INVALID_XML_FORMAT_WITH_DETAIL, new String[]{e.getMessage()}, e);
                status = AssertionStatus.BAD_REQUEST;
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
                    status = AssertionStatus.SERVER_ERROR;
                    return AssertionStatus.SERVER_ERROR;
                } catch (InvalidDocumentFormatException e) {
                    auditor.logAndAudit(MessageProcessingMessages.ERROR_WSS_PROCESSING, null, e);
                    context.setAuditLevel(Level.WARNING);
                    status = AssertionStatus.BAD_REQUEST;
                    return AssertionStatus.BAD_REQUEST;
                } catch (GeneralSecurityException e) {
                    auditor.logAndAudit(MessageProcessingMessages.ERROR_WSS_PROCESSING, null, e);
                    context.setAuditLevel(Level.SEVERE);
                    status = AssertionStatus.SERVER_ERROR;
                    return AssertionStatus.SERVER_ERROR;
                } catch (SAXException e) {
                    auditor.logAndAudit(MessageProcessingMessages.ERROR_RETRIEVE_XML, null, e);
                    context.setAuditLevel(Level.SEVERE);
                    status = AssertionStatus.SERVER_ERROR;
                    return AssertionStatus.SERVER_ERROR;
                } catch (BadSecurityContextException e) {
                    auditor.logAndAudit(MessageProcessingMessages.ERROR_WSS_PROCESSING, null, e);
                    context.setAuditLevel(Level.SEVERE);
                    status = AssertionStatus.FAILED;
                    return AssertionStatus.FAILED;
                }
                auditor.logAndAudit(MessageProcessingMessages.WSS_PROCESSING_COMPLETE);
            }

            // Policy Verification Step
            PublishedService service = serviceCache.resolve(context.getRequest());
            if (service == null) {
                auditor.logAndAudit(MessageProcessingMessages.SERVICE_NOT_FOUND);
                status = AssertionStatus.SERVICE_NOT_FOUND;
                return AssertionStatus.SERVICE_NOT_FOUND;
            }

            metrics = serviceMetricsManager.getServiceMetrics(service.getOid());

            if (service.isDisabled()) {
                auditor.logAndAudit(MessageProcessingMessages.SERVICE_DISABLED);
                status = AssertionStatus.SERVICE_NOT_FOUND;
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
                    String[] auditArgs = new String[] { requestMethod, service.getName() };
                    Object[] faultArgs = new Object[] { requestMethod };
                    auditor.logAndAudit(MessageProcessingMessages.METHOD_NOT_ALLOWED, auditArgs);
                    throw new MethodNotAllowedException(
                            MessageFormat.format(MessageProcessingMessages.METHOD_NOT_ALLOWED_FAULT.getMessage(), faultArgs));
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
                        context.setRequestClaimingWrongPolicyVersion();
                        throw new PolicyVersionException();
                    }
                } else {
                    auditor.logAndAudit(MessageProcessingMessages.POLICY_ID_NOT_PROVIDED);
                }
            }

            // Check request data
            if (context.getRequest().getMimeKnob().isMultipart() &&
                !service.isMultipart()) {
                auditor.logAndAudit(MessageProcessingMessages.MULTIPART_NOT_ALLOWED);
                status = AssertionStatus.BAD_REQUEST;
                return AssertionStatus.BAD_REQUEST;
            }

            // Get the server policy
            try {
                serverPolicy = serviceCache.getServerPolicy(service.getOid());
            } catch (InterruptedException e) {
                auditor.logAndAudit(MessageProcessingMessages.CANNOT_GET_POLICY, null, e);
                serverPolicy = null;
            }
            if (serverPolicy == null) {
                throw new ServiceResolutionException("service is resolved but the corresponding policy is invalid");
            }

            // Run the policy
            auditor.logAndAudit(MessageProcessingMessages.RUNNING_POLICY);
            try {
                stats = serviceCache.getServiceStatistics(service.getOid());
            } catch (InterruptedException e) {
                auditor.logAndAudit(MessageProcessingMessages.CANNOT_GET_STATS_OBJECT, null, e);
            }
            attemptedRequest = true;

            status = serverPolicy.checkRequest(context);
            context.setPolicyResult(status);

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
                        for (DecorationRequirements requirement : allrequirements) {
                            if (SecurityActor.L7ACTOR.getValue().equals(requirement.getSecurityHeaderActor())) {
                                requirement.setSecurityHeaderActor(SecurityActor.NOACTOR.getValue());
                            }
                        }
                    }

                    // do the actual decoration
                    for (final DecorationRequirements responseDecoReq : allrequirements) {
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
                    throw new PolicyAssertionException(null, "Failed to apply WSS decoration to response", e);
                }
            }

            return status;

        //TODO why do these audits pass params to a message that does not accept them?
        //     should this be EXCEPTION_SEVERE_WITH_MORE_INFO?
        } catch (PublishedService.ServiceException se) {
            auditor.logAndAudit(MessageProcessingMessages.EXCEPTION_SEVERE, new String[]{se.getMessage()}, se);
            context.setPolicyResult(AssertionStatus.SERVER_ERROR);
            status = AssertionStatus.SERVER_ERROR;
            return AssertionStatus.SERVER_ERROR;
        } catch (ServiceResolutionException sre) {
            auditor.logAndAudit(MessageProcessingMessages.EXCEPTION_SEVERE, new String[]{sre.getMessage()}, sre);
            context.setPolicyResult(AssertionStatus.SERVER_ERROR);
            status = AssertionStatus.SERVER_ERROR;
            return AssertionStatus.SERVER_ERROR;
        } catch (SAXException e) {
            auditor.logAndAudit(MessageProcessingMessages.EXCEPTION_SEVERE, new String[]{e.getMessage()}, e);
            context.setPolicyResult(AssertionStatus.SERVER_ERROR);
            status = AssertionStatus.SERVER_ERROR;
            return AssertionStatus.SERVER_ERROR;
        } finally {
            context.setEndTime();
            RoutingStatus rstat = context.getRoutingStatus();

            if (serverPolicy != null) serverPolicy.close();

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
                if (rstat == RoutingStatus.ROUTED || rstat == RoutingStatus.NONE) {
                    /* We include NONE because it's valid (albeit silly)
                    for a policy to contain no RoutingAssertion */
                    auditor.logAndAudit(MessageProcessingMessages.COMPLETION_STATUS, new String[]{String.valueOf(status.getNumeric()), status.getMessage()});
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
                        Set<AssertionStatus> statusSet = context.getSeenAssertionStatus();
                        Level highestLevel = getHighestAssertionStatusLevel(statusSet);
                        if(highestLevel.intValue() > context.getAuditLevel().intValue())
                            context.setAuditLevel(highestLevel);
                    }

                    // Most likely the failure was in some other assertion
                    String serviceName = "";
                    if (context != null && context.getService() != null) {
                        serviceName = context.getService().getName() + " [" + context.getService().getOid() + "]";
                    }
                    auditor.logAndAudit(MessageProcessingMessages.POLICY_EVALUATION_RESULT, new String[]{serviceName, String.valueOf(status.getNumeric()), status.getMessage()});
                }
            }

            // ensure result is set
            if (context.getPolicyResult() == null) context.setPolicyResult(status);

            boolean authorizedRequest = false;
            boolean completedRequest = false;
            if (status == AssertionStatus.NONE) {
                // Policy execution concluded successfully.
                authorizedRequest = true;
                // Considered success (i.e., completed); unless we have routing
                // assertion and the routed response has HTTP error status: then
                // it's a routing failure.
                if (rstat == RoutingStatus.NONE) {
                    completedRequest = true;
                } else if (rstat == RoutingStatus.ROUTED) {
                    if (!request.isHttpRequest() || response.getHttpResponseKnob().getStatus() < HttpConstants.STATUS_ERROR_RANGE_START) {
                        completedRequest = true;
                    }
                }
            } else {
                // Policy execution was not successful.
                // Considered policy violation (i.e., not authorized); unless we
                // have routing assertion and it failed or the routed response
                // has HTTP error status: then it's a routing failure.
                if (rstat == RoutingStatus.ATTEMPTED ||
                    (rstat == RoutingStatus.ROUTED && (request.isHttpRequest() && response.getHttpResponseKnob().getStatus() >= HttpConstants.STATUS_ERROR_RANGE_START))) {
                    authorizedRequest = true;
                }
            }

            if (stats != null) {
                if (attemptedRequest) {
                    stats.attemptedRequest();
                    if (authorizedRequest) {
                        stats.authorizedRequest();
                        if (completedRequest) {
                            stats.completedRequest();
                        }
                    }
                }
            }

            if (metrics != null && attemptedRequest) {
                final int frontTime = (int)(context.getEndTime() - context.getStartTime());
                final int backTime = (int)(context.getRoutingEndTime() - context.getRoutingStartTime());
                metrics.addRequest(authorizedRequest, completedRequest, frontTime, backTime);
            }

            try {
                getApplicationContext().publishEvent(new MessageProcessed(context, status, this));
            } catch (Throwable t) {
                auditor.logAndAudit(MessageProcessingMessages.EVENT_MANAGER_EXCEPTION, null, t);
            }
            // this may or may not log traffic based on server properties (by default, not)
            trafficLogger.log(context);
        }
    }

    private AssertionStatus doDeferredAssertions(PolicyEnforcementContext context)
      throws PolicyAssertionException, IOException
    {
        AssertionStatus status = AssertionStatus.NONE;
        for (ServerAssertion serverAssertion : context.getDeferredAssertions()) {
            status = serverAssertion.checkRequest(context);
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

    private Level getHighestAssertionStatusLevel(Set<AssertionStatus> assertionStatusSet) {
        Level level = Level.ALL;
        for (AssertionStatus assertionStatus : assertionStatusSet) {
            if (assertionStatus.getLevel().intValue() > level.intValue()) {
                level = assertionStatus.getLevel();
            }
        }
        return level;
    }

    private boolean isAuditHintingEnabled() {
        String propHintingStr = serverConfig.getPropertyCached(ServerConfig.PARAM_AUDIT_HINTING_ENABLED);
        boolean hintingEnabled = false;
        if(propHintingStr!=null) {
            hintingEnabled = Boolean.valueOf(propHintingStr.trim());
        }
        return hintingEnabled;
    }

    private boolean isAuditAssertionStatusEnabled() {
        String propStatusStr = serverConfig.getPropertyCached(ServerConfig.PARAM_AUDIT_ASSERTION_STATUS_ENABLED);
        boolean statusEnabled = false;
        if(propStatusStr!=null) {
            statusEnabled = Boolean.valueOf(propStatusStr.trim());
        }
        return statusEnabled;
    }
}
/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.MimeBody;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.gateway.common.audit.MessageProcessingMessages;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceStatistics;
import com.l7tech.message.*;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.PolicyType;
import com.l7tech.security.cert.KeyUsageException;
import com.l7tech.security.xml.SecurityActor;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.UnexpectedKeyInfoException;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.WssDecorator;
import com.l7tech.security.xml.processor.BadSecurityContextException;
import com.l7tech.security.xml.processor.ProcessorException;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.security.xml.processor.WssProcessorImpl;
import com.l7tech.security.xml.processor.ProcessorValidationException;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.event.MessageProcessed;
import com.l7tech.server.log.TrafficLogger;
import com.l7tech.server.message.HttpSessionPolicyContextCache;
import com.l7tech.server.message.PolicyContextCache;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.PolicyCache;
import com.l7tech.server.policy.PolicyMetadata;
import com.l7tech.server.policy.PolicyVersionException;
import com.l7tech.server.policy.ServerPolicyHandle;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.secureconversation.SecureConversationContextManager;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.server.service.ServiceMetricsServices;
import com.l7tech.server.service.resolution.ServiceResolutionException;
import com.l7tech.server.util.EventChannel;
import com.l7tech.server.util.SoapFaultManager;
import com.l7tech.server.util.WSSecurityProcessorUtils;
import com.l7tech.util.Background;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.SoapConstants;
import com.l7tech.util.ResourceUtils;
import com.l7tech.xml.InvalidDocumentSignatureException;
import com.l7tech.xml.MessageNotSoapException;
import com.l7tech.xml.SoapFaultLevel;
import com.l7tech.xml.soap.SoapFaultUtils;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.soap.SoapVersion;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.context.ApplicationEventPublisher;
import org.xml.sax.SAXException;

import javax.wsdl.Operation;
import javax.wsdl.WSDLException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The server side component processing messages from any transport layer.
 *
 * @author alex
 */
@SuppressWarnings({ "ThrowableResultOfMethodCallIgnored" })
public class MessageProcessor extends ApplicationObjectSupport implements InitializingBean {
    private static final int SETTINGS_RECHECK_MILLIS = 7937;
    private final ServiceCache serviceCache;
    private final PolicyCache policyCache;
    private final WssDecorator wssDecorator;
    private final SecurityTokenResolver securityTokenResolver;
    private final LicenseManager licenseManager;
    private final ServiceMetricsServices serviceMetricsServices;
    private final AuditContext auditContext;
    private final ServerConfig serverConfig;
    private final TrafficLogger trafficLogger;
    private SoapFaultManager soapFaultManager;
    private final ArrayList<TrafficMonitor> trafficMonitors = new ArrayList<TrafficMonitor>();
    private final AtomicReference<WssSettings> wssSettingsReference = new AtomicReference<WssSettings>();
    private final ApplicationEventPublisher messageProcessingEventChannel;

    /**
     * Create the new <code>MessageProcessor</code> instance with the service
     * manager, Wss Decorator instance and the server private key.
     * All arguments are required
     *
     * @param sc             the service cache
     * @param wssd           the Wss Decorator
     * @param licenseManager the SSG's Licence Manager
     * @param metricsServices the SSG's ServiceMetricsManager
     * @throws IllegalArgumentException if any of the arguments is null
     */
    public MessageProcessor(ServiceCache sc,
                            PolicyCache pc,
                            WssDecorator wssd,
                            SecurityTokenResolver securityTokenResolver,
                            LicenseManager licenseManager,
                            ServiceMetricsServices metricsServices,
                            AuditContext auditContext,
                            ServerConfig serverConfig,
                            TrafficLogger trafficLogger,
                            SoapFaultManager soapFaultManager,
                            ApplicationEventPublisher messageProcessingEventChannel)
      throws IllegalArgumentException {
        if (sc == null) throw new IllegalArgumentException("Service Cache is required");
        if (pc == null) throw new IllegalArgumentException("Policy Cache is required");
        if (wssd == null) throw new IllegalArgumentException("Wss Decorator is required");
        if (licenseManager == null) throw new IllegalArgumentException("License Manager is required");
        if (metricsServices == null) throw new IllegalArgumentException("Service Metrics Manager is required");
        if (auditContext == null) throw new IllegalArgumentException("Audit Context is required");
        if (serverConfig == null) throw new IllegalArgumentException("Server Config is required");
        if (trafficLogger == null) throw new IllegalArgumentException("Traffic Logger is required");
        if (soapFaultManager == null) throw new IllegalArgumentException("SoapFaultManager is required");
        if (messageProcessingEventChannel == null) messageProcessingEventChannel = new EventChannel();
        this.serviceCache = sc;
        this.policyCache = pc;
        this.wssDecorator = wssd;
        this.securityTokenResolver = securityTokenResolver;
        this.licenseManager = licenseManager;
        this.serviceMetricsServices = metricsServices;
        this.auditContext = auditContext;
        this.serverConfig = serverConfig;
        this.trafficLogger = trafficLogger;
        this.soapFaultManager = soapFaultManager;
        this.messageProcessingEventChannel = messageProcessingEventChannel;
        initSettings(SETTINGS_RECHECK_MILLIS);
    }

    private void initSettings(final int period) {
        updateSettings(period);
        Background.scheduleRepeated(new TimerTask() {
            @Override
            public void run() {
                updateSettings(period);
            }
        }, period, period);
    }

    private void updateSettings(int period) {
        long maxBytes = serverConfig.getLongPropertyCached(ServerConfig.PARAM_IO_XML_PART_MAX_BYTES, 0, period - 1);
        MimeBody.setFirstPartXmlMaxBytes(maxBytes);

        wssSettingsReference.set( new WssSettings(
            serverConfig.getLongPropertyCached(ServerConfig.PARAM_SIGNED_PART_MAX_BYTES, 0, period - 1),
            serverConfig.getBooleanProperty(ServerConfig.PARAM_SOAP_REJECT_MUST_UNDERSTAND, true),
            serverConfig.getBooleanProperty(ServerConfig.PARAM_WSS_ALLOW_MULTIPLE_TIMESTAMP_SIGNATURES, false),
            serverConfig.getBooleanProperty(ServerConfig.PARAM_WSS_ALLOW_UNKNOWN_BINARY_SECURITY_TOKENS, false),
            serverConfig.getBooleanProperty(ServerConfig.PARAM_WSS_PROCESSOR_STRICT_SIG_CONFIRMATION, true)
        ) );
    }

    public AssertionStatus processMessage(PolicyEnforcementContext context)
        throws IOException, PolicyAssertionException, PolicyVersionException, LicenseException, MethodNotAllowedException, MessageProcessingSuspendedException {
        return reallyProcessMessage(context);
    }

    private String getIncomingURL(PolicyEnforcementContext context) {
        HttpRequestKnob hrk = context.getRequest().getKnob(HttpRequestKnob.class);
        if (hrk == null) {
            logger.warning("cannot generate incoming URL");
            return "";
        }
        return hrk.getQueryString() == null ? hrk.getRequestUrl() : hrk.getRequestUrl() + "?" + hrk.getQueryString();
    }

    private AssertionStatus reallyProcessMessage(final PolicyEnforcementContext context)
        throws IOException, PolicyAssertionException, PolicyVersionException, LicenseException, MethodNotAllowedException, MessageProcessingSuspendedException {
        context.setAuditLevel(DEFAULT_MESSAGE_AUDIT_LEVEL);
        // License check hook
        licenseManager.requireFeature(GatewayFeatureSets.SERVICE_MESSAGEPROCESSOR);

        // no-processing mode check
        if (SuspendStatus.INSTANCE.isSuspended()) {
            throw new MessageProcessingSuspendedException(SuspendStatus.INSTANCE.getReason());
        }

        final Message request = context.getRequest();
        final Message response = context.getResponse();
        final ProcessorResult[] wssOutput = { null };
        AssertionStatus status = AssertionStatus.UNDEFINED;
        long serviceOid = -1;
        ServiceStatistics stats = null;
        boolean attemptedRequest = false;

        // WSS-Processing Step
        ServerPolicyHandle serverPolicy = null;
        try {
            final AssertionStatus[] securityProcessingAssertionStatus = { null };
            final IOException[] securityProcessingIOException = { null };
            final boolean[] securityProcessingEvaluatedFlag = { false };
            final SecurityProcessingResolutionListener securityProcessingResolutionListener =
                    new SecurityProcessingResolutionListener(policyCache, context, wssOutput, securityProcessingAssertionStatus, securityProcessingIOException, securityProcessingEvaluatedFlag);

            // Policy Verification Step
            PublishedService service = serviceCache.resolve(context.getRequest(), securityProcessingResolutionListener);
            if (service == null) {
                if (securityProcessingIOException[0] != null) {
                    throw securityProcessingIOException[0]; 
                } else if (securityProcessingAssertionStatus[0] != null) {
                    status = securityProcessingAssertionStatus[0];
                    return securityProcessingAssertionStatus[0];
                } else {
                    auditor.logAndAudit(MessageProcessingMessages.SERVICE_NOT_FOUND);
                    status = AssertionStatus.SERVICE_NOT_FOUND;
                    return AssertionStatus.SERVICE_NOT_FOUND;
                }
            }

            if (service.isDisabled()) {
                auditor.logAndAudit(MessageProcessingMessages.SERVICE_DISABLED);
                status = AssertionStatus.SERVICE_NOT_FOUND;
                return AssertionStatus.SERVICE_NOT_FOUND;
            }

            auditor.logAndAudit(MessageProcessingMessages.RESOLVED_SERVICE, service.getName(), String.valueOf(service.getOid()));
            context.setService(service);

            // skip the http request header version checking if it is not a Http request
            if (context.getRequest().isHttpRequest()) {
                HttpRequestKnob httpRequestKnob = context.getRequest().getHttpRequestKnob();

                // Check the request method
                final HttpMethod requestMethod = httpRequestKnob.getMethod();
                if (requestMethod != null && !service.isMethodAllowed(requestMethod)) {
                    String[] auditArgs = new String[] { requestMethod.name(), service.getName() };
                    Object[] faultArgs = new Object[] { requestMethod.name() };
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
                String requestorVersion = httpRequestKnob.getHeaderFirstValue(SecureSpanConstants.HttpHeaders.POLICY_VERSION);
                if (requestorVersion != null && requestorVersion.length() > 0) {
                    // format is policyId|policyVersion (seperated with char '|')
                    boolean wrongPolicyVersion = false;
                    int indexofbar = requestorVersion.indexOf('|');
                    if (indexofbar < 0) {
                        auditor.logAndAudit(MessageProcessingMessages.POLICY_VERSION_WRONG_FORMAT);
                        wrongPolicyVersion = true;
                    } else {
                        try {
                            String expectedPolicyVersion = policyCache.getUniquePolicyVersionIdentifer( service.getPolicy().getOid() );
                            long reqPolicyId = Long.parseLong(requestorVersion.substring(0, indexofbar));
                            String reqPolicyVersion = requestorVersion.substring(indexofbar + 1);
                            if ( reqPolicyId != service.getOid() ||
                                 !reqPolicyVersion.equals( expectedPolicyVersion )) {
                                auditor.logAndAudit(MessageProcessingMessages.POLICY_VERSION_INVALID, requestorVersion, String.valueOf(service.getOid()), expectedPolicyVersion);
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

            // Get the server policy
            serverPolicy = serviceCache.getServerPolicy(service.getOid());
            if (serverPolicy == null) {
                throw new ServiceResolutionException("service is resolved but the corresponding policy is invalid");
            }

            // Check request data
            if (context.getRequest().getMimeKnob().isMultipart() &&
                !(service.isMultipart() || serverPolicy.getPolicyMetadata().isMultipart())) {
                auditor.logAndAudit(MessageProcessingMessages.MULTIPART_NOT_ALLOWED);
                status = AssertionStatus.BAD_REQUEST;
                return AssertionStatus.BAD_REQUEST;
            }

            // Ensure security processing is run if enabled
            if ( !securityProcessingEvaluatedFlag[0] &&
                 (service.isWssProcessingEnabled())) {
                PolicyMetadata policyMetadata = serverPolicy.getPolicyMetadata();
                Set<ServiceCache.ServiceMetadata> metadatas = Collections.singleton( new ServiceCache.ServiceMetadata(policyMetadata, true) );
                if (!securityProcessingResolutionListener.notifyPreParseServices( request, metadatas ) ) {
                    if ( securityProcessingIOException[0] != null ) {
                        throw securityProcessingIOException[0];
                    } else {
                        status = securityProcessingAssertionStatus[0];
                        return securityProcessingAssertionStatus[0];
                    }
                }

                // avoid re-Tarari-ing request that's already DOM parsed unless some assertions need it bad
                XmlKnob xk = request.getKnob(XmlKnob.class);
                if (xk != null) xk.setTarariWanted(metadatas.iterator().next().isTarariWanted());
            }

            // Ensure pre-service policies are run
            if ( !securityProcessingEvaluatedFlag[0] ) {
                boolean success = securityProcessingResolutionListener.processPreServicePolicies(
                    PolicyType.PRE_SECURITY_FRAGMENT,
                    MessageProcessingMessages.RUNNING_PRE_SECURITY_POLICY,
                    MessageProcessingMessages.ERROR_PRE_SECURITY );

                if ( success ) {
                    success = securityProcessingResolutionListener.processPreServicePolicies(
                        PolicyType.PRE_SERVICE_FRAGMENT,
                        MessageProcessingMessages.RUNNING_PRE_SERVICE_POLICY,
                        MessageProcessingMessages.ERROR_PRE_SERVICE );
                }

                if (!success) {
                    if ( securityProcessingIOException[0] != null ) {
                        throw securityProcessingIOException[0];
                    } else {
                        status = securityProcessingAssertionStatus[0];
                        return securityProcessingAssertionStatus[0];
                    }
                }
            }

            // Run the policy
            auditor.logAndAudit(MessageProcessingMessages.RUNNING_POLICY);
            stats = serviceCache.getServiceStatistics(service.getOid());
            attemptedRequest = true;
            serviceOid = service.getOid();

            context.setPolicyExecutionAttempted(true);
            status = serverPolicy.checkRequest(context);
            
            // fail early if there are any (I/O) errors reading the response
            MimeKnob mk;
            try {
                // attempts to read and stash the whole response
                mk = response.getKnob(MimeKnob.class);
                if (mk != null) mk.getFirstPart().getActualContentLength();
            } catch (IOException e) {
                // create fault to be sent
                String fault = soapFaultManager.constructExceptionFault(e, context);

                // there's no response message; substitute with the fault, so that the actual response that will be sent is audited
                response.initialize(
                    (context.getService() != null && context.getService().getSoapVersion() == SoapVersion.SOAP_1_2) ?
                    ContentTypeHeader.SOAP_1_2_DEFAULT : ContentTypeHeader.XML_DEFAULT, 
                    fault.getBytes());

                auditor.logAndAudit(MessageProcessingMessages.RESPONSE_IO_ERROR, e.getMessage());

                // pass the exception up to the SOAP servlet
                throw new MessageResponseIOException(fault, e);
            } catch (NoSuchPartException e) {
                throw new RuntimeException(e); // The first part should always be available
            }

            // Execute deferred actions for request, then response
            if (status == AssertionStatus.NONE) {
                status = AssertionStatus.UNDEFINED;
                status = doDeferredAssertions(context);
            }

            context.setPolicyResult(status);

            // Run post service global policies
            processPostServicePolicies( context,
                    PolicyType.POST_SERVICE_FRAGMENT,
                    MessageProcessingMessages.RUNNING_POST_SERVICE_POLICY,
                    MessageProcessingMessages.ERROR_POST_SERVICE );

            // add signature confirmations
            WSSecurityProcessorUtils.addSignatureConfirmations(response, auditor);

            // Run response through WssDecorator if indicated
            if (status == AssertionStatus.NONE &&
                  response.isXml() &&
                  response.getSecurityKnob().getDecorationRequirements().length > 0 &&
                  response.isSoap())
            {
                try {
                    DecorationRequirements[] allrequirements = response.getSecurityKnob().getDecorationRequirements();
                    SecurityKnob reqSec = request.getSecurityKnob();

                    try {
                        if (request.isSoap()) {
                            final String messageId = SoapUtil.getL7aMessageId(request.getXmlKnob().getDocumentReadOnly());
                            if (messageId != null) {
                                SoapUtil.setL7aRelatesTo(response.getXmlKnob().getDocumentWritable(), messageId);
                            }
                        }
                    } catch(IOException e) {
                        logger.log(Level.WARNING, "Unable to extract message ID from request: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                    } catch(SAXException e) {
                        logger.log(Level.WARNING, "Unable to extract message ID from request: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                    }

                    // if the request was processed on the noactor sec header instead of the l7 sec actor, then
                    // the response's decoration requirements should map this (if applicable)
                    if (reqSec.getProcessorResult() != null &&
                        reqSec.getProcessorResult().getProcessedActor() != null ) {
                        String actorUri = reqSec.getProcessorResult().getProcessedActor()==SecurityActor.NOACTOR ?
                                null :
                                reqSec.getProcessorResult().getProcessedActorUri();

                        // go find the l7 decoreq and adjust the actor
                        for (DecorationRequirements requirement : allrequirements) {
                            if (SoapConstants.L7_SOAP_ACTOR.equals(requirement.getSecurityHeaderActor())) {
                                requirement.setSecurityHeaderActor( actorUri );
                            }
                        }
                    }

                    // do the actual decoration
                    for (final DecorationRequirements responseDecoReq : allrequirements) {
                        if (responseDecoReq != null && wssOutput[0] != null) {
                            if (wssOutput[0].getSecurityNS() != null)
                                responseDecoReq.getNamespaceFactory().setWsseNs(wssOutput[0].getSecurityNS());
                            if (wssOutput[0].getWSUNS() != null)
                                responseDecoReq.getNamespaceFactory().setWsuNs(wssOutput[0].getWSUNS());
                            if (wssOutput[0].isDerivedKeySeen())
                                responseDecoReq.setUseDerivedKeys(true);
                        }
                        wssDecorator.decorateMessage(response, responseDecoReq);
                    }
                } catch (Exception e) {
                    auditor.logAndAudit(MessageProcessingMessages.ERROR_WSS_RESPONSE, ExceptionUtils.getMessage(e));                    
                    throw new PolicyAssertionException(null, "Failed to apply WSS decoration to response", e);
                }
            }

            // Run post WS-Security decoration global service policies
            processPostServicePolicies( context,
                    PolicyType.POST_SECURITY_FRAGMENT,
                    MessageProcessingMessages.RUNNING_POST_SECURITY_POLICY,
                    MessageProcessingMessages.ERROR_POST_SECURITY );

            return status;

        } catch (PublishedService.ServiceException se) {
            auditor.logAndAudit(MessageProcessingMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[]{ExceptionUtils.getMessage(se)}, se);
            context.setPolicyResult(AssertionStatus.SERVER_ERROR);
            status = AssertionStatus.SERVER_ERROR;
            return AssertionStatus.SERVER_ERROR;
        } catch (ServiceResolutionException sre) {
            auditor.logAndAudit(MessageProcessingMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[]{ExceptionUtils.getMessage(sre)}, ExceptionUtils.getDebugException(sre));
            context.setPolicyResult(AssertionStatus.SERVER_ERROR);
            status = AssertionStatus.SERVER_ERROR;
            return AssertionStatus.SERVER_ERROR;
        } catch (SAXException e) {
            auditor.logAndAudit(MessageProcessingMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[]{ExceptionUtils.getMessage(e)}, e);
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
                    auditor.logAndAudit(MessageProcessingMessages.COMPLETION_STATUS, String.valueOf(status.getNumeric()), status.getMessage());
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
                    auditor.logAndAudit(MessageProcessingMessages.ROUTING_FAILED, String.valueOf(status.getNumeric()), status.getMessage());
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
                    auditor.logAndAudit(MessageProcessingMessages.POLICY_EVALUATION_RESULT, serviceName, String.valueOf(status.getNumeric()), status.getMessage());
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

            if (attemptedRequest && serviceMetricsServices.isEnabled()) {
                final int frontTime = (int)(context.getEndTime() - context.getStartTime());
                final int backTime = (int)(context.getRoutingTotalTime());

                serviceMetricsServices.addRequest(
                        serviceOid, 
                        getOperationName(context),
                        context.getDefaultAuthenticationContext().getLastAuthenticatedUser(),
                        context.getMappings(),
                        authorizedRequest, completedRequest, frontTime, backTime);
            }

            for (TrafficMonitor tm : trafficMonitors) {
                tm.recordTransactionStatus(context, status, context.getEndTime() - context.getStartTime());
            }

            try {
                messageProcessingEventChannel.publishEvent(new MessageProcessed(context, status, this));
            } catch (Throwable t) {
                auditor.logAndAudit(MessageProcessingMessages.EVENT_MANAGER_EXCEPTION, null, t);
            }
            // this may or may not log traffic based on server properties (by default, not)
            trafficLogger.log(context);
        }
    }

    /**
     * Process post service policies and audit errors.
     *
     * <p>Post service policy execution does not affect the overall policy processing
     * status.</p>
     */
    private void processPostServicePolicies( final PolicyEnforcementContext context,
                                             final PolicyType policyType,
                                             final AuditDetailMessage notificationMessage,
                                             final AuditDetailMessage errorMessage ) {
        final Set<String> guids = policyCache.getGlobalPoliciesByType( policyType );        
        if ( !guids.isEmpty() ) {
            auditor.logAndAudit( notificationMessage );

            for ( String guid : guids ) {
                try {
                    processServicePolicy( policyCache, context, guid );
                } catch (IOException e) {
                    auditor.logAndAudit(errorMessage, new String[]{ExceptionUtils.getMessage( e )}, ExceptionUtils.getDebugException(e));
                } catch (PolicyAssertionException e) {
                    auditor.logAndAudit(errorMessage, new String[]{ExceptionUtils.getMessage( e )}, e);
                }
            }
        }
    }

    private AssertionStatus processServicePolicy( final PolicyCache policyCache,
                                                  final PolicyEnforcementContext context,
                                                  final String guid ) throws PolicyAssertionException, IOException {
        AssertionStatus result;

        ServerPolicyHandle handle = null;
        PolicyEnforcementContext pec = null;
        try {
            handle = policyCache.getServerPolicy( guid );
            if ( handle != null ) {
                pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext( context );
                result = handle.checkRequest( pec );
            } else {
                result = AssertionStatus.NONE;
            }
        } finally {
            ResourceUtils.closeQuietly( pec );
            ResourceUtils.closeQuietly( handle );
        }

        return result;
    }

    public void registerTrafficMonitorCallback(TrafficMonitor tm) {
        if (!trafficMonitors.contains(tm)) {
            trafficMonitors.add(tm);
        }
    }

    @SuppressWarnings({ "deprecation" })
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

    private String getOperationName( PolicyEnforcementContext context ) {
        String name = null;
        try {
            Operation operation = context.getOperation();
            name = operation==null ? null : operation.getName();
        } catch ( IOException ioe ) {
            logger.log(Level.INFO, "Could not determine soap operation '"+ExceptionUtils.getMessage(ioe)+"'.", ExceptionUtils.getDebugException(ioe));
        } catch ( SAXException saxe ) {
            logger.log(Level.INFO, "Could not determine soap operation '"+ExceptionUtils.getMessage(saxe)+"'.", ExceptionUtils.getDebugException(saxe));
        } catch ( WSDLException wsdle ) {
            logger.log(Level.INFO, "Could not determine soap operation '"+ExceptionUtils.getMessage(wsdle)+"'.", ExceptionUtils.getDebugException(wsdle));
        } catch ( InvalidDocumentFormatException idfe ) {
            logger.log(Level.INFO, "Could not determine soap operation '"+ExceptionUtils.getMessage(idfe)+"'.", ExceptionUtils.getDebugException(idfe));
        }

        return name;
    }

    private static final Level DEFAULT_MESSAGE_AUDIT_LEVEL = Level.INFO;
    private static final AuditDetailMessage.Hint HINT_SAVE_REQUEST = AuditDetailMessage.Hint.getHint("MessageProcessor.saveRequest");
    private static final AuditDetailMessage.Hint HINT_SAVE_RESPONSE = AuditDetailMessage.Hint.getHint("MessageProcessor.saveResponse");

    private Auditor auditor;
    final Logger logger = Logger.getLogger(getClass().getName());

    @Override
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

    private final class SecurityProcessingResolutionListener implements ServiceCache.ResolutionListener {
        private final PolicyCache policyCache;
        private final PolicyEnforcementContext context;
        private final ProcessorResult[] wssOutputHolder;
        private final AssertionStatus[] assertionStatusHolder;
        private final IOException[] ioExceptionHolder;
        private final boolean[] securityProcessingEvaluatedFlag;

        private SecurityProcessingResolutionListener(final PolicyCache policyCache,
                                                     final PolicyEnforcementContext context,
                                                     final ProcessorResult[] wssOutput,
                                                     final AssertionStatus[] assertionStatus,
                                                     final IOException[] ioException,
                                                     final boolean[] securityProcessingEvaluatedFlag ) {
            this.policyCache = policyCache;
            this.context = context;
            this.wssOutputHolder = wssOutput;
            this.assertionStatusHolder = assertionStatus;
            this.ioExceptionHolder = ioException;
            this.securityProcessingEvaluatedFlag = securityProcessingEvaluatedFlag;
        }

        @Override
        public boolean notifyPreParseServices( final Message message,
                                               final Set<ServiceCache.ServiceMetadata> serviceMetadataSet ) {
            securityProcessingEvaluatedFlag[0] = true;

            boolean isSoap = false;
            boolean hasSecurity = false;
            boolean preferDom = true;
            boolean performSecurityProcessing = false;

            // Run pre WS-Security undecoration global service policies
            if ( !processPreServicePolicies(
                    PolicyType.PRE_SECURITY_FRAGMENT,
                    MessageProcessingMessages.RUNNING_PRE_SECURITY_POLICY,
                    MessageProcessingMessages.ERROR_PRE_SECURITY ) ) {
                return false;
            }

            // If any service does not use WSS then don't prefer DOM
            for (ServiceCache.ServiceMetadata serviceMetadata : serviceMetadataSet) {
                if (!serviceMetadata.isWssInPolicy()) {
                    preferDom = false;
                }

                if (serviceMetadata.isWssProcessingRequired()) {
                    performSecurityProcessing = true;
                }
            }

            try {
                isSoap = context.getRequest().isSoap(preferDom);
                hasSecurity = isSoap && context.getRequest().getSoapKnob().isSecurityHeaderPresent();
            } catch (SAXException e) {
                auditor.logAndAudit(MessageProcessingMessages.REQUEST_INVALID_XML_FORMAT_WITH_DETAIL, new String[]{e.getMessage()}, e);
                context.setMalformedRequest();
                assertionStatusHolder[0] = AssertionStatus.BAD_REQUEST;
                return false;
            } catch (MessageNotSoapException e) {
                auditor.logAndAudit(MessageProcessingMessages.MESSAGE_NOT_SOAP, null, e);
            } catch (NoSuchPartException e) {
                auditor.logAndAudit(MessageProcessingMessages.REQUEST_INVALID_XML_FORMAT_WITH_DETAIL, new String[]{e.getMessage()}, e);
                assertionStatusHolder[0] = AssertionStatus.BAD_REQUEST;
                return false;
            } catch (IOException ioe) {
                ioExceptionHolder[0] = ioe;
                return false;
            }

            if (performSecurityProcessing && isSoap && hasSecurity) {
                WssProcessorImpl trogdor = new WssProcessorImpl(); // no need for locator
                WssSettings settings = wssSettingsReference.get();
                trogdor.setSignedAttachmentSizeLimit(settings.signedAttachmentMaxSize);
                trogdor.setRejectOnMustUnderstand(settings.rejectOnMustUnderstand);
                trogdor.setPermitMultipleTimestampSignatures(settings.permitMultipleTimestampSignatures);
                trogdor.setPermitUnknownBinarySecurityTokens(settings.permitUnknownBinarySecurityTokens);
                trogdor.setStrictSignatureConfirmationValidation(settings.strictSignatureConfirmationValidation);
                try {
                    final Message request = context.getRequest();
                    final SecurityKnob reqSec = request.getSecurityKnob();
                    wssOutputHolder[0] = trogdor.undecorateMessage(request,
                                                          null,
                                                          SecureConversationContextManager.getInstance(),
                                                          securityTokenResolver);
                    reqSec.setProcessorResult(wssOutputHolder[0]);
                } catch (MessageNotSoapException e) {
                    auditor.logAndAudit(MessageProcessingMessages.MESSAGE_NOT_SOAP_NO_WSS, null, e);
                    // this shouldn't be possible now
                    // pass through, leaving wssOutput as null
                } catch (UnexpectedKeyInfoException e) {
                    // Must catch before ProcessorException
                    // Use appropriate fault to warn client about unresolvable KeyInfo
                    auditor.logAndAudit(MessageProcessingMessages.ERROR_WSS_PROCESSING, null, e);
                    context.setAuditLevel(Level.WARNING);
                    SoapFaultLevel cfault = new SoapFaultLevel();
                    cfault.setLevel(SoapFaultLevel.TEMPLATE_FAULT);
                    cfault.setFaultTemplate(SoapFaultUtils.badKeyInfoFault(context.getService() != null ? context.getService().getSoapVersion() : SoapVersion.UNKNOWN, getIncomingURL(context)));
                    context.setFaultlevel(cfault);
                    assertionStatusHolder[0] = AssertionStatus.FAILED;
                    return false;
                } catch (ProcessorValidationException pve){
                    auditor.logAndAudit(MessageProcessingMessages.ERROR_WSS_PROCESSING_INFO,
                            new String[]{ExceptionUtils.getMessage( pve )},
                            ExceptionUtils.getDebugException( pve ));
                    assertionStatusHolder[0] = AssertionStatus.BAD_REQUEST;
                    return false;
                } catch (ProcessorException e) {
                    auditor.logAndAudit(MessageProcessingMessages.ERROR_WSS_PROCESSING, null, e);
                    assertionStatusHolder[0] = AssertionStatus.SERVER_ERROR;
                    return false;
                } catch (InvalidDocumentSignatureException e) {
                    auditor.logAndAudit(MessageProcessingMessages.ERROR_WSS_SIGNATURE,
                            new String[]{ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
                    context.setAuditLevel(Level.WARNING);
                    assertionStatusHolder[0] = AssertionStatus.BAD_REQUEST;
                    return false;
                } catch (InvalidDocumentFormatException e) {
                    auditor.logAndAudit(MessageProcessingMessages.ERROR_WSS_PROCESSING, null, e);
                    context.setAuditLevel(Level.WARNING);
                    assertionStatusHolder[0] = AssertionStatus.BAD_REQUEST;
                    return false;
                } catch (KeyUsageException e) {
                    auditor.logAndAudit(MessageProcessingMessages.CERT_KEY_USAGE, e.getActivityName());
                    context.setAuditLevel(Level.WARNING);
                    assertionStatusHolder[0] = AssertionStatus.BAD_REQUEST;
                    return false;
                } catch (GeneralSecurityException e) {
                    auditor.logAndAudit(MessageProcessingMessages.ERROR_WSS_PROCESSING, null, e);
                    context.setAuditLevel(Level.WARNING);
                    assertionStatusHolder[0] = AssertionStatus.SERVER_ERROR;
                    return false;
                } catch (SAXException e) {
                    auditor.logAndAudit(MessageProcessingMessages.ERROR_RETRIEVE_XML, null, e);
                    context.setAuditLevel(Level.WARNING);
                    assertionStatusHolder[0] = AssertionStatus.SERVER_ERROR;
                    return false;
                } catch (BadSecurityContextException e) {
                    auditor.logAndAudit(MessageProcessingMessages.ERROR_WSS_PROCESSING_INFO, ExceptionUtils.getMessage(e));
                    context.setAuditLevel(Level.WARNING);
                    SoapFaultLevel cfault = new SoapFaultLevel();
                    cfault.setLevel(SoapFaultLevel.TEMPLATE_FAULT);
                    SoapVersion version = context.getService() != null ? context.getService().getSoapVersion() : SoapVersion.UNKNOWN;
                    cfault.setFaultTemplate( SoapFaultUtils.badContextTokenFault(version, getIncomingURL(context)) );
                    context.setFaultlevel(cfault);
                    assertionStatusHolder[0] = AssertionStatus.FAILED;
                    return false;
                } catch (IOException ioe) {
                    ioExceptionHolder[0] = ioe;
                    return false;
                }
                auditor.logAndAudit(MessageProcessingMessages.WSS_PROCESSING_COMPLETE);
            }

            // Run pre service global policies
            return processPreServicePolicies(
                    PolicyType.PRE_SERVICE_FRAGMENT,
                    MessageProcessingMessages.RUNNING_PRE_SERVICE_POLICY,
                    MessageProcessingMessages.ERROR_PRE_SERVICE );
        }

        /**
         * Process pre service policies and audit errors.
         *
         * <p>Pre service policy execution affects the overall policy processing
         * status.</p>
         */
        public boolean processPreServicePolicies( final PolicyType policyType,
                                                  final AuditDetailMessage notifiy,
                                                  final AuditDetailMessage error ) {
            boolean success = true;

            final Set<String> guids = policyCache.getGlobalPoliciesByType( policyType );
            if ( !guids.isEmpty() ) {
                auditor.logAndAudit( notifiy );

                // If the service is not resolved then it should be null from the
                // PEC, it is not desirable to set a fake published service. If
                // any assertions rely on a service being set they should be fixed.

                for ( String guid : guids ) {
                    try {
                        AssertionStatus status = processServicePolicy( policyCache, context, guid );
                        if ( status != AssertionStatus.NONE ) {
                            assertionStatusHolder[0] = status;
                            success = false;
                            break;
                        }
                    } catch (IOException ioe) {
                        ioExceptionHolder[0] = ioe;
                        success = false;
                    } catch (PolicyAssertionException e) {
                        auditor.logAndAudit(error, new String[]{ExceptionUtils.getMessage( e )}, e);
                        context.setAuditLevel( Level.WARNING);
                        assertionStatusHolder[0] = AssertionStatus.SERVER_ERROR;
                        success = false;
                    }
                }
            }

            return success;
        }
    }

    /**
     * Global flag used to put the SSG in a "no-processing" mode.
     */
    public static enum SuspendStatus {
        INSTANCE;

        private boolean suspended = false;
        private String reason = "";

        private final ReadWriteLock mutex = new ReentrantReadWriteLock(false);

        public boolean isSuspended() {
            try {
                mutex.readLock().lock();
                return suspended;
            } finally {
                mutex.readLock().unlock();
            }
        }

        public void suspend() {
            try {
                mutex.writeLock().lock();
                suspended = true;
                reason = "";
            } finally {
                mutex.writeLock().unlock();
            }
        }

        public void suspend(String reason) {
            try {
                mutex.writeLock().lock();
                suspended = true;
                this.reason = reason;
            } finally {
                mutex.writeLock().unlock();
            }
        }

        public void resume() {
            try {
                mutex.writeLock().lock();
                suspended = false;
            } finally {
                mutex.writeLock().unlock();
            }
        }

        public String getReason() {
            try {
                mutex.readLock().lock();
                return reason;
            } finally {
                mutex.readLock().unlock();
            }
        }
    }

    private static final class WssSettings {
        private final long signedAttachmentMaxSize;
        private final boolean rejectOnMustUnderstand;
        private final boolean permitMultipleTimestampSignatures;
        private final boolean permitUnknownBinarySecurityTokens;
        private final boolean strictSignatureConfirmationValidation;

        private WssSettings( final long signedAttachmentMaxSize,
                             final boolean rejectOnMustUnderstand,
                             final boolean permitMultipleTimestampSignatures,
                             final boolean permitUnknownBinarySecurityTokens,
                             final boolean strictSignatureConfirmationValidation) {
            this.signedAttachmentMaxSize = signedAttachmentMaxSize;
            this.rejectOnMustUnderstand = rejectOnMustUnderstand;
            this.permitMultipleTimestampSignatures = permitMultipleTimestampSignatures;
            this.permitUnknownBinarySecurityTokens = permitUnknownBinarySecurityTokens;
            this.strictSignatureConfirmationValidation = strictSignatureConfirmationValidation;
        }
    }
}

package com.l7tech.server;

import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.log.HybridDiagnosticContext;
import com.l7tech.common.mime.MimeBody;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.gateway.common.audit.MessageProcessingMessages;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceStatistics;
import com.l7tech.message.*;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.variable.VariableNameSyntaxException;
import com.l7tech.security.cert.KeyUsageException;
import com.l7tech.security.xml.SecurityActor;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.UnexpectedKeyInfoException;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.WssDecorator;
import com.l7tech.security.xml.processor.*;
import com.l7tech.server.audit.*;
import com.l7tech.server.event.MessageProcessed;
import com.l7tech.server.event.MessageReceived;
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
import com.l7tech.server.service.ServiceCache;
import com.l7tech.server.service.ServiceMetricsServices;
import com.l7tech.server.service.resolution.ServiceResolutionException;
import com.l7tech.server.stepdebug.DebugManager;
import com.l7tech.server.trace.TracePolicyEvaluator;
import com.l7tech.server.util.EventChannel;
import com.l7tech.server.util.WSSecurityProcessorUtils;
import com.l7tech.util.*;
import com.l7tech.xml.InvalidDocumentSignatureException;
import com.l7tech.xml.MessageNotSoapException;
import com.l7tech.xml.SoapFaultLevel;
import com.l7tech.xml.soap.SoapFaultUtils;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.soap.SoapVersion;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.support.ApplicationObjectSupport;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.wsdl.Binding;
import javax.wsdl.Operation;
import javax.wsdl.WSDLException;
import java.io.Closeable;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gateway.common.log.GatewayDiagnosticContextKeys.FOLDER_ID;
import static com.l7tech.gateway.common.log.GatewayDiagnosticContextKeys.SERVICE_ID;
import static com.l7tech.objectmodel.EntityUtil.id;
import static com.l7tech.util.Functions.map;

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
    private final SecurityContextFinder securityContextFinder;
    private final LicenseManager licenseManager;
    private final ServiceMetricsServices serviceMetricsServices;
    private final AuditContextFactory auditContextFactory;
    private final MessageSummaryAuditFactory messageSummaryAuditFactory;
    private final Config config;
    private final TrafficLogger trafficLogger;
    private final ArrayList<TrafficMonitor> trafficMonitors = new ArrayList<TrafficMonitor>();
    private final AtomicReference<WssSettings> wssSettingsReference = new AtomicReference<WssSettings>();
    private final ApplicationEventPublisher messageProcessingEventChannel;

    @Inject
    @Named("debugManager")
    private DebugManager debugManager;

    /**
     * Create the new <code>MessageProcessor</code> instance with the service
     * manager, Wss Decorator instance and the server private key.
     * All arguments are required
     *
     * @param sc             the service cache
     * @param pc             the policy cache
     * @param wssd           the Wss Decorator
     * @param securityTokenResolver the security token resolver to use
     * @param licenseManager the SSG's Licence Manager
     * @param metricsServices the SSG's ServiceMetricsManager
     * @param auditContextFactory   factory for producing audit context for message processing
     * @param messageSummaryAuditFactory factory for producing message summary audit records after each request
     * @param config          config provider
     * @param trafficLogger   traffic logger
     * @param messageProcessingEventChannel   channel on which to publish the message processed event (for auditing)
     * @throws IllegalArgumentException if any of the arguments is null
     */
    public MessageProcessor( final ServiceCache sc,
                             final PolicyCache pc,
                             final WssDecorator wssd,
                             final SecurityTokenResolver securityTokenResolver,
                             final SecurityContextFinder securityContextFinder,
                             final LicenseManager licenseManager,
                             final ServiceMetricsServices metricsServices,
                             final AuditContextFactory auditContextFactory,
                             final MessageSummaryAuditFactory messageSummaryAuditFactory,
                             final Config config,
                             final TrafficLogger trafficLogger,
                             ApplicationEventPublisher messageProcessingEventChannel )
      throws IllegalArgumentException {
        if (sc == null) throw new IllegalArgumentException("Service Cache is required");
        if (pc == null) throw new IllegalArgumentException("Policy Cache is required");
        if (wssd == null) throw new IllegalArgumentException("Wss Decorator is required");
        if (licenseManager == null) throw new IllegalArgumentException("License Manager is required");
        if (metricsServices == null) throw new IllegalArgumentException("Service Metrics Manager is required");
        if (auditContextFactory == null) throw new IllegalArgumentException("auditContextFactory is required");
        if (config == null) throw new IllegalArgumentException("Server Config is required");
        if (trafficLogger == null) throw new IllegalArgumentException("Traffic Logger is required");
        if (messageProcessingEventChannel == null) messageProcessingEventChannel = new EventChannel();
        this.serviceCache = sc;
        this.policyCache = pc;
        this.wssDecorator = wssd;
        this.securityTokenResolver = securityTokenResolver;
        this.securityContextFinder = securityContextFinder;
        this.licenseManager = licenseManager;
        this.serviceMetricsServices = metricsServices;
        this.auditContextFactory = auditContextFactory;
        this.messageSummaryAuditFactory = messageSummaryAuditFactory;
        this.config = config;
        this.trafficLogger = trafficLogger;
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
        long maxBytes = config.getLongProperty( ServerConfigParams.PARAM_IO_FIRST_PART_MAX_BYTES, 2621440 );
        MimeBody.setFirstPartMaxBytes( maxBytes );

        wssSettingsReference.set( new WssSettings(
            config.getLongProperty( ServerConfigParams.PARAM_SIGNED_PART_MAX_BYTES, 0 ),
            config.getBooleanProperty( ServerConfigParams.PARAM_SOAP_REJECT_MUST_UNDERSTAND, true),
            config.getBooleanProperty( ServerConfigParams.PARAM_WSS_ALLOW_MULTIPLE_TIMESTAMP_SIGNATURES, false),
            config.getBooleanProperty( ServerConfigParams.PARAM_WSS_ALLOW_UNKNOWN_BINARY_SECURITY_TOKENS, false),
            config.getBooleanProperty( ServerConfigParams.PARAM_WSS_PROCESSOR_STRICT_SIG_CONFIRMATION, true)
        ) );
    }

    /**
     * Process a message.
     * <p/>
     * This method will perform service resolution to find a policy and will then invoke the policy on the specified
     * policy enforcement context (PEC).
     * <p/>
     * The PEC will be configured to flush the thread-local audit context when then PEC is closed.  Callers
     * should not flush the audit context themselves -- just close the PEC.
     *
     * @param context the PEC to process.  Required. Typically contains an initialized Request, for service resolution, but this may not be strictly required in all cases.
     * @return the AssertionStatus from policy evaluation.  Never null.
     * @throws IOException if an IOException occurs during service resolution or policy processing.
     * @throws PolicyAssertionException if a policy assertion is misconfigured.
     * @throws PolicyVersionException if the request arrived over HTTP with a policy version header that indicates use of an obsolete version of the resolved policy.
     *                                TODO pass in strategy for locating transport-level policy version metadata rather than hardcoding support only for policy version HTTP header
     * @throws LicenseException if a required operation is not permitted by the current license
     * @throws MethodNotAllowedException if the request's HTTP method is not permitted by the current port or policy
     * @throws MessageProcessingSuspendedException thrown if message processing is suspended due to a full audit audit log
     */
    public AssertionStatus processMessage(final PolicyEnforcementContext context)
        throws IOException, PolicyAssertionException, PolicyVersionException, LicenseException, MethodNotAllowedException, MessageProcessingSuspendedException
    {
        try {
            final AssertionStatus status[] = { AssertionStatus.UNDEFINED };
            return auditContextFactory.doWithNewAuditContext(new Callable<AssertionStatus>() {
                 @Override
                 public AssertionStatus call() throws Exception {
                     final AuditContext auditContext = AuditContextFactory.getCurrent();
                     context.setAuditContext(auditContext);
                     try {
                         status[0] = reallyProcessMessage(context);
                         return status[0];
                     } finally {
                         doRequestPostProcessing(context, status[0]);

                         /*
                         * 5.0 Audit Request Id
                         * need to extract the required context variables from PEC used in the audit logging
                         */
                         String[] ctxVariables = AuditLogFormatter.getContextVariablesUsed();
                         if (ctxVariables != null && ctxVariables.length > 0) {
                             auditContext.setContextVariables(context.getVariableMap(ctxVariables, auditor));
                         }
                     }
                 }
             }, new Functions.Nullary<com.l7tech.gateway.common.audit.AuditRecord>() {
                 @Override
                 public AuditRecord call() {
                     return messageSummaryAuditFactory.makeEvent(context, status[0]);
                 }
            });
        } catch (IOException e) {
            throw e;
        } catch (PolicyAssertionException e) {
            throw e;
        } catch (PolicyVersionException e) {
            throw e;
        } catch (LicenseException e) {
            throw e;
        } catch (MethodNotAllowedException e) {
            throw e;
        } catch (MessageProcessingSuspendedException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            // Shouldn't be possible
            throw new RuntimeException(e);
        }
    }

    private AssertionStatus reallyProcessMessage(final PolicyEnforcementContext context)
        throws IOException, PolicyAssertionException, PolicyVersionException, LicenseException, MethodNotAllowedException, MessageProcessingSuspendedException
    {
        doRequestPreChecks(context);

        final MessageProcessingContext mc = new MessageProcessingContext(context);

        try {
            return mc.processRequest();

        } catch (PublishedService.ServiceException se) {
            auditor.logAndAudit(MessageProcessingMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[]{ExceptionUtils.getMessage(se)}, se);
            context.setPolicyResult(AssertionStatus.SERVER_ERROR);
            return AssertionStatus.SERVER_ERROR;
        } catch (ServiceResolutionException sre) {
            auditor.logAndAudit(MessageProcessingMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[]{ExceptionUtils.getMessage(sre)}, ExceptionUtils.getDebugException(sre));
            context.setPolicyResult(AssertionStatus.SERVER_ERROR);
            return AssertionStatus.SERVER_ERROR;
        } catch (SAXException e) {
            auditor.logAndAudit(MessageProcessingMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[]{ExceptionUtils.getMessage(e)}, e);
            context.setPolicyResult(AssertionStatus.SERVER_ERROR);
            return AssertionStatus.SERVER_ERROR;
        } catch (VariableNameSyntaxException e) {
            auditor.logAndAudit(MessageProcessingMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[]{"Missing or invalid context variable: " + ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
            context.setPolicyResult(AssertionStatus.SERVER_ERROR);
            return AssertionStatus.SERVER_ERROR;
        } finally {
            ResourceUtils.closeQuietly(mc);
        }
    }

    private void doRequestPreChecks(PolicyEnforcementContext context) throws LicenseException, MessageProcessingSuspendedException {
        context.setAuditLevel(DEFAULT_MESSAGE_AUDIT_LEVEL);
        // License check hook
        licenseManager.requireFeature(GatewayFeatureSets.SERVICE_MESSAGEPROCESSOR);

        // no-processing mode check
        if (SuspendStatus.INSTANCE.isSuspended()) {
            auditor.logAndAudit(MessageProcessingMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{"Message processing suspended: "+SuspendStatus.INSTANCE.getReason()},null);
            throw new MessageProcessingSuspendedException("Message processing suspended");
        }
    }

    private void doRequestPostProcessing(PolicyEnforcementContext context, AssertionStatus status) {
        context.setEndTime();

        // Check auditing hints, position here since our "success" may be a back end service fault
        if(isAuditHintingEnabled()) {
            if(!context.isAuditSaveRequest() || !context.isAuditSaveResponse()) {
                Set auditingHints = AuditContextFactory.getCurrent().getHints();
                if(auditingHints.contains(HINT_SAVE_REQUEST)) context.setAuditSaveRequest(true);
                if(auditingHints.contains(HINT_SAVE_RESPONSE)) context.setAuditSaveResponse(true);
            }
        }

        status = adjustAndAuditAssertionStatus(context, status);

        // ensure result is set
        if (context.getPolicyResult() == null) context.setPolicyResult(status);

        updateServiceStatisticsAndMetrics(context, status);
        notifyTrafficMonitors(context, status);

        publishMessageProcessedEvent(context, status);

        // this may or may not log traffic based on server properties (by default, not)
        trafficLogger.log(context);
    }

    private void publishMessageProcessedEvent(PolicyEnforcementContext context, AssertionStatus status) {
        try {
            messageProcessingEventChannel.publishEvent(new MessageProcessed(context, status, this));
        } catch (Throwable t) {
            auditor.logAndAudit(MessageProcessingMessages.EVENT_MANAGER_EXCEPTION, null, t);
        }
    }

    private void publishMessageReceivedEvent(PolicyEnforcementContext context) {
        try {
            messageProcessingEventChannel.publishEvent(new MessageReceived(this, context));
        } catch (Throwable t){
            auditor.logAndAudit(MessageProcessingMessages.EVENT_MANAGER_EXCEPTION, null, t);
        }
    }

    private void notifyTrafficMonitors(PolicyEnforcementContext context, AssertionStatus status) {
        for (TrafficMonitor tm : trafficMonitors) {
            tm.recordTransactionStatus(context, status, context.getEndTime() - context.getStartTime());
        }
    }

    private AssertionStatus adjustAndAuditAssertionStatus(PolicyEnforcementContext context, AssertionStatus status) {
        RoutingStatus rstat = context.getRoutingStatus();
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
                if (context.getService() != null) {
                    serviceName = context.getService().getName() + " [" + context.getService().getGoid() + "]";
                }
                auditor.logAndAudit(MessageProcessingMessages.POLICY_EVALUATION_RESULT, serviceName, String.valueOf(status.getNumeric()), status.getMessage());
            }
        }
        return status;
    }

    private void updateServiceStatisticsAndMetrics(PolicyEnforcementContext context, AssertionStatus status) {
        boolean authorizedRequest = false;
        boolean completedRequest = false;
        RoutingStatus rstat = context.getRoutingStatus();
        if (status == AssertionStatus.NONE) {
            // Policy execution concluded successfully.
            authorizedRequest = true;
            // Considered success (i.e., completed); unless we have routing
            // assertion and the routed response has HTTP error status: then
            // it's a routing failure.
            if (rstat == RoutingStatus.NONE) {
                completedRequest = true;
            } else if (rstat == RoutingStatus.ROUTED) {
                if (!context.getRequest().isHttpRequest() || context.getResponse().getHttpResponseKnob().getStatus() < HttpConstants.STATUS_ERROR_RANGE_START) {
                    completedRequest = true;
                }
            }
        } else {
            // Policy execution was not successful.
            // Considered policy violation (i.e., not authorized); unless we
            // have routing assertion and it failed or the routed response
            // has HTTP error status: then it's a routing failure.
            if (rstat == RoutingStatus.ATTEMPTED ||
                (rstat == RoutingStatus.ROUTED && (context.getRequest().isHttpRequest() && context.getResponse().getHttpResponseKnob().getStatus() >= HttpConstants.STATUS_ERROR_RANGE_START))) {
                authorizedRequest = true;
            }
        }

        updateServiceStatistics(context, authorizedRequest, completedRequest);
        updateServiceMetrics(context, authorizedRequest, completedRequest);
    }

    private void updateServiceMetrics(PolicyEnforcementContext context, boolean authorizedRequest, boolean completedRequest) {
        if (context.isPolicyExecutionAttempted() && serviceMetricsServices.isEnabled()) {
            final int frontTime = (int)(context.getEndTime() - context.getStartTime());
            final int backTime = (int)(context.getRoutingTotalTime());
            Goid serviceGoid = context.getService() == null ? PublishedService.DEFAULT_GOID : context.getService().getGoid();

            serviceMetricsServices.addRequest(
                    serviceGoid,
                    getOperationName(context),
                    context.getDefaultAuthenticationContext().getLastAuthenticatedUser(),
                    context.getMappings(),
                    authorizedRequest, completedRequest, frontTime, backTime);
        }
    }

    private void updateServiceStatistics(PolicyEnforcementContext context, boolean authorizedRequest, boolean completedRequest) {
        PublishedService service = context.getService();
        if (service != null && context.isPolicyExecutionAttempted()) {
            ServiceStatistics stats = serviceCache.getServiceStatistics(service.getGoid());
            stats.attemptedRequest();
            if (authorizedRequest) {
                stats.authorizedRequest();
                if (completedRequest) {
                    stats.completedRequest();
                }
            }
        }
    }

    public void registerTrafficMonitorCallback(TrafficMonitor tm) {
        if (!trafficMonitors.contains(tm)) {
            trafficMonitors.add(tm);
        }
    }

    private String getOperationName( PolicyEnforcementContext context ) {
        String name = null;
        try {
            final Pair<Binding, Operation> pair = context.getBindingAndOperation();
            if(pair != null){
                Operation operation = pair.right;
                name = operation.getName();
            }
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
        if (this.debugManager == null) {
            throw new IllegalArgumentException("Debug Manager is required");
        }

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
        return config.getBooleanProperty(ServerConfigParams.PARAM_AUDIT_HINTING_ENABLED,false);
    }

    private boolean isAuditAssertionStatusEnabled() {
        return config.getBooleanProperty(ServerConfigParams.PARAM_AUDIT_ASSERTION_STATUS_ENABLED,false);
    }

    private final class MessageProcessingContext implements ServiceCache.ResolutionListener, Closeable {
        private final PolicyEnforcementContext context;
        ProcessorResult wssOutput;
        boolean securityProcessingEvaluatedFlag;
        AssertionStatus securityProcessingAssertionStatus;
        Exception securityProcessingException;
        ServerPolicyHandle serverPolicy = null; // Needs to be closed

        /**
         * Create a message processing context.
         * This message processing context must be closed in order to release the server policy handle (should
         * processing make it as far as resolving the server policy).
         *
         * @param context policy enforcement context we will be processing.  We make use of it but do not take ownership;
         *                caller must still take responsibility for closing the PEC even after this MPC is closed.
         */
        private MessageProcessingContext(final PolicyEnforcementContext context) {
            this.context = context;
        }

        private AssertionStatus processRequest()
            throws ServiceResolutionException, IOException, MethodNotAllowedException, PolicyVersionException, PublishedService.ServiceException, PolicyAssertionException, SAXException
        {
            boolean completed = false;
            AssertionStatus status = AssertionStatus.NONE;
            try {
                // Process message received policy
                status = processMessageReceivedPolicy();
                if ( status == AssertionStatus.NONE ) {
                    // Process request
                    status = reallyProcessRequest();
                }
                completed = true;
            } finally {
                // Process message completed
                if ( status == AssertionStatus.NONE && completed ) {
                    status = processMessageCompletedPolicy( false );
                } else {
                    // If there is an exception or policy failure then run the
                    // completed policy but an exception/failure should not be
                    // used
                    processMessageCompletedPolicy( true );
                }

                debugManager.onMessageFinished(context);
            }

            return status;
        }

        private AssertionStatus reallyProcessRequest()
                throws ServiceResolutionException, IOException, MethodNotAllowedException, PolicyVersionException, PublishedService.ServiceException, PolicyAssertionException, SAXException
        {
            // Policy Verification Step
            AssertionStatus serviceResolutionStatus = resolveService();
            if (serviceResolutionStatus != AssertionStatus.NONE)
                return serviceResolutionStatus;

            if (context.getRequest().isHttpRequest())
                processRequestHttpHeaders(context);

           publishMessageReceivedEvent(context);

            // Get the server policy
            lookupServerPolicy();

            if (checkForUnexpectedMultipartRequest())
                return AssertionStatus.BAD_REQUEST;

            if (ensureSecurityProcessingIfEnabledForService())
                return securityProcessingAssertionStatus;

            AssertionStatus status = processPreServicePolicies();
            if ( status != AssertionStatus.NONE ) {
                return status;            
            }

            // Run the policy
            auditor.logAndAudit(MessageProcessingMessages.RUNNING_POLICY);

            context.setPolicyExecutionAttempted(true);
            if (context.getService().isTracingEnabled())
                maybeEnableTracing();

            debugManager.onMessageArrived(context, context.getService().getGoid());
            status = serverPolicy.checkRequest(context);

            // Execute deferred actions for request, then response
            if (status == AssertionStatus.NONE) {
                status = doDeferredAssertions(context);
            }

            context.setPolicyResult(status);

            // Run post service global policies
            if ( status == AssertionStatus.NONE ) {
                status = processPostServicePolicies(
                        PolicyType.TAG_GLOBAL_POST_SERVICE,
                        MessageProcessingMessages.RUNNING_POST_SERVICE_POLICY,
                        MessageProcessingMessages.ERROR_POST_SERVICE,
                        false );
            }

            doResponseSecurityProcessing(status);

            // Run post WS-Security decoration global service policies
            if ( status == AssertionStatus.NONE ) {
                status = processPostServicePolicies(
                        PolicyType.TAG_GLOBAL_POST_SECURITY,
                        MessageProcessingMessages.RUNNING_POST_SECURITY_POLICY,
                        MessageProcessingMessages.ERROR_POST_SECURITY,
                        false );
            }

            return status;
        }

        private void maybeEnableTracing() {
            String traceGuid = config.getProperty( ServerConfigParams.PARAM_TRACE_POLICY_GUID );
            if (traceGuid == null || traceGuid.trim().length() < 1) {
                logger.info("Tracing enabled on service but no trace policy configured");
                return;
            }

            // Evaluator takes ownership of trace policy handle
            TracePolicyEvaluator.createAndAttachToContext(context, policyCache.getServerPolicy(traceGuid));
        }

        /**
         * Resolve the published service and set it in the PolicyEnforcementContext.
         *
         * @return an assertion status.  Never null.
         * @throws ServiceResolutionException if thrown by service cache
         * @throws IOException if WSS processing is attempted during resolution, and it throws IOException
         */
        AssertionStatus resolveService() throws ServiceResolutionException, IOException, PolicyAssertionException {
            PublishedService service = serviceCache.resolve(context.getRequest(), this);
            if (service == null) {
                checkSecurityProcessingException();
                if (securityProcessingAssertionStatus != null) {
                    return securityProcessingAssertionStatus;
                } else {
                    auditor.logAndAudit(MessageProcessingMessages.SERVICE_NOT_FOUND);
                    try {
                        String httpURL = context.getRequest().getHttpRequestKnob().getRequestUrl();
                        logger.info("Request URL not resolved: " + httpURL);
                    } catch (IllegalStateException e) {
                        // do nothing here, if the message is not HTTP.
                    }
                    return AssertionStatus.SERVICE_NOT_FOUND;
                }
            }

            if (service.isDisabled()) {
                auditor.logAndAudit(MessageProcessingMessages.SERVICE_DISABLED);
                return AssertionStatus.SERVICE_NOT_FOUND;
            }

            auditor.logAndAudit(MessageProcessingMessages.RESOLVED_SERVICE, service.getName(), String.valueOf(service.getGoid()));
            context.setService(service);
            HybridDiagnosticContext.put( SERVICE_ID, service.getId() );
            HybridDiagnosticContext.put( FOLDER_ID, map( policyCache.getFolderPath( service.getPolicy().getGoid() ), id() ));
            return AssertionStatus.NONE;
        }

        private void processRequestHttpHeaders(PolicyEnforcementContext context) throws MethodNotAllowedException, PolicyVersionException {
            HttpRequestKnob httpRequestKnob = context.getRequest().getHttpRequestKnob();
            PublishedService service = context.getService();
            assert service != null;

            // Check the request method
            final HttpMethod requestMethod = httpRequestKnob.getMethod();
            if (requestMethod != null && !service.isMethodAllowed(requestMethod)) {
                String[] auditArgs = new String[] { requestMethod.name(), service.getName() };
                Object[] faultArgs = new Object[] { requestMethod.name() };
                auditor.logAndAudit(MessageProcessingMessages.METHOD_NOT_ALLOWED, auditArgs);

                String faultDetails = MessageFormat.format(
                    MessageProcessingMessages.METHOD_NOT_ALLOWED_FAULT.getMessage(), faultArgs) + ": " + httpRequestKnob.getRequestUrl();
                throw new MethodNotAllowedException(faultDetails);
            }

            // initialize cache
            if(httpRequestKnob instanceof HttpServletRequestKnob) {
                String cacheId = service.getGoid() + "." + service.getVersion();
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
                        String expectedPolicyVersion = policyCache.getUniquePolicyVersionIdentifer( service.getPolicy().getGoid() );
                        Goid reqPolicyId = Goid.parseGoid(requestorVersion.substring(0, indexofbar));
                        String reqPolicyVersion = requestorVersion.substring(indexofbar + 1);
                        if ( !Goid.equals(reqPolicyId, service.getGoid())  ||
                             !reqPolicyVersion.equals( expectedPolicyVersion )) {
                            auditor.logAndAudit(MessageProcessingMessages.POLICY_VERSION_INVALID, requestorVersion, String.valueOf(service.getGoid()), expectedPolicyVersion);
                            wrongPolicyVersion = true;
                        }
                    } catch (IllegalArgumentException e) {
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

        @Override
        public boolean notifyPreParseServices( final Message message,
                                               final Set<ServiceCache.ServiceMetadata> serviceMetadataSet ) {
            securityProcessingEvaluatedFlag = true;

            boolean isSoap = false;
            boolean hasSecurity = false;
            boolean preferDom = true;
            boolean performSecurityProcessing = false;

            // Run pre WS-Security undecoration global service policies
            if ( !processPreServicePoliciesNoThrow(
                    PolicyType.TAG_GLOBAL_PRE_SECURITY,
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
                auditor.logAndAudit(MessageProcessingMessages.INVALID_REQUEST_WITH_DETAIL, new String[]{ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
                context.setMalformedRequest();
                securityProcessingAssertionStatus = AssertionStatus.BAD_REQUEST;
                return false;
            } catch (MessageNotSoapException e) {
                auditor.logAndAudit(MessageProcessingMessages.MESSAGE_NOT_SOAP, null, ExceptionUtils.getDebugException(e));
            } catch (NoSuchPartException e) {
                auditor.logAndAudit(MessageProcessingMessages.INVALID_REQUEST_WITH_DETAIL, new String[]{ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
                securityProcessingAssertionStatus = AssertionStatus.BAD_REQUEST;
                return false;
            } catch (InvalidDocumentFormatException e) {
                auditor.logAndAudit(MessageProcessingMessages.INVALID_REQUEST_WITH_DETAIL, new String[]{ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
                securityProcessingAssertionStatus = AssertionStatus.BAD_REQUEST;
                return false;
            } catch (IOException ioe) {
                securityProcessingException = ioe;
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
                trogdor.setErrorHandler(new WssProcessorErrorHandler() {
                    @Override
                    public void onDecryptionError(Throwable t) {
                        auditor.logAndAudit(MessageProcessingMessages.ERROR_XML_DECRYPTION);
                    }
                });
                try {
                    final Message request = context.getRequest();
                    final SecurityKnob reqSec = request.getSecurityKnob();
                    wssOutput = trogdor.undecorateMessage(request,
                                                          securityContextFinder,
                                                          securityTokenResolver);
                    reqSec.setProcessorResult(wssOutput);
                } catch (MessageNotSoapException e) {
                    auditor.logAndAudit(MessageProcessingMessages.MESSAGE_NOT_SOAP_NO_WSS, null, e);
                    // this shouldn't be possible now
                    // pass through, leaving wssOutput as null
                } catch (UnexpectedKeyInfoException e) {
                    // Must catch before ProcessorException
                    // Use appropriate fault to warn client about unresolvable KeyInfo
                    auditor.logAndAudit(MessageProcessingMessages.ERROR_WSS_PROCESSING_INFO,
                            new String[]{ExceptionUtils.getMessage( e )},
                            ExceptionUtils.getDebugException( e ));
                    context.setAuditLevel(Level.WARNING);
                    SoapFaultLevel cfault = new SoapFaultLevel();
                    cfault.setLevel(SoapFaultLevel.TEMPLATE_FAULT);
                    cfault.setFaultTemplate(SoapFaultUtils.badKeyInfoFault(context.getService() != null ? context.getService().getSoapVersion() : SoapVersion.UNKNOWN, getIncomingURL(context)));
                    context.setFaultlevel(cfault);
                    securityProcessingAssertionStatus = AssertionStatus.FAILED;
                    return false;
                } catch (ProcessorValidationException pve){
                    auditor.logAndAudit(MessageProcessingMessages.ERROR_WSS_PROCESSING_INFO,
                            new String[]{ExceptionUtils.getMessage( pve )},
                            ExceptionUtils.getDebugException( pve ));
                    securityProcessingAssertionStatus = AssertionStatus.BAD_REQUEST;
                    return false;
                } catch (ProcessorException e) {
                    auditor.logAndAudit(MessageProcessingMessages.ERROR_WSS_PROCESSING_INFO, new String[] { ExceptionUtils.getMessage(e) }, ExceptionUtils.getDebugException(e));
                    securityProcessingAssertionStatus = AssertionStatus.SERVER_ERROR;
                    return false;
                } catch (InvalidDocumentSignatureException e) {
                    auditor.logAndAudit(MessageProcessingMessages.ERROR_WSS_SIGNATURE,
                            new String[]{ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
                    context.setAuditLevel(Level.WARNING);
                    securityProcessingAssertionStatus = AssertionStatus.BAD_REQUEST;
                    return false;
                } catch (InvalidDocumentFormatException e) {
                    auditor.logAndAudit(MessageProcessingMessages.ERROR_WSS_PROCESSING_INFO,
                            new String[]{ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
                    context.setAuditLevel(Level.WARNING);
                    securityProcessingAssertionStatus = AssertionStatus.BAD_REQUEST;
                    return false;
                } catch (KeyUsageException e) {
                    auditor.logAndAudit(MessageProcessingMessages.CERT_KEY_USAGE, e.getActivityName());
                    context.setAuditLevel(Level.WARNING);
                    securityProcessingAssertionStatus = AssertionStatus.BAD_REQUEST;
                    return false;
                } catch (GeneralSecurityException e) {
                    auditor.logAndAudit(MessageProcessingMessages.ERROR_WSS_PROCESSING_INFO, new String[] { ExceptionUtils.getMessage(e) }, ExceptionUtils.getDebugException(e));
                    context.setAuditLevel(Level.WARNING);
                    securityProcessingAssertionStatus = AssertionStatus.SERVER_ERROR;
                    return false;
                } catch (SAXException e) {
                    auditor.logAndAudit(MessageProcessingMessages.ERROR_RETRIEVE_XML, null, e);
                    context.setAuditLevel(Level.WARNING);
                    securityProcessingAssertionStatus = AssertionStatus.SERVER_ERROR;
                    return false;
                } catch (BadSecurityContextException e) {
                    auditor.logAndAudit(MessageProcessingMessages.ERROR_WSS_PROCESSING_INFO, ExceptionUtils.getMessage(e));
                    context.setAuditLevel(Level.WARNING);
                    SoapFaultLevel cfault = new SoapFaultLevel();
                    cfault.setLevel(SoapFaultLevel.TEMPLATE_FAULT);
                    SoapVersion version = context.getService() != null ? context.getService().getSoapVersion() : SoapVersion.UNKNOWN;
                    cfault.setFaultTemplate( SoapFaultUtils.badContextTokenFault(version, e.getWsscNamespace(), getIncomingURL(context)) );
                    context.setFaultlevel(cfault);
                    securityProcessingAssertionStatus = AssertionStatus.FAILED;
                    return false;
                } catch (IOException ioe) {
                    securityProcessingException = ioe;
                    return false;
                }
                auditor.logAndAudit(MessageProcessingMessages.WSS_PROCESSING_COMPLETE);
            }

            return true;
        }

        @Override
        public boolean notifyResolveCatchAll() {
            return securityProcessingException == null && securityProcessingAssertionStatus == null;
        }

        private String getIncomingURL(PolicyEnforcementContext context) {
            HttpRequestKnob hrk = context.getRequest().getKnob(HttpRequestKnob.class);
            if (hrk == null) {
                logger.warning("cannot generate incoming URL");
                return "";
            }
            return hrk.getQueryString() == null ? hrk.getRequestUrl() : hrk.getRequestUrl() + "?" + hrk.getQueryString();
        }

        private boolean checkForUnexpectedMultipartRequest() throws PublishedService.ServiceException {
            // Check request data
            if (context.getRequest().getMimeKnob().isMultipart() &&
                !(context.getService().isMultipart() || serverPolicy.getPolicyMetadata().isMultipart())) {
                auditor.logAndAudit(MessageProcessingMessages.MULTIPART_NOT_ALLOWED);
                return true;
            }
            return false;
        }

        private void checkSecurityProcessingException() throws PolicyAssertionException, IOException {
            if ( securityProcessingException != null ) {
                if ( securityProcessingException instanceof IOException ) {
                    throw (IOException) securityProcessingException;
                } else if ( securityProcessingException instanceof PolicyAssertionException ) {
                    throw (PolicyAssertionException) securityProcessingException;
                } else {
                    throw ExceptionUtils.wrap( securityProcessingException );
                }
            }
        }

        private boolean ensureSecurityProcessingIfEnabledForService() throws IOException, PolicyAssertionException {
            // Ensure security processing is run if enabled
            if ( !securityProcessingEvaluatedFlag &&
                 (context.getService().isWssProcessingEnabled())) {
                PolicyMetadata policyMetadata = serverPolicy.getPolicyMetadata();
                Set<ServiceCache.ServiceMetadata> metadatas = Collections.singleton( new ServiceCache.ServiceMetadata(policyMetadata, true) );
                if (!notifyPreParseServices( context.getRequest(), metadatas ) ) {
                    checkSecurityProcessingException();
                    return true;
                }

                // avoid re-Tarari-ing request that's already DOM parsed unless some assertions need it bad
                XmlKnob xk = context.getRequest().getKnob(XmlKnob.class);
                if (xk != null) xk.setTarariWanted(metadatas.iterator().next().isTarariWanted());
            }
            return false;
        }

        private AssertionStatus processMessageReceivedPolicy() throws IOException, PolicyAssertionException {
            return processPreServicePolicies(
                PolicyType.TAG_GLOBAL_MESSAGE_RECEIVED,
                MessageProcessingMessages.RUNNING_MESSAGE_RECEIVED_POLICY,
                MessageProcessingMessages.ERROR_MESSAGE_RECEIVED );
        }

        private AssertionStatus processMessageCompletedPolicy( final boolean handleException ) throws IOException, PolicyAssertionException {
            return processPostServicePolicies(
                    PolicyType.TAG_GLOBAL_MESSAGE_COMPLETED,
                    MessageProcessingMessages.RUNNING_MESSAGE_COMPLETED_POLICY,
                    MessageProcessingMessages.ERROR_MESSAGE_COMPLETED,
                    handleException );
        }

        private AssertionStatus processPreServicePolicies() throws IOException, PolicyAssertionException {
            AssertionStatus status = AssertionStatus.NONE;
            // Ensure pre-security policies are run
            if ( !securityProcessingEvaluatedFlag ) {
                status = processPreServicePolicies(
                    PolicyType.TAG_GLOBAL_PRE_SECURITY,
                    MessageProcessingMessages.RUNNING_PRE_SECURITY_POLICY,
                    MessageProcessingMessages.ERROR_PRE_SECURITY );
            }

            if ( status == AssertionStatus.NONE ) {
                status = processPreServicePolicies(
                    PolicyType.TAG_GLOBAL_PRE_SERVICE,
                    MessageProcessingMessages.RUNNING_PRE_SERVICE_POLICY,
                    MessageProcessingMessages.ERROR_PRE_SERVICE );
            }

            return status;
        }

        /*
         * Process pre service policies stashing errors/failures for later use.
         */
        public boolean processPreServicePoliciesNoThrow( final String policyTag,
                                                         final AuditDetailMessage notify,
                                                         final AuditDetailMessage error ) {
            boolean success = true;
            try {
                AssertionStatus status = processPreServicePolicies( policyTag, notify, error );
                if ( status != AssertionStatus.NONE ) {
                    securityProcessingAssertionStatus = status;
                    success = false;
                }
            } catch (IOException ioe) {
                securityProcessingException = ioe;
                success = false;
            } catch ( PolicyAssertionException e ) {
                securityProcessingException = e;
                success = false;
            }
            return success;
        }

        /*
         * Process pre service policies and audit errors.
         */
        public AssertionStatus processPreServicePolicies( final String policyTag,
                                                          final AuditDetailMessage notify,
                                                          final AuditDetailMessage error ) throws IOException, PolicyAssertionException {
            AssertionStatus status = AssertionStatus.NONE;

            final Set<String> guids = policyCache.getPoliciesByTypeAndTag( PolicyType.GLOBAL_FRAGMENT, policyTag );
            if ( !guids.isEmpty() ) {
                auditor.logAndAudit( notify );

                // If the service is not resolved then it should be null from the
                // PEC, it is not desirable to set a fake published service. If
                // any assertions rely on a service being set they should be fixed.
                try {
                    for ( final String guid : guids ) {
                        status = processGlobalPolicy( policyCache, context, guid );
                        if ( status != AssertionStatus.NONE ) {
                            break;
                        }
                    }
                } catch (PolicyAssertionException e) {
                    auditor.logAndAudit(error, ExceptionUtils.getMessage( e ));
                    throw e;
                }  catch ( RuntimeException e ) {
                    auditor.logAndAudit(error, ExceptionUtils.getMessage( e ));
                    throw e;
                }
            }

            return status;
        }

        /*
         * Process post service policies and audit errors.
         *
         * <p>If exceptions are being handled then the resulting status is not meaningful.</p>
         */
        private AssertionStatus processPostServicePolicies( final String policyTag,
                                                            final AuditDetailMessage notificationMessage,
                                                            final AuditDetailMessage errorMessage,
                                                            final boolean handleException ) throws IOException, PolicyAssertionException {
            AssertionStatus assertionStatus = AssertionStatus.NONE;
            final Set<String> guids = policyCache.getPoliciesByTypeAndTag( PolicyType.GLOBAL_FRAGMENT, policyTag );
            if ( !guids.isEmpty() ) {
                auditor.logAndAudit( notificationMessage );

                try {
                    for ( final String guid : guids ) {
                        assertionStatus = processGlobalPolicy( policyCache, context, guid );
                        if ( assertionStatus != AssertionStatus.NONE ) {
                            break;
                        }
                    }
                } catch ( PolicyAssertionException e ) {
                    auditor.logAndAudit(errorMessage, new String[]{ExceptionUtils.getMessage( e )}, handleException ? null : e);
                    if (!handleException) throw e;
                } catch ( RuntimeException e ) {
                    auditor.logAndAudit(errorMessage, new String[]{ExceptionUtils.getMessage( e )}, handleException ? null : e);
                    if (!handleException) throw e;
                }
            }

            return assertionStatus;
        }

        private AssertionStatus processGlobalPolicy( final PolicyCache policyCache,
                                                     final PolicyEnforcementContext context,
                                                     final String guid ) throws PolicyAssertionException, IOException {
            AssertionStatus result;

            ServerPolicyHandle handle = null;
            PolicyEnforcementContext pec = null;
            try {
                handle = policyCache.getServerPolicy( guid );
                if ( handle != null ) {
                    pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext( context );
                    debugManager.onMessageArrived(pec, handle.getPolicyMetadata().getPolicyHeader().getGoid());
                    result = handle.checkRequest( pec );
                } else {
                    result = AssertionStatus.NONE;
                }
            } finally {
                if (pec != null) {
                    debugManager.onMessageFinished(pec);
                }
                ResourceUtils.closeQuietly( pec );
                ResourceUtils.closeQuietly( handle );
            }

            return result;
        }

        public void lookupServerPolicy() throws ServiceResolutionException {
            final Goid serviceGoid = context.getService().getGoid();
            serverPolicy = serviceCache.getServerPolicy(serviceGoid);
            if (serverPolicy == null)
                throw new ServiceResolutionException("service is resolved but the corresponding policy is invalid for service GOID " + serviceGoid + " (" + context.getService().getName() + ")");
            context.setServicePolicyMetadata( serverPolicy.getPolicyMetadata() );
        }

        @Override
        public void close() throws IOException {
            ResourceUtils.closeQuietly(serverPolicy);
            serverPolicy = null;
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

        private void doResponseSecurityProcessing(AssertionStatus status) throws IOException, SAXException, PolicyAssertionException {
            // add signature confirmations
            WSSecurityProcessorUtils.addSignatureConfirmations(context.getResponse(), auditor);

            // Run response through WssDecorator if indicated
            if (status == AssertionStatus.NONE &&
                  context.getResponse().isXml() &&
                  context.getResponse().getSecurityKnob().getDecorationRequirements().length > 0 &&
                  context.getResponse().isSoap())
            {
                try {
                    DecorationRequirements[] allrequirements = context.getResponse().getSecurityKnob().getDecorationRequirements();
                    SecurityKnob reqSec = context.getRequest().getSecurityKnob();

                    addL7aRelatesTo();
                    adjustSecurityActor(allrequirements, reqSec);

                    // do the actual decoration
                    for (final DecorationRequirements responseDecoReq : allrequirements) {
                        if (responseDecoReq != null && wssOutput != null) {
                            if (wssOutput.getSecurityNS() != null)
                                responseDecoReq.getNamespaceFactory().setWsseNs(wssOutput.getSecurityNS());
                            if (wssOutput.getWSUNS() != null)
                                responseDecoReq.getNamespaceFactory().setWsuNs(wssOutput.getWSUNS());
                            if (wssOutput.getWsscNS() != null)
                                responseDecoReq.getNamespaceFactory().setWsscNs(wssOutput.getWsscNS());
                            if (wssOutput.isDerivedKeySeen())
                                responseDecoReq.setUseDerivedKeys(true);
                        }
                        wssDecorator.decorateMessage(context.getResponse(), responseDecoReq);
                    }
                } catch (Exception e) {
                    auditor.logAndAudit(MessageProcessingMessages.ERROR_WSS_RESPONSE, ExceptionUtils.getMessage(e));
                    throw new PolicyAssertionException(null, "Failed to apply WSS decoration to response", e);
                }
            }
        }

        private void adjustSecurityActor(DecorationRequirements[] allrequirements, SecurityKnob reqSec) {
            // if the request was processed on the noactor sec header instead of the l7 sec actor, then
            // the response's decoration requirements should map this (if applicable)
            if (reqSec.getProcessorResult() != null &&
                reqSec.getProcessorResult().getProcessedActor() != null ) {
                String actorUri = reqSec.getProcessorResult().getProcessedActor()== SecurityActor.NOACTOR ?
                        null :
                        reqSec.getProcessorResult().getProcessedActorUri();

                // go find the l7 decoreq and adjust the actor
                for (DecorationRequirements requirement : allrequirements) {
                    if (SoapConstants.L7_SOAP_ACTOR.equals(requirement.getSecurityHeaderActor())) {
                        requirement.setSecurityHeaderActor( actorUri );
                    }
                }
            }
        }

        private void addL7aRelatesTo() throws InvalidDocumentFormatException {
            try {
                String messageId = context.getSavedRequestL7aMessageId();
                if (messageId == null && context.getRequest().isSoap()) {
                    try {
                        messageId = SoapUtil.getL7aMessageId(context.getRequest().getXmlKnob().getDocumentReadOnly());
                    } catch (InvalidDocumentFormatException e) {
                        // Request was probably transformed.  Fallthrough and do without messageId
                    } catch (SAXException e) {
                        // Request was probably transformed.  Fallthrough and do without messageId
                    }
                }
                if (messageId != null && messageId.length() > 0) {
                    SoapUtil.setL7aRelatesTo(context.getResponse().getXmlKnob().getDocumentWritable(), messageId);
                }
            } catch(IOException e) {
                logger.log(Level.WARNING, "Unable to extract message ID from request: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            } catch(SAXException e) {
                logger.log(Level.WARNING, "Unable to extract message ID from request: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
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
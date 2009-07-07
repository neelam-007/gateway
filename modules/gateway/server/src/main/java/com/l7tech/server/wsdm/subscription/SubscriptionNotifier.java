/*
 * Copyright (C) 2007-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.wsdm.subscription;

import com.l7tech.common.http.*;
import com.l7tech.common.io.ByteLimitInputStream;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.audit.ServiceMessages;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.service.MetricsSummaryBin;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.HttpRequestKnobAdapter;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.MessageSummaryAuditFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.PolicyCache;
import com.l7tech.server.policy.ServerPolicyHandle;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.server.wsdm.Aggregator;
import com.l7tech.server.wsdm.MetricsRequestContext;
import com.l7tech.server.wsdm.QoSMetricsService;
import com.l7tech.server.wsdm.Namespaces;
import com.l7tech.server.wsdm.faults.FaultMappableException;
import com.l7tech.server.wsdm.faults.GenericWSRFExceptionFault;
import com.l7tech.server.wsdm.faults.ResourceUnknownFault;
import com.l7tech.server.wsdm.faults.UnacceptableTerminationTimeFault;
import com.l7tech.server.wsdm.method.Subscribe;
import com.l7tech.util.*;
import com.l7tech.xml.soap.SoapUtil;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.w3c.dom.Element;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages ESM subscriptions
 */
@SuppressWarnings({ "ThrowableResultOfMethodCallIgnored" })
public class SubscriptionNotifier implements ServiceStateMonitor, ApplicationContextAware {
    private static final Logger logger = Logger.getLogger(SubscriptionNotifier.class.getName());

    public static final String PRODUCT = "Layer7-SecureSpan-Gateway";
    public static final String DEFAULT_USER_AGENT = PRODUCT + "/v" + BuildInfo.getProductVersion() + "-b" + BuildInfo.getBuildNumber();

    public static final String CLUSTER_PROP_NOTIFY_INTERVAL = "esm.notification.interval";
    public static final String CLUSTER_PROP_ESM_ENABLED = "esm.notification.enabled";

    public static final String SERVERCONFIG_PROP_NOTIFY_INTERVAL = ClusterProperty.asServerConfigPropertyName(CLUSTER_PROP_NOTIFY_INTERVAL);
    public static final String SERVERCONFIG_PROP_ESM_ENABLED = ClusterProperty.asServerConfigPropertyName(CLUSTER_PROP_ESM_ENABLED);

    private static final int CLEANUP_PERIOD = 60000;
    private static final int DEFAULT_NOTIFICATION_PERIOD = CLEANUP_PERIOD;
    private static final boolean DEFAULT_ESM_ENABLED = true;

    private static final EnumSet<NotificationType> DISABLED_OR_ENABLED = EnumSet.of(NotificationType.DISABLED, NotificationType.ENABLED);

    private Auditor auditor;

    private URL incomingUrl;

    private final AtomicLong notificationPeriod = new AtomicLong(0);
    private final AtomicBoolean notificationEnabled = new AtomicBoolean(false);
    private volatile long lastNotificationRun;

    @Resource
    private ServiceCache serviceCache;
    @Resource
    private SubscriptionManager subscriptionManager;
    @Resource
    private PolicyCache policyCache;
    @Resource
    private MessageSummaryAuditFactory auditFactory;
    @Resource
    private ServerConfig serverConfig;
    @Resource
    private GenericHttpClientFactory httpClientFactory;
    @Resource
    private Aggregator aggregator;

    private final String clusterNodeId;

    private TimerTask maintenanceTask;
    private TimerTask notificationTask ;
    private TimerTask notificationStealerTask;

    private final ExecutorService threadPool = Executors.newSingleThreadExecutor();

    private AuditContext auditContext;

    public SubscriptionNotifier(final String clusterNodeId) {
        this.clusterNodeId = clusterNodeId;
    }

    @PostConstruct
    private void start() {
        loadNotificationProperties(true);

        String hostname = serverConfig.getHostname();
        int clusterHttpPort = serverConfig.getIntProperty( "clusterhttpport", 8080);
        String clusterHostName = serverConfig.getProperty( "clusterHost" );
        if ( clusterHostName == null || clusterHostName.length()==0 ) {
            clusterHostName = hostname;
        }

        try {
            incomingUrl = new URL("http://" + clusterHostName + ":" + clusterHttpPort);
            logger.log(Level.CONFIG, "Using SSG URL '" + incomingUrl.toString() + "'.");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e); // Uh-oh
        }

        this.subscriptionManager.setAggregator(aggregator);

        // worker thread deleting expired subscriptions
        maintenanceTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    subscriptionManager.deleteExpiredSubscriptions();
                    if ( loadNotificationProperties(false) ) {
                        scheduleTasks( true, notificationEnabled.get() ? notificationPeriod.get() : 0 );
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Unable to perform subscription cleanup; will retry in " + CLEANUP_PERIOD + "ms.", e);
                }
            }
            
            @Override
            public String toString() {
                return "ESM Maintenance Task";
            }
        };

        notificationTask = new TimerTask() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();

                pushMetricsNotifications(lastNotificationRun);

                lastNotificationRun = now;
            }
            @Override
            public String toString() {
                return "ESM Notification Task";
            }
        };

        notificationStealerTask = new TimerTask() {
            @Override
            public void run() {
                subscriptionManager.stealSubscriptionsFromDeadNodes(clusterNodeId, System.currentTimeMillis() - (5 * notificationPeriod.get()));
            }
            @Override
            public String toString() {
                return "ESM Notification Failover Task";
            }
        };

        scheduleTasks( false, notificationEnabled.get() ? notificationPeriod.get() : 0 );
    }

    /**
     * Reload notification properties, return true if updated.
     */
    private boolean loadNotificationProperties(boolean logValues) {
        boolean updated = false;

        // load period
        {
            long period;
            String notifyPropStr = serverConfig.getPropertyCached(SERVERCONFIG_PROP_NOTIFY_INTERVAL);
            if (notifyPropStr != null) {
                try {
                    if (logValues) logger.info("Found Config Property '" + CLUSTER_PROP_NOTIFY_INTERVAL + "' with value '" + notifyPropStr + "'.");
                    period = TimeUnit.parse(notifyPropStr,TimeUnit.MILLIS);
                } catch (NumberFormatException e) {
                    logger.warning("Unable to parse ESM Notification delay property (" + CLUSTER_PROP_NOTIFY_INTERVAL + ") with value '"+notifyPropStr+"'.");
                    period = DEFAULT_NOTIFICATION_PERIOD;
                }
            } else {
                logger.info(CLUSTER_PROP_NOTIFY_INTERVAL + " cluster property not found. Using the default of 60000 ms.");
                period = DEFAULT_NOTIFICATION_PERIOD;
            }
            if ( notificationPeriod.get() != period ) {
                updated = true;
            }
            this.notificationPeriod.set(period);
        }

        // load enabled
        {
            boolean enabled;
            String notifyEnablePropStr = serverConfig.getPropertyCached(SERVERCONFIG_PROP_ESM_ENABLED);
            if (notifyEnablePropStr != null) {
                if (logValues) logger.info("Found Config Property '" + CLUSTER_PROP_ESM_ENABLED + "' with value '" + notifyEnablePropStr + "'.");
                if ( "true".equalsIgnoreCase(notifyEnablePropStr) || "false".equalsIgnoreCase(notifyEnablePropStr) ) {
                    enabled = Boolean.parseBoolean(notifyEnablePropStr);
                } else {
                    logger.warning("Unable to parse ESM enabled property (" + CLUSTER_PROP_ESM_ENABLED + ") with value '"+notifyEnablePropStr+"'.");
                    enabled = DEFAULT_ESM_ENABLED;
                }
            } else {
                logger.info(CLUSTER_PROP_ESM_ENABLED + " cluster property not found. Using the default of 60000 ms.");
                enabled = DEFAULT_ESM_ENABLED;
            }
            if ( notificationEnabled.get() != enabled ) {
                updated = true;
            }
            this.notificationEnabled.set(enabled);
        }

        return updated;
    }

    /**
     * (Re)Schedule notification tasks, zero notifyPeriod means don't run.
     */
    private void scheduleTasks( final boolean reschedule, final long notifyPeriod ) {
        if ( reschedule ) {
            // Will be restarted with given notification period
            Background.cancel( notificationTask );
            Background.cancel( notificationStealerTask );
        } else {
            // Shedule once only
            Background.scheduleRepeated( maintenanceTask, 123077, CLEANUP_PERIOD );
        }

        if ( notifyPeriod > 0 ) {
            logger.log( Level.INFO, "Scheduling subscription notification task with period {0}ms.", notifyPeriod);
            Background.scheduleRepeated( notificationTask, 15011, notifyPeriod );
            Background.scheduleRepeated( notificationStealerTask, notifyPeriod, (long)(2.49 * notifyPeriod) );
        }
    }

    public void renewSubscription(String subscriptionId, long newTermination, String policyGuid) throws FaultMappableException {
        try {
            subscriptionManager.renewSubscription(subscriptionId, newTermination, policyGuid);
        } catch (FindException e) {
            throwUnknownSubscriptionId(subscriptionId, e);
        } catch (UpdateException e) {
            logger.log(Level.WARNING, "Unable to update subscription", e);
            throw new GenericWSRFExceptionFault("Unable to update subscription " + subscriptionId);
        }
    }

    public void unsubscribe(String subscriptionId) throws ResourceUnknownFault {
        try {
            subscriptionManager.deleteByUuid(subscriptionId);
        } catch (ObjectModelException e) {
            throwUnknownSubscriptionId(subscriptionId, e);
        }
    }

    private void throwUnknownSubscriptionId(String subscriptionId, Exception e) throws ResourceUnknownFault {
        if (e != null) logger.log(Level.WARNING, "Subscription persistence problem: " + ExceptionUtils.getMessage(e), e);
        throw new ResourceUnknownFault("The subscription id is unknown " + subscriptionId);
    }

    public String generateNewSubscriptionId() {
        return UUID.randomUUID().toString();
    }

    @SuppressWarnings({"DuplicateThrows"})
    public Subscription subscribe(Subscribe method, String policyGuid) throws FaultMappableException, UnacceptableTerminationTimeFault {
        long now = System.currentTimeMillis();
        if(method.getTermination() < now) {
            throw new UnacceptableTerminationTimeFault("Cannot have a termination time in the past.");
        }
        
        try {
            PublishedService svc = serviceCache.getCachedService(Long.parseLong(method.getServiceId()));
            if (svc == null) throw new ResourceUnknownFault("No service with ID " + method.getServiceId());
            
            final Subscription sub = new Subscription(method, generateNewSubscriptionId(), clusterNodeId);
            sub.setNotificationPolicyGuid(policyGuid);

            SubscriptionKey key = new SubscriptionKey(sub.getPublishedServiceOid(), sub.getTopic(), sub.getReferenceCallback());
            Collection<Subscription> duplicateSubs = subscriptionManager.findBySubscriptionKey( key );
            if ( !duplicateSubs.isEmpty() ) {
                String uuid = duplicateSubs.iterator().next().getUuid();
                logger.warning("Found duplicate subscription, not creating new subscription, duplicate of '" + uuid + "' for URL '"+sub.getReferenceCallback()+"'.");
                // Don't put the UUID in the error message since this would allow discovery of other peoples subscription UUIDs
                throw new GenericWSRFExceptionFault("Duplicate subscription. Please cancel existing subscription.");
            }

            subscriptionManager.save(sub);
            logger.info("New subscription registered " + sub.getUuid() + " will expire in " +
                        ((sub.getTermination()  - System.currentTimeMillis())/1000) + " seconds");
            return sub;
        } catch (ObjectModelException e) {
            throw new RuntimeException(e); // Can't happen
        }
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        this.auditor = new Auditor(this, applicationContext, logger);
        this.auditContext = (AuditContext)applicationContext.getBean("auditContext", AuditContext.class);
    }

    public class ServiceStatusNotificationContext {
        public String subscriptionId;
        public long serviceId;
        public String target;
        public String refParamsXml;
        public long ts;
        public String eventId;
        public String notificationPolicyGuid;
        public MetricsSummaryBin bin;
    }

    @Override
    public void onServiceDisabled(final long serviceoid) {
        if ( notificationEnabled.get() ) {
            notify(serviceoid, NotificationType.DISABLED);
        }
    }

    @Override
    public void onServiceEnabled(final long serviceoid) {
        if ( notificationEnabled.get() ) {
            notify(serviceoid, NotificationType.ENABLED);
        }
    }

    private enum NotificationType {
        ENABLED,
        DISABLED,
        METRICS
    }

    private void notify(final long serviceoid, final NotificationType type) {
        final Collection<Subscription> subs;
        try {
            subs = subscriptionManager.findByNodeAndServiceOid(clusterNodeId, serviceoid);
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to find Subscriptions", e);
            return;
        }

        final List<ServiceStatusNotificationContext> callbacks = new ArrayList<ServiceStatusNotificationContext>();
        final long now = System.currentTimeMillis();
        for (Subscription s : subs) {
            if (s.getPublishedServiceOid() != serviceoid) continue; // Shouldn't be here anyway

            if (s.getTermination() < now) {
                logger.info(MessageFormat.format("Ignoring subscription {0} because it expired {1} seconds ago", s.getUuid(), (System.currentTimeMillis() - s.getTermination()) / 1000));
            } else {
                if ((type == NotificationType.METRICS && s.getTopic() == Subscription.TOPIC_METRICS_CAPABILITY) ||
                    (DISABLED_OR_ENABLED.contains(type) && s.getTopic() == Subscription.TOPIC_OPERATIONAL_STATUS))
                {
                    ServiceStatusNotificationContext ssnc = new ServiceStatusNotificationContext();
                    ssnc.subscriptionId = s.getUuid();
                    ssnc.serviceId = serviceoid;
                    ssnc.target = s.getReferenceCallback();
                    ssnc.notificationPolicyGuid = s.getNotificationPolicyGuid();
                    ssnc.ts = now;
                    callbacks.add(ssnc);
                }
            }
        }

        if (callbacks.size() <= 0) return;

        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                for (ServiceStatusNotificationContext ssnc : callbacks) {
                    try {
                        String notificationMessage;
                        if (DISABLED_OR_ENABLED.contains(type)) {
                            notificationMessage = prepareNotificationForEnabledEvent(ssnc, type);
                        } else if (type == NotificationType.METRICS) {
                            notificationMessage = prepareNotificationForMetricsEvent(ssnc);
                        } else {
                            throw new IllegalStateException("Unsupported notification type: " + type);
                        }
                        sendNotification(ssnc.target, notificationMessage, ssnc.notificationPolicyGuid);
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Unable to generate notification document", e);
                    }
                }
            }
        });
    }

    private void sendNotification(String notificationUrl, String message, String policyGuid) {
        if ( logger.isLoggable( Level.FINEST) ) {
            logger.log(Level.FINEST, "Sending notification {0} to {1}", new String[]{message, notificationUrl});
        }

        PolicyEnforcementContext context = null;
        AssertionStatus status = AssertionStatus.SERVER_ERROR;
        try {
            final Message request = new Message(XmlUtil.stringAsDocument(message));
            request.attachHttpRequestKnob(new HttpRequestKnobAdapter(null));
            context = new PolicyEnforcementContext(request, new Message());
            context.setAuditContext(auditContext);
            context.setAuditLevel(Level.INFO);

            // Use fake service
            final PublishedService svc = new PublishedService();
            svc.setName("ESM Notification Service [Internal]");
            svc.setSoap(true);
            context.setService(svc);

            auditor.logAndAudit(ServiceMessages.ESM_NOTIFY_NOTIFICATION, notificationUrl);

            URL urltarget = null;
            try {
                urltarget = new URL(notificationUrl);
            } catch (MalformedURLException e) {
                auditor.logAndAudit(ServiceMessages.ESM_NOTIFY_INVALID_URL, notificationUrl );
            }

            if ( urltarget != null ) {
                if ( policyGuid == null ) {
                    auditor.logAndAudit(ServiceMessages.ESM_NOTIFY_NO_POLICY, notificationUrl );
                    if ( sendDefaultNotification( context, urltarget, message ) ) {
                        status = AssertionStatus.NONE;
                    } else {
                        status = AssertionStatus.FALSIFIED;
                    }
                } else {
                    ServerPolicyHandle sph = null;
                    try {
                        sph = policyCache.getServerPolicy( policyGuid );
                        if ( sph == null ) {
                            auditor.logAndAudit(ServiceMessages.ESM_NOTIFY_INVALID_POLICY, notificationUrl, policyGuid );
                            status = AssertionStatus.FAILED;
                        } else {
                            context.setVariable("esmNotificationUrl", notificationUrl);
                            try {
                                status = sph.checkRequest(context);
                            } catch (PolicyAssertionException e) {
                                auditor.logAndAudit(ServiceMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                                        new String[]{"ESM Notification Error: " + ExceptionUtils.getMessage(e)},
                                        e );
                            } catch (IOException e) {
                                auditor.logAndAudit(ServiceMessages.ESM_NOTIFY_IO_ERROR,
                                        new String[]{notificationUrl, policyGuid, ExceptionUtils.getMessage(e)},
                                        ExceptionUtils.getDebugException(e) );
                            }
                        }
                    } finally {
                        ResourceUtils.closeQuietly( sph );
                    }
                }
            }
        } finally {
            auditContext.setCurrentRecord(auditFactory.makeEvent(context, status));
            auditContext.flush();
            ResourceUtils.closeQuietly( context );
        }
    }

    private boolean sendDefaultNotification( final PolicyEnforcementContext context,
                                             final URL urltarget,
                                             final String message ) {
        boolean notificationSent = false;

        GenericHttpClient client = httpClientFactory.createHttpClient(-1,-1,getConnectionTimeout(),getTimeout(), null);
        GenericHttpRequestParams requestParams = new GenericHttpRequestParams( urltarget );
        requestParams.setFollowRedirects( false );
        requestParams.setContentType( ContentTypeHeader.XML_DEFAULT );
        requestParams.addExtraHeader( new GenericHttpHeader( HttpConstants.HEADER_USER_AGENT, DEFAULT_USER_AGENT) );
        requestParams.addExtraHeader( new GenericHttpHeader( SoapUtil.SOAPACTION, "" ) );

        // dont add content-type for get and deletes
        if ( Boolean.valueOf(serverConfig.getPropertyCached("ioHttpUseExpectContinue")) ) {
            requestParams.setUseExpectContinue(true);
        }
        if ( Boolean.valueOf(serverConfig.getPropertyCached("ioHttpNoKeepAlive")) ) {
            requestParams.setUseKeepAlives(false); // note that server config property is for NO Keep-Alives
        }
        if ( "1.0".equals(serverConfig.getPropertyCached("ioHttpVersion")) ) {
            requestParams.setHttpVersion(GenericHttpRequestParams.HttpVersion.HTTP_VERSION_1_0);
        }

        GenericHttpRequest request = null;
        GenericHttpResponse response = null;
        try {
            context.routingStarted();

            final byte[] requestBody = message.getBytes(ContentTypeHeader.XML_DEFAULT.getEncoding());
            requestParams.setContentLength( (long)requestBody.length );
            request = client.createRequest(HttpMethod.POST, requestParams);
            if ( request instanceof RerunnableHttpRequest ) {
                RerunnableHttpRequest rerunnableHttpRequest = (RerunnableHttpRequest) request;
                rerunnableHttpRequest.setInputStreamFactory(new RerunnableHttpRequest.InputStreamFactory() {
                    @Override
                    public InputStream getInputStream() {
                         return new ByteArrayInputStream(requestBody);
                    }
                });
            } else {
                request.setInputStream(new ByteArrayInputStream(requestBody));
            }

            response = request.getResponse();
            int status = response.getStatus();
            if ( status == 200 ) {
                notificationSent = true;
                auditor.logAndAudit( ServiceMessages.ESM_NOTIFY_SUCCESS, urltarget.toString(), Integer.toString(status) );
                if ( logger.isLoggable(Level.FINE) )  {
                    InputStream in = response.getInputStream();
                    if ( in != null ) {
                        byte[] body = IOUtils.slurpStream(new ByteLimitInputStream(in, 16, 10*1024*1024));
                        String charset = response.getContentType().getEncoding();
                        logger.fine("Notification target responded 200 with " + new String(body, charset));
                    }
                } 
            } else if ( status < 300 ) {
                notificationSent = true;
                auditor.logAndAudit( ServiceMessages.ESM_NOTIFY_SUCCESS, urltarget.toString(), Integer.toString(status) );
            } else {
                auditor.logAndAudit( ServiceMessages.ESM_NOTIFY_FAILURE, urltarget.toString(), Integer.toString(status) );
            }
        } catch ( IOException ioe ) {
            auditor.logAndAudit(ServiceMessages.ESM_NOTIFY_IO_ERROR,
                    new String[]{urltarget.toString(), "N/A", ExceptionUtils.getMessage(ioe)},
                    ExceptionUtils.getDebugException(ioe) );
        } finally {
            ResourceUtils.closeQuietly(request, response);
            context.routingFinished();
        }

        return notificationSent;
    }

    /**
     * Get the connection timeout to use (set using a cluster/system property)
     *
     * @return the configured or default timeout.
     */
    private int getConnectionTimeout() {
        return getIntProperty(ServerConfig.PARAM_IO_BACK_CONNECTION_TIMEOUT,0,Integer.MAX_VALUE,0);
    }

    /**
     * Get the timeout to use (set using a cluster/system property)
     *
     * @return the configured or default timeout.
     */
    private int getTimeout() {
        return getIntProperty(ServerConfig.PARAM_IO_BACK_READ_TIMEOUT,0,Integer.MAX_VALUE,0);
    }

    /**
     * Get a server config property using the configured min, max and default values.
     */
    private int getIntProperty(String propName, int min, int max, int defaultValue) {
        int value = defaultValue;

        try {
            String configuredValue = serverConfig.getPropertyCached(propName);
            if( configuredValue != null ) {
                value = Integer.parseInt(configuredValue);

                boolean useDefault = false;
                if(value<min) {
                    useDefault = true;
                    logger.warning("Configured value for property '"+propName+"', is BELOW the minimum '"+min+"', using default value '"+defaultValue+"'.");
                }
                else if(value>max) {
                    useDefault = true;
                    logger.warning("Configured value for property '"+propName+"', is ABOVE the maximum '"+max+"', using default value '"+defaultValue+"'.");
                }

                if(useDefault) value = defaultValue;
            }
        } catch(SecurityException se) {
            logger.warning("Cannot access property '"+propName+"', using default value '"+defaultValue+"', error is: " + se.getMessage());
        } catch(NumberFormatException nfe) {
            logger.warning("Cannot parse property '"+propName+"', using default value '"+defaultValue+"', error is: " + nfe.getMessage());
        }

        return value;
    }

    @Override
    public void onServiceCreated(final long serviceoid) {
        onServiceEnabled(serviceoid);
    }

    @Override
    public void onServiceDeleted(final long serviceoid) {
        onServiceDisabled(serviceoid);
    }

    public void pushMetricsNotifications(long when) {
        Map<SubscriptionKey, Pair<Subscription, MetricsSummaryBin>> metrics;
        try {
            metrics = subscriptionManager.findNotifiableMetricsForNode(clusterNodeId, when);
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to find notifications", e);
            return;
        }

        final List<ServiceStatusNotificationContext> callbacks = new ArrayList<ServiceStatusNotificationContext>();
        final long now = System.currentTimeMillis();
        for (Pair<Subscription,MetricsSummaryBin> subscriptionData : metrics.values()) {
            Subscription sub = subscriptionData.left;
            MetricsSummaryBin bin = subscriptionData.right;

            ServiceStatusNotificationContext ssnc = new ServiceStatusNotificationContext();
            ssnc.subscriptionId = sub.getUuid();
            ssnc.serviceId = sub.getPublishedServiceOid();
            ssnc.target = sub.getReferenceCallback();
            ssnc.refParamsXml = processReferenceParameters(sub);
            ssnc.notificationPolicyGuid = sub.getNotificationPolicyGuid();
            ssnc.ts = now;
            ssnc.bin = bin;
            callbacks.add(ssnc);
        }

        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                for (ServiceStatusNotificationContext ssnc : callbacks) {
                    try {
                        String notificationMessage = prepareNotificationForMetricsEvent(ssnc);
                        sendNotification(ssnc.target, notificationMessage, ssnc.notificationPolicyGuid);
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Unable to generate notification document", e);
                    }
                }

                try {
                    subscriptionManager.notified(clusterNodeId);
                } catch (ObjectModelException e) {
                    logger.log(Level.WARNING, "Unable to record notification status", e);
                }
            }
        });
    }

    private String processReferenceParameters(Subscription subscription) {
        String referenceParams = subscription.getReferenceParams();
        if (referenceParams == null) return "";
        StringBuilder result = new StringBuilder();

        Element paramElement = XmlUtil.findFirstChildElement(XmlUtil.stringAsDocument(referenceParams).getDocumentElement());
        while(paramElement != null) {
            paramElement.setAttributeNS(Namespaces.WSA, "IsReferenceParameter", "true");
            final BufferPoolByteArrayOutputStream out = new BufferPoolByteArrayOutputStream(1024);
            try {
                XmlUtil.nodeToOutputStreamWithXss4j(paramElement, out);
                result.append(out.toString("UTF-8"));
            } catch (IOException e) {
                auditor.logAndAudit(ServiceMessages.ESM_NOTIFY_IO_ERROR,
                        new String[]{subscription.getReferenceCallback(), subscription.getNotificationPolicyGuid(), ExceptionUtils.getMessage(e)},
                        ExceptionUtils.getDebugException(e) );
            } finally {
                out.close();
            }
            paramElement = XmlUtil.findNextElementSibling(paramElement);
        }

        return result.toString();
    }

    private String prepareNotificationForEnabledEvent(ServiceStatusNotificationContext ssnc, NotificationType what) throws IOException {
        String out = getResource(what == NotificationType.ENABLED ? "NotifyAvailable.xml" : "NotifyUnavailable.xml", ssnc);
        logger.info(MessageFormat.format("Service {0} notification generated {1}", what == NotificationType.ENABLED ? "enabled" : "disabled", ssnc.eventId));
        return out;
    }

    @SuppressWarnings({"StringConcatenationInsideStringBufferAppend"})
    private String prepareNotificationForMetricsEvent(ServiceStatusNotificationContext ssnc) throws IOException {
        String output = getResource("NotifyMetrics.xml", ssnc);
        logger.info("Metrics notification generated " + ssnc.eventId);
        long uptime = System.currentTimeMillis() - ssnc.bin.getStartTime(); // TODO get downtime from Metrics
        boolean operational;
        try {
            PublishedService ps = serviceCache.getCachedService(ssnc.serviceId);
            operational = !ps.isDisabled();
        } catch (Exception e) {
            logger.log(Level.WARNING, "error getting service from cache", e);
            operational = false;
        }
        MetricsRequestContext context = new MetricsRequestContext(ssnc.bin, operational, incomingUrl, uptime);

        StringBuffer metricsBuf = new StringBuffer();
        metricsBuf.append(
                "            <wsrf-rp:ResourcePropertyValueChangeNotification>\n" +
                "              <wsrf-rp:NewValues>\n" +
                QoSMetricsService.outputResourceID(context) +
                "              </wsrf-rp:NewValues>\n" +
                "            </wsrf-rp:ResourcePropertyValueChangeNotification>\n");
        metricsBuf.append(
                "            <wsrf-rp:ResourcePropertyValueChangeNotification>\n" +
                "              <wsrf-rp:NewValues>\n" +
                QoSMetricsService.outputOperationalStatus(context) +
                "              </wsrf-rp:NewValues>\n" +
                "            </wsrf-rp:ResourcePropertyValueChangeNotification>\n");
        metricsBuf.append(
                "            <wsrf-rp:ResourcePropertyValueChangeNotification>\n" +
                "              <wsrf-rp:NewValues>\n" +
                QoSMetricsService.outputNrRequests(context) +
                "              </wsrf-rp:NewValues>\n" +
                "            </wsrf-rp:ResourcePropertyValueChangeNotification>\n");
        metricsBuf.append(
                "            <wsrf-rp:ResourcePropertyValueChangeNotification>\n" +
                "              <wsrf-rp:NewValues>\n" +
                QoSMetricsService.outputNrSuccessRequests(context) +
                "              </wsrf-rp:NewValues>\n" +
                "            </wsrf-rp:ResourcePropertyValueChangeNotification>\n");
        metricsBuf.append(
                "            <wsrf-rp:ResourcePropertyValueChangeNotification>\n" +
                "              <wsrf-rp:NewValues>\n" +
                QoSMetricsService.outputNrFailedRequests(context) +
                "              </wsrf-rp:NewValues>\n" +
                "            </wsrf-rp:ResourcePropertyValueChangeNotification>\n");
        metricsBuf.append(
                "            <wsrf-rp:ResourcePropertyValueChangeNotification>\n" +
                "              <wsrf-rp:NewValues>\n" +
                QoSMetricsService.outputThroughput(context) +
                "              </wsrf-rp:NewValues>\n" +
                "            </wsrf-rp:ResourcePropertyValueChangeNotification>\n");
        metricsBuf.append(
                "            <wsrf-rp:ResourcePropertyValueChangeNotification>\n" +
                "              <wsrf-rp:NewValues>\n" +
                QoSMetricsService.outputAvgResponseTime(context) +
                "              </wsrf-rp:NewValues>\n" +
                "            </wsrf-rp:ResourcePropertyValueChangeNotification>\n");
        metricsBuf.append(
                "            <wsrf-rp:ResourcePropertyValueChangeNotification>\n" +
                "              <wsrf-rp:NewValues>\n" +
                QoSMetricsService.outputMaxResponseTime(context) +
                "              </wsrf-rp:NewValues>\n" +
                "            </wsrf-rp:ResourcePropertyValueChangeNotification>\n");
        metricsBuf.append(
                "            <wsrf-rp:ResourcePropertyValueChangeNotification>\n" +
                "              <wsrf-rp:NewValues>\n" +
                QoSMetricsService.outputLastResponseTime(context) +
                "              </wsrf-rp:NewValues>\n" +
                "            </wsrf-rp:ResourcePropertyValueChangeNotification>\n");
        metricsBuf.append(
                "            <wsrf-rp:ResourcePropertyValueChangeNotification>\n" +
                "              <wsrf-rp:NewValues>\n" +
                QoSMetricsService.outputServiceTime(context) +
                "              </wsrf-rp:NewValues>\n" +
                "            </wsrf-rp:ResourcePropertyValueChangeNotification>\n");

        output = output.replace("^$^$^_WRAPPED_METRICS_^$^$^", metricsBuf.toString());
        return output;
    }

    private String getResource(String resName, ServiceStatusNotificationContext ssnc) throws IOException {
        ClassLoader cl = SubscriptionNotifier.class.getClassLoader();
        String pathToLoad = "com/l7tech/server/wsdm/subscription/resources/" + resName;
        InputStream i = cl.getResourceAsStream(pathToLoad);
        if (i == null) {
            throw new FileNotFoundException(pathToLoad);
        }
        String output = new String(IOUtils.slurpStream(i));
        String baseURL = incomingUrl.toString();
        String notificationEventId = generateNewSubscriptionId();
        ssnc.eventId = notificationEventId;
        output = output.replace("^$^$^_WSA_TARGET_^$^$^", ssnc.target);
        output = output.replace("^$^$^_REFERENCE_PARAMS_^$^$^", ssnc.refParamsXml);
        output = output.replace("^$^$^_SUBSCRIPTION_ID_^$^$^", ssnc.subscriptionId);
        output = output.replace("^$^$^_ESM_SUBS_SVC_URL_^$^$^", baseURL + "/ssg/wsdm/esmsubscriptions");
        output = output.replace("^$^$^_EVENT_UUID_^$^$^", "urn:uuid:" + notificationEventId);
        output = output.replace("^$^$^_SRC_SVC_URL_^$^$^", baseURL + "/service/" + ssnc.serviceId);
        output = output.replace("^$^$^_NOW_TIMESTAMP_^$^$^", ISO8601Date.format(new Date(ssnc.ts)));
        return output;
    }

}

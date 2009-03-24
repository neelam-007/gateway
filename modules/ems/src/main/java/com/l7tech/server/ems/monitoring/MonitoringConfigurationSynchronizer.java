package com.l7tech.server.ems.monitoring;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.audit.AuditContextUtils;
import com.l7tech.server.ems.enterprise.JSONConstants;
import com.l7tech.server.ems.enterprise.SsgCluster;
import com.l7tech.server.ems.enterprise.SsgClusterManager;
import com.l7tech.server.ems.enterprise.SsgNode;
import com.l7tech.server.ems.gateway.GatewayContextFactory;
import com.l7tech.server.ems.gateway.GatewayException;
import com.l7tech.server.ems.gateway.ProcessControllerContext;
import com.l7tech.server.event.EntityChangeSet;
import com.l7tech.server.event.admin.Deleted;
import com.l7tech.server.event.admin.PersistenceEvent;
import com.l7tech.server.event.admin.Updated;
import com.l7tech.server.event.system.Started;
import com.l7tech.server.management.api.monitoring.BuiltinMonitorables;
import com.l7tech.server.management.api.monitoring.MonitorableProperty;
import com.l7tech.server.management.config.monitoring.MonitoringConfiguration;
import com.l7tech.server.management.config.monitoring.NotificationRule;
import com.l7tech.server.management.config.monitoring.PropertyTrigger;
import com.l7tech.server.management.config.monitoring.Trigger;
import com.l7tech.util.ComparisonOperator;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;
import com.l7tech.util.TimeSource;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sends the monitoring configuration to all known process controllers on ESM startup and whenever
 * the configuration changes.
 */
public class MonitoringConfigurationSynchronizer implements ApplicationListener {
    private static final Logger logger = Logger.getLogger(MonitoringConfigurationSynchronizer.class.getName());

    private static final String PROP_PREFIX = "com.l7tech.server.ems.monitoring.configPush.";
    static final long DELAY_UNTIL_FIRST_CONFIG_PUSH = SyspropUtil.getLong(PROP_PREFIX + "delayUntilFirst", 1637L);
    static final long DELAY_BETWEEN_CONFIG_PUSHES = SyspropUtil.getLong(PROP_PREFIX + "delayBetween", 7457L);
    static final long INITIAL_RETRY_DELAY = SyspropUtil.getLong(PROP_PREFIX + "failRetry.initialDelay", DELAY_BETWEEN_CONFIG_PUSHES + 1);
    static final long MAX_RETRY_DELAY = SyspropUtil.getLong(PROP_PREFIX + "failRetry.maxDelay", 86028157L);

    /** We should mark all monitoring configurations as dirty any time one of these entities changes. */
    private static final Set<Class<? extends Entity>> ENTITIES_TRIGGERING_COMPLETE_PUSHDOWN = Collections.unmodifiableSet(new HashSet<Class<? extends Entity>>() {{
        add(SystemMonitoringNotificationRule.class);
    }});

    /** We should mark all monitoring configurations as dirty any time one of these cluster properties changes. */
    private static final Set<String> CLUSTER_PROPS_TRIGGERING_COMPLETE_PUSHDOWN = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            ServerConfig.PARAM_SYSTEM_MONITORING_SETUP_SETTINGS
    )));

    private static final Set<String> RELEVANT_SSGNODE_PROPERTIES = new HashSet<String>(Arrays.asList(
            "name",
            "guid",
            "ipAddress",
            "ssgCluster"
    ));

    private static final Set<String> RELEVANT_SSGCLUSTER_PROPERTIES = new HashSet<String>(Arrays.asList(
            "name",
            "guid",
            "ipAddress",
            "sslHostName",
            "nodes"
    ));

    private final Timer timer;
    private final TimeSource timeSource;
    private final SsgClusterManager ssgClusterManager;
    private final GatewayContextFactory gatewayContextFactory;
    private final SsgClusterNotificationSetupManager ssgClusterNotificationSetupManager;
    private final EntityMonitoringPropertySetupManager entityMonitoringPropertySetupManager;
    private final SystemMonitoringSetupSettingsManager systemMonitoringSetupSettingsManager;
    private final TimerTask configPusherTask = makeConfigPusherTask();

    // Contains an entry for each cluster GUID that has the latest monitoring configuration.
    private final ConcurrentHashMap<String, Object> cleanClusters = new ConcurrentHashMap<String, Object>();

    // Contains an entry for each cluster GUID that has failed to have its config pushed down to at least one node.
    private final ConcurrentHashMap<String, ClusterPushdownFailure> failedClusters = new ConcurrentHashMap<String, ClusterPushdownFailure>();

    // If true, all retry backoff delays will be ignored for the very next pushdown pass.
    private final AtomicBoolean suspendBackoffForNextRun = new AtomicBoolean(true);

    public MonitoringConfigurationSynchronizer(Timer timer,
                                               TimeSource timeSource,
                                               SsgClusterManager ssgClusterManager,
                                               GatewayContextFactory gatewayContextFactory,
                                               SsgClusterNotificationSetupManager ssgClusterNotificationSetupManager,
                                               EntityMonitoringPropertySetupManager entityMonitoringPropertySetupManager,
                                               SystemMonitoringSetupSettingsManager systemMonitoringSetupSettingsManager)
    {
        this.timer = timer;
        this.timeSource = timeSource != null ? timeSource : new TimeSource();
        this.ssgClusterManager = ssgClusterManager;
        this.gatewayContextFactory = gatewayContextFactory;
        this.ssgClusterNotificationSetupManager = ssgClusterNotificationSetupManager;
        this.entityMonitoringPropertySetupManager = entityMonitoringPropertySetupManager;
        this.systemMonitoringSetupSettingsManager = systemMonitoringSetupSettingsManager;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof PersistenceEvent) {
            PersistenceEvent pe = (PersistenceEvent) event;
            Entity entity = pe.getEntity();
            if (entity instanceof EntityMonitoringPropertySetup) {
                EntityMonitoringPropertySetup setup = (EntityMonitoringPropertySetup) entity;
                String clusterGuid = setup.getSsgClusterNotificationSetup().getSsgClusterGuid();
                setClusterDirty(clusterGuid, null);
                suspendBackoffForNextRun.set(true);
            } else if (entity instanceof SsgClusterNotificationSetup) {
                SsgClusterNotificationSetup sgns = (SsgClusterNotificationSetup) entity;
                setClusterDirty(sgns.getSsgClusterGuid(), null);
                suspendBackoffForNextRun.set(true);
            } else if (entity instanceof SsgCluster) {
                final SsgCluster cluster = (SsgCluster) entity;
                if (event instanceof Updated && isIgnorableUpdate((Updated) event, RELEVANT_SSGCLUSTER_PROPERTIES))
                    return;
                if (event instanceof Deleted)
                    for (SsgNode node : cluster.getNodes())
                        onNodeDeleted(node);
                setClusterDirty(cluster.getGuid(), cluster.getName());
                suspendBackoffForNextRun.set(true);
            } else if (entity instanceof SsgNode) {
                if (event instanceof Updated && isIgnorableUpdate((Updated) event, RELEVANT_SSGNODE_PROPERTIES))
                    return;
                if (event instanceof Deleted)
                    onNodeDeleted((SsgNode)entity); // Turn off monitoring config before we forget all about node
                final SsgCluster cluster = ((SsgNode) entity).getSsgCluster();
                setClusterDirty(cluster.getGuid(), cluster.getName());
                suspendBackoffForNextRun.set(true);
            } else if (entity instanceof ClusterProperty) {
                String name = ((ClusterProperty)entity).getName();
                if (CLUSTER_PROPS_TRIGGERING_COMPLETE_PUSHDOWN.contains(name))
                    setAllDirty();
                suspendBackoffForNextRun.set(true);
            } else if (entity != null) {
                for (Class<? extends Entity> entClass : ENTITIES_TRIGGERING_COMPLETE_PUSHDOWN) {
                    if (entClass.isAssignableFrom(entity.getClass())) {
                        setAllDirty();
                        return;
                    }
                }
            }
        } else if (event instanceof Started) {
            Started started = (Started) event;
            if (Component.ENTERPRISE_MANAGER.equals(started.getComponent()))
                start();
        }
    }

    private static final String NODEINFO = "{0} for node {1} ({2}) of cluster {3} ({4})";
    private static final String NOFINALPUSH = "Unable to push down monitoring configuration to " + NODEINFO;
    private static final String NOFINALPUSHNET = NOFINALPUSH + " due to network error";

    // Schedule a one-shot task to tell the specified node to forget its monitoring configuration.
    private void onNodeDeleted(final SsgNode node) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                final SsgCluster cluster = node.getSsgCluster();
                final String[] nodeInfo = { node.getIpAddress(), node.getName(), node.getGuid(), cluster.getName(), cluster.getGuid() };
                try {
                    logger.log(Level.INFO, "Ordering final purge of monitoring configuration on " + NODEINFO, nodeInfo);
                    gatewayContextFactory.createProcessControllerContext(node).getMonitoringApi().pushMonitoringConfiguration(null);
                } catch (IOException e) {
                    logPush(Level.INFO, NOFINALPUSH, nodeInfo, e, false);
                } catch (GatewayException e) {
                    logPush(Level.WARNING, NOFINALPUSH, nodeInfo, e, false);
                } catch (javax.xml.ws.ProtocolException e) {
                    if ( ProcessControllerContext.isNetworkException(e) ) {
                        logPush(Level.WARNING, NOFINALPUSHNET, nodeInfo, ExceptionUtils.unnestToRoot(e), false);
                    } else {
                        logPush(Level.WARNING, NOFINALPUSH, nodeInfo, e, false);
                    }
                } catch (Throwable t) {
                    logPush(Level.WARNING, NOFINALPUSH, nodeInfo, t, true);
                }
            }
        }, DELAY_BETWEEN_CONFIG_PUSHES + DELAY_UNTIL_FIRST_CONFIG_PUSH);
    }

    private boolean isIgnorableUpdate(Updated updated, Set<String> relevantProps) {
        EntityChangeSet cs = updated.getChangeSet();
        if (cs == null || cs.getNumProperties() == 0)
            return false;

        final Iterator<String> iterator = cs.getProperties();
        while (iterator.hasNext()) {
            String propName = iterator.next();

            // Ignore the property if it didn't actually change
            Object old = cs.getOldValue(propName);
            Object nval = cs.getNewValue(propName);
            if ((old == null && nval == null) || (old != null && old.equals(nval)))
                continue;

            if (relevantProps.contains(propName))
                return false;
        }

        // Didn't see any relevant properties, ignore it
        return true;
    }

    private void start() {
        timer.schedule(configPusherTask, DELAY_UNTIL_FIRST_CONFIG_PUSH, DELAY_BETWEEN_CONFIG_PUSHES);
    }

    private TimerTask makeConfigPusherTask() {
        return new TimerTask() {
            @Override
            public void run() {
                AuditContextUtils.doAsSystem(new Runnable() {
                    @Override
                    public void run() {
                        pushAllConfig();
                    }
                });
            }
        };
    }

    private void pushAllConfig() {
        boolean wasImmediate = suspendBackoffForNextRun.get();
        try {
            doPushAllConfig(wasImmediate);
        } catch (ObjectModelException e) {
            logger.log(Level.WARNING, "Exception while pushing down monitoring configuration: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unexpected exception while pushing down monitoring configuration: " + ExceptionUtils.getMessage(e), e);
        } catch (Throwable t) {
            // Try to log and continue to keep periodic task from dying completely
            logger.log(Level.SEVERE, "Unexpected error while pushing down monitoring configuration: " + ExceptionUtils.getMessage(t), t);
        } finally {
            if (wasImmediate) suspendBackoffForNextRun.set(false);
        }
    }

    private void doPushAllConfig(boolean suspendBackoffTimers) throws FindException {
        boolean notificationsDisabled = areNotificationsGloballyDisabled();

        Collection<SsgCluster> clusters = ssgClusterManager.findAll();
        for (SsgCluster cluster : clusters) {
            final String clusterGuid = cluster.getGuid();
            if (clusterNeedsPushdown(clusterGuid, suspendBackoffTimers)) {
                boolean success = false;
                try {
                    success = configureCluster(cluster, notificationsDisabled);
                } finally {
                    onClusterPushdownAttempt(clusterGuid, cluster.getName(), success);
                }
            }
        }
    }

    private boolean configureCluster(SsgCluster cluster, boolean notificationsDisabled) {
        boolean hugeSuccess = true;
        boolean needClusterMaster = true;
        Set<SsgNode> nodes = cluster.getNodes();
        for (SsgNode node : nodes) {
            if (configureNode(cluster, node, notificationsDisabled, needClusterMaster))
                needClusterMaster = false;
            else
                hugeSuccess = false;
        }
        return hugeSuccess;
    }

    private static final String NOPUSH = "Unable to push down monitoring configuration to " + NODEINFO;
    private static final String NOPUSHNET = NOPUSH + " due to network error";

    private boolean configureNode(SsgCluster cluster, SsgNode node, boolean notificationsDisabled, boolean needClusterMaster) {
        final String[] nodeInfo = { node.getIpAddress(), node.getName(), node.getGuid(), cluster.getName(), cluster.getGuid() };
        try {
            logger.log(Level.FINE, "Pushing down monitoring configuration to " + NODEINFO, nodeInfo);
            doConfigureNode(cluster, node, notificationsDisabled, needClusterMaster);
            logger.log(Level.INFO, "Pushed down monitoring configuration to " + NODEINFO, nodeInfo);
            return true;
        } catch (IOException e) {
            logPush(Level.INFO, NOPUSH, nodeInfo, e, false);
        } catch (GatewayException e) {
            logPush(Level.WARNING, NOPUSH, nodeInfo, e, false);
        } catch (javax.xml.ws.ProtocolException e) {
            if ( ProcessControllerContext.isNetworkException(e) ) {
                logPush(Level.WARNING, NOPUSHNET, nodeInfo, ExceptionUtils.unnestToRoot(e), false);
            } else {
                logPush(Level.WARNING, NOPUSH, nodeInfo, e, false);
            }
        } catch (Exception e) {
            logPush(Level.WARNING, NOPUSH, nodeInfo, e, true);
        }
        return false;
    }

    private void logPush(Level level, String baseTemplate, String[] subst, Throwable t, boolean includeStack) {
        logger.log(level, MessageFormat.format(baseTemplate, subst) + ": " + ExceptionUtils.getMessage(t), includeStack ? t : ExceptionUtils.getDebugException(t));
    }

    private void doConfigureNode(SsgCluster cluster, SsgNode node, boolean notificationsDisabled, boolean needClusterMaster) throws GatewayException, IOException, FindException {
        ProcessControllerContext ctx = gatewayContextFactory.createProcessControllerContext(node);
        MonitoringConfiguration config = makeMonitoringConfiguration(cluster, node, notificationsDisabled, needClusterMaster);
        ctx.getMonitoringApi().pushMonitoringConfiguration(config);
    }

    private MonitoringConfiguration makeMonitoringConfiguration(SsgCluster cluster, SsgNode node, boolean notificationsDisabled, boolean responsibleForClusterMonitoring) throws FindException {
        MonitoringConfiguration config = new MonitoringConfiguration();
        config.setName(node.getName());
        config.setOid(node.getOid());
        config.setVersion(node.getVersion());
        config.setResponsibleForClusterMonitoring(responsibleForClusterMonitoring);

        SsgClusterNotificationSetup clusterSetup = ssgClusterNotificationSetupManager.findByEntityGuid(cluster.getGuid());
        Map<Long, NotificationRule> notRules = clusterSetup==null ?
                Collections.<Long, NotificationRule>emptyMap() :
                convertNotificationRules(notificationsDisabled, clusterSetup.getSystemNotificationRules());
        config.setNotificationRules(new HashSet<NotificationRule>(notRules.values()));

        String componentId = cluster.getSslHostName();

        Collection<Trigger> clusterTriggers =
                convertTriggers(notificationsDisabled,
                        notRules,
                        entityMonitoringPropertySetupManager.findByEntityGuid(cluster.getGuid()),
                        componentId);

        Collection<Trigger> hostTrigger =
                convertTriggers(notificationsDisabled,
                        notRules,
                        entityMonitoringPropertySetupManager.findByEntityGuid(node.getGuid()),
                        componentId);

        Set<Trigger> triggers = new HashSet<Trigger>();
        triggers.addAll(clusterTriggers);
        triggers.addAll(hostTrigger);
        config.setTriggers(triggers);

        return config;
    }

    // notificationRules is map of SystemMonitoringNotificationRule OID => NotificationRule instance
    private Collection<Trigger> convertTriggers(boolean notificationsDisabled,
                                                Map<Long, NotificationRule> notificationRules,
                                                List<EntityMonitoringPropertySetup> setups,
                                                String componentId)
    {
        Collection<Trigger> ret = new ArrayList<Trigger>();

        for (EntityMonitoringPropertySetup setup : setups) {
            Trigger trigger = convertTrigger(notificationsDisabled, notificationRules, setup, componentId);
            if (trigger != null)
                ret.add(trigger);
        }

        return ret;
    }

    Trigger convertTrigger(boolean notificationsDisabled,
                           Map<Long, NotificationRule> notificationRules,
                           EntityMonitoringPropertySetup setup,
                           String componentId)
    {
        if (!setup.isMonitoringEnabled())
            return null;

        String propertyName = setup.getPropertyType();
        MonitorableProperty property = BuiltinMonitorables.getAtMostOneBuiltinPropertyByName(propertyName);
        if (property == null) {
            logger.warning("Ignoring PC trigger for unrecognized property name: " + propertyName);
            return null;
        }
        final Long value = setup.getTriggerValue();
        final ComparisonOperator operator;
        final String triggerValue;

        operator = setup.isTriggerEnabled() ? property.getSuggestedComparisonOperator() : ComparisonOperator.FALSE;
        triggerValue = value != null ? Long.toString(value) : property.getSuggestedComparisonValue();

        long maxSamplingInterval = 5000L; // TODO is this the same value from monitoring.samplingInterval.lowerLimit in emconfig.properties that default to 2 sec?

        PropertyTrigger trigger = new PropertyTrigger(property, componentId, operator, triggerValue, maxSamplingInterval);
        trigger.setOid(setup.getOid());
        trigger.setVersion(setup.getVersion());
        trigger.setNotificationRules(lookupNotificationRules(notificationsDisabled, setup, notificationRules, propertyName));
        return trigger;
    }

    // notificationRules is map of SystemMonitoringNotificationRule OID => NotificationRule instance
    private static List<NotificationRule> lookupNotificationRules(boolean notificationsDisabled,
                                                                  EntityMonitoringPropertySetup entityMonitoringPropertySetup,
                                                                  Map<Long, NotificationRule> notificationRules,
                                                                  String propertyName)
    {
        if (notificationsDisabled || !entityMonitoringPropertySetup.isNotificationEnabled())
            return Collections.emptyList();

        Set<SystemMonitoringNotificationRule> rules = entityMonitoringPropertySetup.getSsgClusterNotificationSetup().getSystemNotificationRules();
        List<NotificationRule> notRules = new ArrayList<NotificationRule>();
        for (SystemMonitoringNotificationRule rule : rules) {
            NotificationRule notRule = notificationRules.get(rule.getOid());
            if (notRule != null) {
                notRules.add(notRule);
            } else {
                if (logger.isLoggable(Level.WARNING))
                    logger.log(Level.WARNING, MessageFormat.format("Trigger on monitorable property {0} refers to unavailable notification rule with OID {2}", propertyName, rule.getOid()));
            }

        }
        return notRules;
    }

    // returns map of SystemMonitoringNotificationRule OID => NotificationRule instance
    private Map<Long, NotificationRule> convertNotificationRules(boolean notificationsDisabled, Set<SystemMonitoringNotificationRule> systemNotificationRules) {
        Map<Long, NotificationRule> rules = new HashMap<Long, NotificationRule>();
        if (!notificationsDisabled)
            for (SystemMonitoringNotificationRule rule : systemNotificationRules)
                rules.put(rule.getOid(), rule.asNotificationRule());
        return rules;
    }

    private boolean areNotificationsGloballyDisabled() {
        try {
            Map<String, Object> globalSettings = systemMonitoringSetupSettingsManager.findSetupSettings();
            return Boolean.valueOf(String.valueOf(globalSettings.get(JSONConstants.SystemMonitoringSetup.DISABLE_ALL_NOTIFICATIONS)));
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to get monitoring settings: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        } catch (InvalidMonitoringSetupSettingException e) {
            logger.log(Level.WARNING, "Unable to get monitoring settings: " + ExceptionUtils.getMessage(e), e);
        }
        return false;
    }

    /**
     * Check if the specified cluster GUID is in need of a fresh monitoring configuration.
     *
     * @param clusterGuid the cluster to check.  Required.
     * @return true if this cluster needs a new config.
     */
    boolean isClusterDirty(String clusterGuid) {
        return !cleanClusters.containsKey(clusterGuid);
    }

    /**
     * Mark all monitoring configurations in the specified cluster as dirty.
     * <p/>
     * The synchronizer will push down new configuration to all PCs in this cluster next time it runs.
     *
     * @param clusterGuid GUID of a cluster whose PCs are in need of new monitoring configuration.
     * @param clusterName name of cluster, if known
     */
    void setClusterDirty(String clusterGuid, String clusterName) {
        if ( cleanClusters.remove(clusterGuid) != null ) {
            String name = clusterName == null ? "" : " (" + clusterName + ")";
            logger.log(Level.INFO, "Marking cluster GUID " + clusterGuid + name + " as in need of a monitoring configuration pushdown");
        }
    }

    /**
     * Mark all monitoring configurations in the specified cluster as clean.
     *
     * @param clusterGuid GUID of a cluster whose PCs have the latest monitoring configuraiton.
     */
    void setClusterClean(String clusterGuid) {
        cleanClusters.put(clusterGuid, Boolean.TRUE);
    }

    /**
     * Mark all monitoring configurations in all known clusters as dirty.
     * <p/>
     * The synchronizer will push down new configuration to all PCs in all clusters next time it runs.
     */
    void setAllDirty() {
        logger.log(Level.INFO, "Marking all known clusters as in need of a monitoring configuration pushdown");
        cleanClusters.clear();
    }

    private void onClusterPushdownAttempt(String clusterGuid, String clusterName, boolean success) {
        if (success) {
            setClusterClean(clusterGuid);
            failedClusters.remove(clusterGuid);
        } else {
            setClusterDirty(clusterGuid, clusterName);
            failedClusters.put(clusterGuid, new ClusterPushdownFailure(failedClusters.get(clusterGuid), clusterGuid, timeSource.currentTimeMillis()));
        }
    }

    private boolean clusterNeedsPushdown(String clusterGuid, boolean suspendBackoffTimers) {
        if (!isClusterDirty(clusterGuid))
            return false;
        if (suspendBackoffTimers)
            return true;
        ClusterPushdownFailure lastFailure = failedClusters.get(clusterGuid);
        return lastFailure == null || lastFailure.failureTime + lastFailure.delayMillis <= timeSource.currentTimeMillis();
    }

    private static class ClusterPushdownFailure {
        final String clusterGuid;
        final long failureTime;
        final long delayMillis;

        private ClusterPushdownFailure(ClusterPushdownFailure lastFailure, String clusterGuid, long failureTime) {
            if (clusterGuid == null) throw new NullPointerException("clusterGuid");
            this.clusterGuid = clusterGuid;
            this.failureTime = failureTime;
            this.delayMillis = lastFailure == null ? INITIAL_RETRY_DELAY : Math.min(lastFailure.delayMillis * 2, MAX_RETRY_DELAY);
        }
    }
}

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
import com.l7tech.server.event.admin.PersistenceEvent;
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
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sends the monitoring configuration to all known process controllers on ESM startup and whenever
 * the configuration changes.
 */
public class MonitoringConfigurationSynchronizer implements ApplicationListener {
    private static final Logger logger = Logger.getLogger(MonitoringConfigurationSynchronizer.class.getName());

    private static final long DELAY_UNTIL_FIRST_CONFIG_PUSH = SyspropUtil.getLong("com.l7tech.server.ems.monitoring.configPush.delayUntilFirst", 1637L);
    private static final long DELAY_BETWEEN_CONFIG_PUSHES = SyspropUtil.getLong("com.l7tech.server.ems.monitoring.configPush.delayBetween", 7457L);

    /** We should mark all monitoring configurations as dirty any time one of these entities changes. */
    private static final Set<Class<? extends Entity>> ENTITIES_TRIGGERING_COMPLETE_PUSHDOWN = Collections.unmodifiableSet(new HashSet<Class<? extends Entity>>() {{
        add(SystemMonitoringNotificationRule.class);
    }});

    /** We should mark all monitoring configurations as dirty any time one of these cluster properties changes. */
    private static final Set<String> CLUSTER_PROPS_TRIGGERING_COMPLETE_PUSHDOWN = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            ServerConfig.PARAM_SYSTEM_MONITORING_SETUP_SETTINGS
    )));

    private final Timer timer;
    private final SsgClusterManager ssgClusterManager;
    private final GatewayContextFactory gatewayContextFactory;
    private final SsgClusterNotificationSetupManager ssgClusterNotificationSetupManager;
    private final EntityMonitoringPropertySetupManager entityMonitoringPropertySetupManager;
    private final SystemMonitoringSetupSettingsManager systemMonitoringSetupSettingsManager;
    private final TimerTask configPusherTask = makeConfigPusherTask();

    // Contains an entry for each cluster GUID that has the latest monitoring configuration.
    private final ConcurrentHashMap<String, Object> cleanClusters = new ConcurrentHashMap<String, Object>();

    public MonitoringConfigurationSynchronizer(Timer timer,
                                               SsgClusterManager ssgClusterManager,
                                               GatewayContextFactory gatewayContextFactory,
                                               SsgClusterNotificationSetupManager ssgClusterNotificationSetupManager,
                                               EntityMonitoringPropertySetupManager entityMonitoringPropertySetupManager,
                                               SystemMonitoringSetupSettingsManager systemMonitoringSetupSettingsManager)
    {
        this.timer = timer;
        this.ssgClusterManager = ssgClusterManager;
        this.gatewayContextFactory = gatewayContextFactory;
        this.ssgClusterNotificationSetupManager = ssgClusterNotificationSetupManager;
        this.entityMonitoringPropertySetupManager = entityMonitoringPropertySetupManager;
        this.systemMonitoringSetupSettingsManager = systemMonitoringSetupSettingsManager;
    }

    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof PersistenceEvent) {
            PersistenceEvent pe = (PersistenceEvent) event;
            Entity entity = pe.getEntity();
            if (entity instanceof EntityMonitoringPropertySetup) {
                EntityMonitoringPropertySetup setup = (EntityMonitoringPropertySetup) entity;
                String clusterGuid = setup.getSsgClusterNotificationSetup().getSsgClusterGuid();
                setClusterDirty(clusterGuid);
            } else if (entity instanceof SsgClusterNotificationSetup) {
                SsgClusterNotificationSetup sgns = (SsgClusterNotificationSetup) entity;
                setClusterDirty(sgns.getSsgClusterGuid());
            } else if (entity instanceof SsgCluster) {
                setClusterDirty(((SsgCluster)entity).getGuid());
            } else if (entity instanceof SsgNode) {
                setClusterDirty(((SsgNode)entity).getSsgCluster().getGuid());
            } else if (entity instanceof ClusterProperty) {
                String name = ((ClusterProperty)entity).getName();
                if (CLUSTER_PROPS_TRIGGERING_COMPLETE_PUSHDOWN.contains(name))
                    setAllDirty();
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

    private void start() {
        timer.schedule(configPusherTask, DELAY_UNTIL_FIRST_CONFIG_PUSH, DELAY_BETWEEN_CONFIG_PUSHES);
    }

    private TimerTask makeConfigPusherTask() {
        return new TimerTask() {
            public void run() {
                AuditContextUtils.doAsSystem(new Runnable() {
                    public void run() {
                        pushAllConfig();
                    }
                });
            }
        };
    }

    private void pushAllConfig() {
        try {
            doPushAllConfig();
        } catch (ObjectModelException e) {
            logger.log(Level.WARNING, "Exception while pushing down monitoring configuration: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unexpected exception while pushing down monitoring configuration: " + ExceptionUtils.getMessage(e), e);
        } catch (Throwable t) {
            // Try to log and continue to keep periodic task from dying completely
            logger.log(Level.SEVERE, "Unexpected error while pushing down monitoring configuration: " + ExceptionUtils.getMessage(t), t);
        }
    }

    private void doPushAllConfig() throws FindException {
        boolean notificationsDisabled = areNotificationsGloballyDisabled();

        Collection<SsgCluster> clusters = ssgClusterManager.findAll();
        for (SsgCluster cluster : clusters) {
            final String clusterGuid = cluster.getGuid();
            if (isClusterDirty(clusterGuid)) {
                setClusterClean(clusterGuid);
                if (!configureCluster(cluster, notificationsDisabled))
                    setClusterDirty(clusterGuid);
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

    private boolean configureNode(SsgCluster cluster, SsgNode node, boolean notificationsDisabled, boolean needClusterMaster) {
        try {
            logger.log(Level.INFO, "Pushing down monitoring configuration to " + node.getIpAddress());
            doConfigureNode(cluster, node, notificationsDisabled, needClusterMaster);
            return true;
        } catch (IOException e) {
            logger.log(Level.INFO, "Unable to push down monitoring configuration to node " + node.getIpAddress() + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        } catch (GatewayException e) {
            logger.log(Level.WARNING, "Unable to push down monitoring configuration to node " + node.getIpAddress() + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        } catch (javax.xml.ws.ProtocolException e) {
            if ( ProcessControllerContext.isNetworkException(e) ) {
                logger.log(Level.WARNING, "Unable to push down monitoring configuration to node " + node.getIpAddress() + " due to network error: " + ExceptionUtils.getMessage(ExceptionUtils.unnestToRoot(e)), ExceptionUtils.getDebugException(e));
            } else {
                logger.log(Level.WARNING, "Unable to push down monitoring configuration to node " + node.getIpAddress() + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to push down monitoring configuration to node " + node.getIpAddress() + ": " + ExceptionUtils.getMessage(e), e);
        }
        return false;
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
        if (value == null) {
            operator = property.getSuggestedComparisonOperator();
            triggerValue = property.getSuggestedComparisonValue();
        } else {
            operator = ComparisonOperator.GE;
            triggerValue = Long.toString(value);
        }

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
     */
    void setClusterDirty(String clusterGuid) {
        logger.log(Level.INFO, "Marking cluster GUID " + clusterGuid + " as in need of a monitoring configuration pushdown");
        cleanClusters.remove(clusterGuid);
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
}

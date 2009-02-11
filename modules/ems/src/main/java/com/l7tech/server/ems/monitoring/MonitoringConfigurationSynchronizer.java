package com.l7tech.server.ems.monitoring;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ems.enterprise.JSONConstants;
import com.l7tech.server.ems.enterprise.SsgCluster;
import com.l7tech.server.ems.enterprise.SsgClusterManager;
import com.l7tech.server.ems.enterprise.SsgNode;
import com.l7tech.server.ems.gateway.GatewayContext;
import com.l7tech.server.ems.gateway.GatewayContextFactory;
import com.l7tech.server.ems.gateway.GatewayException;
import com.l7tech.server.event.admin.PersistenceEvent;
import com.l7tech.server.event.system.Started;
import com.l7tech.server.management.config.monitoring.MonitoringConfiguration;
import com.l7tech.server.management.config.monitoring.NotificationRule;
import com.l7tech.server.management.config.monitoring.PropertyTrigger;
import com.l7tech.server.management.config.monitoring.Trigger;
import com.l7tech.util.ExceptionUtils;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sends the monitoring configuration to all known process controllers on ESM startup and whenever
 * the configuration changes.
 */
public class MonitoringConfigurationSynchronizer extends TimerTask implements ApplicationListener {
    private static final Logger logger = Logger.getLogger(MonitoringConfigurationSynchronizer.class.getName());

    private final SsgClusterManager ssgClusterManager;
    private final GatewayContextFactory gatewayContextFactory;
    private final SsgClusterNotificationSetupManager ssgClusterNotificationSetupManager;
    private final EntityMonitoringPropertySetupManager entityMonitoringPropertySetupManager;
    private final Timer timer = new Timer();
    private final AtomicBoolean dirty = new AtomicBoolean(true);

    public MonitoringConfigurationSynchronizer(SsgClusterManager ssgClusterManager,
                                               GatewayContextFactory gatewayContextFactory,
                                               SsgClusterNotificationSetupManager ssgClusterNotificationSetupManager,
                                               EntityMonitoringPropertySetupManager entityMonitoringPropertySetupManager)
    {
        this.ssgClusterManager = ssgClusterManager;
        this.gatewayContextFactory = gatewayContextFactory;
        this.ssgClusterNotificationSetupManager = ssgClusterNotificationSetupManager;
        this.entityMonitoringPropertySetupManager = entityMonitoringPropertySetupManager;
    }

    /** We should mark all monitoring configurations as dirty any time one of these entities changes. */
    private final Set<Class<? extends Entity>> MONITORING_ENTITIES = new HashSet<Class<? extends Entity>>() {{
        add(SystemMonitoringNotificationRule.class);
        add(SsgClusterNotificationSetup.class);
        add(EntityMonitoringPropertySetup.class);
    }};

    /** We should mark all monitoring configurations as dirty any time one of these cluster properties changes. */
    // TODO It may turn out that the PCs don't care about these settings after all
    private final Set<String> MONITORING_CLUSTER_PROPS = new HashSet<String>(Arrays.asList(
            ServerConfig.PARAM_SYSTEM_MONITORING_SETUP_SETTINGS
    ));

    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof PersistenceEvent) {
            PersistenceEvent pe = (PersistenceEvent) event;
            Entity entity = pe.getEntity();
            if (entity instanceof EntityMonitoringPropertySetup) {
                EntityMonitoringPropertySetup setup = (EntityMonitoringPropertySetup) entity;
                String clusterGuid = setup.getSsgClusterNotificationSetup().getSsgClusterGuid();
                setClusterDirty(clusterGuid);
            } else if (entity instanceof ClusterProperty) {
                String name = ((ClusterProperty)entity).getName();
                if (MONITORING_CLUSTER_PROPS.contains(name))
                    setAllDirty();
            } else if (entity != null) {
                for (Class<? extends Entity> entClass : MONITORING_ENTITIES) {
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
        timer.schedule(this, 1000L, 15000L);
    }

    public void run() {
        if (!dirty.get())
            return;

        try {
            if (pushAllConfig())
                dirty.set(false);
        } catch (ObjectModelException e) {
            logger.log(Level.WARNING, "Exception while pushing down monitoring configuration: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unexpected exception while pushing down monitoring configuration: " + ExceptionUtils.getMessage(e), e);
        } catch (Throwable t) {
            // Try to log and continue to keep periodic task from dying completely
            logger.log(Level.SEVERE, "Unexpected error while pushing down monitoring configuration: " + ExceptionUtils.getMessage(t), t);
        }
    }

    private boolean pushAllConfig() throws FindException {
        logger.info("Pushing down monitoring configurations to all known process controllers");

        boolean sawFailure = false;
        Collection<SsgCluster> clusters = ssgClusterManager.findAll();
        for (SsgCluster cluster : clusters) {
            boolean needClusterMaster = true;
            Set<SsgNode> nodes = cluster.getNodes();
            for (SsgNode node : nodes) {
                if (configureNode(cluster, node, needClusterMaster))
                    needClusterMaster = false;
                else
                    sawFailure = true;
            }
        }
        return !sawFailure;
    }

    private boolean configureNode(SsgCluster cluster, SsgNode node, boolean needClusterMaster) {
        try {
            doConfigureNode(cluster, node, needClusterMaster);
            return true;
        } catch (IOException e) {
            logger.log(Level.INFO, "Unable to push down monitoring configuration to node " + node.getIpAddress() + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        } catch (GatewayException e) {
            logger.log(Level.WARNING, "Unable to push down monitoring configuration to node " + node.getIpAddress() + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        } catch (javax.xml.ws.soap.SOAPFaultException e) {
            logger.log(Level.WARNING, "Unable to push down monitoring configuration to node " + node.getIpAddress() + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to push down monitoring configuration to node " + node.getIpAddress() + ": " + ExceptionUtils.getMessage(e), e);
        }
        return false;
    }

    private void doConfigureNode(SsgCluster cluster, SsgNode node, boolean needClusterMaster) throws GatewayException, IOException, FindException {
        GatewayContext ctx = gatewayContextFactory.getGatewayContext(null, node.getIpAddress(), 0);
        MonitoringConfiguration config = makeMonitoringConfiguration(cluster, node);
        ctx.getMonitoringApi().pushMonitoringConfiguration(config, needClusterMaster);
    }

    private MonitoringConfiguration makeMonitoringConfiguration(SsgCluster cluster, SsgNode node) throws FindException {
        MonitoringConfiguration config = new MonitoringConfiguration();
        config.setName(node.getName());

        SsgClusterNotificationSetup clusterSetup = ssgClusterNotificationSetupManager.findByEntityGuid(cluster.getGuid());
        config.setNotificationRules(convertNotificationRules(config, clusterSetup.getSystemNotificationRules()));

        Set<Trigger> triggers = new HashSet<Trigger>();
        triggers.addAll(convertTriggers(entityMonitoringPropertySetupManager.findByEntityGuid(cluster.getGuid())));
        triggers.addAll(convertTriggers(entityMonitoringPropertySetupManager.findByEntityGuid(node.getGuid())));
        config.setTriggers(triggers);

        return config;
    }

    private Collection<? extends Trigger> convertTriggers(List<EntityMonitoringPropertySetup> clusterSetups) {
        Collection<? extends Trigger> ret = new ArrayList<Trigger>();

        for (EntityMonitoringPropertySetup setup : clusterSetups) {
            PropertyTrigger trigger = new PropertyTrigger(); // TODO ???
            // TODO build a PropertyTrigger somehow 
        }

        return ret;
    }

    private Set<NotificationRule> convertNotificationRules(MonitoringConfiguration config, Set<SystemMonitoringNotificationRule> systemNotificationRules) {
        Set<NotificationRule> rules = new HashSet<NotificationRule>();
        for (SystemMonitoringNotificationRule rule : systemNotificationRules) {
            final String ruleType = rule.getType();
            if (JSONConstants.NotificationType.E_MAIL.equals(ruleType)) {
                rules.add(rule.asEmailNotificationRule(config));
            } else if (JSONConstants.NotificationType.SNMP_TRAP.equals(ruleType)) {
                rules.add(rule.asSnmpTrapNotificationRule(config));
            } else if (JSONConstants.NotificationType.HTTP_REQUEST.equals(ruleType)) {
                rules.add(rule.asHttpNotificationRule(config));
            } else {
                logger.log(Level.WARNING, "Ignoring configured notification rule of unrecognized type: " + ruleType);
            }
        }
        return rules;
    }

    /**
     * Mark all monitoring configurations in the specified cluster as dirty.
     * <p/>
     * The synchronizer will push down new configuration to all PCs in this cluster next time it runs.
     *
     * @param clusterGuid GUID of a cluster whose PCs are in need of new monitoring configuration.
     */
    void setClusterDirty(String clusterGuid) {
        // TODO dirty only the affected cluster
        setAllDirty();
    }

    /**
     * Mark all monitoring configurations in all known clusters as dirty.
     * <p/>
     * The synchronizer will push down new configuration to all PCs in all clusters next time it runs.
     */
    void setAllDirty() {
        // TODO keep track of dirtiness per PC GUID
        dirty.set(false);
    }
}

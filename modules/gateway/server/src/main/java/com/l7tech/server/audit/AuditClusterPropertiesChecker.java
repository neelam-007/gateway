package com.l7tech.server.audit;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.server.event.GoidEntityInvalidationEvent;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.event.system.SystemEvent;
import com.l7tech.server.util.PostStartupApplicationListener;
import com.l7tech.util.Config;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.core.Ordered;

import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.server.ServerConfigParams.*;

/**
 * Check the status of Audit Cluster Properties such as audit sink properties ("audit.sink.alwaysSaveInternal" and "audit.sink.policy.guid",
 * "audit.sink.fallbackToInternal") and "audit.adminThreshold", and audit their status, when the gateway starts, the audit sink systems is
 * changed, or the admin audit threshold is changed to WARNING or SEVERE.
 *
 * @author ghuang
 */
public class AuditClusterPropertiesChecker implements ApplicationContextAware, PostStartupApplicationListener, Ordered {
    public static final String CLUSTER_PROP_ADMIN_AUDIT_THRESHOLD = "audit.adminThreshold";
    public static final String INTERNAL_AUDIT_SYSTEM = "Internal Audit System";
    public static final String AUDIT_SINK_POLICY = "Audit Sink Policy";
    public static final String AUDIT_SINK_SYSTEM_STARTED = "{0} started";
    public static final String AUDIT_SINK_SYSTEM_DISABLED = "{0} disabled";
    public static final String AUDIT_SINK_FALL_BACK_STATUS = "Fall back on internal audit system {0}";
    public static final String ENABLED = "enabled";
    public static final String DISABLED = "disabled";

    public static final String INTERNAL_AUDIT_SYSTEM_STARTED = MessageFormat.format(AUDIT_SINK_SYSTEM_STARTED, INTERNAL_AUDIT_SYSTEM);
    public static final String INTERNAL_AUDIT_SYSTEM_DISABLED = MessageFormat.format(AUDIT_SINK_SYSTEM_DISABLED, INTERNAL_AUDIT_SYSTEM);
    public static final String AUDIT_SINK_POLICY_STARTED = MessageFormat.format(AUDIT_SINK_SYSTEM_STARTED, AUDIT_SINK_POLICY);
    public static final String AUDIT_SINK_POLICY_DISABLED = MessageFormat.format(AUDIT_SINK_SYSTEM_DISABLED, AUDIT_SINK_POLICY);
    public static final String AUDIT_SINK_FALL_BACK_ENABLED = MessageFormat.format(AUDIT_SINK_FALL_BACK_STATUS, ENABLED);
    public static final String AUDIT_SINK_FALL_BACK_DISABLED = MessageFormat.format(AUDIT_SINK_FALL_BACK_STATUS, DISABLED);
    public static final String AUDIT_SINK_FALL_BACK_WARNING = "Audit Sink Policy failed and Internal Audit Fall Back is disabled.";
    public static final String AUDIT_ADMIN_THRESHOLD_WARNING =
        "The admin audit threshold set to {0} level will cause most admin audits to not be persisted or sent to the audit sink policy.";

    private static final Logger logger = Logger.getLogger(AuditClusterPropertiesChecker.class.getName());

    private ApplicationContext applicationContext;
    private final Config config;
    private final Timer timer;
    private final Map<String, Boolean> propStatusMap = new HashMap<String, Boolean>();

    public AuditClusterPropertiesChecker(Config config, Timer timer) {
        this.config = config;
        this.timer = timer;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(final ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof ReadyForMessages) {
            checkAndAuditInitialStatus();
        } else if (applicationEvent instanceof GoidEntityInvalidationEvent) {
            GoidEntityInvalidationEvent entityInvalidationEvent = (GoidEntityInvalidationEvent) applicationEvent;
            if (ClusterProperty.class.equals(entityInvalidationEvent.getEntityClass())) {
                final Object source = entityInvalidationEvent.getSource();
                if (! (source instanceof ClusterProperty)) return;
                final ClusterProperty clusterProperty = (ClusterProperty) source;

                if (! PARAM_AUDIT_SINK_ALWAYS_FALLBACK.equals(clusterProperty.getName()) &&
                    ! PARAM_AUDIT_SINK_POLICY_GUID.equals(clusterProperty.getName()) &&
                    ! PARAM_AUDIT_SINK_FALLBACK_ON_FAIL.equals(clusterProperty.getName()) &&
                    ! CLUSTER_PROP_ADMIN_AUDIT_THRESHOLD.equals(clusterProperty.getName())) {
                    return;
                }

                final boolean deleted = GoidEntityInvalidationEvent.DELETE == (entityInvalidationEvent.getEntityOperations()[0]);

                if (CLUSTER_PROP_ADMIN_AUDIT_THRESHOLD.equals(clusterProperty.getName())) {
                    // Check if it is the case where adminThreshold was deleted.  If so, do nothing, since D.N.E is equivalent to INFO.
                    if (deleted) return;

                    final Level currentThreshold = getAdminAuditThresholdByName(clusterProperty.getValue());
                    if (currentThreshold.intValue() > Level.INFO.intValue()) {
                        applicationContext.publishEvent(
                            new SystemEvent(AuditClusterPropertiesChecker.this,
                                Component.GW_AUDIT_SINK_CONFIG,
                                null,
                                Level.WARNING,
                                MessageFormat.format(AUDIT_ADMIN_THRESHOLD_WARNING, currentThreshold.getName())) {

                                @Override
                                public String getAction() {
                                    return "Admin Audit Threshold Evaluation";
                                }
                            }
                        );
                    }
                } else {
                    final AuditPropertyStatus propStatus = getAndUpdatePropertyStatus(clusterProperty, propStatusMap, deleted);
                    if (propStatus == null) return;

                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            checkAndAuditPropsStatus(propStatus);
                        }
                    }, 100L);
                }
            }
        }
    }

    @Override
    public int getOrder() {
        return 50000;
    }

    /**
     * Check and audit the initial status of Audit Sink System Cluster Properties.
     */
    private void checkAndAuditInitialStatus() {
        propStatusMap.put(PARAM_AUDIT_SINK_ALWAYS_FALLBACK, isInternalAuditSystemEnabled(true));
        propStatusMap.put(PARAM_AUDIT_SINK_POLICY_GUID, isAuditSinkPolicyEnabled());
        propStatusMap.put(PARAM_AUDIT_SINK_FALLBACK_ON_FAIL, isFallbackToDatabaseIfSinkPolicyFails());

        // Check Internal Audit System Property
        if (isInternalAuditSystemEnabled(true)) {
            applicationContext.publishEvent(
                new SystemEvent(AuditClusterPropertiesChecker.this,
                    Component.GW_AUDIT_SINK_CONFIG,
                    null,
                    Level.INFO,
                    INTERNAL_AUDIT_SYSTEM_STARTED) {

                    @Override
                    public String getAction() {
                        return "Properties Evaluation";
                    }
                }
            );
        }

        // Check Audit Sink Policy Property
        if (isAuditSinkPolicyEnabled()) {
            applicationContext.publishEvent(
                new SystemEvent(AuditClusterPropertiesChecker.this,
                    Component.GW_AUDIT_SINK_CONFIG,
                    null,
                    Level.INFO,
                    AUDIT_SINK_POLICY_STARTED) {

                    @Override
                    public String getAction() {
                        return "Properties Evaluation";
                    }
                }
            );
        }

        // Check Admin Audit Threshold Property
        final Level currentThreshold = getAdminAuditThresholdByName( config.getProperty( PARAM_AUDIT_ADMIN_THRESHOLD ) );
        if (currentThreshold.intValue() > Level.INFO.intValue()) {
            applicationContext.publishEvent(
                new SystemEvent(AuditClusterPropertiesChecker.this,
                    Component.GW_AUDIT_SINK_CONFIG,
                    null,
                    Level.WARNING,
                    MessageFormat.format(AUDIT_ADMIN_THRESHOLD_WARNING, currentThreshold.getName())) {

                    @Override
                    public String getAction() {
                        return "Admin Audit Threshold Evaluation";
                    }
                }
            );
        }
    }

    /**
     * Check if Internal Audit System is enabled or not.
     * @param toCheckAuditSinkPolicy: if true, combine the status of Internal Audit System and the status of Audit Sink Policy together to determine the final result.
     * @return true if "Internal Audit System" property is evaluated as enabled.
     */
    public boolean isInternalAuditSystemEnabled(boolean toCheckAuditSinkPolicy) {
        final boolean enabled = config.getBooleanProperty( PARAM_AUDIT_SINK_ALWAYS_FALLBACK, false );

        // If the Internal Audit System property is enabled, then just return true, since the internal audit system has been enabled.
        // If the property is not enabled and the flag toCheckAuditSinkPolicy is true, then check if Audit Sink Policy is disabled or not,
        // since the internal audit system is implied to be enabled if Audit Sink Policy is disabled.
        return enabled || (toCheckAuditSinkPolicy && !isAuditSinkPolicyEnabled());
    }

    /**
     * Check if Audit Sink Policy is enabled or not.
     * @return true of "Audit Sink Policy" property is evaluated as enabled.
     */
    public boolean isAuditSinkPolicyEnabled() {
        final String auditPolicyGuid = config.getProperty( PARAM_AUDIT_SINK_POLICY_GUID );

        return auditPolicyGuid != null && !auditPolicyGuid.trim().isEmpty();
    }

    /**
     * Check if Fall back to internal is enabled or not.
     * @return true if "Fall back to internal" property is evaluated as enabled.
     */
    public boolean isFallbackToDatabaseIfSinkPolicyFails() {
        final String propValue = config.getProperty( PARAM_AUDIT_SINK_FALLBACK_ON_FAIL );
        return propValue == null || Boolean.parseBoolean(propValue); // If this prop does not exist, it is equivalent to set "true".
    }

    /**
     * Get the level of Admin Audit Threshold by converting a level name to a level object.
     * @param levelName: the name of a Level
     * @return a Level object corresponding to the level name
     */
    private Level getAdminAuditThresholdByName(String levelName) {
        Level level = null;
        if (levelName != null) {
            try {
                level = Level.parse(levelName);
            } catch (IllegalArgumentException e) {
                logger.warning("Invalid admin threshold value '" + levelName + "'. Will use default " +
                    DefaultAuditThresholds.DEFAULT_ADMIN_THRESHOLD.getName() + " instead.");
            }
        }
        if (level == null) {
            level = DefaultAuditThresholds.DEFAULT_ADMIN_THRESHOLD;
        }
        return level;
    }

    /**
     * The class store the property name, previous status, and current status of an audit cluster property.
     * Currently this class is package-wide accessible.
     */
    static class AuditPropertyStatus {
        String propName;
        boolean currPropStatus;
        boolean prevPropStatus;
        AuditPropertyStatus relatedPropStatus;

        AuditPropertyStatus(String propName, boolean currPropStatus, boolean prevPropStatus) {
            this.propName = propName;
            this.currPropStatus = currPropStatus;
            this.prevPropStatus = prevPropStatus;
        }

        AuditPropertyStatus(String propName, boolean currPropStatus, boolean prevPropStatus, AuditPropertyStatus relatedPropStatus) {
            this.propName = propName;
            this.currPropStatus = currPropStatus;
            this.prevPropStatus = prevPropStatus;
            this.relatedPropStatus = relatedPropStatus;
        }
    }

    /**
     * Get and update the previous and current enabling status of an audit cluster property.
     *
     * @param clusterProperty: the cluster property being operated, such as created, updated, or deleted.
     * @param propStatusMap: the map contains all properties with their status.
     * @param deleted: a flag to indicate if the cluster property has been deleted or not.
     * @return the property status including property name, previous status, and current status.
     */
    public AuditPropertyStatus getAndUpdatePropertyStatus(final ClusterProperty clusterProperty, final Map<String, Boolean> propStatusMap, final boolean deleted) {
        AuditPropertyStatus propStatus = null;

        if (PARAM_AUDIT_SINK_ALWAYS_FALLBACK.equals(clusterProperty.getName())) {
            if (deleted) return null;

            final boolean currInternalAuditStatus = Boolean.parseBoolean(clusterProperty.getValue()) || !propStatusMap.get(PARAM_AUDIT_SINK_POLICY_GUID);
            propStatus = new AuditPropertyStatus(
                PARAM_AUDIT_SINK_ALWAYS_FALLBACK,
                currInternalAuditStatus,
                propStatusMap.get(PARAM_AUDIT_SINK_ALWAYS_FALLBACK)
            );
            propStatusMap.put(PARAM_AUDIT_SINK_ALWAYS_FALLBACK, currInternalAuditStatus);
        } else if (PARAM_AUDIT_SINK_POLICY_GUID.equals(clusterProperty.getName())) {
            if (deleted) {
                // When the audit sink policy prop is deleted, we need to handle deleting the internal audit system prop.
                // If the audit sink policy prop is deleted, the internal audit system is automatically turned on, so currInternalAuditStatus is set to true.
                final boolean currInternalAuditStatus = true;
                AuditPropertyStatus relatedStatus = new AuditPropertyStatus(
                    PARAM_AUDIT_SINK_ALWAYS_FALLBACK,
                    currInternalAuditStatus,
                    propStatusMap.get(PARAM_AUDIT_SINK_ALWAYS_FALLBACK)
                );
                propStatusMap.put(PARAM_AUDIT_SINK_ALWAYS_FALLBACK, currInternalAuditStatus);

                // Since the aud it sink policy prop is deleted, the audit sink policy must be disabled, so currAuditPolicyStatus is set to false.
                final boolean currAuditPolicyStatus = false;
                propStatus = new AuditPropertyStatus(
                    PARAM_AUDIT_SINK_POLICY_GUID,
                    currAuditPolicyStatus,
                    propStatusMap.get(PARAM_AUDIT_SINK_POLICY_GUID),
                    relatedStatus // It includes the status of Internal Audit System Property
                );
                propStatusMap.put(PARAM_AUDIT_SINK_POLICY_GUID, currAuditPolicyStatus);
            } else {
                final String guid = clusterProperty.getValue();
                final boolean currAuditPolicyStatus = (guid != null && !guid.trim().isEmpty());
                final boolean prevAuditPolicyStatus = propStatusMap.get(PARAM_AUDIT_SINK_POLICY_GUID);

                propStatus = new AuditPropertyStatus(
                    PARAM_AUDIT_SINK_POLICY_GUID,
                    currAuditPolicyStatus,
                    prevAuditPolicyStatus
                );
                propStatusMap.put(PARAM_AUDIT_SINK_POLICY_GUID, currAuditPolicyStatus);

                if (currAuditPolicyStatus != prevAuditPolicyStatus) {
                    final boolean currInternalAuditStatus = isInternalAuditSystemEnabled(false); // Audit Sink Policy has been enabled.

                    if (currAuditPolicyStatus && (! isInternalAuditSystemEnabled(false))) {
                        AuditPropertyStatus relatedStatus = new AuditPropertyStatus(
                            PARAM_AUDIT_SINK_ALWAYS_FALLBACK,
                            currInternalAuditStatus,
                            propStatusMap.get(PARAM_AUDIT_SINK_ALWAYS_FALLBACK)
                        );
                        propStatusMap.put(PARAM_AUDIT_SINK_ALWAYS_FALLBACK, currInternalAuditStatus);

                        propStatus.relatedPropStatus = relatedStatus;
                    }
                }
            }
        } else if (PARAM_AUDIT_SINK_FALLBACK_ON_FAIL.equals(clusterProperty.getName())) {
            final boolean currFallbackStatus = deleted || Boolean.parseBoolean(clusterProperty.getValue()); // If audit.sink.fallbackToInternal is deleted, then set its status as the default: true.
            propStatus = new AuditPropertyStatus(
                PARAM_AUDIT_SINK_FALLBACK_ON_FAIL,
                currFallbackStatus,
                propStatusMap.get(PARAM_AUDIT_SINK_FALLBACK_ON_FAIL)
            );
            propStatusMap.put(PARAM_AUDIT_SINK_FALLBACK_ON_FAIL, currFallbackStatus);
        }

        return propStatus;
    }

    public List<String> checkAndAuditPropsStatus(final AuditPropertyStatus propertyStatus) {
        final List<String> auditMessages = new ArrayList<String>();
        if (propertyStatus == null) return auditMessages;

        if (PARAM_AUDIT_SINK_ALWAYS_FALLBACK.equals(propertyStatus.propName)) {
            if (propertyStatus.currPropStatus != propertyStatus.prevPropStatus) {
                auditMessages.add(propertyStatus.currPropStatus? INTERNAL_AUDIT_SYSTEM_STARTED : INTERNAL_AUDIT_SYSTEM_DISABLED);
            }
        } else if (PARAM_AUDIT_SINK_POLICY_GUID.equals(propertyStatus.propName)) {
            // Process Audit Sink Policy Status
            if (propertyStatus.currPropStatus != propertyStatus.prevPropStatus) {
                auditMessages.add(propertyStatus.currPropStatus? AUDIT_SINK_POLICY_STARTED : AUDIT_SINK_POLICY_DISABLED);
            }

            // Process Internal Audit System
            if (propertyStatus.relatedPropStatus != null && propertyStatus.relatedPropStatus.currPropStatus != propertyStatus.relatedPropStatus.prevPropStatus) {
                // The related property status is the status of Internal Audit System.
                auditMessages.add(propertyStatus.relatedPropStatus.currPropStatus? INTERNAL_AUDIT_SYSTEM_STARTED : INTERNAL_AUDIT_SYSTEM_DISABLED);
            }
        } else if (PARAM_AUDIT_SINK_FALLBACK_ON_FAIL.equals(propertyStatus.propName)) {
            if (propertyStatus.currPropStatus != propertyStatus.prevPropStatus) {
                auditMessages.add(propertyStatus.currPropStatus ? AUDIT_SINK_FALL_BACK_ENABLED : AUDIT_SINK_FALL_BACK_DISABLED);
            }
        }

        if (!auditMessages.isEmpty()) {
            for (String auditMessage: auditMessages) {
                applicationContext.publishEvent(
                    new SystemEvent(AuditClusterPropertiesChecker.this,
                        Component.GW_AUDIT_SINK_CONFIG,
                        null,
                        Level.INFO,
                        auditMessage) {

                        @Override
                        public String getAction() {
                            return "Properties Evaluation";
                        }
                    }
                );
            }
        }

        return auditMessages;
    }
}

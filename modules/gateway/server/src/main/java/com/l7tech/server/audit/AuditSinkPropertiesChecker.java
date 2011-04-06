package com.l7tech.server.audit;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.event.system.SystemEvent;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.util.*;
import java.util.logging.Level;

import static com.l7tech.server.ServerConfig.PARAM_AUDIT_SINK_ALWAYS_FALLBACK;
import static com.l7tech.server.ServerConfig.PARAM_AUDIT_SINK_FALLBACK_ON_FAIL;
import static com.l7tech.server.ServerConfig.PARAM_AUDIT_SINK_POLICY_GUID;

/**
 * Check the status of Audit Sink Properties such as  "audit.sink.alwaysSaveInternal" and "audit.sink.policy.guid", and "audit.sink.fallbackToInternal",
 * and audit their status, when the gateway starts and the audit sink systems is enabled back from disabled.
 *
 * @author ghuang
 */
public class AuditSinkPropertiesChecker implements ApplicationContextAware, ApplicationListener {
    public static final String INTERNAL_AUDIT_SYSTEM_STARTED = "Internal Audit System started";
    public static final String INTERNAL_AUDIT_SYSTEM_DISABLED = "Internal Audit System disabled";
    public static final String AUDIT_SINK_POLICY_STARTED = "Audit Sink Policy started";
    public static final String AUDIT_SINK_POLICY_DISABLED = "Audit Sink Policy disabled";
    public static final String AUDIT_SINK_FALL_BACK_ENABLED = "Fall back on internal audit system enabled";
    public static final String AUDIT_SINK_FALL_BACK_DISABLED = "Fall back on internal audit system disabled";
    public static final String AUDIT_SINK_FALL_BACK_WARNING = "Audit Sink Policy failed and Internal Audit Fall Back is disabled.";

    private ApplicationContext applicationContext;
    private final ServerConfig serverConfig;
    private final Timer timer;
    private final Map<String, Boolean> propStatusMap = new HashMap<String, Boolean>();

    public AuditSinkPropertiesChecker(ServerConfig serverConfig, Timer timer) {
        this.serverConfig = serverConfig;
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
        } else if (applicationEvent instanceof EntityInvalidationEvent) {
            EntityInvalidationEvent entityInvalidationEvent = (EntityInvalidationEvent) applicationEvent;
            if (ClusterProperty.class.equals(entityInvalidationEvent.getEntityClass())) {
                final Object source = entityInvalidationEvent.getSource();
                if (! (source instanceof ClusterProperty)) return;
                final ClusterProperty clusterProperty = (ClusterProperty) source;

                if (! PARAM_AUDIT_SINK_ALWAYS_FALLBACK.equals(clusterProperty.getName()) &&
                    ! PARAM_AUDIT_SINK_POLICY_GUID.equals(clusterProperty.getName()) &&
                    ! PARAM_AUDIT_SINK_FALLBACK_ON_FAIL.equals(clusterProperty.getName())) {
                    return;
                }

                final boolean deleted = EntityInvalidationEvent.DELETE == (entityInvalidationEvent.getEntityOperations()[0]);
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

    /**
     * Check and audit the initial status of Audit Sink System Cluster Properties.
     */
    private void checkAndAuditInitialStatus() {
        propStatusMap.put(PARAM_AUDIT_SINK_ALWAYS_FALLBACK, isInternalAuditSystemEnabled(true));
        propStatusMap.put(PARAM_AUDIT_SINK_POLICY_GUID, isAuditSinkPolicyEnabled());
        propStatusMap.put(PARAM_AUDIT_SINK_FALLBACK_ON_FAIL, isFallbackToDatabaseIfSinkPolicyFails());

        if (isInternalAuditSystemEnabled(true)) {
            applicationContext.publishEvent(
                new SystemEvent(AuditSinkPropertiesChecker.this,
                    Component.GW_AUDIT_SINK_CONFIG,
                    null,
                    Level.INFO,
                    INTERNAL_AUDIT_SYSTEM_STARTED) {

                    @Override
                    public String getAction() {
                        return "Audit Sink Properties Evaluation";
                    }
                }
            );
        }

        if (isAuditSinkPolicyEnabled()) {
            applicationContext.publishEvent(
                new SystemEvent(AuditSinkPropertiesChecker.this,
                    Component.GW_AUDIT_SINK_CONFIG,
                    null,
                    Level.INFO,
                    AUDIT_SINK_POLICY_STARTED) {

                    @Override
                    public String getAction() {
                        return "Audit Sink Properties Evaluation";
                    }
                }
            );
        }
    }

    /**
     * Check if Internal Audit System is enabled or not.
     * @param toCheckAuditSinkPolicy: if true, combine the status of Internal Audit System and the status of Audit Sink Policy together to determine the final result.
     * @return: true if "Internal Audit System" property is evaluated as enabled.
     */
    public boolean isInternalAuditSystemEnabled(boolean toCheckAuditSinkPolicy) {
        final boolean enabled = Boolean.parseBoolean(serverConfig.getPropertyUncached(PARAM_AUDIT_SINK_ALWAYS_FALLBACK));

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
        final String auditPolicyGuid = serverConfig.getPropertyUncached(PARAM_AUDIT_SINK_POLICY_GUID);

        return auditPolicyGuid != null && !auditPolicyGuid.trim().isEmpty();
    }

    /**
     * Check if Fall back to internal is enabled or not.
     * @return true if "Fall back to internal" property is evaluated as enabled.
     */
    public boolean isFallbackToDatabaseIfSinkPolicyFails() {
        final String propValue = serverConfig.getPropertyUncached(PARAM_AUDIT_SINK_FALLBACK_ON_FAIL);
        return propValue == null || Boolean.parseBoolean(propValue); // If this prop does not exist, it is equivalent to set "true".
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
                    new SystemEvent(AuditSinkPropertiesChecker.this,
                        Component.GW_AUDIT_SINK_CONFIG,
                        null,
                        Level.INFO,
                        auditMessage) {

                        @Override
                        public String getAction() {
                            return "Audit Sink Properties Evaluation";
                        }
                    }
                );
            }
        }

        return auditMessages;
    }
}
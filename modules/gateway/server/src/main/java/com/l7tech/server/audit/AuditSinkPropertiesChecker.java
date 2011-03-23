package com.l7tech.server.audit;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.audit.Messages;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.event.admin.AdminEvent;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.event.system.SystemEvent;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Check the status of Audit Sink Properties such as  "audit.sink.alwaysSaveInternal" and "audit.sink.policy.guid", and "audit.sink.fallbackToInternal",
 * and audit their status, when the gateway starts and the audit sink systems is enabled back from disabled.
 *
 * @author ghuang
 */
public class AuditSinkPropertiesChecker implements ApplicationContextAware, ApplicationListener {
    private ApplicationContext applicationContext;
    private ServerConfig serverConfig;

    public AuditSinkPropertiesChecker(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof ReadyForMessages) {
            checkInitialStatus();
        }
    }

    private void checkInitialStatus() {
        if (isInternalAuditSystemEnabled(true)) {
            applicationContext.publishEvent(
                new SystemEvent(AuditSinkPropertiesChecker.this,
                    Component.GW_AUDIT_SINK_CONFIG,
                    null,
                    SystemMessages.AUDIT_SINK_START.getLevel(),
                    MessageFormat.format(SystemMessages.AUDIT_SINK_START.getMessage(), "Internal Audit System")) {

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
                    SystemMessages.AUDIT_SINK_START.getLevel(),
                    MessageFormat.format(SystemMessages.AUDIT_SINK_START.getMessage(), "Audit Sink Policy")) {

                    @Override
                    public String getAction() {
                        return "Audit Sink Properties Evaluation";
                    }
                }
            );
        }
    }

    public boolean isInternalAuditSystemEnabled(boolean toCheckAuditSinkPolicy) {
        final boolean enabled = Boolean.parseBoolean(serverConfig.getPropertyUncached(ServerConfig.PARAM_AUDIT_SINK_ALWAYS_FALLBACK));

        // If the Internal Audit System property is enabled, then just return true, since the internal audit system has been enabled.
        // If the property is not enabled and the flag toCheckAuditSinkPolicy is true, then check if Audit Sink Policy is disabled or not,
        // since the internal audit system is implied to be enabled if Audit Sink Policy is disabled.
        return enabled || (toCheckAuditSinkPolicy && !isAuditSinkPolicyEnabled());
    }

    public boolean isAuditSinkPolicyEnabled() {
        final String auditPolicyGuid = serverConfig.getPropertyUncached(ServerConfig.PARAM_AUDIT_SINK_POLICY_GUID);

        return auditPolicyGuid != null && !auditPolicyGuid.trim().isEmpty();
    }

    public boolean isFallbackToDatabaseIfSinkPolicyFails() {
        final String propValue = serverConfig.getPropertyUncached(ServerConfig.PARAM_AUDIT_SINK_FALLBACK_ON_FAIL);
        return propValue == null || Boolean.parseBoolean(propValue); // If this prop does not exist, it is equivalent to set "true".
    }

    /**
     * Check the status of audit sink cluster properties, "audit.sink.fallbackToInternal", "audit.sink.alwaysSaveInternal" and "audit.sink.policy.guid", and audit their status.
     *
     * @param clusterProperty: The cluster property to be checked and its status will be audited.
     * @param prevPropStatus: The status of the properties before deleted/updated/created.
     * @param toBeDeleted: A flag to indicate that the cluster property is to be deleted.  If it is false, then it means it is to updated/created.
     * @return a list of audited messages.  This is only for testing purpose (Please see @link AuditSinkPropertiesCheckerTest})
     */
    public List<String> checkAndAuditPropsStatus(final ClusterProperty clusterProperty, final boolean[] prevPropStatus, boolean toBeDeleted) {
        final List<String> auditMessages = new ArrayList<String>();

        if (ServerConfig.PARAM_AUDIT_SINK_ALWAYS_FALLBACK.equals(clusterProperty.getName())) {
            // If the internal audit prop is deleted, auditing the status of Internal Audit System will be handled together with the deletion of Audit Sink Policy.
            if (toBeDeleted) return auditMessages;

            // If not deleted, the status of internal audit system should be verified by combining the status of audit sink policy.
            // For example, suppose that the database has not set the both audit sink properties.  If the internal audit prop is set as false
            // via Manager Cluster-wide Property dialog, since the audit sink policy prop does not exist, then the internal audit system
            // is still evaluated as enabled even though it is set as "false" in the Manager Cluster-wide Property dialog.
            final boolean currInternalAuditStatus = Boolean.parseBoolean(clusterProperty.getValue()) || (! isAuditSinkPolicyEnabled());
            final boolean prevInternalAuditStatus = prevPropStatus[0];
            if (currInternalAuditStatus != prevInternalAuditStatus) {
                final Messages.M message = currInternalAuditStatus? SystemMessages.AUDIT_SINK_START : SystemMessages.AUDIT_SINK_DISABLED;
                auditMessages.add(MessageFormat.format(message.getMessage(), "Internal Audit System"));
            }
        } else if (ServerConfig.PARAM_AUDIT_SINK_POLICY_GUID.equals(clusterProperty.getName())) {
            if (toBeDeleted) {
                // If the audit sink policy prop is deleted, then the internal audit system is automatically turned on, so currInternalAuditStatus is set to true.
                final boolean currInternalAuditStatus = true;
                final boolean prevInternalAuditStatus = prevPropStatus[1];
                if (currInternalAuditStatus != prevInternalAuditStatus) {
                    auditMessages.add(MessageFormat.format(SystemMessages.AUDIT_SINK_START.getMessage(), "Internal Audit System"));
                }

                // Since the audit sink policy prop is deleted, the audit sink policy must be disabled, so currAuditPolicyStatus is set to false.
                final boolean currAuditPolicyStatus = false;
                final boolean prevAuditPolicyStatus = prevPropStatus[0];
                if (currAuditPolicyStatus != prevAuditPolicyStatus) {
                    auditMessages.add(MessageFormat.format(SystemMessages.AUDIT_SINK_DISABLED.getMessage(), "Audit Sink Policy"));
                }
            } else {
                final String guid = clusterProperty.getValue();
                final boolean currAuditPolicyStatus = (guid != null && !guid.trim().isEmpty());
                final boolean prevAuditPolicyStatus = prevPropStatus[0];

                if (currAuditPolicyStatus != prevAuditPolicyStatus) {
                    final Messages.M message = currAuditPolicyStatus? SystemMessages.AUDIT_SINK_START : SystemMessages.AUDIT_SINK_DISABLED;
                    auditMessages.add(MessageFormat.format(message.getMessage(), "Audit Sink Policy"));

                    // The below part is to handle a corner case:
                    // Both audit sink properties did not exist originally, then set the audit sink policy in Manager Cluster-wide Property dialog.
                    // In this case, the audit sink policy will be evaluated as enabled and the internal audit system will be evaluated as disabled.
                    if (currAuditPolicyStatus && (! isInternalAuditSystemEnabled(false))) {
                        auditMessages.add(MessageFormat.format(SystemMessages.AUDIT_SINK_DISABLED.getMessage(), "Internal Audit System"));
                    }
                }
            }
        } else if (ServerConfig.PARAM_AUDIT_SINK_FALLBACK_ON_FAIL.equals(clusterProperty.getName())) {
            final boolean currFallbackStatus = toBeDeleted || Boolean.parseBoolean(clusterProperty.getValue()); // If audit.sink.fallbackToInternal is deleted, then set its status as the default: true.
            final boolean prevFallbackStatus = prevPropStatus[0];

            if (currFallbackStatus != prevFallbackStatus) {
                auditMessages.add(MessageFormat.format(SystemMessages.AUDIT_SINK_FALL_BACK_STATUS.getMessage(), (currFallbackStatus ? "enabled" : "disabled")));
            }
        }

        if (!auditMessages.isEmpty()) {
            for (String auditMessage: auditMessages) {
                applicationContext.publishEvent(new AdminEvent(this, auditMessage) {
                    @Override
                    public Level getMinimumLevel() {
                        return Level.INFO;
                    }
                });
            }
        }

        return auditMessages;
    }
}
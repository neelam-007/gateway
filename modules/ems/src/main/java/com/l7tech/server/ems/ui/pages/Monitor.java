package com.l7tech.server.ems.ui.pages;

import com.l7tech.server.ems.ui.NavigationPage;
import com.l7tech.server.ems.enterprise.JSONException;
import com.l7tech.server.ems.enterprise.JSONConstants;
import com.l7tech.server.ems.monitoring.SystemMonitoringSetupSettingsManager;
import com.l7tech.server.ems.monitoring.InvalidMonitoringSetupSettingException;
import com.l7tech.server.ems.monitoring.SystemMonitoringNotificationRule;
import com.l7tech.server.ems.monitoring.SystemMonitoringNotificationRulesManager;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.objectmodel.*;
import com.l7tech.util.ExceptionUtils;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;

import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.mortbay.util.ajax.JSON;

/**
 *
 */
@NavigationPage(page="Monitor",section="ManageGateways",sectionPage="Configure",pageUrl="Monitor.html")
public class Monitor extends EsmStandardWebPage {
    private static final Logger logger = Logger.getLogger(Monitor.class.getName());

    @SpringBean(name="serverConfig")
    private ServerConfig serverConfig;

    @SpringBean(name="systemMonitoringSetupSettingsManager")
    private SystemMonitoringSetupSettingsManager systemMonitoringSetupSettingsManager;

    @SpringBean(name="systemMonitoringNotificationRulesManager")
    private SystemMonitoringNotificationRulesManager systemMonitoringNotificationRulesManager;

    @SpringBean
    private RoleManager roleManager;

    private boolean isReadOnly;

    public Monitor() {
        isReadOnly = checkReadOnly();

        // Wicket component for getting system monitoring setup settings
        add(new JsonInteraction("getSystemMonitoringSetupSettings", "getSystemMonitoringSetupSettingsUrl", new GettingSystemMonitoringSetupSettingsDataProvider()));

        // Wicket component for saving system monitoring setup settings
        add(new JsonPostInteraction("saveSystemMonitoringSetupSettings", "saveSystemMonitoringSetupSettingsUrl", new SavingSystemMonitoringSetupSettingsDataProvider()));

        // Wicket component for getting system monitoring notification rules
        add(new JsonInteraction("getSystemMonitoringNotificationRules", "getSystemMonitoringNotificationRulesUrl", new GettingSystemMonitoringNotificationRulesDataProvider()));

        // Wicket component for saving/editing a system monitoring notification rule
        add(new JsonPostInteraction("saveSystemMonitoringNotificationRule", "saveSystemMonitoringNotificationRuleUrl", new SavingSystemMonitoringNotificationRuleDataProvider()));

        // Wicket component for deleting a system monitoring notification rule
        final HiddenField deleteNotificationRuleDialog_id = new HiddenField("deleteNotificationRuleDialog_id", new Model(""));
        Form deleteNotificationRuleForm = new JsonDataResponseForm("deleteNotificationRuleForm") {
            @Override
            protected Object getJsonResponseData() {
                String guid = (String)deleteNotificationRuleDialog_id.getConvertedInput();
                try {
                    systemMonitoringNotificationRulesManager.deleteByGuid(guid);
                    logger.fine("Deleting a system monitoring notification rule (GUID = "+ guid + ").");
                    return null;    // No response object expected if successful.
                } catch (Exception e) {
                    String errmsg = "Cannot delete the system monitoring notification rule (GUID = '" + guid + "').";
                    logger.warning(errmsg);
                    return new JSONException(new Exception(errmsg, e));
                }
            }
        };
        deleteNotificationRuleForm.add(deleteNotificationRuleDialog_id);
        add(deleteNotificationRuleForm);
    }

    /**
     * Check if the user has read permission in the Monitor Page.
     * @return true if the user has only read permission.
     */
    private boolean checkReadOnly() {
        boolean readonly = true;
        try {
            for (Role role: roleManager.getAssignedRoles(getUser())) {
                if (role.getTag().equals(Role.Tag.ADMIN)) {
                    readonly = false;
                    break;
                }
            }
        } catch (FindException e) {
            logger.warning("Cannot find roles for the user (NAME = '" + getUser().getName() + "').");
        }
        return readonly;
    }

    /**
     * Data provider for getting system monitoring setup settings.
     */
    private final class GettingSystemMonitoringSetupSettingsDataProvider implements JsonDataProvider {

        @Override
        public Object getData() {
            try {
                Map<String, Object> setupSettings = systemMonitoringSetupSettingsManager.findSetupSettings();
                return toJsonFormat(setupSettings);
            } catch (Exception e) {
                return new JSONException(new Exception("Cannot load the settings of the system monitoring setup.", e));
            }
        }

        @Override
        public void setData(Object jsonData) {
            // No data expected from the browser.
        }
    }

    /**
     * Data provider for saving system monitoring setup settings.
     */
    private final class SavingSystemMonitoringSetupSettingsDataProvider implements JsonDataProvider {
        private Object returnValue;

        @Override
        public Object getData() {
            return returnValue;
        }

        @Override
        public void setData(Object jsonData) {
            if (jsonData instanceof JSONException ){
                //some exception happened whilst trying to retrieve the payload from upload
                returnValue = jsonData;
            } else if (jsonData instanceof String) {
                Object jsonDataObj;
                try {
                    jsonDataObj = JSON.parse(jsonData.toString());
                } catch(Exception e){
                    logger.log(Level.FINER, "Cannot parse uploaded JSON data", e.getCause());
                    returnValue = new JSONException(new Exception("Cannot parse uploaded JSON data", e.getCause()));
                    return;
                }

                if (!(jsonDataObj instanceof Map)) {
                    logger.log(Level.FINER, "Incorrect JSON data. Not convertible to a Map");
                    returnValue = new JSONException(new Exception("Incorrect JSON data. Not convertible to a Map"));
                    return;
                }

                Map<String, Object> jsonFormatMap = (Map<String, Object>) jsonDataObj;

                try {
                    Map<String, Object> clusterPropertyFormatMap = toClusterPropertyFormat(jsonFormatMap);
                    systemMonitoringSetupSettingsManager.saveSetupSettings(clusterPropertyFormatMap);
                } catch (Exception e) {
                    returnValue = new JSONException(new Exception("Cannot save the system monitoring setup settings.", e));
                }
            } else {
                returnValue = new JSONException(new IllegalArgumentException("jsonData must be either a JSONException or a JSON formatted String"));
            }
        }
    }

    /**
     * Data provider for getting system monitoring setup settings.
     */
    private final class GettingSystemMonitoringNotificationRulesDataProvider implements JsonDataProvider {

        @Override
        public Object getData() {
            try {
                // Get a list of notification rules.
                Collection<SystemMonitoringNotificationRule> notificationRules = systemMonitoringNotificationRulesManager.findAll();

                // Remove password for those E-mail notification rules.
                for (SystemMonitoringNotificationRule rule: notificationRules) {
                    if (rule.getType().equals(JSONConstants.NotifiationType.E_MAIL)) {
                        rule.obtainParamsProps().remove(JSONConstants.NotifiationEMailParams.PASSWORD);
                    }
                }

                // Form the JSON content map
                Map<String, Object> jsonDataMap = new HashMap<String, Object>();
                if (notificationRules != null && !notificationRules.isEmpty()) {
                    jsonDataMap.put(JSONConstants.READONLY, isReadOnly);
                    jsonDataMap.put(JSONConstants.NotifiationRule.RECORDS, notificationRules);
                }

                // Return the JSON data back to the browser.
                return jsonDataMap;
            } catch (Exception e) {
                return new JSONException(new Exception("Cannot load the system monitoring notification rules.", e));
            }
        }

        @Override
        public void setData(Object jsonData) {
            // No data expected from the browser.
        }
    }

    /**
     * Data provider for saving system monitoring notification rules.
     */
    private final class SavingSystemMonitoringNotificationRuleDataProvider implements JsonDataProvider {
        private Object returnValue;

        @Override
        public Object getData() {
            return returnValue;
        }

        @Override
        public void setData(Object jsonData) {
            if (jsonData instanceof JSONException ){
                //some exception happened whilst trying to retrieve the payload from upload request
                returnValue = jsonData;
            } else if (jsonData instanceof String) {
                Object jsonDataObj;
                try {
                    jsonDataObj = JSON.parse(jsonData.toString());
                } catch(Exception e){
                    logger.log(Level.FINER, "Cannot parse uploaded JSON data", e.getCause());
                    returnValue = new JSONException(new Exception("Cannot parse uploaded JSON data", e.getCause()));
                    return;
                }

                if (!(jsonDataObj instanceof Map)) {
                    logger.log(Level.FINER, "Incorrect JSON data. Not convertible to a Map");
                    returnValue = new JSONException(new Exception("Incorrect JSON data. Not convertible to a Map"));
                    return;
                }

                Map<String, Object> jsonDataMap = (Map<String, Object>) jsonDataObj;
                String guid = (String) jsonDataMap.get(JSONConstants.ID);
                String name = (String) jsonDataMap.get(JSONConstants.NAME);
                String type = (String) jsonDataMap.get(JSONConstants.TYPE);
                Map<String, Object> params = (Map<String, Object>) jsonDataMap.get(JSONConstants.NotifiationRule.PARAMS);

                String errmsg = null;
                Throwable err = null;
                // Case 1: to save a new notification rule.
                if (guid == null) {
                    SystemMonitoringNotificationRule newNotificationRule = new SystemMonitoringNotificationRule(name, type, params);
                    try {
                        systemMonitoringNotificationRulesManager.save(newNotificationRule);
                    } catch (DuplicateObjectException e) {
                        errmsg = "There already exists a system monitoring notification rule with the same name, '" + name + "'.";
                        err = e;
                    } catch (SaveException e) {
                        errmsg = "Cannot save a system monitoring notification rule.";
                        err = e;
                    }
                }
                // Case 2: to edit and save an existing notification rule.
                else {
                    try {
                        // Get the notification rule by GUID
                        SystemMonitoringNotificationRule notificationRule = systemMonitoringNotificationRulesManager.findByGuid(guid);

                        // Update the password for the E-mail notification rule.
                        if (notificationRule.getType().equals(JSONConstants.NotifiationType.E_MAIL)) {
                            boolean requiresAuth = (Boolean) params.get(JSONConstants.NotifiationEMailParams.REQUIRES_AUTHENTICATION);
                            if (requiresAuth) {
                                String oldPassword = (String) notificationRule.getParamProp(JSONConstants.NotifiationEMailParams.PASSWORD);
                                String newPassword = (String) params.get(JSONConstants.NotifiationEMailParams.PASSWORD);
                                if (newPassword == null) {
                                    params.put(JSONConstants.NotifiationEMailParams.PASSWORD, oldPassword);
                                }
                            } else {
                                params.remove(JSONConstants.NotifiationEMailParams.USERNAME);
                                params.remove(JSONConstants.NotifiationEMailParams.PASSWORD);
                            }
                        }

                        // Update this notification rule.
                        notificationRule.copyFrom(name, type, params);
                        systemMonitoringNotificationRulesManager.update(notificationRule);
                    } catch (FindException e) {
                        errmsg = "Cannot find the system monitoring notification rule (GUID = '" + guid + "').";
                        err = e;
                    } catch (UpdateException e) {
                        errmsg = ExceptionUtils.causedBy(e, DuplicateObjectException.class)?
                            "There already exists a system monitoring notification rule with the same name, '" + name + "'." :
                            "Cannot update the system monitoring notification rule (GUID = '" + guid + "').";
                        err = e;
                    }
                }

                if (errmsg != null) {
                    logger.warning(errmsg);
                    returnValue = new JSONException(new Exception(errmsg, err));
                } else {
                    // Return null if saving is successful.
                    returnValue = null;
                }
            } else {
                returnValue = new JSONException(new IllegalArgumentException("jsonData must be either a JSONException or a JSON formatted String"));
            }
        }
    }

    /**
     * Convert a cluster-property format map (retrieved from the DB table, cluster_properties.) to a JSON format map (to be sent to the browser).
     * @param clusterPropertyFormatMap: the given map in the clsuter property convention.
     * @return a map in the JSON convention.
     */
    private Map<String, Object> toJsonFormat(Map<String, Object> clusterPropertyFormatMap) {
        Map<String, Object> jsonFormatMap = new HashMap<String, Object>();

        // "auditSize"
        Map<String, Object> auditSizeMap = new HashMap<String, Object>();
        auditSizeMap.put(JSONConstants.MonitoringPropertySettings.SAMPLING_INTERVAL,         clusterPropertyFormatMap.get(ServerConfig.PARAM_MONITORING_INTERVAL_SSGAUDITSIZE));
        auditSizeMap.put(JSONConstants.MonitoringPropertySettings.DEFAULT_TRIGGER_VALUE,     clusterPropertyFormatMap.get(ServerConfig.PARAM_MONITORING_TRIGGER_SSGAUDITSIZE));
        auditSizeMap.put(JSONConstants.MonitoringPropertySettings.TRIGGER_VALUE_LOWER_LIMIT, Integer.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGCLUSTER_SSGAUDITSIZE_LOWERLIMIT)));
        auditSizeMap.put(JSONConstants.MonitoringPropertySettings.UNIT,                      serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGCLUSTER_SSGAUDITSIZE_UNIT));

        // "operatingStatus"
        Map<String, Object> operatingStatusMap = new HashMap<String, Object>();
        operatingStatusMap.put(JSONConstants.MonitoringPropertySettings.SAMPLING_INTERVAL,   clusterPropertyFormatMap.get(ServerConfig.PARAM_MONITORING_INTERVAL_OPERATINGSTATUS));

        // "logSize"
        Map<String, Object> logSizeMap = new HashMap<String, Object>();
        logSizeMap.put(JSONConstants.MonitoringPropertySettings.SAMPLING_INTERVAL,           clusterPropertyFormatMap.get(ServerConfig.PARAM_MONITORING_INTERVAL_SSGLOGSIZE));
        logSizeMap.put(JSONConstants.MonitoringPropertySettings.DEFAULT_TRIGGER_VALUE,       clusterPropertyFormatMap.get(ServerConfig.PARAM_MONITORING_TRIGGER_SSGLOGSIZE));
        logSizeMap.put(JSONConstants.MonitoringPropertySettings.TRIGGER_VALUE_LOWER_LIMIT,   Integer.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGNODE_SSGLOGSIZE_LOWERLIMIT)));
        logSizeMap.put(JSONConstants.MonitoringPropertySettings.UNIT,                        serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGNODE_SSGLOGSIZE_UNIT));

        // "diskUsage"
        Map<String, Object> diskUsageMap = new HashMap<String, Object>();
        diskUsageMap.put(JSONConstants.MonitoringPropertySettings.SAMPLING_INTERVAL,         clusterPropertyFormatMap.get(ServerConfig.PARAM_MONITORING_INTERVAL_DISKUSAGE));
        diskUsageMap.put(JSONConstants.MonitoringPropertySettings.DEFAULT_TRIGGER_VALUE,     clusterPropertyFormatMap.get(ServerConfig.PARAM_MONITORING_TRIGGER_DISKUSAGE));
        diskUsageMap.put(JSONConstants.MonitoringPropertySettings.TRIGGER_VALUE_LOWER_LIMIT, Integer.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGNODE_DISKUSAGE_LOWERLIMIT)));
        diskUsageMap.put(JSONConstants.MonitoringPropertySettings.TRIGGER_VALUE_UPPER_LIMIT, Integer.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGNODE_DISKUSAGE_UPPERLIMIT)));
        diskUsageMap.put(JSONConstants.MonitoringPropertySettings.UNIT,                      serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGNODE_DISKUSAGE_UNIT));

        // "raidStatus"
        Map<String, Object> raidStatusMap = new HashMap<String, Object>();
        raidStatusMap.put(JSONConstants.MonitoringPropertySettings.SAMPLING_INTERVAL,        clusterPropertyFormatMap.get(ServerConfig.PARAM_MONITORING_INTERVAL_RAIDSTATUS));

        // "cpuTemp"
        Map<String, Object> cpuTempMap = new HashMap<String, Object>();
        cpuTempMap.put(JSONConstants.MonitoringPropertySettings.SAMPLING_INTERVAL,           clusterPropertyFormatMap.get(ServerConfig.PARAM_MONITORING_INTERVAL_CPUTEMPERATURE));
        cpuTempMap.put(JSONConstants.MonitoringPropertySettings.DEFAULT_TRIGGER_VALUE,       clusterPropertyFormatMap.get(ServerConfig.PARAM_MONITORING_TRIGGER_CPUTEMPERATURE));
        cpuTempMap.put(JSONConstants.MonitoringPropertySettings.TRIGGER_VALUE_LOWER_LIMIT,   Integer.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGNODE_CPUTEMPERATURE_LOWERLIMIT)));
        cpuTempMap.put(JSONConstants.MonitoringPropertySettings.UNIT,                        serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGNODE_CPUTEMPERATURE_UNIT));

        // "cpuUsage"
        Map<String, Object> cpuUsageMap = new HashMap<String, Object>();
        cpuUsageMap.put(JSONConstants.MonitoringPropertySettings.SAMPLING_INTERVAL,          clusterPropertyFormatMap.get(ServerConfig.PARAM_MONITORING_INTERVAL_CPUUSAGE));
        cpuUsageMap.put(JSONConstants.MonitoringPropertySettings.DEFAULT_TRIGGER_VALUE,      clusterPropertyFormatMap.get(ServerConfig.PARAM_MONITORING_TRIGGER_CPUUSAGE));
        cpuUsageMap.put(JSONConstants.MonitoringPropertySettings.TRIGGER_VALUE_LOWER_LIMIT,  Integer.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGNODE_CPUUSAGE_LOWERLIMIT)));
        cpuUsageMap.put(JSONConstants.MonitoringPropertySettings.TRIGGER_VALUE_UPPER_LIMIT,  Integer.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGNODE_CPUUSAGE_UPPERLIMIT)));
        cpuUsageMap.put(JSONConstants.MonitoringPropertySettings.UNIT,                       serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGNODE_CPUUSAGE_UNIT));

        // "swapUsage"
        Map<String, Object> swapUsageMap = new HashMap<String, Object>();
        swapUsageMap.put(JSONConstants.MonitoringPropertySettings.SAMPLING_INTERVAL,         clusterPropertyFormatMap.get(ServerConfig.PARAM_MONITORING_INTERVAL_SWAPUSAGE));
        swapUsageMap.put(JSONConstants.MonitoringPropertySettings.DEFAULT_TRIGGER_VALUE,     clusterPropertyFormatMap.get(ServerConfig.PARAM_MONITORING_TRIGGER_SWAPUSAGE));
        swapUsageMap.put(JSONConstants.MonitoringPropertySettings.TRIGGER_VALUE_LOWER_LIMIT, Integer.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGNODE_SWAPUSAGE_LOWERLIMIT)));
        swapUsageMap.put(JSONConstants.MonitoringPropertySettings.UNIT,                      serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGNODE_SWAPUSAGE_UNIT));

        // "ntpStatus"
        Map<String, Object> ntpStatusMap = new HashMap<String, Object>();
        ntpStatusMap.put(JSONConstants.MonitoringPropertySettings.SAMPLING_INTERVAL,         clusterPropertyFormatMap.get(ServerConfig.PARAM_MONITORING_INTERVAL_NTPSTATUS));

        // "propertySetup"
        Map<String, Object> propertySetupMap = new HashMap<String, Object>();
        propertySetupMap.put(JSONConstants.SsgClusterMonitoringProperty.AUDIT_SIZE,          auditSizeMap);
        propertySetupMap.put(JSONConstants.SsgNodeMonitoringProperty.OPERATING_STATUS,       operatingStatusMap);
        propertySetupMap.put(JSONConstants.SsgNodeMonitoringProperty.LOG_SIZE,               logSizeMap);
        propertySetupMap.put(JSONConstants.SsgNodeMonitoringProperty.DISK_USAGE,             diskUsageMap);
        propertySetupMap.put(JSONConstants.SsgNodeMonitoringProperty.RAID_STATUS,            raidStatusMap);
        propertySetupMap.put(JSONConstants.SsgNodeMonitoringProperty.CPU_TEMP,               cpuTempMap);
        propertySetupMap.put(JSONConstants.SsgNodeMonitoringProperty.CPU_USAGE,              cpuUsageMap);
        propertySetupMap.put(JSONConstants.SsgNodeMonitoringProperty.SWAP_USAGE,             swapUsageMap);
        propertySetupMap.put(JSONConstants.SsgNodeMonitoringProperty.NTP_STATUS,             ntpStatusMap);

        // Fill up jsonFormatMap
        jsonFormatMap.put(JSONConstants.READONLY, isReadOnly);
        jsonFormatMap.put(JSONConstants.SystemMonitoringSetup.PROPERTY_SETUP,                propertySetupMap);
        jsonFormatMap.put(JSONConstants.SystemMonitoringSetup.SAMPLING_INTERVAL_LOWER_LIMIT, Integer.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SAMPLINGINTERVAL_LOWERLIMIT)));
        jsonFormatMap.put(JSONConstants.SystemMonitoringSetup.DISABLE_ALL_NOTIFICATIONS,     clusterPropertyFormatMap.get(ServerConfig.PARAM_MONITORING_DISABLEALLNOTIFICATIONS));
        jsonFormatMap.put(JSONConstants.SystemMonitoringSetup.AUDIT_UPON_ALERT_STATE,        clusterPropertyFormatMap.get(ServerConfig.PARAM_MONITORING_AUDITUPONALERTSTATE));
        jsonFormatMap.put(JSONConstants.SystemMonitoringSetup.AUDIT_UPON_NORMAL_STATE,       clusterPropertyFormatMap.get(ServerConfig.PARAM_MONITORING_AUDITUPONNORMALSTATE));
        jsonFormatMap.put(JSONConstants.SystemMonitoringSetup.AUDIT_UPON_NOTIFICATION,       clusterPropertyFormatMap.get(ServerConfig.PARAM_MONITORING_AUDITUPONNOTIFICATION));

        return jsonFormatMap;
    }

    /**
     * Convert a JSON format map (from the browser) to a cluster-property format map (to be saved in the DB table, cluster_properties.)
     * @param jsonFormatMap: the given map storing all settings in the JSON convention.
     * @return a map in the cluster property convention.
     * @throws InvalidMonitoringSetupSettingException
     */
    private Map<String, Object> toClusterPropertyFormat(Map<String, Object> jsonFormatMap) throws InvalidMonitoringSetupSettingException {
        Map<String, Object> clusterPropertyFormatMap = new HashMap<String, Object>();

        // "propertySetup"
        Map<String, Object> propertySetupMap = (Map<String, Object>) jsonFormatMap.get(JSONConstants.SystemMonitoringSetup.PROPERTY_SETUP);
        if (propertySetupMap != null && !propertySetupMap.isEmpty()) {
            // "auditSize"
            Map<String, Object> auditSizeMap = (Map<String, Object>) propertySetupMap.get(JSONConstants.SsgClusterMonitoringProperty.AUDIT_SIZE);
            if (auditSizeMap != null && !auditSizeMap.isEmpty()) {
                int interval = getSamplingInterval(auditSizeMap, JSONConstants.SsgClusterMonitoringProperty.AUDIT_SIZE);
                clusterPropertyFormatMap.put(ServerConfig.PARAM_MONITORING_INTERVAL_SSGAUDITSIZE, interval);

                int triggerVal = getTriggerValue(auditSizeMap, JSONConstants.SsgClusterMonitoringProperty.AUDIT_SIZE, Integer.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGCLUSTER_SSGAUDITSIZE_LOWERLIMIT)), null);
                clusterPropertyFormatMap.put(ServerConfig.PARAM_MONITORING_TRIGGER_SSGAUDITSIZE, triggerVal);
            }

            // "operatingStatus"
            Map<String, Object> operatingStatusMap = (Map<String, Object>) propertySetupMap.get(JSONConstants.SsgNodeMonitoringProperty.OPERATING_STATUS);
            if (operatingStatusMap != null && !operatingStatusMap.isEmpty()) {
                int interval = getSamplingInterval(operatingStatusMap, JSONConstants.SsgNodeMonitoringProperty.OPERATING_STATUS);
                clusterPropertyFormatMap.put(ServerConfig.PARAM_MONITORING_INTERVAL_OPERATINGSTATUS, interval);
            }

            // "logSize"
            Map<String, Object> logSizeMap = (Map<String, Object>) propertySetupMap.get(JSONConstants.SsgNodeMonitoringProperty.LOG_SIZE);
            if (logSizeMap != null && !logSizeMap.isEmpty()) {
                int interval = getSamplingInterval(logSizeMap, JSONConstants.SsgNodeMonitoringProperty.LOG_SIZE);
                clusterPropertyFormatMap.put(ServerConfig.PARAM_MONITORING_INTERVAL_SSGLOGSIZE, interval);

                int triggerVal = getTriggerValue(logSizeMap, JSONConstants.SsgNodeMonitoringProperty.LOG_SIZE, Integer.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGNODE_SSGLOGSIZE_LOWERLIMIT)), null);
                clusterPropertyFormatMap.put(ServerConfig.PARAM_MONITORING_TRIGGER_SSGLOGSIZE, triggerVal);
            }

            // "diskUsage"
            Map<String, Object> diskUsageMap = (Map<String, Object>) propertySetupMap.get(JSONConstants.SsgNodeMonitoringProperty.DISK_USAGE);
            if (diskUsageMap != null && !diskUsageMap.isEmpty()) {
                int interval = getSamplingInterval(diskUsageMap, JSONConstants.SsgNodeMonitoringProperty.DISK_USAGE);
                clusterPropertyFormatMap.put(ServerConfig.PARAM_MONITORING_INTERVAL_DISKUSAGE, interval);

                int triggerVal = getTriggerValue(diskUsageMap, JSONConstants.SsgNodeMonitoringProperty.DISK_USAGE,
                    Integer.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGNODE_DISKUSAGE_LOWERLIMIT)),
                    Integer.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGNODE_DISKUSAGE_UPPERLIMIT)));
                clusterPropertyFormatMap.put(ServerConfig.PARAM_MONITORING_TRIGGER_DISKUSAGE, triggerVal);
            }

            // "raidStatus"
            Map<String, Object> raidStatusMap = (Map<String, Object>) propertySetupMap.get(JSONConstants.SsgNodeMonitoringProperty.RAID_STATUS);
            if (raidStatusMap != null && !raidStatusMap.isEmpty()) {
                int interval = getSamplingInterval(raidStatusMap, JSONConstants.SsgNodeMonitoringProperty.RAID_STATUS);
                clusterPropertyFormatMap.put(ServerConfig.PARAM_MONITORING_INTERVAL_RAIDSTATUS, interval);
            }

            // "cpuTemp"
            Map<String, Object> cpuTempMap = (Map<String, Object>) propertySetupMap.get(JSONConstants.SsgNodeMonitoringProperty.CPU_TEMP);
            if (cpuTempMap != null && !cpuTempMap.isEmpty()) {
                int interval = getSamplingInterval(cpuTempMap, JSONConstants.SsgNodeMonitoringProperty.CPU_TEMP);
                clusterPropertyFormatMap.put(ServerConfig.PARAM_MONITORING_INTERVAL_CPUTEMPERATURE, interval);

                int triggerVal = getTriggerValue(cpuTempMap, JSONConstants.SsgNodeMonitoringProperty.CPU_TEMP, Integer.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGNODE_CPUTEMPERATURE_LOWERLIMIT)), null);
                clusterPropertyFormatMap.put(ServerConfig.PARAM_MONITORING_TRIGGER_CPUTEMPERATURE, triggerVal);
            }

            // "cpuUsage"
            Map<String, Object> cpuUsageMap = (Map<String, Object>) propertySetupMap.get(JSONConstants.SsgNodeMonitoringProperty.CPU_USAGE);
            if (cpuUsageMap != null && !cpuUsageMap.isEmpty()) {
                int interval = getSamplingInterval(cpuUsageMap, JSONConstants.SsgNodeMonitoringProperty.CPU_USAGE);
                clusterPropertyFormatMap.put(ServerConfig.PARAM_MONITORING_INTERVAL_CPUUSAGE, interval);

                int triggerVal = getTriggerValue(cpuUsageMap, JSONConstants.SsgNodeMonitoringProperty.CPU_USAGE,
                    Integer.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGNODE_CPUUSAGE_LOWERLIMIT)),
                    Integer.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGNODE_CPUUSAGE_UPPERLIMIT)));
                clusterPropertyFormatMap.put(ServerConfig.PARAM_MONITORING_TRIGGER_CPUUSAGE, triggerVal);
            }

            // "swapUsage"
            Map<String, Object> swapUsageMap = (Map<String, Object>) propertySetupMap.get(JSONConstants.SsgNodeMonitoringProperty.SWAP_USAGE);
            if (swapUsageMap != null && !swapUsageMap.isEmpty()) {
                int interval = getSamplingInterval(swapUsageMap, JSONConstants.SsgNodeMonitoringProperty.SWAP_USAGE);
                clusterPropertyFormatMap.put(ServerConfig.PARAM_MONITORING_INTERVAL_SWAPUSAGE, interval);

                int triggerVal = getTriggerValue(swapUsageMap, JSONConstants.SsgNodeMonitoringProperty.SWAP_USAGE, Integer.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGNODE_SWAPUSAGE_LOWERLIMIT)), null);
                clusterPropertyFormatMap.put(ServerConfig.PARAM_MONITORING_TRIGGER_SWAPUSAGE, triggerVal);
            }

            // "ntpStatus"
            Map<String, Object> ntpStatusMap = (Map<String, Object>) propertySetupMap.get(JSONConstants.SsgNodeMonitoringProperty.NTP_STATUS);
            if (ntpStatusMap != null && !ntpStatusMap.isEmpty()) {
                int interval = getSamplingInterval(ntpStatusMap, JSONConstants.SsgNodeMonitoringProperty.NTP_STATUS);
                clusterPropertyFormatMap.put(ServerConfig.PARAM_MONITORING_INTERVAL_NTPSTATUS, interval);
            }
        }

        // "disableAllNotifications"
        Object optionObj = jsonFormatMap.get(JSONConstants.SystemMonitoringSetup.DISABLE_ALL_NOTIFICATIONS);
        if (optionObj != null) clusterPropertyFormatMap.put(ServerConfig.PARAM_MONITORING_DISABLEALLNOTIFICATIONS, validateOption(optionObj, JSONConstants.SystemMonitoringSetup.DISABLE_ALL_NOTIFICATIONS));

        // "auditUponAlertState"
        optionObj = jsonFormatMap.get(JSONConstants.SystemMonitoringSetup.AUDIT_UPON_ALERT_STATE);
        if (optionObj != null) clusterPropertyFormatMap.put(ServerConfig.PARAM_MONITORING_AUDITUPONALERTSTATE, validateOption(optionObj, JSONConstants.SystemMonitoringSetup.AUDIT_UPON_ALERT_STATE));

        // "auditUponNormalState"
        optionObj = jsonFormatMap.get(JSONConstants.SystemMonitoringSetup.AUDIT_UPON_NORMAL_STATE);
        if (optionObj != null) clusterPropertyFormatMap.put(ServerConfig.PARAM_MONITORING_AUDITUPONNORMALSTATE, validateOption(optionObj, JSONConstants.SystemMonitoringSetup.AUDIT_UPON_NORMAL_STATE));

        // "auditUponNotification"
        optionObj = jsonFormatMap.get(JSONConstants.SystemMonitoringSetup.AUDIT_UPON_NOTIFICATION);
        if (optionObj != null) clusterPropertyFormatMap.put(ServerConfig.PARAM_MONITORING_AUDITUPONNOTIFICATION, validateOption(optionObj, JSONConstants.SystemMonitoringSetup.AUDIT_UPON_NOTIFICATION));

        return clusterPropertyFormatMap;
    }

    /**
     * Validate a sampling interval and return it if it is valid.  Otherwise, throw an InvalidMonitoringSetupSettingException.
     * @param propertyMap: the property map, such as "auditSize" map, "operatingStatus" map, "diskUsage map", etc.
     * @param propertyName: the property name
     * @return a valid sampling interval.
     * @throws InvalidMonitoringSetupSettingException if there exists an invalid setup setting.
     */
    private int getSamplingInterval(Map<String, Object> propertyMap, String propertyName) throws InvalidMonitoringSetupSettingException {
        Object intervalObj = propertyMap.get(JSONConstants.MonitoringPropertySettings.SAMPLING_INTERVAL);
        if (intervalObj == null) {
            throw new InvalidMonitoringSetupSettingException("The property (NAME = '" + propertyName + "') does not include " + JSONConstants.MonitoringPropertySettings.SAMPLING_INTERVAL + ".");
        }

        int interval;
        try {
            interval = Integer.parseInt(intervalObj.toString());
        } catch (NumberFormatException e) {
            throw new InvalidMonitoringSetupSettingException("The property (NAME = '" + propertyName + "') includes an invalid " + JSONConstants.MonitoringPropertySettings.SAMPLING_INTERVAL + ".");
        }

        int samplingIntervalLowerLimit = Integer.parseInt(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SAMPLINGINTERVAL_LOWERLIMIT));
        if (interval < samplingIntervalLowerLimit) {
            throw new InvalidMonitoringSetupSettingException("In the property (NAME = '" + propertyName + "'), " + JSONConstants.MonitoringPropertySettings.SAMPLING_INTERVAL + " is less than the sampling interval lower limit (VALUE = " + samplingIntervalLowerLimit + ").");
        }

        return interval;
    }

    /**
     * Validate a trigger value and return it if it is valid against the lower limit or the upper limit.  Otherwise, throw an InvalidMonitoringSetupSettingException.
     * @param propertyMap: the property map, such as "auditSize" map, "operatingStatus" map, "diskUsage map", etc.
     * @param propertyName: the property name
     * @param lowerLimit: the lower limit of all trigger values associated with the property.
     * @param upperLimit: the upper limit of all trigger values associated with the property.
     * @return a valid trigger value
     * @throws InvalidMonitoringSetupSettingException if there exists an invalid setup setting.
     */
    private int getTriggerValue(Map<String, Object> propertyMap, String propertyName, Integer lowerLimit, Integer upperLimit) throws InvalidMonitoringSetupSettingException {
        Object triggerValObj = propertyMap.get(JSONConstants.MonitoringPropertySettings.DEFAULT_TRIGGER_VALUE);
        if (triggerValObj == null) {
            throw new InvalidMonitoringSetupSettingException("The property (NAME = '" + propertyName + "') does not include " + JSONConstants.MonitoringPropertySettings.DEFAULT_TRIGGER_VALUE + ".");
        }

        int triggerVal;
        try {
            triggerVal = Integer.parseInt(triggerValObj.toString());
        } catch (NumberFormatException e) {
            throw new InvalidMonitoringSetupSettingException("The property (NAME = '" + propertyName + "') includes an invalid " + JSONConstants.MonitoringPropertySettings.DEFAULT_TRIGGER_VALUE + ".");
        }

        if (lowerLimit != null && triggerVal < lowerLimit) {
            throw new InvalidMonitoringSetupSettingException("In the property (NAME = '" + propertyName + "'), " + JSONConstants.MonitoringPropertySettings.DEFAULT_TRIGGER_VALUE + " is less than the trigger lower limit (VALUE = " + lowerLimit + ").");
        }

        if (upperLimit != null && triggerVal > upperLimit) {
            throw new InvalidMonitoringSetupSettingException("In the property (NAME = '" + propertyName + "'), " + JSONConstants.MonitoringPropertySettings.DEFAULT_TRIGGER_VALUE + " is greater than the trigger upper limit (VALUE = " + upperLimit + ").");
        }

        return triggerVal;
    }

    /**
     * Validate a boolean-value option.  If it is not valid, throw an InvalidMonitoringSetupSettingException.
     * @param option: an option to be checked.
     * @param optionName: the name of an option.
     * @return true if the option has a valid boolean.
     * @throws InvalidMonitoringSetupSettingException if the option is an invalid boolean object.
     */
    private boolean validateOption(Object option, String optionName) throws InvalidMonitoringSetupSettingException {
        if (! (option instanceof Boolean)) {
            throw new InvalidMonitoringSetupSettingException("The setting of " + optionName + " has an invalid value.");
        }

        return (Boolean) option;
    }
}
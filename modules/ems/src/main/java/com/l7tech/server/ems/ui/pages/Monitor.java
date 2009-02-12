package com.l7tech.server.ems.ui.pages;

import com.l7tech.server.ems.ui.NavigationPage;
import com.l7tech.server.ems.enterprise.*;
import com.l7tech.server.ems.monitoring.*;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.security.rbac.AttemptedDeleteSpecific;
import com.l7tech.gateway.common.security.rbac.AttemptedReadSpecific;
import com.l7tech.objectmodel.*;
import com.l7tech.util.ExceptionUtils;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.*;

import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.RequestCycle;
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

    @SpringBean(name="entityMonitoringPropertySetupManager")
    private EntityMonitoringPropertySetupManager entityMonitoringPropertySetupManager;

    @SpringBean(name="ssgClusterNotificationSetupManager")
    private SsgClusterNotificationSetupManager ssgClusterNotificationSetupManager;

    @SpringBean(name="roleManager")
    private RoleManager roleManager;

    @SpringBean(name="enterpriseFolderManager")
    private EnterpriseFolderManager enterpriseFolderManager;

    @SpringBean(name="ssgClusterManager")
    private SsgClusterManager ssgClusterManager;

    @SpringBean(name="ssgNodeManager")
    private SsgNodeManager ssgNodeManager;

    @SpringBean(name="monitoringService")
    private MonitoringService monitoringService;

    private boolean isReadOnly;
    private Map<String, String> userProperties = Collections.emptyMap();

    public Monitor() {
        isReadOnly = checkReadOnly();

        try {
            userProperties = userPropertyManager.getUserProperties(getUser());
        } catch (FindException e) {
            logger.log(Level.WARNING, "Error loading user properties", e);
        }

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

        // Wicket component for getting system monitoring setup settings
        add(new JsonInteraction("getEntities", "getEntitiesUrl", new GettingEntitiesDataProvider()));

        // Wicket component for getting the monitoring properties of all entities
        add(new JsonInteraction("getCurrentEntitiesPropertyValues", "getCurrentEntitiesPropertyValuesUrl", new GettingCurrentEntitiesPropertyValuesDataProvider()));

        // Wicket component for getting entity monitoring property setup settings
        add(new JsonInteraction("getEntityMonitoringPropertySetupSettings", "getEntityMonitoringPropertySetupSettingsUrl", new GettingEntityMonitoringPropertySetupSettingsDataProvider()));

        // Wicket component for saving entity monitoring property setup settings
        add(new JsonPostInteraction("saveEntityMonitoringPropertySetupSettings", "saveEntityMonitoringPropertySetupSettingsUrl", new SaveEntityMonitoringPropertySetupSettingsDataProvider()));
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
                    if (rule.getType().equals(JSONConstants.NotificationType.E_MAIL)) {
                        rule.obtainParamsProps().remove(JSONConstants.NotificationEmailParams.PASSWORD);
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
                        if (notificationRule.getType().equals(JSONConstants.NotificationType.E_MAIL)) {
                            boolean requiresAuth = (Boolean) params.get(JSONConstants.NotificationEmailParams.REQUIRES_AUTH);
                            if (requiresAuth) {
                                String oldPassword = (String) notificationRule.getParamProp(JSONConstants.NotificationEmailParams.PASSWORD);
                                String newPassword = (String) params.get(JSONConstants.NotificationEmailParams.PASSWORD);
                                if (newPassword == null) {
                                    params.put(JSONConstants.NotificationEmailParams.PASSWORD, oldPassword);
                                }
                            } else {
                                params.remove(JSONConstants.NotificationEmailParams.USERNAME);
                                params.remove(JSONConstants.NotificationEmailParams.PASSWORD);
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
     * Data provider for getting all entities in the enterprise tree.
     */
    private final class GettingEntitiesDataProvider implements JsonDataProvider {
        @Override
        public Object getData() {
            try {
                final List<Object> entities = new ArrayList<Object>();
                final EnterpriseFolder rootFolder = enterpriseFolderManager.findRootFolder();
                entities.add(new JSONSupport( rootFolder ){
                    @Override
                    protected void writeJson() {
                        super.writeJson();
                        add(JSONConstants.RBAC_CUD, securityManager.hasPermission( new AttemptedDeleteSpecific(EntityType.ESM_ENTERPRISE_FOLDER, rootFolder)) );
                    }
                } );
                addChildren(entities, rootFolder);
                return entities;
            } catch (FindException e) {
                logger.warning(e.toString());
                return new JSONException(e);
            }
        }

        @Override
        public void setData(Object jsonData) {
            throw new UnsupportedOperationException("setData not required in JsonInteraction");
        }

        private void addChildren(final List<Object> nodes, final EnterpriseFolder folder) throws FindException {
            // Display order is alphabetical on name, with folders before clusters.
            for ( final EnterpriseFolder childFolder : enterpriseFolderManager.findChildFolders(folder)) {
                nodes.add( new JSONSupport(childFolder) {
                    @Override
                    protected void writeJson() {
                        super.writeJson();
                        add(JSONConstants.RBAC_CUD, securityManager.hasPermission( new AttemptedDeleteSpecific(EntityType.ESM_ENTERPRISE_FOLDER, childFolder)) );
                    }
                } );
                addChildren(nodes, childFolder);
            }

            for (final SsgCluster childCluster : ssgClusterManager.findChildSsgClusters(folder)) {
                nodes.add( new JSONSupport( childCluster ){
                    @Override
                    protected void writeJson() {
                        super.writeJson();
                        add(JSONConstants.RBAC_CUD, securityManager.hasPermission( new AttemptedDeleteSpecific(EntityType.ESM_SSG_CLUSTER, childCluster)) );
                        add(JSONConstants.ACCESS_STATUS, userProperties.containsKey("cluster." +  childCluster.getGuid() + ".trusteduser"));
                    }
                });

                // Add SSG nodes
                for ( SsgNode node : new TreeSet<SsgNode>(childCluster.getNodes()) ) {
                    nodes.add( new JSONSupport( node ){
                        @Override
                        protected void writeJson() {
                            super.writeJson();
                            add( JSONConstants.ACCESS_STATUS, securityManager.hasPermission( new AttemptedReadSpecific(EntityType.ESM_SSG_CLUSTER, childCluster.getId())) );
                        }
                    });
                }
            }
        }
    }

    /**
     * Data provider for getting entities monitoring properties.
     */
    private final class GettingCurrentEntitiesPropertyValuesDataProvider implements JsonDataProvider {

        @Override
        public Object getData() {
            try {
                // Note: currently we still use demo data.  After the functionality of MonitoringApi is completely done in Process Controller, we will use the real data.
                boolean useDemoData = true;

                List<EntityMonitoringPropertyValues> entitiesList = new ArrayList<EntityMonitoringPropertyValues>();

                if (useDemoData) {
                    // Demo data
                    entitiesList = getDemoPropertyValuesData();
                } else {
                    // Real data
                    // Use MonitoringService to call MonitoringApi to get current property statuses
                    for (SsgCluster ssgCluster: ssgClusterManager.findAll()) {
                        // First get the current values for each SSG node.
                        for (SsgNode ssgNode: ssgCluster.getNodes()) {
                            entitiesList.add(monitoringService.getCurrentSsgNodePropertiesStatus(ssgNode));
                        }
                        // Then, get the current value (audit size) for the SSG cluster.
                        entitiesList.add(monitoringService.getCurrentSsgClusterPropertyStatus(ssgCluster.getGuid()));
                    }
                }

                Map<String, Object> jsonDataMap = new HashMap<String, Object>();
                jsonDataMap.put("nextRefreshInterval", Long.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_NEXTREFRESH_INTERVAL)));
                jsonDataMap.put("entities", entitiesList);

                return jsonDataMap;
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
     * Data provider for getting entity monitoring property setup settings.
     */
    private final class GettingEntityMonitoringPropertySetupSettingsDataProvider implements JsonDataProvider {
        @Override
        public Object getData() {
            Object returnValue = null;

            WebRequest request = (WebRequest) RequestCycle.get().getRequest();
            String entityGuid = request.getParameter("entityId");
            String entityType = request.getParameter("entityType");
            String propertyType = request.getParameter("propertyType");

            String errmsg = null;
            Throwable err = null;

            if (entityGuid == null || entityType == null || propertyType == null) {
                errmsg = "Missing parameter(s): " +
                        entityGuid==null? "entityId, ":"" +
                        entityType==null? "entityType, ":"" +
                        propertyType==null? "propertyType.":"";
            } else {
                try {
                    NamedEntity entity = null;
                    if (entityType.equals(JSONConstants.EntityType.SSG_CLUSTER)) {
                        entity = ssgClusterManager.findByGuid(entityGuid);
                    } else if (entityType.equals(JSONConstants.EntityType.SSG_NODE)) {
                        entity = ssgNodeManager.findByGuid(entityGuid);
                    } else {
                        errmsg = "Entity type ('" + entityType + "') is invalid.";
                    }
                    EntityMonitoringPropertySetup propertySetup = entityMonitoringPropertySetupManager.findByEntityGuidAndPropertyType(entityGuid, propertyType);

                    // Case 1: property setup already exists in the database.
                    if (propertySetup != null) {
                        propertySetup.setEntity(entity);
                        returnValue = propertySetup;
                    }
                    // Case 2: this is a new property setup.
                    else if (entity != null) {
                        EntityMonitoringPropertySetup initSetup = new EntityMonitoringPropertySetup(entity, propertyType);

                        // Set all boolean values
                        initSetup.setMonitoringEnabled(false);
                        initSetup.setTriggerEnabled(false);
                        initSetup.setNotificationEnabled(false);

                        // Set trigger value and unit.
                        Map<String, Object> systemSetup = systemMonitoringSetupSettingsManager.findSetupSettings();
                        initSetup.setTriggerValue((Long) systemSetup.get("trigger." + propertyType));
                        initSetup.setUnit(serverConfig.getProperty("monitoring." + entityType + "." + propertyType + ".unit"));

                        // Set notification rules.
                        String ssgClusterGuid = entityType.equals(JSONConstants.EntityType.SSG_NODE)? ((SsgNode)entity).getSsgCluster().getGuid() : entityGuid;
                        SsgClusterNotificationSetup notificationSetup = ssgClusterNotificationSetupManager.findByEntityGuid(ssgClusterGuid);
                        if (notificationSetup == null) {
                            notificationSetup = new SsgClusterNotificationSetup(ssgClusterGuid);
                            ssgClusterNotificationSetupManager.save(notificationSetup);
                        }
                        initSetup.setSsgClusterNotificationSetup(notificationSetup);

                        // Save the initial entity property setup
                        entityMonitoringPropertySetupManager.save(initSetup);

                        // Return the initial entity property setup.
                        returnValue = initSetup;
                    }
                } catch (FindException e) {
                    errmsg = "Cannot find the entity property setup (Entity_GUID = '" + entityGuid + "' and Entity_TYPE = '" + entityType + "').";
                    err = e;
                } catch (SaveException e) {
                    errmsg = "Cannot save the initial entity property setup (Entity_GUID = '" + entityGuid + "' and Entity_TYPE = '" + entityType + "').";
                    err = e;
                } catch (InvalidMonitoringSetupSettingException e) {
                    errmsg = e.getMessage();
                    err = e;
                }
            }

            if (errmsg != null) {
                logger.warning(errmsg);
                err.printStackTrace();
                returnValue = new JSONException(new Exception(errmsg, err));
            } else {
                returnValue = new JSONSupport((JSON.Convertible)returnValue){
                    @Override
                    protected void writeJson() {
                        super.writeJson();
                        add(JSONConstants.READONLY, checkReadOnly());
                    }
                };
            }

            return returnValue;
        }

        @Override
        public void setData(Object jsonData) {
        }
    }

    /**
     * Data provider for saving system monitoring setup settings.
     */
    private final class SaveEntityMonitoringPropertySetupSettingsDataProvider implements JsonDataProvider {
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
                Map<String, Object> entityProps = (Map<String, Object>) jsonFormatMap.get(JSONConstants.ENTITY_PROPS_SETUP.ENTITY);
                String entityGuid = (String) entityProps.get(JSONConstants.ID);
                String entityType = (String) entityProps.get(JSONConstants.TYPE);
                String propertyType = (String) jsonFormatMap.get(JSONConstants.ENTITY_PROPS_SETUP.PROP_TYPE);
                try {
                    EntityMonitoringPropertySetup propertySetup = entityMonitoringPropertySetupManager.findByEntityGuidAndPropertyType(entityGuid, propertyType);

                    // Update monitoring enable status
                    boolean monitoringEnabled = (Boolean)jsonFormatMap.get(JSONConstants.ENTITY_PROPS_SETUP.MONITORING_ENABLED);
                    propertySetup.setMonitoringEnabled(monitoringEnabled);

                    // Update trigger enable status.  Also update trigger value and notification enable status if appliable.
                    if (monitoringEnabled) {
                        boolean triggerEnabled = (Boolean)jsonFormatMap.get(JSONConstants.ENTITY_PROPS_SETUP.TRIGGER_ENABLED);
                        propertySetup.setTriggerEnabled(triggerEnabled);
                        if (triggerEnabled) {
                            propertySetup.setTriggerValue((Long)jsonFormatMap.get(JSONConstants.ENTITY_PROPS_SETUP.TRIGGER_VALUE));
                            propertySetup.setNotificationEnabled((Boolean)jsonFormatMap.get(JSONConstants.ENTITY_PROPS_SETUP.NOTIFICATION_ENABLED));
                        }
                    }

                    // Find and update notification rules
                    NamedEntity entity;
                    if (entityType.equals(JSONConstants.EntityType.SSG_CLUSTER)) {
                        entity = ssgClusterManager.findByGuid(entityGuid);
                    } else if (entityType.equals(JSONConstants.EntityType.SSG_NODE)) {
                        entity = ssgNodeManager.findByGuid(entityGuid);
                    } else {
                        throw new InvalidMonitoringSetupSettingException("Entity type ('" + entityType + "') is invalid.");
                    }

                    String ssgClusterGuid = entityType.equals(JSONConstants.EntityType.SSG_NODE)? ((SsgNode)entity).getSsgCluster().getGuid() : entityGuid;
                    SsgClusterNotificationSetup ssgClusterNotificationSetup = ssgClusterNotificationSetupManager.findByEntityGuid(ssgClusterGuid);
                    if (ssgClusterNotificationSetup == null) {
                        ssgClusterNotificationSetup = new SsgClusterNotificationSetup(ssgClusterGuid);
                    }
                    Object[] notificationRuleMaps = (Object[]) jsonFormatMap.get(JSONConstants.ENTITY_PROPS_SETUP.NOTIFICATION_RULES);
                    Set<SystemMonitoringNotificationRule> systemNotificationRules = new HashSet<SystemMonitoringNotificationRule>();
                    for (Object ruleMap: notificationRuleMaps) {
                        String guid = (String) ((Map<String, Object>)ruleMap).get(JSONConstants.ID);
                        systemNotificationRules.add(systemMonitoringNotificationRulesManager.findByGuid(guid));
                    }
                    ssgClusterNotificationSetup.setSystemNotificationRules(systemNotificationRules);
                    ssgClusterNotificationSetupManager.update(ssgClusterNotificationSetup);
                    propertySetup.setSsgClusterNotificationSetup(ssgClusterNotificationSetup);

                    // Update the entity monitoring property setup settings
                    entityMonitoringPropertySetupManager.update(propertySetup);

                    // Send null if saving is successful.
                    returnValue = null;
                } catch (Exception e) {
                    e.printStackTrace();
                    String errmsg = "Cannot save the entity property setup (Entity_GUID = '" + entityGuid + "' and Entity_TYPE = '" + entityType + "').";
                    logger.log(Level.WARNING, errmsg);
                    returnValue = new JSONException(new Exception(errmsg, e));
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
        auditSizeMap.put(JSONConstants.MonitoringPropertySettings.SAMPLING_INTERVAL,         clusterPropertyFormatMap.get(ServerConfig.PARAM_MONITORING_INTERVAL_AUDITSIZE));
        auditSizeMap.put(JSONConstants.MonitoringPropertySettings.DEFAULT_TRIGGER_VALUE,     clusterPropertyFormatMap.get(ServerConfig.PARAM_MONITORING_TRIGGER_AUDITSIZE));
        auditSizeMap.put(JSONConstants.MonitoringPropertySettings.TRIGGER_VALUE_LOWER_LIMIT, Long.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGCLUSTER_AUDITSIZE_LOWERLIMIT)));
        auditSizeMap.put(JSONConstants.MonitoringPropertySettings.UNIT,                      serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGCLUSTER_AUDITSIZE_UNIT));

        // "operatingStatus"
        Map<String, Object> operatingStatusMap = new HashMap<String, Object>();
        operatingStatusMap.put(JSONConstants.MonitoringPropertySettings.SAMPLING_INTERVAL,   clusterPropertyFormatMap.get(ServerConfig.PARAM_MONITORING_INTERVAL_OPERATINGSTATUS));

        // "logSize"
        Map<String, Object> logSizeMap = new HashMap<String, Object>();
        logSizeMap.put(JSONConstants.MonitoringPropertySettings.SAMPLING_INTERVAL,           clusterPropertyFormatMap.get(ServerConfig.PARAM_MONITORING_INTERVAL_LOGSIZE));
        logSizeMap.put(JSONConstants.MonitoringPropertySettings.DEFAULT_TRIGGER_VALUE,       clusterPropertyFormatMap.get(ServerConfig.PARAM_MONITORING_TRIGGER_LOGSIZE));
        logSizeMap.put(JSONConstants.MonitoringPropertySettings.TRIGGER_VALUE_LOWER_LIMIT,   Long.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGNODE_LOGSIZE_LOWERLIMIT)));
        logSizeMap.put(JSONConstants.MonitoringPropertySettings.UNIT,                        serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGNODE_LOGSIZE_UNIT));

        // "diskUsage"
        Map<String, Object> diskUsageMap = new HashMap<String, Object>();
        diskUsageMap.put(JSONConstants.MonitoringPropertySettings.SAMPLING_INTERVAL,         clusterPropertyFormatMap.get(ServerConfig.PARAM_MONITORING_INTERVAL_DISKUSAGE));
        diskUsageMap.put(JSONConstants.MonitoringPropertySettings.DEFAULT_TRIGGER_VALUE,     clusterPropertyFormatMap.get(ServerConfig.PARAM_MONITORING_TRIGGER_DISKUSAGE));
        diskUsageMap.put(JSONConstants.MonitoringPropertySettings.TRIGGER_VALUE_LOWER_LIMIT, Long.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGNODE_DISKUSAGE_LOWERLIMIT)));
        diskUsageMap.put(JSONConstants.MonitoringPropertySettings.TRIGGER_VALUE_UPPER_LIMIT, Long.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGNODE_DISKUSAGE_UPPERLIMIT)));
        diskUsageMap.put(JSONConstants.MonitoringPropertySettings.UNIT,                      serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGNODE_DISKUSAGE_UNIT));

        // "raidStatus"
        Map<String, Object> raidStatusMap = new HashMap<String, Object>();
        raidStatusMap.put(JSONConstants.MonitoringPropertySettings.SAMPLING_INTERVAL,        clusterPropertyFormatMap.get(ServerConfig.PARAM_MONITORING_INTERVAL_RAIDSTATUS));

        // "cpuTemp"
        Map<String, Object> cpuTempMap = new HashMap<String, Object>();
        cpuTempMap.put(JSONConstants.MonitoringPropertySettings.SAMPLING_INTERVAL,           clusterPropertyFormatMap.get(ServerConfig.PARAM_MONITORING_INTERVAL_CPUTEMPERATURE));
        cpuTempMap.put(JSONConstants.MonitoringPropertySettings.DEFAULT_TRIGGER_VALUE,       clusterPropertyFormatMap.get(ServerConfig.PARAM_MONITORING_TRIGGER_CPUTEMPERATURE));
        cpuTempMap.put(JSONConstants.MonitoringPropertySettings.TRIGGER_VALUE_LOWER_LIMIT,   Long.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGNODE_CPUTEMPERATURE_LOWERLIMIT)));
        cpuTempMap.put(JSONConstants.MonitoringPropertySettings.UNIT,                        serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGNODE_CPUTEMPERATURE_UNIT));

        // "cpuUsage"
        Map<String, Object> cpuUsageMap = new HashMap<String, Object>();
        cpuUsageMap.put(JSONConstants.MonitoringPropertySettings.SAMPLING_INTERVAL,          clusterPropertyFormatMap.get(ServerConfig.PARAM_MONITORING_INTERVAL_CPUUSAGE));
        cpuUsageMap.put(JSONConstants.MonitoringPropertySettings.DEFAULT_TRIGGER_VALUE,      clusterPropertyFormatMap.get(ServerConfig.PARAM_MONITORING_TRIGGER_CPUUSAGE));
        cpuUsageMap.put(JSONConstants.MonitoringPropertySettings.TRIGGER_VALUE_LOWER_LIMIT,  Long.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGNODE_CPUUSAGE_LOWERLIMIT)));
        cpuUsageMap.put(JSONConstants.MonitoringPropertySettings.TRIGGER_VALUE_UPPER_LIMIT,  Long.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGNODE_CPUUSAGE_UPPERLIMIT)));
        cpuUsageMap.put(JSONConstants.MonitoringPropertySettings.UNIT,                       serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGNODE_CPUUSAGE_UNIT));

        // "swapUsage"
        Map<String, Object> swapUsageMap = new HashMap<String, Object>();
        swapUsageMap.put(JSONConstants.MonitoringPropertySettings.SAMPLING_INTERVAL,         clusterPropertyFormatMap.get(ServerConfig.PARAM_MONITORING_INTERVAL_SWAPUSAGE));
        swapUsageMap.put(JSONConstants.MonitoringPropertySettings.DEFAULT_TRIGGER_VALUE,     clusterPropertyFormatMap.get(ServerConfig.PARAM_MONITORING_TRIGGER_SWAPUSAGE));
        swapUsageMap.put(JSONConstants.MonitoringPropertySettings.TRIGGER_VALUE_LOWER_LIMIT, Long.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGNODE_SWAPUSAGE_LOWERLIMIT)));
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
        jsonFormatMap.put(JSONConstants.SystemMonitoringSetup.SAMPLING_INTERVAL_LOWER_LIMIT, Long.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SAMPLINGINTERVAL_LOWERLIMIT)));
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
                long interval = getSamplingInterval(auditSizeMap, JSONConstants.SsgClusterMonitoringProperty.AUDIT_SIZE);
                clusterPropertyFormatMap.put(ServerConfig.PARAM_MONITORING_INTERVAL_AUDITSIZE, interval);

                long triggerVal = getTriggerValue(auditSizeMap, JSONConstants.SsgClusterMonitoringProperty.AUDIT_SIZE, Long.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGCLUSTER_AUDITSIZE_LOWERLIMIT)), null);
                clusterPropertyFormatMap.put(ServerConfig.PARAM_MONITORING_TRIGGER_AUDITSIZE, triggerVal);
            }

            // "operatingStatus"
            Map<String, Object> operatingStatusMap = (Map<String, Object>) propertySetupMap.get(JSONConstants.SsgNodeMonitoringProperty.OPERATING_STATUS);
            if (operatingStatusMap != null && !operatingStatusMap.isEmpty()) {
                long interval = getSamplingInterval(operatingStatusMap, JSONConstants.SsgNodeMonitoringProperty.OPERATING_STATUS);
                clusterPropertyFormatMap.put(ServerConfig.PARAM_MONITORING_INTERVAL_OPERATINGSTATUS, interval);
            }

            // "logSize"
            Map<String, Object> logSizeMap = (Map<String, Object>) propertySetupMap.get(JSONConstants.SsgNodeMonitoringProperty.LOG_SIZE);
            if (logSizeMap != null && !logSizeMap.isEmpty()) {
                long interval = getSamplingInterval(logSizeMap, JSONConstants.SsgNodeMonitoringProperty.LOG_SIZE);
                clusterPropertyFormatMap.put(ServerConfig.PARAM_MONITORING_INTERVAL_LOGSIZE, interval);

                long triggerVal = getTriggerValue(logSizeMap, JSONConstants.SsgNodeMonitoringProperty.LOG_SIZE, Long.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGNODE_LOGSIZE_LOWERLIMIT)), null);
                clusterPropertyFormatMap.put(ServerConfig.PARAM_MONITORING_TRIGGER_LOGSIZE, triggerVal);
            }

            // "diskUsage"
            Map<String, Object> diskUsageMap = (Map<String, Object>) propertySetupMap.get(JSONConstants.SsgNodeMonitoringProperty.DISK_USAGE);
            if (diskUsageMap != null && !diskUsageMap.isEmpty()) {
                long interval = getSamplingInterval(diskUsageMap, JSONConstants.SsgNodeMonitoringProperty.DISK_USAGE);
                clusterPropertyFormatMap.put(ServerConfig.PARAM_MONITORING_INTERVAL_DISKUSAGE, interval);

                long triggerVal = getTriggerValue(diskUsageMap, JSONConstants.SsgNodeMonitoringProperty.DISK_USAGE,
                    Long.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGNODE_DISKUSAGE_LOWERLIMIT)),
                    Long.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGNODE_DISKUSAGE_UPPERLIMIT)));
                clusterPropertyFormatMap.put(ServerConfig.PARAM_MONITORING_TRIGGER_DISKUSAGE, triggerVal);
            }

            // "raidStatus"
            Map<String, Object> raidStatusMap = (Map<String, Object>) propertySetupMap.get(JSONConstants.SsgNodeMonitoringProperty.RAID_STATUS);
            if (raidStatusMap != null && !raidStatusMap.isEmpty()) {
                long interval = getSamplingInterval(raidStatusMap, JSONConstants.SsgNodeMonitoringProperty.RAID_STATUS);
                clusterPropertyFormatMap.put(ServerConfig.PARAM_MONITORING_INTERVAL_RAIDSTATUS, interval);
            }

            // "cpuTemp"
            Map<String, Object> cpuTempMap = (Map<String, Object>) propertySetupMap.get(JSONConstants.SsgNodeMonitoringProperty.CPU_TEMP);
            if (cpuTempMap != null && !cpuTempMap.isEmpty()) {
                long interval = getSamplingInterval(cpuTempMap, JSONConstants.SsgNodeMonitoringProperty.CPU_TEMP);
                clusterPropertyFormatMap.put(ServerConfig.PARAM_MONITORING_INTERVAL_CPUTEMPERATURE, interval);

                long triggerVal = getTriggerValue(cpuTempMap, JSONConstants.SsgNodeMonitoringProperty.CPU_TEMP, Long.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGNODE_CPUTEMPERATURE_LOWERLIMIT)), null);
                clusterPropertyFormatMap.put(ServerConfig.PARAM_MONITORING_TRIGGER_CPUTEMPERATURE, triggerVal);
            }

            // "cpuUsage"
            Map<String, Object> cpuUsageMap = (Map<String, Object>) propertySetupMap.get(JSONConstants.SsgNodeMonitoringProperty.CPU_USAGE);
            if (cpuUsageMap != null && !cpuUsageMap.isEmpty()) {
                long interval = getSamplingInterval(cpuUsageMap, JSONConstants.SsgNodeMonitoringProperty.CPU_USAGE);
                clusterPropertyFormatMap.put(ServerConfig.PARAM_MONITORING_INTERVAL_CPUUSAGE, interval);

                long triggerVal = getTriggerValue(cpuUsageMap, JSONConstants.SsgNodeMonitoringProperty.CPU_USAGE,
                    Long.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGNODE_CPUUSAGE_LOWERLIMIT)),
                    Long.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGNODE_CPUUSAGE_UPPERLIMIT)));
                clusterPropertyFormatMap.put(ServerConfig.PARAM_MONITORING_TRIGGER_CPUUSAGE, triggerVal);
            }

            // "swapUsage"
            Map<String, Object> swapUsageMap = (Map<String, Object>) propertySetupMap.get(JSONConstants.SsgNodeMonitoringProperty.SWAP_USAGE);
            if (swapUsageMap != null && !swapUsageMap.isEmpty()) {
                long interval = getSamplingInterval(swapUsageMap, JSONConstants.SsgNodeMonitoringProperty.SWAP_USAGE);
                clusterPropertyFormatMap.put(ServerConfig.PARAM_MONITORING_INTERVAL_SWAPUSAGE, interval);

                long triggerVal = getTriggerValue(swapUsageMap, JSONConstants.SsgNodeMonitoringProperty.SWAP_USAGE, Long.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGNODE_SWAPUSAGE_LOWERLIMIT)), null);
                clusterPropertyFormatMap.put(ServerConfig.PARAM_MONITORING_TRIGGER_SWAPUSAGE, triggerVal);
            }

            // "ntpStatus"
            Map<String, Object> ntpStatusMap = (Map<String, Object>) propertySetupMap.get(JSONConstants.SsgNodeMonitoringProperty.NTP_STATUS);
            if (ntpStatusMap != null && !ntpStatusMap.isEmpty()) {
                long interval = getSamplingInterval(ntpStatusMap, JSONConstants.SsgNodeMonitoringProperty.NTP_STATUS);
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
    private long getSamplingInterval(Map<String, Object> propertyMap, String propertyName) throws InvalidMonitoringSetupSettingException {
        Object intervalObj = propertyMap.get(JSONConstants.MonitoringPropertySettings.SAMPLING_INTERVAL);
        if (intervalObj == null) {
            throw new InvalidMonitoringSetupSettingException("The property (NAME = '" + propertyName + "') does not include " + JSONConstants.MonitoringPropertySettings.SAMPLING_INTERVAL + ".");
        }

        long interval;
        try {
            interval = Long.parseLong(intervalObj.toString());
        } catch (NumberFormatException e) {
            throw new InvalidMonitoringSetupSettingException("The property (NAME = '" + propertyName + "') includes an invalid " + JSONConstants.MonitoringPropertySettings.SAMPLING_INTERVAL + ".");
        }

        long samplingIntervalLowerLimit = Long.parseLong(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SAMPLINGINTERVAL_LOWERLIMIT));
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
    private long getTriggerValue(Map<String, Object> propertyMap, String propertyName, Long lowerLimit, Long upperLimit) throws InvalidMonitoringSetupSettingException {
        Object triggerValObj = propertyMap.get(JSONConstants.MonitoringPropertySettings.DEFAULT_TRIGGER_VALUE);
        if (triggerValObj == null) {
            throw new InvalidMonitoringSetupSettingException("The property (NAME = '" + propertyName + "') does not include " + JSONConstants.MonitoringPropertySettings.DEFAULT_TRIGGER_VALUE + ".");
        }

        long triggerVal;
        try {
            triggerVal = Long.parseLong(triggerValObj.toString());
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

    // For demo only, temporarily create property values for all SSG clusters and SSG nodes.
    private List<EntityMonitoringPropertyValues> getDemoPropertyValuesData() {
        List<EntityMonitoringPropertyValues> entitiesList = new ArrayList<EntityMonitoringPropertyValues>();

        Map<String, Object> demo_ssgCluster_propsMap = new HashMap<String, Object>();
        EntityMonitoringPropertyValues.PropertyValues demo_ssgCluster_propValues              = new EntityMonitoringPropertyValues.PropertyValues(true, "31190", serverConfig.getProperty(ServerConfig.PARAM_MONITORING_SSGCLUSTER_AUDITSIZE_UNIT), false);
        demo_ssgCluster_propsMap.put(JSONConstants.SsgClusterMonitoringProperty.AUDIT_SIZE, demo_ssgCluster_propValues);

        Map<String, Object> demo_ssgNode_propsMap = new HashMap<String, Object>();
        EntityMonitoringPropertyValues.PropertyValues demo_ssgNode_operatingStatus_propValues = new EntityMonitoringPropertyValues.PropertyValues(true, "OK", null, false);
        EntityMonitoringPropertyValues.PropertyValues demo_ssgNode_logSize_propValues         = new EntityMonitoringPropertyValues.PropertyValues(true, "4", "MB", false);
        EntityMonitoringPropertyValues.PropertyValues demo_ssgNode_diskUsage_propValues       = new EntityMonitoringPropertyValues.PropertyValues(true, "10", "%", false);
        EntityMonitoringPropertyValues.PropertyValues demo_ssgNode_raidStatus_propValues      = new EntityMonitoringPropertyValues.PropertyValues();
        EntityMonitoringPropertyValues.PropertyValues demo_ssgNode_cpuTemp_propValues         = new EntityMonitoringPropertyValues.PropertyValues(true, "53", "\u00b0C", true);
        EntityMonitoringPropertyValues.PropertyValues demo_ssgNode_cpuUsage_propValues        = new EntityMonitoringPropertyValues.PropertyValues(true, "80", "%", false);
        EntityMonitoringPropertyValues.PropertyValues demo_ssgNode_swapUsage_propValues       = new EntityMonitoringPropertyValues.PropertyValues(true, "1200", "MB", false);
        EntityMonitoringPropertyValues.PropertyValues demo_ssgNode_ntpStatus_propValues       = new EntityMonitoringPropertyValues.PropertyValues(true, "OK", null, false);
        demo_ssgNode_propsMap.put(JSONConstants.SsgNodeMonitoringProperty.OPERATING_STATUS, demo_ssgNode_operatingStatus_propValues);
        demo_ssgNode_propsMap.put(JSONConstants.SsgNodeMonitoringProperty.LOG_SIZE,         demo_ssgNode_logSize_propValues);
        demo_ssgNode_propsMap.put(JSONConstants.SsgNodeMonitoringProperty.DISK_USAGE,       demo_ssgNode_diskUsage_propValues);
        demo_ssgNode_propsMap.put(JSONConstants.SsgNodeMonitoringProperty.RAID_STATUS,      demo_ssgNode_raidStatus_propValues);
        demo_ssgNode_propsMap.put(JSONConstants.SsgNodeMonitoringProperty.CPU_TEMP,         demo_ssgNode_cpuTemp_propValues);
        demo_ssgNode_propsMap.put(JSONConstants.SsgNodeMonitoringProperty.CPU_USAGE,        demo_ssgNode_cpuUsage_propValues);
        demo_ssgNode_propsMap.put(JSONConstants.SsgNodeMonitoringProperty.SWAP_USAGE,       demo_ssgNode_swapUsage_propValues);
        demo_ssgNode_propsMap.put(JSONConstants.SsgNodeMonitoringProperty.NTP_STATUS,       demo_ssgNode_ntpStatus_propValues);

        try {
            for (SsgCluster ssgCluster: ssgClusterManager.findAll()) {
                entitiesList.add(new EntityMonitoringPropertyValues(ssgCluster.getGuid(), demo_ssgCluster_propsMap));
                for (SsgNode ssgNode: ssgCluster.getNodes()) {
                    entitiesList.add(new EntityMonitoringPropertyValues(ssgNode.getGuid(), demo_ssgNode_propsMap));
                }
            }
        } catch (FindException e) {
        }

        return entitiesList;
    }
}

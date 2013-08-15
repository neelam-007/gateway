package com.l7tech.server.ems.ui.pages;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.gateway.common.audit.SystemAuditRecord;
import com.l7tech.gateway.common.security.rbac.AttemptedDeleteAll;
import com.l7tech.gateway.common.security.rbac.AttemptedDeleteSpecific;
import com.l7tech.gateway.common.security.rbac.AttemptedReadSpecific;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.server.audit.AuditContextUtils;
import com.l7tech.server.ems.EsmConfigParams;
import com.l7tech.server.ems.EsmMessages;
import com.l7tech.server.ems.audit.AuditContextFactoryImpl;
import com.l7tech.server.ems.enterprise.*;
import com.l7tech.server.ems.enterprise.JSONConstants.MonitoringPropertySettings;
import com.l7tech.server.ems.monitoring.*;
import com.l7tech.server.ems.ui.NavigationPage;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.WebRequest;
import org.mortbay.util.ajax.JSON;

import javax.inject.Inject;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
@NavigationPage(page="Monitor",section="ManageGateways",sectionPage="Monitor",pageUrl="Monitor.html")
public class Monitor extends EsmStandardWebPage {
    private static final Logger logger = Logger.getLogger(Monitor.class.getName());
    private static final Object previousPropValuesListSync = new Object();
    private static List<EntityMonitoringPropertyValues> previousPropValuesList = new ArrayList<EntityMonitoringPropertyValues>();

    @Inject
    private Config config;

    @Inject
    private SystemMonitoringSetupSettingsManager systemMonitoringSetupSettingsManager;

    @Inject
    private SystemMonitoringNotificationRulesManager systemMonitoringNotificationRulesManager;

    @Inject
    private EntityMonitoringPropertySetupManager entityMonitoringPropertySetupManager;

    @Inject
    private SsgClusterNotificationSetupManager ssgClusterNotificationSetupManager;

    @Inject
    private RoleManager roleManager;

    @Inject
    private EnterpriseFolderManager enterpriseFolderManager;

    @Inject
    private SsgClusterManager ssgClusterManager;

    @Inject
    private SsgNodeManager ssgNodeManager;

    @Inject
    private MonitoringService monitoringService;

    @Inject
    private AuditContextFactoryImpl auditContextFactory;

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
        add(new JsonInteraction("getSystemMonitoringSetupSettings", "getSystemMonitoringSetupSettingsUrl",
            new GettingSystemMonitoringSetupSettingsDataProvider()));

        // Wicket component for saving system monitoring setup settings
        add(new JsonPostInteraction("saveSystemMonitoringSetupSettings", "saveSystemMonitoringSetupSettingsUrl",
            new SavingSystemMonitoringSetupSettingsDataProvider()));

        // Wicket component for getting system monitoring notification rules
        add(new JsonInteraction("getSystemMonitoringNotificationRules", "getSystemMonitoringNotificationRulesUrl",
            new GettingSystemMonitoringNotificationRulesDataProvider()));

        // Wicket component for saving/editing a system monitoring notification rule
        add(new JsonPostInteraction("saveSystemMonitoringNotificationRule", "saveSystemMonitoringNotificationRuleUrl",
            new SavingSystemMonitoringNotificationRuleDataProvider()));

        // Wicket component for deleting a system monitoring notification rule
        final HiddenField<String> deleteNotificationRuleDialog_id = new HiddenField<String>("deleteNotificationRuleDialog_id", new Model<String>(""));
        Form deleteNotificationRuleForm = new JsonDataResponseForm("deleteNotificationRuleForm", new AttemptedDeleteAll( EntityType.ESM_NOTIFICATION_RULE ) ) {
            @Override
            protected Object getJsonResponseData() {
                String guid = deleteNotificationRuleDialog_id.getConvertedInput();
                if (guid == null || guid.isEmpty()) {
                    return new JSONException("The selected notification rule has empty GUID.");
                }
                try {
                    // Check if the notification rule is used by any cluster or nodes in monitoring property setups.
                    for (SsgClusterNotificationSetup setup: ssgClusterNotificationSetupManager.findAll()) {
                        String ssgClusterGuid = setup.getSsgClusterGuid();
                        for (SystemMonitoringNotificationRule rule: setup.getSystemNotificationRules()) {
                            if (guid.equals(rule.getGuid())) {
                                String clusterName = ssgClusterManager.findByGuid(ssgClusterGuid).getName();
                                return new JSONException("Notification rule '" + rule.getName() + "' is still in use by Gateway cluster '" + clusterName + "'.");
                            }
                        }
                    }

                    String notificationRuleName = systemMonitoringNotificationRulesManager.findByGuid(guid).getName();
                    systemMonitoringNotificationRulesManager.deleteByGuid(guid);

                    logger.fine("Deleting a system monitoring notification rule (GUID = "+ guid + ").");
                    auditSystemNotificationChange(notificationRuleName, "deleted");

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
        add(new JsonInteraction("getCurrentEntitiesPropertyValues", "getCurrentEntitiesPropertyValuesUrl",
            new GettingCurrentEntitiesPropertyValuesDataProvider()));

        // Wicket component for getting entity monitoring property setup settings
        add(new JsonInteraction("getEntityMonitoringPropertySetupSettings", "getEntityMonitoringPropertySetupSettingsUrl",
            new GettingEntityMonitoringPropertySetupSettingsDataProvider()));

        // Wicket component for saving entity monitoring property setup settings
        add(new JsonPostInteraction("saveEntityMonitoringPropertySetupSettings", "saveEntityMonitoringPropertySetupSettingsUrl",
            new SaveEntityMonitoringPropertySetupSettingsDataProvider()));
    }

    /**
     * Check if the user has read permission in the Monitor Page.
     * @return true if the user has only read permission.
     */
    private boolean checkReadOnly() {
        boolean readonly = true;
        try {
            for (Role role: roleManager.getAssignedRoles(getUser())) {
                if (Role.Tag.ADMIN == role.getTag()) {
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
                return new JSONException(e);
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
                    returnValue = new JSONException(new Exception("Server error: cannot parse uploaded JSON data", e.getCause()));
                    return;
                }

                if (!(jsonDataObj instanceof Map)) {
                    logger.log(Level.FINER, "Incorrect JSON data. Not convertible to a Map");
                    returnValue = new JSONException(new Exception("Server error: uploaded JSON data is not convertible to a map"));
                    return;
                }

                Map<String, Object> jsonFormatMap = (Map<String, Object>) jsonDataObj;

                try {
                    final Map<String, Object> clusterPropertyFormatMap = toClusterPropertyFormat(jsonFormatMap);
                    final Map<String, Object> oldSettingsMap = systemMonitoringSetupSettingsManager.findSetupSettings();
                    final Object[] exceptionHolder = new Object[1];

                    AuditContextUtils.doAsSystem(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                systemMonitoringSetupSettingsManager.saveSetupSettings(clusterPropertyFormatMap);
                                auditSystemMonitoringSetupChange(oldSettingsMap, clusterPropertyFormatMap);
                            } catch (Exception e) {
                                exceptionHolder[0] = e;
                            }
                        }
                    });

                    if (exceptionHolder[0] != null && (exceptionHolder[0] instanceof Exception)) {
                        throw (Exception) exceptionHolder[0];
                    }
                } catch (Exception e) {
                    returnValue = new JSONException(e);
                }
            } else {
                returnValue = new JSONException("Server error: jsonData must be either a JSONException or a JSON formatted String");
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
                jsonDataMap.put(JSONConstants.READONLY, isReadOnly);
                jsonDataMap.put(JSONConstants.NotifiationRule.RECORDS, notificationRules);

                // Return the JSON data back to the browser.
                return jsonDataMap;
            } catch (Exception e) {
                return new JSONException(e);
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
                    logger.log(Level.FINER, "Cannot parse uploaded JSON data.", e.getCause());
                    returnValue = new JSONException(new Exception("Server error: cannot parse uploaded JSON data", e.getCause()));
                    return;
                }

                if (!(jsonDataObj instanceof Map)) {
                    logger.log(Level.FINER, "Incorrect JSON data. Not convertible to a Map");
                    returnValue = new JSONException(new Exception("Server error: uploaded JSON data is not convertible to a map"));
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
                        auditSystemNotificationChange(name, "created");
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
                        auditSystemNotificationChange(name, "updated");
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
                returnValue = new JSONException("Server error: jsonData must be either a JSONException or a JSON formatted String");
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
            // Use MonitoringService to call MonitoringApi to get current property statuses
            final List<EntityMonitoringPropertyValues> entitiesList = new ArrayList<EntityMonitoringPropertyValues>();
            try {
                for ( final SsgCluster ssgCluster: ssgClusterManager.findOnlineClusters() ) {
                    final boolean toFetchClusterStatuses = toFetchStatuses(ssgCluster.getGuid());
                    // First get the current values for each SSG node.
                    for ( final SsgNode ssgNode: ssgCluster.getNodes() ) {
                        // If a SSG cluster has property to monitor, no matter if its SSG nodes have monitored properties or not, the ESM needs to
                        // fetch properties statuses, because the ESM gets the statuses of a SSG cluster, while fetching SSG nodes properties statuses.
                        if ( !JSONConstants.SsgNodeOnlineState.OFFLINE.equals(ssgNode.getOnlineStatus()) &&
                             (toFetchClusterStatuses || toFetchStatuses(ssgNode.getGuid())) ) {
                            final EntityMonitoringPropertyValues statuses = monitoringService.getCurrentSsgNodePropertiesStatus(ssgNode);
                            if ( statuses != null ) {
                                statuses.setEntityType(EntityMonitoringPropertyValues.EntityType.SSG_NODE);
                                entitiesList.add(statuses);
                            }
                        }
                    }

                    // Then, get the current value (audit size) for the SSG cluster.
                    if ( toFetchClusterStatuses ) {
                        final EntityMonitoringPropertyValues statuses = monitoringService.getCurrentSsgClusterPropertyStatus(ssgCluster.getGuid());
                        if ( statuses != null ) {
                            statuses.setEntityType(EntityMonitoringPropertyValues.EntityType.SSG_CLUSTER);
                            entitiesList.add(statuses);
                        }
                    }
                }
            } catch (FindException e) {
                logger.log(Level.WARNING, "Cannot find Gateway clusters due to data access failure.", ExceptionUtils.getDebugException(e));
                return new JSONException("Failed to get entity property values due to data access failure.");
            }

            final Map<String, Object> jsonDataMap = new HashMap<String, Object>();
            jsonDataMap.put("nextRefreshInterval", Long.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_NEXTREFRESH_INTERVAL ) ));
            jsonDataMap.put("entities", entitiesList);

            synchronized( previousPropValuesListSync ) {
                // Removed audit upon state transition per Bug 6995:
                // auditEntityPropertyAlertStateChange(previousPropValuesList, entitiesList);
                previousPropValuesList = entitiesList;
            }

            return jsonDataMap;
        }

        @Override
        public void setData(Object jsonData) {
            // No data expected from the browser.
        }
    }

    /**
     * To check if the ESM needs to fetch properties statuses.
     * @param entityGuid: the GUID of entity such as SSG cluster or SSG node.
     * @return true if one of properties of the entity has "monitoringEnabled" set to true.
     * @throws FindException: throw this expception if there exists data access failure.
     */
    private boolean toFetchStatuses(String entityGuid) throws FindException {
        boolean toFetch = false;

        Collection<EntityMonitoringPropertySetup> entitiesSetup = entityMonitoringPropertySetupManager.findByEntityGuid(entityGuid);
        if (entitiesSetup != null && !entitiesSetup.isEmpty()) {
            for (EntityMonitoringPropertySetup setup: entitiesSetup) {
                if (setup.isMonitoringEnabled()) {
                    toFetch = true;
                    break;
                }
            }
        }

        return toFetch;
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
                        if (propertyType.equals(JSONConstants.SsgNodeMonitoringProperty.LOG_SIZE)) {
                            // Convert KB to MB, since the UI displays the log size in MB.
                            propertySetup.setTriggerValue(convertKbToMbOrGb(true, propertySetup.getTriggerValue()));
                        } else if (propertyType.equals(JSONConstants.SsgNodeMonitoringProperty.DISK_FREE)) {
                            // Convert KB to GB, since the UI displays the disk free in GB.
                            propertySetup.setTriggerValue(convertKbToMbOrGb(false, propertySetup.getTriggerValue()));
                        } else if (propertyType.equals(JSONConstants.SsgNodeMonitoringProperty.SWAP_USAGE)) {
                            // Convert KB to MB, since the UI displays the swap usage in MB.
                            propertySetup.setTriggerValue(convertKbToMbOrGb(true, propertySetup.getTriggerValue()));
                        }
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
                        Long triggerValue = (Long) systemSetup.get("trigger." + propertyType);
                        if (triggerValue != null) {
                            if (propertyType.equals(JSONConstants.SsgNodeMonitoringProperty.LOG_SIZE)) {
                                // Convert KB to MB, since the UI displays the log size in MB.
                                triggerValue = convertKbToMbOrGb(true, triggerValue);
                            } else if (propertyType.equals(JSONConstants.SsgNodeMonitoringProperty.DISK_FREE)) {
                                // Convert KB to GB, since the UI displays the disk free in GB.
                                triggerValue = convertKbToMbOrGb(false, triggerValue);
                            } else if (propertyType.equals(JSONConstants.SsgNodeMonitoringProperty.SWAP_USAGE)) {
                                // Convert KB to MB, since the UI displays the swap usage in MB.
                                triggerValue = convertKbToMbOrGb(true, triggerValue);
                            }
                        }
                        initSetup.setTriggerValue(triggerValue);
                        initSetup.setUnit( config.getProperty( "monitoring." + entityType + "." + propertyType + ".unit" ) );

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
                logger.log(Level.WARNING, errmsg, ExceptionUtils.getDebugException(err));
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
                    logger.log(Level.FINER, "Cannot parse uploaded JSON data.", e.getCause());
                    returnValue = new JSONException(new Exception("Server error: cannot parse uploaded JSON data", e));
                    return;
                }

                if (!(jsonDataObj instanceof Map)) {
                    logger.log(Level.FINER, "Incorrect JSON data. Not convertible to a Map.");
                    returnValue = new JSONException("Server error: uploaded JSON data is not convertible to a map");
                    return;
                }

                Map<String, Object> jsonFormatMap = (Map<String, Object>) jsonDataObj;
                Map<String, Object> entityProps = (Map<String, Object>) jsonFormatMap.get(JSONConstants.ENTITY_PROPS_SETUP.ENTITY);
                String entityGuid = (String) entityProps.get(JSONConstants.ID);
                String entityType = (String) entityProps.get(JSONConstants.TYPE);
                String propertyType = (String) jsonFormatMap.get(JSONConstants.ENTITY_PROPS_SETUP.PROP_TYPE);
                List<AuditDetail> auditDetailList = new ArrayList<AuditDetail>();
                try {
                    // Note: propertySetup is unlikely to be null, since the manager has saved the setup settings after the browser requests property setup.
                    EntityMonitoringPropertySetup propertySetup = entityMonitoringPropertySetupManager.findByEntityGuidAndPropertyType(entityGuid, propertyType);

                    // Update monitoring enable status
                    boolean monitoringEnabled = (Boolean)jsonFormatMap.get(JSONConstants.ENTITY_PROPS_SETUP.MONITORING_ENABLED);
                    if (propertySetup.isMonitoringEnabled() != monitoringEnabled) {
                        propertySetup.setMonitoringEnabled(monitoringEnabled);
                        auditDetailList.add(getAuditDetailForEntityPropertySetupChanging(JSONConstants.ENTITY_PROPS_SETUP.MONITORING_ENABLED, monitoringEnabled, null));
                    }

                    // Update trigger enable status.  Also update trigger value and notification enable status if appliable.
                    if (monitoringEnabled) {
                        boolean triggerEnabled = (Boolean)jsonFormatMap.get(JSONConstants.ENTITY_PROPS_SETUP.TRIGGER_ENABLED);
                        if (propertySetup.isTriggerEnabled() != triggerEnabled) {
                            propertySetup.setTriggerEnabled(triggerEnabled);
                            auditDetailList.add(getAuditDetailForEntityPropertySetupChanging(JSONConstants.ENTITY_PROPS_SETUP.TRIGGER_ENABLED, monitoringEnabled, null));
                        }

                        if (triggerEnabled) {
                            Long triggerValue = (Long)jsonFormatMap.get(JSONConstants.ENTITY_PROPS_SETUP.TRIGGER_VALUE);
                            if (triggerValue != null) {
                                Long guiValue = triggerValue;
                                if (propertyType.equals(JSONConstants.SsgNodeMonitoringProperty.LOG_SIZE)) {
                                    // Convert MB to KB, since the database stores the log size in KB.
                                    triggerValue = convertMbOrGbToKb(true, triggerValue);
                                } else if (propertyType.equals(JSONConstants.SsgNodeMonitoringProperty.DISK_FREE)) {
                                    // Convert GB to KB, since the database stores the disk free KB.
                                    triggerValue = convertMbOrGbToKb(false, triggerValue);
                                } else if (propertyType.equals(JSONConstants.SsgNodeMonitoringProperty.SWAP_USAGE)) {
                                    // Convert MB to KB, since the database stores the swap usage in KB.
                                    triggerValue = convertMbOrGbToKb(true, triggerValue);
                                }

                                if (! propertySetup.getTriggerValue().equals(triggerValue)) {
                                    String unit = (String)jsonFormatMap.get(JSONConstants.ENTITY_PROPS_SETUP.UNIT);
                                    auditDetailList.add(getAuditDetailForEntityPropertySetupChanging(JSONConstants.ENTITY_PROPS_SETUP.TRIGGER_VALUE, guiValue, unit));
                                }
                            }
                            propertySetup.setTriggerValue(triggerValue);

                            boolean notificationEnabled = (Boolean)jsonFormatMap.get(JSONConstants.ENTITY_PROPS_SETUP.NOTIFICATION_ENABLED);
                            if (propertySetup.isNotificationEnabled() != notificationEnabled) {
                                propertySetup.setNotificationEnabled(notificationEnabled);
                                auditDetailList.add(getAuditDetailForEntityPropertySetupChanging(JSONConstants.ENTITY_PROPS_SETUP.NOTIFICATION_ENABLED, notificationEnabled, null));
                            }
                        } else {
                            propertySetup.setNotificationEnabled(false);
                        }
                    } else {
                        propertySetup.setTriggerEnabled(false);
                        propertySetup.setNotificationEnabled(false);
                    }

                    // Find and update notification rules
                    NamedEntity entity;
                    boolean isSsgNode;
                    String ipAddress;
                    if (entityType.equals(JSONConstants.EntityType.SSG_CLUSTER)) {
                        entity = ssgClusterManager.findByGuid(entityGuid);
                        isSsgNode = false;
                        ipAddress = ((SsgCluster)entity).getIpAddress();
                    } else if (entityType.equals(JSONConstants.EntityType.SSG_NODE)) {
                        entity = ssgNodeManager.findByGuid(entityGuid);
                        isSsgNode = true;
                        ipAddress = ((SsgNode)entity).getIpAddress();
                    } else {
                        throw new InvalidMonitoringSetupSettingException("Entity type ('" + entityType + "') is invalid.");
                    }

                    String ssgClusterGuid = entityType.equals(JSONConstants.EntityType.SSG_NODE)? ((SsgNode)entity).getSsgCluster().getGuid() : entityGuid;
                    SsgClusterNotificationSetup ssgClusterNotificationSetup = ssgClusterNotificationSetupManager.findByEntityGuid(ssgClusterGuid);
                    if (ssgClusterNotificationSetup == null) {
                        ssgClusterNotificationSetup = new SsgClusterNotificationSetup(ssgClusterGuid);
                    }
                    Object[] notificationRuleMaps = (Object[]) jsonFormatMap.get(JSONConstants.ENTITY_PROPS_SETUP.NOTIFICATION_RULES);
                    Set<SystemMonitoringNotificationRule> newNotificationSet = new HashSet<SystemMonitoringNotificationRule>();
                    for (Object ruleMap: notificationRuleMaps) {
                        String guid = (String) ((Map<String, Object>)ruleMap).get(JSONConstants.ID);
                        newNotificationSet.add(systemMonitoringNotificationRulesManager.findByGuid(guid));
                    }

                    Set<SystemMonitoringNotificationRule> previousNotificationSet = propertySetup.getSsgClusterNotificationSetup().getSystemNotificationRules();
                    if (! previousNotificationSet.equals(newNotificationSet)) {
                        ssgClusterNotificationSetup.setSystemNotificationRules(newNotificationSet);
                        ssgClusterNotificationSetupManager.update(ssgClusterNotificationSetup);
                        propertySetup.setSsgClusterNotificationSetup(ssgClusterNotificationSetup);

                        setAuditDetailsForEntityNotificationSetupChanging(previousNotificationSet, newNotificationSet, auditDetailList);
                    }

                    // Update the entity monitoring property setup settings
                    entityMonitoringPropertySetupManager.update(propertySetup);

                    if (!auditDetailList.isEmpty()) {
                        auditEntityPropertySetupChange(isSsgNode, entityGuid, ipAddress, auditDetailList);
                    }

                    // Send null if saving is successful.
                    returnValue = null;
                } catch (Exception e) {
                    String errmsg = "Cannot save the entity property setup (Entity_GUID = '" + entityGuid + "' and Entity_TYPE = '" + entityType + "').";
                    logger.log(Level.WARNING, errmsg, e);
                    returnValue = new JSONException(new Exception(errmsg, e));
                }
            } else {
                returnValue = new JSONException("Server error: jsonData must be either a JSONException or a JSON formatted String");
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
        auditSizeMap.put(JSONConstants.MonitoringPropertySettings.SAMPLING_INTERVAL,         clusterPropertyFormatMap.get(EsmConfigParams.PARAM_MONITORING_INTERVAL_AUDITSIZE));
        auditSizeMap.put(JSONConstants.MonitoringPropertySettings.DEFAULT_TRIGGER_VALUE,     clusterPropertyFormatMap.get(EsmConfigParams.PARAM_MONITORING_TRIGGER_AUDITSIZE));
        auditSizeMap.put(JSONConstants.MonitoringPropertySettings.TRIGGER_VALUE_LOWER_LIMIT, Long.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_SSGCLUSTER_AUDITSIZE_LOWERLIMIT ) ));
        auditSizeMap.put( MonitoringPropertySettings.UNIT, config.getProperty( EsmConfigParams.PARAM_MONITORING_SSGCLUSTER_AUDITSIZE_UNIT ) );

        // "databaseReplicationDelay"
        Map<String, Object> databaseReplicationDelayMap = new HashMap<String, Object>();
        databaseReplicationDelayMap.put(JSONConstants.MonitoringPropertySettings.SAMPLING_INTERVAL,         clusterPropertyFormatMap.get(EsmConfigParams.PARAM_MONITORING_INTERVAL_DATABASEREPLICATIONDELAY));
        databaseReplicationDelayMap.put(JSONConstants.MonitoringPropertySettings.DEFAULT_TRIGGER_VALUE,     clusterPropertyFormatMap.get(EsmConfigParams.PARAM_MONITORING_TRIGGER_DATABASEREPLICATIONDELAY));
        databaseReplicationDelayMap.put(JSONConstants.MonitoringPropertySettings.TRIGGER_VALUE_LOWER_LIMIT, Long.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_SSGCLUSTER_DATABASEREPLICATIONDELAY_LOWERLIMIT ) ));
        databaseReplicationDelayMap.put( MonitoringPropertySettings.UNIT, config.getProperty( EsmConfigParams.PARAM_MONITORING_SSGCLUSTER_DATABASEREPLICATIONDELAY_UNIT ) );

        // "operatingStatus"
        Map<String, Object> operatingStatusMap = new HashMap<String, Object>();
        operatingStatusMap.put(JSONConstants.MonitoringPropertySettings.SAMPLING_INTERVAL,   clusterPropertyFormatMap.get(EsmConfigParams.PARAM_MONITORING_INTERVAL_OPERATINGSTATUS));

        // "logSize"
        Map<String, Object> logSizeMap = new HashMap<String, Object>();
        logSizeMap.put(JSONConstants.MonitoringPropertySettings.SAMPLING_INTERVAL,           clusterPropertyFormatMap.get(EsmConfigParams.PARAM_MONITORING_INTERVAL_LOGSIZE));
        logSizeMap.put(JSONConstants.MonitoringPropertySettings.DEFAULT_TRIGGER_VALUE,       convertKbToMbOrGb(true, (Long)clusterPropertyFormatMap.get(EsmConfigParams.PARAM_MONITORING_TRIGGER_LOGSIZE)));
        logSizeMap.put(JSONConstants.MonitoringPropertySettings.TRIGGER_VALUE_LOWER_LIMIT,   Long.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_SSGNODE_LOGSIZE_LOWERLIMIT ) ));
        logSizeMap.put( MonitoringPropertySettings.UNIT, config.getProperty( EsmConfigParams.PARAM_MONITORING_SSGNODE_LOGSIZE_UNIT ) );

        // "diskUsage"
        Map<String, Object> diskUsageMap = new HashMap<String, Object>();
        diskUsageMap.put(JSONConstants.MonitoringPropertySettings.SAMPLING_INTERVAL,         clusterPropertyFormatMap.get(EsmConfigParams.PARAM_MONITORING_INTERVAL_DISKUSAGE));
        diskUsageMap.put(JSONConstants.MonitoringPropertySettings.DEFAULT_TRIGGER_VALUE,     clusterPropertyFormatMap.get(EsmConfigParams.PARAM_MONITORING_TRIGGER_DISKUSAGE));
        diskUsageMap.put(JSONConstants.MonitoringPropertySettings.TRIGGER_VALUE_LOWER_LIMIT, Long.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_SSGNODE_DISKUSAGE_LOWERLIMIT ) ));
        diskUsageMap.put(JSONConstants.MonitoringPropertySettings.TRIGGER_VALUE_UPPER_LIMIT, Long.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_SSGNODE_DISKUSAGE_UPPERLIMIT ) ));
        diskUsageMap.put( MonitoringPropertySettings.UNIT, config.getProperty( EsmConfigParams.PARAM_MONITORING_SSGNODE_DISKUSAGE_UNIT ) );

        // "diskFree"
        Map<String, Object> diskFreeMap = new HashMap<String, Object>();
        diskFreeMap.put(JSONConstants.MonitoringPropertySettings.SAMPLING_INTERVAL,         clusterPropertyFormatMap.get(EsmConfigParams.PARAM_MONITORING_INTERVAL_DISKFREE));
        diskFreeMap.put(JSONConstants.MonitoringPropertySettings.DEFAULT_TRIGGER_VALUE,     convertKbToMbOrGb(false, (Long)clusterPropertyFormatMap.get(EsmConfigParams.PARAM_MONITORING_TRIGGER_DISKFREE)));
        diskFreeMap.put(JSONConstants.MonitoringPropertySettings.TRIGGER_VALUE_LOWER_LIMIT, Long.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_SSGNODE_DISKFREE_LOWERLIMIT ) ));
        diskFreeMap.put( MonitoringPropertySettings.UNIT, config.getProperty( EsmConfigParams.PARAM_MONITORING_SSGNODE_DISKFREE_UNIT ) );

        // "raidStatus"
        Map<String, Object> raidStatusMap = new HashMap<String, Object>();
        raidStatusMap.put(JSONConstants.MonitoringPropertySettings.SAMPLING_INTERVAL,        clusterPropertyFormatMap.get(EsmConfigParams.PARAM_MONITORING_INTERVAL_RAIDSTATUS));

        // "cpuTemp"
        Map<String, Object> cpuTempMap = new HashMap<String, Object>();
        cpuTempMap.put(JSONConstants.MonitoringPropertySettings.SAMPLING_INTERVAL,           clusterPropertyFormatMap.get(EsmConfigParams.PARAM_MONITORING_INTERVAL_CPUTEMPERATURE));
        cpuTempMap.put(JSONConstants.MonitoringPropertySettings.DEFAULT_TRIGGER_VALUE,       clusterPropertyFormatMap.get(EsmConfigParams.PARAM_MONITORING_TRIGGER_CPUTEMPERATURE));
        cpuTempMap.put(JSONConstants.MonitoringPropertySettings.TRIGGER_VALUE_LOWER_LIMIT,   Long.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_SSGNODE_CPUTEMPERATURE_LOWERLIMIT ) ));
        cpuTempMap.put( MonitoringPropertySettings.UNIT, config.getProperty( EsmConfigParams.PARAM_MONITORING_SSGNODE_CPUTEMPERATURE_UNIT ) );

        // "cpuUsage"
        Map<String, Object> cpuUsageMap = new HashMap<String, Object>();
        cpuUsageMap.put(JSONConstants.MonitoringPropertySettings.SAMPLING_INTERVAL,          clusterPropertyFormatMap.get(EsmConfigParams.PARAM_MONITORING_INTERVAL_CPUUSAGE));
        cpuUsageMap.put(JSONConstants.MonitoringPropertySettings.DEFAULT_TRIGGER_VALUE,      clusterPropertyFormatMap.get(EsmConfigParams.PARAM_MONITORING_TRIGGER_CPUUSAGE));
        cpuUsageMap.put(JSONConstants.MonitoringPropertySettings.TRIGGER_VALUE_LOWER_LIMIT,  Long.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_SSGNODE_CPUUSAGE_LOWERLIMIT ) ));
        cpuUsageMap.put(JSONConstants.MonitoringPropertySettings.TRIGGER_VALUE_UPPER_LIMIT,  Long.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_SSGNODE_CPUUSAGE_UPPERLIMIT ) ));
        cpuUsageMap.put( MonitoringPropertySettings.UNIT, config.getProperty( EsmConfigParams.PARAM_MONITORING_SSGNODE_CPUUSAGE_UNIT ) );

        // "swapUsage"
        Map<String, Object> swapUsageMap = new HashMap<String, Object>();
        swapUsageMap.put(JSONConstants.MonitoringPropertySettings.SAMPLING_INTERVAL,         clusterPropertyFormatMap.get(EsmConfigParams.PARAM_MONITORING_INTERVAL_SWAPUSAGE));
        swapUsageMap.put(JSONConstants.MonitoringPropertySettings.DEFAULT_TRIGGER_VALUE,     convertKbToMbOrGb(true, (Long)clusterPropertyFormatMap.get(EsmConfigParams.PARAM_MONITORING_TRIGGER_SWAPUSAGE)));
        swapUsageMap.put(JSONConstants.MonitoringPropertySettings.TRIGGER_VALUE_LOWER_LIMIT, Long.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_SSGNODE_SWAPUSAGE_LOWERLIMIT ) ));
        swapUsageMap.put( MonitoringPropertySettings.UNIT, config.getProperty( EsmConfigParams.PARAM_MONITORING_SSGNODE_SWAPUSAGE_UNIT ) );

        // "ntpStatus"
        Map<String, Object> ntpStatusMap = new HashMap<String, Object>();
        ntpStatusMap.put(JSONConstants.MonitoringPropertySettings.SAMPLING_INTERVAL,         clusterPropertyFormatMap.get(EsmConfigParams.PARAM_MONITORING_INTERVAL_NTPSTATUS));

        // "propertySetup"
        Map<String, Object> propertySetupMap = new HashMap<String, Object>();
        propertySetupMap.put(JSONConstants.SsgClusterMonitoringProperty.AUDIT_SIZE,          auditSizeMap);
        propertySetupMap.put(JSONConstants.SsgClusterMonitoringProperty.DATABASE_REPLICATION_DELAY, databaseReplicationDelayMap);
        propertySetupMap.put(JSONConstants.SsgNodeMonitoringProperty.OPERATING_STATUS,       operatingStatusMap);
        propertySetupMap.put(JSONConstants.SsgNodeMonitoringProperty.LOG_SIZE,               logSizeMap);
        propertySetupMap.put(JSONConstants.SsgNodeMonitoringProperty.DISK_USAGE,             diskUsageMap);
        propertySetupMap.put(JSONConstants.SsgNodeMonitoringProperty.DISK_FREE,              diskFreeMap);
        propertySetupMap.put(JSONConstants.SsgNodeMonitoringProperty.RAID_STATUS,            raidStatusMap);
        propertySetupMap.put(JSONConstants.SsgNodeMonitoringProperty.CPU_TEMP,               cpuTempMap);
        propertySetupMap.put(JSONConstants.SsgNodeMonitoringProperty.CPU_USAGE,              cpuUsageMap);
        propertySetupMap.put(JSONConstants.SsgNodeMonitoringProperty.SWAP_USAGE,             swapUsageMap);
        propertySetupMap.put(JSONConstants.SsgNodeMonitoringProperty.NTP_STATUS,             ntpStatusMap);

        // Fill up jsonFormatMap
        jsonFormatMap.put(JSONConstants.READONLY, isReadOnly);
        jsonFormatMap.put(JSONConstants.SystemMonitoringSetup.PROPERTY_SETUP,                propertySetupMap);
        jsonFormatMap.put(JSONConstants.SystemMonitoringSetup.SAMPLING_INTERVAL_LOWER_LIMIT, Long.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_SAMPLINGINTERVAL_LOWERLIMIT ) ));
        jsonFormatMap.put(JSONConstants.SystemMonitoringSetup.DISABLE_ALL_NOTIFICATIONS,     clusterPropertyFormatMap.get(EsmConfigParams.PARAM_MONITORING_DISABLEALLNOTIFICATIONS));
        jsonFormatMap.put(JSONConstants.SystemMonitoringSetup.AUDIT_UPON_ALERT_STATE,        clusterPropertyFormatMap.get(EsmConfigParams.PARAM_MONITORING_AUDITUPONALERTSTATE));
        jsonFormatMap.put(JSONConstants.SystemMonitoringSetup.AUDIT_UPON_NORMAL_STATE,       clusterPropertyFormatMap.get(EsmConfigParams.PARAM_MONITORING_AUDITUPONNORMALSTATE));
        jsonFormatMap.put(JSONConstants.SystemMonitoringSetup.AUDIT_UPON_NOTIFICATION,       clusterPropertyFormatMap.get(EsmConfigParams.PARAM_MONITORING_AUDITUPONNOTIFICATION));

        return jsonFormatMap;
    }

    private Long convertKbToMbOrGb(boolean toMb, Long valInKb) {
        if (valInKb == null)
            return null;
        return valInKb / (toMb? SystemMonitoringSetupSettingsManager.KB_MB_CONVERTOR : SystemMonitoringSetupSettingsManager.KB_GB_CONVERTOR);
    }

    private Long convertMbOrGbToKb(boolean fromMb, Long valInMbOrGb) {
        if (valInMbOrGb == null)
            return null;
        return valInMbOrGb * (fromMb? SystemMonitoringSetupSettingsManager.KB_MB_CONVERTOR : SystemMonitoringSetupSettingsManager.KB_GB_CONVERTOR);
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
                clusterPropertyFormatMap.put(EsmConfigParams.PARAM_MONITORING_INTERVAL_AUDITSIZE, interval);

                long triggerVal = getTriggerValue(auditSizeMap, JSONConstants.SsgClusterMonitoringProperty.AUDIT_SIZE, Long.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_SSGCLUSTER_AUDITSIZE_LOWERLIMIT ) ), null);
                clusterPropertyFormatMap.put(EsmConfigParams.PARAM_MONITORING_TRIGGER_AUDITSIZE, triggerVal);
            }

            // "databaseReplicationDelay"
            Map<String, Object> databaseReplicationDelayMap = (Map<String, Object>) propertySetupMap.get(JSONConstants.SsgClusterMonitoringProperty.DATABASE_REPLICATION_DELAY);
            if (databaseReplicationDelayMap != null && !databaseReplicationDelayMap.isEmpty()) {
                long interval = getSamplingInterval(databaseReplicationDelayMap, JSONConstants.SsgClusterMonitoringProperty.DATABASE_REPLICATION_DELAY);
                clusterPropertyFormatMap.put(EsmConfigParams.PARAM_MONITORING_INTERVAL_DATABASEREPLICATIONDELAY, interval);

                long triggerVal = getTriggerValue(databaseReplicationDelayMap, JSONConstants.SsgClusterMonitoringProperty.DATABASE_REPLICATION_DELAY, Long.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_SSGCLUSTER_DATABASEREPLICATIONDELAY_LOWERLIMIT ) ), null);
                clusterPropertyFormatMap.put(EsmConfigParams.PARAM_MONITORING_TRIGGER_DATABASEREPLICATIONDELAY, triggerVal);
            }

            // "operatingStatus"
            Map<String, Object> operatingStatusMap = (Map<String, Object>) propertySetupMap.get(JSONConstants.SsgNodeMonitoringProperty.OPERATING_STATUS);
            if (operatingStatusMap != null && !operatingStatusMap.isEmpty()) {
                long interval = getSamplingInterval(operatingStatusMap, JSONConstants.SsgNodeMonitoringProperty.OPERATING_STATUS);
                clusterPropertyFormatMap.put(EsmConfigParams.PARAM_MONITORING_INTERVAL_OPERATINGSTATUS, interval);
            }

            // "logSize"
            Map<String, Object> logSizeMap = (Map<String, Object>) propertySetupMap.get(JSONConstants.SsgNodeMonitoringProperty.LOG_SIZE);
            if (logSizeMap != null && !logSizeMap.isEmpty()) {
                long interval = getSamplingInterval(logSizeMap, JSONConstants.SsgNodeMonitoringProperty.LOG_SIZE);
                clusterPropertyFormatMap.put(EsmConfigParams.PARAM_MONITORING_INTERVAL_LOGSIZE, interval);

                long triggerVal = getTriggerValue(logSizeMap, JSONConstants.SsgNodeMonitoringProperty.LOG_SIZE, Long.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_SSGNODE_LOGSIZE_LOWERLIMIT ) ), null);
                clusterPropertyFormatMap.put(EsmConfigParams.PARAM_MONITORING_TRIGGER_LOGSIZE, convertMbOrGbToKb(true, triggerVal));
            }

            // "diskUsage"
            Map<String, Object> diskUsageMap = (Map<String, Object>) propertySetupMap.get(JSONConstants.SsgNodeMonitoringProperty.DISK_USAGE);
            if (diskUsageMap != null && !diskUsageMap.isEmpty()) {
                long interval = getSamplingInterval(diskUsageMap, JSONConstants.SsgNodeMonitoringProperty.DISK_USAGE);
                clusterPropertyFormatMap.put(EsmConfigParams.PARAM_MONITORING_INTERVAL_DISKUSAGE, interval);

                long triggerVal = getTriggerValue(diskUsageMap, JSONConstants.SsgNodeMonitoringProperty.DISK_USAGE,
                    Long.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_SSGNODE_DISKUSAGE_LOWERLIMIT ) ),
                    Long.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_SSGNODE_DISKUSAGE_UPPERLIMIT ) ));
                clusterPropertyFormatMap.put(EsmConfigParams.PARAM_MONITORING_TRIGGER_DISKUSAGE, triggerVal);
            }

            // "diskFree"
            Map<String, Object> diskFreeMap = (Map<String, Object>) propertySetupMap.get(JSONConstants.SsgNodeMonitoringProperty.DISK_FREE);
            if (diskFreeMap != null && !diskFreeMap.isEmpty()) {
                long interval = getSamplingInterval(diskFreeMap, JSONConstants.SsgNodeMonitoringProperty.DISK_FREE);
                clusterPropertyFormatMap.put(EsmConfigParams.PARAM_MONITORING_INTERVAL_DISKFREE, interval);

                long triggerVal = getTriggerValue(diskFreeMap, JSONConstants.SsgNodeMonitoringProperty.DISK_FREE, Long.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_SSGNODE_DISKFREE_LOWERLIMIT ) ), null);
                clusterPropertyFormatMap.put(EsmConfigParams.PARAM_MONITORING_TRIGGER_DISKFREE, convertMbOrGbToKb(false, triggerVal));
            }

            // "raidStatus"
            Map<String, Object> raidStatusMap = (Map<String, Object>) propertySetupMap.get(JSONConstants.SsgNodeMonitoringProperty.RAID_STATUS);
            if (raidStatusMap != null && !raidStatusMap.isEmpty()) {
                long interval = getSamplingInterval(raidStatusMap, JSONConstants.SsgNodeMonitoringProperty.RAID_STATUS);
                clusterPropertyFormatMap.put(EsmConfigParams.PARAM_MONITORING_INTERVAL_RAIDSTATUS, interval);
            }

            // "cpuTemp"
            Map<String, Object> cpuTempMap = (Map<String, Object>) propertySetupMap.get(JSONConstants.SsgNodeMonitoringProperty.CPU_TEMP);
            if (cpuTempMap != null && !cpuTempMap.isEmpty()) {
                long interval = getSamplingInterval(cpuTempMap, JSONConstants.SsgNodeMonitoringProperty.CPU_TEMP);
                clusterPropertyFormatMap.put(EsmConfigParams.PARAM_MONITORING_INTERVAL_CPUTEMPERATURE, interval);

                long triggerVal = getTriggerValue(cpuTempMap, JSONConstants.SsgNodeMonitoringProperty.CPU_TEMP, Long.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_SSGNODE_CPUTEMPERATURE_LOWERLIMIT ) ), null);
                clusterPropertyFormatMap.put(EsmConfigParams.PARAM_MONITORING_TRIGGER_CPUTEMPERATURE, triggerVal);
            }

            // "cpuUsage"
            Map<String, Object> cpuUsageMap = (Map<String, Object>) propertySetupMap.get(JSONConstants.SsgNodeMonitoringProperty.CPU_USAGE);
            if (cpuUsageMap != null && !cpuUsageMap.isEmpty()) {
                long interval = getSamplingInterval(cpuUsageMap, JSONConstants.SsgNodeMonitoringProperty.CPU_USAGE);
                clusterPropertyFormatMap.put(EsmConfigParams.PARAM_MONITORING_INTERVAL_CPUUSAGE, interval);

                long triggerVal = getTriggerValue(cpuUsageMap, JSONConstants.SsgNodeMonitoringProperty.CPU_USAGE,
                    Long.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_SSGNODE_CPUUSAGE_LOWERLIMIT ) ),
                    Long.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_SSGNODE_CPUUSAGE_UPPERLIMIT ) ));
                clusterPropertyFormatMap.put(EsmConfigParams.PARAM_MONITORING_TRIGGER_CPUUSAGE, triggerVal);
            }

            // "swapUsage"
            Map<String, Object> swapUsageMap = (Map<String, Object>) propertySetupMap.get(JSONConstants.SsgNodeMonitoringProperty.SWAP_USAGE);
            if (swapUsageMap != null && !swapUsageMap.isEmpty()) {
                long interval = getSamplingInterval(swapUsageMap, JSONConstants.SsgNodeMonitoringProperty.SWAP_USAGE);
                clusterPropertyFormatMap.put(EsmConfigParams.PARAM_MONITORING_INTERVAL_SWAPUSAGE, interval);

                long triggerVal = getTriggerValue(swapUsageMap, JSONConstants.SsgNodeMonitoringProperty.SWAP_USAGE, Long.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_SSGNODE_SWAPUSAGE_LOWERLIMIT ) ), null);
                clusterPropertyFormatMap.put(EsmConfigParams.PARAM_MONITORING_TRIGGER_SWAPUSAGE, convertMbOrGbToKb(true, triggerVal));
            }

            // "ntpStatus"
            Map<String, Object> ntpStatusMap = (Map<String, Object>) propertySetupMap.get(JSONConstants.SsgNodeMonitoringProperty.NTP_STATUS);
            if (ntpStatusMap != null && !ntpStatusMap.isEmpty()) {
                long interval = getSamplingInterval(ntpStatusMap, JSONConstants.SsgNodeMonitoringProperty.NTP_STATUS);
                clusterPropertyFormatMap.put(EsmConfigParams.PARAM_MONITORING_INTERVAL_NTPSTATUS, interval);
            }
        }

        // "disableAllNotifications"
        Object optionObj = jsonFormatMap.get(JSONConstants.SystemMonitoringSetup.DISABLE_ALL_NOTIFICATIONS);
        if (optionObj != null) clusterPropertyFormatMap.put(EsmConfigParams.PARAM_MONITORING_DISABLEALLNOTIFICATIONS, validateOption(optionObj, JSONConstants.SystemMonitoringSetup.DISABLE_ALL_NOTIFICATIONS));

        // "auditUponAlertState"
        optionObj = jsonFormatMap.get(JSONConstants.SystemMonitoringSetup.AUDIT_UPON_ALERT_STATE);
        if (optionObj != null) clusterPropertyFormatMap.put(EsmConfigParams.PARAM_MONITORING_AUDITUPONALERTSTATE, validateOption(optionObj, JSONConstants.SystemMonitoringSetup.AUDIT_UPON_ALERT_STATE));

        // "auditUponNormalState"
        optionObj = jsonFormatMap.get(JSONConstants.SystemMonitoringSetup.AUDIT_UPON_NORMAL_STATE);
        if (optionObj != null) clusterPropertyFormatMap.put(EsmConfigParams.PARAM_MONITORING_AUDITUPONNORMALSTATE, validateOption(optionObj, JSONConstants.SystemMonitoringSetup.AUDIT_UPON_NORMAL_STATE));

        // "auditUponNotification"
        optionObj = jsonFormatMap.get(JSONConstants.SystemMonitoringSetup.AUDIT_UPON_NOTIFICATION);
        if (optionObj != null) clusterPropertyFormatMap.put(EsmConfigParams.PARAM_MONITORING_AUDITUPONNOTIFICATION, validateOption(optionObj, JSONConstants.SystemMonitoringSetup.AUDIT_UPON_NOTIFICATION));

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

        long samplingIntervalLowerLimit = Long.parseLong( config.getProperty( EsmConfigParams.PARAM_MONITORING_SAMPLINGINTERVAL_LOWERLIMIT ) );
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

    /**
     * Audit the change in the system monitoring setup settings.
     * @param oldMap: the old cluster-property-format map of setup settings.
     * @param newMap: the new cluster-property-format map of setup settings.
     */
    private void auditSystemMonitoringSetupChange(final Map<String, Object> oldMap, final Map<String, Object> newMap) {
        AuditRecord auditRecord = new SystemAuditRecord(
            Level.INFO,
            "",
            Component.ENTERPRISE_MANAGER,
            "Change in the system monitoring setup",
            true,
            null,
            null,
            null,
            "Monitoring Setup Change",
            ""
        );

        Collection<AuditDetail> details = new ArrayList<AuditDetail>();
        for (String key: newMap.keySet()) {
            Object newVal = newMap.get(key);
            Object oldVal = oldMap.get(key);

            if (newVal != null && ! newVal.equals(oldVal)) {
                String entityType = key.equals(EsmConfigParams.PARAM_MONITORING_TRIGGER_AUDITSIZE) ||
                                    key.equals(EsmConfigParams.PARAM_MONITORING_TRIGGER_DATABASEREPLICATIONDELAY) ?
                        "ssgCluster" :
                        "ssgNode";
                String propertyName = key.split("\\.").length < 2? key : key.split("\\.")[1];

                if (key.equals(EsmConfigParams.PARAM_MONITORING_TRIGGER_LOGSIZE)) {
                    // Convert KB to MB, since the UI displays the log size in MB.
                    newVal = convertKbToMbOrGb(true, (Long)newVal);
                } else if (key.equals(EsmConfigParams.PARAM_MONITORING_TRIGGER_DISKFREE)) {
                    // Convert KB to GB, since the UI displays the disk free in GB.
                    newVal = convertKbToMbOrGb(false, (Long)newVal);
                } else if (key.equals(EsmConfigParams.PARAM_MONITORING_TRIGGER_SWAPUSAGE)) {
                    // Convert KB to MB, since the UI displays the swap usage in MB.
                    newVal = convertKbToMbOrGb(true, (Long)newVal);
                }

                String unit;
                if (key.startsWith("interval.")) {
                    unit = "sec";
                } else {
                    unit = config.getProperty( "monitoring." + entityType + "." + propertyName + ".unit" );
                    if (unit == null) {
                        unit = "";
                    }
                }

                details.add(new AuditDetail(EsmMessages.CHANGE_SYSTEM_MONITORING_SETUP_MESSAGE, key, newVal.toString(), unit));
            }
        }

        auditContextFactory.emitAuditRecordWithDetails(auditRecord, false, this, details);
    }

    /**
     * Audit the change in a notification rule such as create, update, or delete.
     * @param name: the name of the notification rule
     * @param changingAction: the action applied on the notification, such as create, update, or delete.
     */
    private void auditSystemNotificationChange(final String name, final String changingAction) {
        AuditRecord auditRecord = new SystemAuditRecord(
            Level.INFO,
            "",
            Component.ENTERPRISE_MANAGER,
            "Change in a defined notification rule (create, update, or delete)",
            true,
            null,
            null,
            null,
            "Notification Rule Change",
            ""
        );

        auditContextFactory.emitAuditRecordWithDetails(auditRecord, false, this,
                                                              Collections.singletonList(new AuditDetail(EsmMessages.CHANGE_NOTIFICATION_SETUP_MESSAGE, name, changingAction)));
    }

    /**
     * Audit the changes in entity property setup.
     * @param isSsgNode: a flag indicating if the entity is a SSG node or a SSG cluster.
     * @param entityGuid: the GUID of the entity
     * @param ipAddress: the IP Address associated with the entity
     * @param auditDetailList: the list of AuditDetail objects for entity property setup changes.
     */
    private void auditEntityPropertySetupChange(boolean isSsgNode, String entityGuid, String ipAddress, final List<AuditDetail> auditDetailList) {
        AuditRecord auditRecord = new SystemAuditRecord(
            Level.INFO,
            isSsgNode? entityGuid : "",
            Component.ENTERPRISE_MANAGER,
            "Change in the monitoring property settings of an individual " + (isSsgNode? "Gateway node" : "Gateway cluster"),
            true,
            null,
            null,
            null,
            "Entity Property Setup Change",
            isSsgNode? ipAddress : ""
        );

        auditContextFactory.emitAuditRecordWithDetails(auditRecord, false, this, auditDetailList);
    }

    /**
     * Audit the change in alert state of entity property values (e.g., normal to alert, or alert to normal).
     * By comparing the below two lists, audit the change if appliable.
     * @param previousEntitiesList: the list storing the previous property values of entities (e.g., ssg cluster and ssg node)
     * @param newEntitiesList: the list storing the current property values of entities (e.g., ssg cluster and ssg node)
     */
    private void auditEntityPropertyAlertStateChange(List<EntityMonitoringPropertyValues> previousEntitiesList, List<EntityMonitoringPropertyValues> newEntitiesList) {
        // Find the two system monitoring setup settings related to audit.
        Map<String, Object> monitoringSetupSettings;
        try {
            monitoringSetupSettings = systemMonitoringSetupSettingsManager.findSetupSettings();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Cannot find system monitoring setup.", e);
            return;
        }
        if (monitoringSetupSettings == null) return;

        boolean auditUponAlertState = (Boolean) monitoringSetupSettings.get(EsmConfigParams.PARAM_MONITORING_AUDITUPONALERTSTATE);
        boolean auditUponNormalState = (Boolean) monitoringSetupSettings.get(EsmConfigParams.PARAM_MONITORING_AUDITUPONNORMALSTATE);

        // Audit a change if there is an alert state change from normal to alert.
        if (auditUponAlertState) {
            auditEntityPropertyAlertStateChange(true, previousEntitiesList, newEntitiesList);
        }
        // Audit a change if there is an alert state change from alert to normal.
        if (auditUponNormalState) {
            auditEntityPropertyAlertStateChange(false, previousEntitiesList, newEntitiesList);
        }
    }

    /**
     * Get AuditDetail objects and audit them if there is a change in alert state (e.g., normal to alert, or alert to normal)
     * @param checkNormalToAlert: the boolean flag indicating if checking the state transaction is from normal to alert or alert to normal.
     * @param previousEntitiesList: the list storing the previous property values of entities (e.g., ssg cluster and ssg node)
     * @param newEntitiesList: the list storing the current property values of entities (e.g., ssg cluster and ssg node)
     */
    private void auditEntityPropertyAlertStateChange(boolean checkNormalToAlert,
                                                     List<EntityMonitoringPropertyValues> previousEntitiesList,
                                                     List<EntityMonitoringPropertyValues> newEntitiesList) {
        for (EntityMonitoringPropertyValues newEntity: newEntitiesList) {
            String newGuid = newEntity.getEntityGuid();
            EntityMonitoringPropertyValues.EntityType entityType = newEntity.getEntityType();
            NamedEntityImp entity;
            boolean isSsgCluster;
            String host;
            try {
                if (entityType.equals(EntityMonitoringPropertyValues.EntityType.SSG_CLUSTER)) {
                    entity = ssgClusterManager.findByGuid(newGuid);
                    isSsgCluster = true;
                    host = ((SsgCluster)entity).getSslHostName();
                    if (host == null) host = "UNKNOWN";
                } else if (entityType.equals(EntityMonitoringPropertyValues.EntityType.SSG_NODE)) {
                    entity = ssgNodeManager.findByGuid(newGuid);
                    isSsgCluster = false;
                    host = ((SsgNode)entity).obtainHostName();
                    if (host == null) host = "UNKNOWN";
                } else {
                    logger.warning("Invalid entity type, " + newEntity.getEntityType().getName());
                    continue;
                }
            } catch (FindException e) {
                logger.warning("Cannot find the entity with the GUID, " + newGuid);
                continue;
            }

            // Find the new (current) properties map
            Map<String, Object> newPropsMap = newEntity.getPropsMap();
            // Find the previous properties map
            Map<String, Object> prevPropsMap = new HashMap<String, Object>();
            for (EntityMonitoringPropertyValues prevEntity: previousEntitiesList) {
                String prevGuid = prevEntity.getEntityGuid();
                if (newGuid.equals(prevGuid)) {
                    prevPropsMap = prevEntity.getPropsMap();
                    break;
                }
            }
            // Check alert state
            for (String property: newPropsMap.keySet()) {
                EntityMonitoringPropertyValues.PropertyValues newPropertyValues = (EntityMonitoringPropertyValues.PropertyValues) newPropsMap.get(property);
                EntityMonitoringPropertyValues.PropertyValues prevPropertyValues = (EntityMonitoringPropertyValues.PropertyValues) prevPropsMap.get(property);

                if (newPropertyValues == null) continue;

                boolean newMonitored = newPropertyValues.isMonitored();
                if (! newMonitored) continue;

                boolean newAlert = newPropertyValues.isAlert();
                StringBuilder auditMessage = new StringBuilder();
                AuditDetail auditDetail = null;
                if (checkNormalToAlert) {
                    if (! newAlert) continue;

                    // Precondition: newAlert has been true (i.e., "alert").
                    if (prevPropertyValues == null || !prevPropertyValues.isMonitored() || !prevPropertyValues.isAlert()) {
                        auditDetail = new AuditDetail(EsmMessages.AUDIT_NORMAL_TO_ALERT_MESSAGE, property);
                        auditMessage.append("State transition from normal to alert for an individual monitoring property '");
                    }
                } else { // check for "alert to normal"
                    if (newAlert) continue;

                    // Precondition: newAlert has been false (i.e., "normal")
                    if (prevPropertyValues != null && prevPropertyValues.isMonitored() && prevPropertyValues.isAlert()) {
                        auditDetail = new AuditDetail(EsmMessages.AUDIT_ALERT_TO_NORMAL_MESSAGE, property);
                        auditMessage.append("State transition from alert to normal for an individual monitoring property '");
                    }
                }

                if (auditDetail != null) {
                    auditMessage.append(property).append("' of Gateway ").append(isSsgCluster? "cluster" : "node").append(" '").append(entity.getName()).append("' (").append(host).append(")");
                    if (isSsgCluster) {
                        auditMessage.append(".");
                    } else {
                        SsgCluster cluster = ((SsgNode)entity).getSsgCluster();
                        auditMessage.append(" in Gateway cluster '").append(cluster.getName()).append("' (").append(cluster.getSslHostName()).append(").");
                    }
                    AuditRecord auditRecord = new SystemAuditRecord(
                        checkNormalToAlert? Level.WARNING : Level.INFO,
                        "",
                        Component.ENTERPRISE_MANAGER,
                        auditMessage.toString(),
                        true,
                        null,
                        null,
                        null,
                        "Property Alert State Change",
                        ""
                    );
                    auditContextFactory.emitAuditRecordWithDetails(auditRecord, false, this,
                                                                          Collections.singletonList(auditDetail));
                }
            }
        }
    }

    /**
     * Create an AuditDetail if there is change in entity property setup.
     * @param name: the property name
     * @param value: the value of the property setup
     * @param unit: the unit of the property
     * @return an AuditDetail object.
     */
    private AuditDetail getAuditDetailForEntityPropertySetupChanging(String name, Object value, String  unit) {
        if (unit == null) unit = "";
        
        return new AuditDetail(EsmMessages.CHANGE_ENTITY_PROPERTY_SETUP_MESSAGE, new String[] {name, value.toString(), unit});
    }

    /**
     * Add all AuditDetail objects into the audit detail list if there are changes in entity notification rule setup.
     * @param previousSet: the pervious notification rule set before any setup changes made.
     * @param newSet: the new notification rule set after any setup changes made.
     * @param auditDetailList: the list to store the added AuditDetail objects
     */
    private void setAuditDetailsForEntityNotificationSetupChanging(Set<SystemMonitoringNotificationRule> previousSet, Set<SystemMonitoringNotificationRule> newSet, List<AuditDetail> auditDetailList) {
        Set<SystemMonitoringNotificationRule> tempNewSet = new HashSet(newSet);
        tempNewSet.removeAll(previousSet);
        for (SystemMonitoringNotificationRule rule: tempNewSet) {
            auditDetailList.add(new AuditDetail(EsmMessages.CHANGE_ENTITY_NOTIFICATION_SETUP_MESSAGE, new String[] {rule.getName(), "selected"}));
        }

        Set<SystemMonitoringNotificationRule> tempPrevSet = new HashSet(previousSet);
        tempPrevSet.removeAll(newSet);
        for (SystemMonitoringNotificationRule rule: tempPrevSet) {
            auditDetailList.add(new AuditDetail(EsmMessages.CHANGE_ENTITY_NOTIFICATION_SETUP_MESSAGE, new String[] {rule.getName(), "unselected"}));
        }
    }
}

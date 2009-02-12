package com.l7tech.server.ems.ui.pages;

import com.l7tech.gateway.common.security.rbac.AttemptedDeleteAll;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.ems.enterprise.*;
import com.l7tech.server.ems.gateway.*;
import com.l7tech.server.ems.standardreports.*;
import com.l7tech.server.ems.ui.NavigationPage;
import com.l7tech.server.management.api.node.ReportApi;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Resolver;
import com.l7tech.util.ResolvingComparator;
import com.l7tech.util.TimeUnit;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.mortbay.util.ajax.JSON;

import java.io.Serializable;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
@NavigationPage(page="StandardReports",section="Reports",pageUrl="StandardReports.html")
public class StandardReports extends EsmStandardWebPage {
    private static final Logger logger = Logger.getLogger(StandardReports.class.getName());
    private static final SimpleDateFormat format = new SimpleDateFormat( JsonReportParameterConvertor.DATE_FORMAT );
    private static final String WARNING_LOADING_SETTINGS = "{0} in the SSG Cluster with GUID {1} cannot be added because the SSG Cluster no longer exists in the enterprise tree.";

    @SpringBean
    private ReportService reportService;

    @SpringBean
    private SsgClusterManager ssgClusterManager;

    @SpringBean
    private GatewayContextFactory gatewayContextFactory;

    @SpringBean
    private GatewayTrustTokenFactory gatewayTrustTokenFactory;

    @SpringBean
    private StandardReportSettingsManager standardReportSettingsManager;

    private DropDownChoice timeZoneChoice;
    private YuiDateSelector fromDateField;
    private YuiDateSelector toDateField;
    private Label fromDateJavascript;
    private Label toDateJavascript;

    // Keep tracking on the time-zone changing.
    private String currentTimeZoneId;

    public StandardReports() {
        currentTimeZoneId = getSession().getTimeZoneId();

        Form reportParametersForm = new Form("reportParametersForm");

        Date now = new Date();
        Date yesterday = new Date(now.getTime() - TimeUnit.DAYS.toMillis(1));
        fromDateField = new YuiDateSelector("fromDate", "absoluteTimePeriodFromDateTextBox", new Model(yesterday), null, now, currentTimeZoneId);
        toDateField = new YuiDateSelector("toDate", "absoluteTimePeriodToDateTextBox", new Model(now), yesterday, now, currentTimeZoneId);

        fromDateJavascript = new Label("fromDateJavascript", buildDateJavascript(true, fromDateField.getDateTextField().getModelObject()));
        fromDateJavascript.setEscapeModelStrings(false);

        toDateJavascript = new Label("toDateJavascript", buildDateJavascript(false, toDateField.getDateTextField().getModelObject()));
        toDateJavascript.setEscapeModelStrings(false);

        fromDateField.addInteractionWithOtherDateSelector(toDateField, false, new YuiDateSelector.InteractionTasker() {
            @Override
            protected void doExtraTask(AjaxRequestTarget ajaxRequestTarget) {
                fromDateJavascript.setModelObject(buildDateJavascript(true, fromDateField.getDateTextField().getModelObject()));
                ajaxRequestTarget.addComponent(fromDateJavascript);
            }
        });
        toDateField.addInteractionWithOtherDateSelector(fromDateField, true, new YuiDateSelector.InteractionTasker() {
            @Override
            protected void doExtraTask(AjaxRequestTarget ajaxRequestTarget) {
                toDateJavascript.setModelObject(buildDateJavascript(false, toDateField.getDateTextField().getModelObject()));
                ajaxRequestTarget.addComponent(toDateJavascript);
            }
        });

        List<String> zoneIds = Arrays.asList(TimeZone.getAvailableIDs());
        Collections.sort(zoneIds);
        timeZoneChoice = new DropDownChoice("timezone", new Model(getSession().getTimeZoneId()), zoneIds, new IChoiceRenderer(){
            @Override
            public Object getDisplayValue(Object object) {
                return object;
            }

            @Override
            public String getIdValue(Object object, int index) {
                return object.toString();
            }
        });
        timeZoneChoice.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            @Override
            protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
                Date now = new Date();
                String newTimeZoneId = (String)timeZoneChoice.getModelObject();
                format.setTimeZone(TimeZone.getTimeZone(newTimeZoneId));
                fromDateField.setNewTimeZoneUsed(newTimeZoneId);
                toDateField.setNewTimeZoneUsed(newTimeZoneId);

                fromDateField.getDateTextField().setModelObject(new Date(now.getTime() - TimeUnit.DAYS.toMillis(1)));
                fromDateField.setDateSelectorModel(null, now);
                fromDateJavascript.setModelObject(buildDateJavascript(true, fromDateField.getModelObject()));
                ajaxRequestTarget.addComponent(fromDateJavascript);
                ajaxRequestTarget.addComponent(fromDateField);

                toDateField.getDateTextField().setModelObject(now);
                toDateField.setDateSelectorModel(new Date(now.getTime() - TimeUnit.DAYS.toMillis(1)), now);
                toDateJavascript.setModelObject(buildDateJavascript(false, toDateField.getModelObject()));
                ajaxRequestTarget.addComponent(toDateJavascript);
                ajaxRequestTarget.addComponent(toDateField);

                currentTimeZoneId = newTimeZoneId;
            }
        });
        timeZoneChoice.setMarkupId("timePeriodTimeZoneDropDown");

        reportParametersForm.add( timeZoneChoice );
        reportParametersForm.add( fromDateField.setOutputMarkupId(true) );
        reportParametersForm.add( toDateField.setOutputMarkupId(true) );

        final HiddenField deleteSettingsDialogInputId = new HiddenField("deleteSettingsDialog_id", new Model(""));
        Form deleteSettingsForm = new JsonDataResponseForm("deleteSettingsForm", new AttemptedDeleteAll( EntityType.ESM_STANDARD_REPORT )){
            @SuppressWarnings({"ThrowableInstanceNeverThrown"})
            @Override
            protected Object getJsonResponseData() {
                String deletedSettingsOid = (String)deleteSettingsDialogInputId.getConvertedInput();
                try {
                    logger.fine("Deleting standard report settings (OID = "+ deletedSettingsOid + ").");

                    standardReportSettingsManager.delete(Long.parseLong(deletedSettingsOid));
                    return null;    // No response object expected if successful.
                } catch (Exception e) {
                    String errmsg = "Cannot delete the standard report settings (OID = '" + deletedSettingsOid + "').";
                    logger.warning(errmsg);
                    return new JSONException(new Exception(errmsg));
                }
            }
        };
        deleteSettingsForm.add(deleteSettingsDialogInputId );

        final Form getGatewayTrustServletInputsForm = new JsonDataResponseForm("getGatewayTrustServletInputsForm"){
            @Override
            protected Object getJsonResponseData() {
                try {
                    logger.fine("Responding to request for trust token.");
                    final String token = gatewayTrustTokenFactory.getTrustToken();
                    return new JSONSupport() {
                        @Override
                        protected void writeJson() {
                            add("token", token);
                        }
                    };
                } catch (Exception e) {
                    logger.warning(e.toString());
                    return new JSONException(e);
                }
            }
        };

        add( reportParametersForm);
        add( fromDateJavascript.setOutputMarkupId(true) );
        add( toDateJavascript.setOutputMarkupId(true) );
        add( deleteSettingsForm );
        add( getGatewayTrustServletInputsForm );

        add( new JsonInteraction("jsonMappings", "jsonMappingUrl", new MappingDataProvider()) );
        add( new JsonPostInteraction("postReportParameters", "generateReportUrl", new ReportParametersDataProvider()) );
        add( new JsonPostInteraction("postReportSavingSettings", "saveSettingsUrl", new ReportSavingSettingsDataProvider(false)) );
        add( new JsonPostInteraction("reconfirmPostReportSavingSettings", "saveSettingsOverwriteUrl", new ReportSavingSettingsDataProvider(true)) );
        add( new JsonInteraction("getReportSettingsList", "getSettingsListUrl", new SettingsListDataProvider()) );
        add( new ReportSettingsJsonPostInteraction("getReportSettings", "getSettingsUrl", new ReportSettingsDataProvider()) );

        add( new StandardReportsManagementPanel("generatedReports") );
    }

    /**
     * Create a javascript for the selected date
     *
     * @param fromDate: a flag indicating if the date is a starting date or an ending date.
     * @param date: the selected date.
     * @return
     */
    private String buildDateJavascript(boolean fromDate, Object date) {
        StringBuilder scriptBuilder = new StringBuilder();
        scriptBuilder.append( "YAHOO.util.Event.onDOMReady( function(){ " );
        scriptBuilder.append(fromDate? "absoluteTimePeriodFromDate" : "absoluteTimePeriodToDate").append(" = '").append(format.format(date)).append("'; ");
        scriptBuilder.append("onChangeAbsoluteTimePeriod(); onChangeTimePeriodType(); ");
        scriptBuilder.append( "} );" );

        return scriptBuilder.toString();
    }

    private final class MappingDataProvider implements JsonDataProvider {
        @Override
        public Object getData() {
            Object[] data = new Object[0];

            String[] clusterGuids = RequestCycle.get().getRequest().getParameters("clusterGuid");
            if ( clusterGuids != null ) {
                Collection<MappingKey> clusterKeys = new ArrayList<MappingKey>();
                for ( String clusterGuid : clusterGuids ) {
                    Collection<String> standardValues = new ArrayList<String>();
                    Collection<String> customValues = new ArrayList<String>();
                    try {
                        SsgCluster cluster = ssgClusterManager.findByGuid( clusterGuid );
                        if ( cluster != null ) {
                            GatewayContext context = gatewayContextFactory.createGatewayContext( getUser(), cluster.getGuid(), cluster.getSslHostName(), cluster.getAdminPort() );
                            ReportApi reportApi = context.getReportApi();
                            Collection<ReportApi.GroupingKey> keys = reportApi.getGroupingKeys();
                            if ( keys != null ) {
                                for ( ReportApi.GroupingKey key : keys ) {
                                    if ( key.getType() == ReportApi.GroupingKey.GroupingKeyType.STANDARD ) {
                                        standardValues.add( key.getName() );
                                    } else if ( key.getType() == ReportApi.GroupingKey.GroupingKeyType.CUSTOM ) {
                                        customValues.add( key.getName() );
                                    }
                                }
                            }
                        }
                    } catch ( GatewayNotMappedException gnme ) {
                        // ok, don't show any keys
                    } catch ( GatewayNoTrustException e ) {
                        // ok, don't show any keys
                    } catch ( GatewayException e ) {
                        // ok, don't show any keys
                    } catch ( FindException e ) {
                        logger.log( Level.WARNING, "Error getting mapping keys", e );
                    } catch (ReportApi.ReportException e) {
                        logger.log( Level.WARNING, "Error getting mapping keys '"+ ExceptionUtils.getMessage(e) +"'.", ExceptionUtils.getDebugException(e) );
                    } catch ( RuntimeException re ) {
                        if ( !GatewayContext.isConfigurationException(re) &&
                             !GatewayContext.isNetworkException(re)) {
                            logger.log( Level.WARNING, "Error getting mapping keys", re );
                        }
                    }
                    clusterKeys.add( new MappingKey( clusterGuid, standardValues, customValues ) );
                }
                data = clusterKeys.toArray(new Object[clusterKeys.size()]);
            }

            return data;
        }

        @Override
        public void setData(Object jsonData) {
        }
    }

    private final class ReportParametersDataProvider implements JsonDataProvider {
        private Object returnValue = null;

        @Override
        public Object getData() {
            return returnValue;
        }

        @Override
        public void setData(Object jsonData) {
            if(jsonData instanceof JSONException) {
                //some exception happened whilst trying to retrieve the payload from upload request
                returnValue = jsonData;
            } else if (jsonData instanceof String) {
                Object jsonDataObj;
                try {
                    jsonDataObj = JSON.parse(jsonData.toString());
                } catch(Exception e) {
                    returnValue = new JSONException(new Exception("Cannot parse uploaded JSON data", e.getCause()));
                    logger.log(Level.FINER, "Cannot parse uploaded JSON data", e.getCause());
                    return;
                }

                try {
                    if (!(jsonDataObj instanceof Map)){
                        logger.log(Level.FINER, "Incorrect JSON data. Not convertible to a Map");
                        throw new ReportException("Incorrect JSON data. Not convertible to a Map");
                    }
                    Map jsonDataMap = (Map) jsonDataObj;
                    JsonReportParameterConvertor convertor = JsonReportParameterConvertorFactory.getConvertor(jsonDataMap);
                    Collection<ReportSubmissionClusterBean> clusterBeans =
                        convertor.getReportSubmissions(jsonDataMap, getUser().getLogin());

                    for (ReportSubmissionClusterBean clusterBean: clusterBeans){
                        reportService.enqueueReport(clusterBean.getClusterId(), getUser(), clusterBean.getReportSubmission());
                    }

                    // null for success
                    returnValue = null;
                } catch (ReportException ex) {
                    logger.log(Level.FINER, "Problem running report: " + ex.getMessage(), ex.getCause());
                    returnValue = new JSONException(new Exception("Problem running report: " + ex.getMessage(), ex.getCause()));
                }
            } else {
                returnValue = new JSONException(new IllegalArgumentException("jsonData must be either a JSONException or a JSON formatted String"));
            }
        }

    }

    private final class ReportSavingSettingsDataProvider implements JsonDataProvider {
        private final boolean overwrite;
        private Object returnValue;

        public ReportSavingSettingsDataProvider( boolean overwrite ) {
            this.overwrite = overwrite;        
        }

        @Override
        public Object getData() {
            Object data = returnValue;

            if ( returnValue instanceof Map ) {
                data = new JSON.Convertible() {
                    @SuppressWarnings({"unchecked"})
                    @Override
                    public void toJSON(JSON.Output output) {
                        for ( Map.Entry<String,Object> entry : ((Map<String,Object>)returnValue).entrySet() ) {
                            output.add( entry.getKey(), entry.getValue() );
                        }
                    }

                    @Override
                    public void fromJSON(Map map) {
                        throw new UnsupportedOperationException("Mapping from JSON not supported.");
                    }
                };
            }

            return data;
        }

        @Override
        public void setData( final Object jsonData ) {
            if ( jsonData instanceof JSONException ){
                //some exception happened whilst trying to retrieve the payload from upload request
                returnValue = jsonData;
            } else if ( jsonData instanceof String ) {
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

                Map jsonDataMap = (Map) jsonDataObj;
                String name = (String) jsonDataMap.get("name");
                StandardReportSettings settings = null;

                try {
                    settings = standardReportSettingsManager.findByNameAndUser( StandardReports.this.getUser(), name );
                } catch ( FindException fe ) {
                    returnValue = new JSONException(new Exception("Problem saving report settings: " + fe.getMessage(), fe.getCause()));
                    return;
                }

                if ( settings != null && !overwrite ) {
                    returnValue = Collections.singletonMap("reconfirm", true);
                    return;
                }

                Map<String, Object> settingsProps = new HashMap<String, Object>();
                settingsProps.put(JSONConstants.REPORT_TYPE, jsonDataMap.get(JSONConstants.REPORT_TYPE));
                settingsProps.put(JSONConstants.ENTITY_TYPE, jsonDataMap.get(JSONConstants.ENTITY_TYPE));
                settingsProps.put(JSONConstants.REPORT_ENTITIES, jsonDataMap.get(JSONConstants.REPORT_ENTITIES));
                settingsProps.put(JSONConstants.TimePeriodTypeKeys.TIME_PERIOD_MAIN, jsonDataMap.get(JSONConstants.TimePeriodTypeKeys.TIME_PERIOD_MAIN));
                settingsProps.put(JSONConstants.TimePeriodTypeKeys.TIME_INTERVAL, jsonDataMap.get(JSONConstants.TimePeriodTypeKeys.TIME_INTERVAL));
                settingsProps.put(JSONConstants.GROUPINGS, jsonDataMap.get(JSONConstants.GROUPINGS));
                settingsProps.put(JSONConstants.SUMMARY_CHART, jsonDataMap.get(JSONConstants.SUMMARY_CHART));
                settingsProps.put(JSONConstants.SUMMARY_REPORT, jsonDataMap.get(JSONConstants.SUMMARY_REPORT));
                settingsProps.put(JSONConstants.REPORT_NAME, jsonDataMap.get(JSONConstants.REPORT_NAME));

                try {
                    if ( settings == null ) {
                        standardReportSettingsManager.save( new StandardReportSettings(name, StandardReports.this.getUser(), settingsProps) );
                    } else {
                        settings.setProperties( settingsProps );
                        standardReportSettingsManager.update( settings );
                    }

                    // null for success
                    returnValue = null;
                } catch (Exception e) {
                    logger.log(Level.FINER, "Problem saving report settings: " + e.getMessage(), e.getCause());

                    if (ExceptionUtils.causedBy(e, DuplicateObjectException.class)) {
                        returnValue = Collections.singletonMap("reconfirm", true);
                    } else {
                        returnValue = new JSONException(new Exception("Problem saving report settings: " + e.getMessage(), e.getCause()));
                    }
                }
            } else {
                returnValue = new JSONException(new IllegalArgumentException("jsonData must be either a JSONException or a JSON formatted String"));
            }
        }
    }

    private final class SettingsListDataProvider implements JsonDataProvider {
        @SuppressWarnings({"unchecked"})
        @Override
        public Object getData() {
            final List<StandardReportSettings> settingsList = new ArrayList<StandardReportSettings>();
            try {
                for (StandardReportSettings settings: standardReportSettingsManager.findByUser( StandardReports.this.getUser() )) {
                    settingsList.add(settings);
                }
            } catch (FindException e) {
                logger.warning("Cannot find standard reports settings.");
                return new JSONException(e);
            }
            Collections.sort( settingsList, new ResolvingComparator( new Resolver(){
                @Override
                public Object resolve( Object key ) {
                    StandardReportSettings standardReportSettings = (StandardReportSettings) key;
                    return standardReportSettings.getName().toLowerCase();
                }
            }, false ));
            return settingsList;
        }

        @Override
        public void setData(Object jsonData) {
            throw new UnsupportedOperationException("setData not required in JsonInteraction");
        }
    }

    private final class ReportSettingsDataProvider implements JsonDataProvider {
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
                String oid = (String) jsonData;
                try {
                    StandardReportSettings reportSettings = standardReportSettingsManager.findByPrimaryKeyForUser( StandardReports.this.getUser(), Integer.parseInt(oid));
                    if ( reportSettings == null ) {
                        returnValue = new JSONException(new NumberFormatException("The OID ('" + oid + "') is not a valid settings.."));
                    } else {
                        Map<String, Object> settingsPropsMap = new HashMap<String,Object>(reportSettings.getProperties());
                        updateReportSettings(settingsPropsMap);

                        // null for success
                        returnValue = settingsPropsMap;
                    }
                } catch (NumberFormatException e) {
                    String errmsg = "The OID ('" + oid + "') is not an integer.";
                    logger.warning(errmsg);
                    returnValue = new JSONException(new NumberFormatException("The OID ('" + oid + "') is not an integer."));
                } catch (FindException e) {
                    String errmsg = "Cannot find standard reports settings by OID ('" + oid + "').";
                    logger.warning(errmsg);
                    returnValue = new JSONException(new FindException(errmsg));
                }
            } else {
                returnValue = new JSONException(new IllegalArgumentException("jsonData must be either a JSONException or a JSON formatted String"));
            }
        }
    }

    private final class ReportSettingsJsonPostInteraction extends JsonPostInteraction {
        public ReportSettingsJsonPostInteraction(final String id, final String jsonUrlVariable, final JsonDataProvider provider) {
            super(id, jsonUrlVariable, provider);

            add(buildLoadingTimePeriodSettingsBehavior("loadTimePeriodSettingsUrl"));
        }
    }

    private static final class MappingKey implements JSON.Convertible, Serializable {
        private final String id;
        private final String[] standard;
        private final String[] custom;

        private MappingKey( String id, Collection<String> standard, Collection<String> custom ) {
            this.id = id;
            this.standard = standard.toArray( new String[standard.size()] );
            this.custom = custom.toArray( new String[custom.size()] );
        }

        public String getId() {
            return id;
        }

        public String[] getStandard() {
            return standard;
        }

        public String[] getCustom() {
            return custom;
        }

        @Override
        public void toJSON(final JSON.Output out) {
            out.add("id", id);
            out.add("standard", standard);
            out.add("custom", custom);
        }

        @Override
        public void fromJSON(final Map object) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Add some runtime properties (such as clusterName and clusterAncestors for "entities" and "groupings") into the
     * the settings and also validate all properties of the setting.
     *
     * @param settingsPropsMap: the map of the settings properties.
     */
    @SuppressWarnings({"unchecked"})
    private void updateReportSettings(Map<String, Object> settingsPropsMap) {
        List<String> warnings = new ArrayList<String>();
        MessageFormat warningMsgFormat = new MessageFormat(WARNING_LOADING_SETTINGS);

        // Step 1: update and validate "entities"
        List<String> removedGuidsForEntities = new ArrayList<String>();
        List<Map<String,Object>> entitiesPropsMapList = new ArrayList<Map<String,Object>>();
        for ( Object item : (Object[])settingsPropsMap.get(JSONConstants.REPORT_ENTITIES) ) {
            entitiesPropsMapList.add( (Map<String,Object>) item );    
        }
        for ( Iterator<Map<String,Object>> entityPropsMapIter = entitiesPropsMapList.iterator(); entityPropsMapIter.hasNext(); ) {
            Map<String,Object> entityPropsMap = entityPropsMapIter.next();
            String clusterGuid = (String) entityPropsMap.get(JSONConstants.ReportEntities.CLUSTER_ID);
            try {
                SsgCluster ssgCluster = ssgClusterManager.findByGuid(clusterGuid);
                if (ssgCluster == null) {
                    throw new FindException("Cannot find the SSG cluster (GUID = '" + clusterGuid + "').");
                }

                entityPropsMap.put(JSONConstants.CLUSTER_NAME, ssgCluster.getName());
                entityPropsMap.put(JSONConstants.CLUSTER_ANCESTORS, getClusterAncestors(ssgCluster));
            } catch (FindException e) {
                if (! removedGuidsForEntities.contains(clusterGuid)) {
                    removedGuidsForEntities.add(clusterGuid);
                    warnings.add(warningMsgFormat.format(new Object[]{"Entities", clusterGuid}));
                }
                entityPropsMapIter.remove();
            }
        }

        // Update the "entities" property
        settingsPropsMap.put(JSONConstants.REPORT_ENTITIES, entitiesPropsMapList.toArray());

        // Step 2: update and validate "groupings"
        List<Object> groupingsPropsMapList = new ArrayList<Object>(Arrays.asList((Object[])settingsPropsMap.get(JSONConstants.GROUPINGS)));
        List<String> removedGuidsForGroupings = new ArrayList<String>();
        for (Iterator<Object> itr = groupingsPropsMapList.iterator(); itr.hasNext(); ) {
            Map groupingPropsMap = (Map) itr.next();
            String clusterGuid = (String) groupingPropsMap.get(JSONConstants.ReportEntities.CLUSTER_ID);
            try {
                SsgCluster ssgCluster = ssgClusterManager.findByGuid(clusterGuid);
                if (ssgCluster == null) {
                    throw new FindException("Cannot find the SSG cluster (GUID = '" + clusterGuid + "').");
                }
                
                groupingPropsMap.put(JSONConstants.CLUSTER_NAME, ssgCluster.getName());
                groupingPropsMap.put(JSONConstants.CLUSTER_ANCESTORS, getClusterAncestors(ssgCluster));
            } catch (FindException e) {
                if (! removedGuidsForGroupings.contains(clusterGuid)) {
                    removedGuidsForGroupings.add(clusterGuid);
                    warnings.add(warningMsgFormat.format(new Object[]{"Groupings", clusterGuid}));
                }
                itr.remove();
            }
        }
        // Update the "groupings" property
        settingsPropsMap.put(JSONConstants.GROUPINGS, groupingsPropsMapList.toArray());

        // Step 3: Check if there are warnings.
        if (! warnings.isEmpty()) {
            settingsPropsMap.put(JSONConstants.REPORT_SETTINGS_WARNING_ITEMS, warnings.toArray());
        }
    }

    private Object[] getClusterAncestors(SsgCluster ssgCluster) {
        List<String> ancestorNames = new ArrayList<String>();

        for (EnterpriseFolder ancestor: ssgClusterManager.findAllAncestors(ssgCluster)) {
            ancestorNames.add(ancestor.getName());
        }

        return ancestorNames.toArray();
    }

    private AbstractDefaultAjaxBehavior buildLoadingTimePeriodSettingsBehavior(final String callbackUrl) {
        return new AbstractDefaultAjaxBehavior(){
            @Override
            public void renderHead( final IHeaderResponse iHeaderResponse ) {
                super.renderHead( iHeaderResponse );
                iHeaderResponse.renderJavascript("var " + callbackUrl + " = '" + getCallbackUrl(true) + "';", null);
            }

            @Override
            protected void respond( final AjaxRequestTarget ajaxRequestTarget ) {
                WebRequest request = (WebRequest) RequestCycle.get().getRequest();
                String type = request.getParameter("type");
                String timeZone = request.getParameter("timeZone");

                // Update time zone
                timeZoneChoice.setModelObject(timeZone);
                ajaxRequestTarget.addComponent(timeZoneChoice);

                if ("relative".equals(type)) {
                    return;
                }

                String absoluteFrom = request.getParameter("absoluteFrom");
                String absoluteTo = request.getParameter("absoluteTo");

                // Update "from" and "to" date selectors 
                Date now = new Date();
                String newTimeZoneId = (String)timeZoneChoice.getModelObject();
                format.setTimeZone(TimeZone.getTimeZone(newTimeZoneId));
                fromDateField.setNewTimeZoneUsed(newTimeZoneId);
                toDateField.setNewTimeZoneUsed(newTimeZoneId);

                Date from, to;
                try {
                    from = new Date(absoluteFrom);
                    to = new Date(absoluteTo);
                } catch (Exception e) {
                    to = now;
                    from = new Date(now.getTime() - TimeUnit.DAYS.toMillis(1));
                }

                fromDateField.getDateTextField().setModelObject(from);
                fromDateField.setDateSelectorModel(null, to);
                fromDateJavascript.setModelObject(buildDateJavascript(true, fromDateField.getModelObject()));
                ajaxRequestTarget.addComponent(fromDateJavascript);
                ajaxRequestTarget.addComponent(fromDateField);

                toDateField.getDateTextField().setModelObject(to);
                toDateField.setDateSelectorModel(from, now);
                toDateJavascript.setModelObject(buildDateJavascript(false, toDateField.getModelObject()));
                ajaxRequestTarget.addComponent(toDateJavascript);
                ajaxRequestTarget.addComponent(toDateField);

                currentTimeZoneId = newTimeZoneId;
            }
        };
    }
}

package com.l7tech.server.ems.ui.pages;

import com.l7tech.server.ems.ui.NavigationPage;
import com.l7tech.server.ems.enterprise.JSONException;
import com.l7tech.server.ems.enterprise.SsgCluster;
import com.l7tech.server.ems.enterprise.SsgClusterManager;
import com.l7tech.server.ems.gateway.GatewayContext;
import com.l7tech.server.ems.gateway.GatewayContextFactory;
import com.l7tech.server.ems.gateway.GatewayException;
import com.l7tech.server.ems.gateway.GatewayNotMappedException;
import com.l7tech.server.ems.gateway.GatewayNoTrustException;
import com.l7tech.server.ems.standardreports.*;
import com.l7tech.server.management.api.node.ReportApi;
import com.l7tech.util.TimeUnit;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.objectmodel.FindException;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.mortbay.util.ajax.JSON;

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
    
    @SpringBean
    private ReportService reportService;

    @SpringBean
    private SsgClusterManager ssgClusterManager;

    @SpringBean
    private GatewayContextFactory gatewayContextFactory;

    // Keep tracking on the time-zone changing.
    private String currentTimeZoneId;

    public StandardReports() {
        currentTimeZoneId = getSession().getTimeZoneId();

        add(new JsonPostInteraction("postReportParameters", "generateReportUrl", new JsonDataProvider(){

            Object returnValue = null;

            @Override
            public Object getData() {
                return returnValue;
            }

            @Override
            public void setData(Object jsonData) {
                if( jsonData instanceof JSONException ){
                    //some exception happened whilst trying to retrieve the payload from upload request
                    returnValue = jsonData;
                } else if ( jsonData instanceof String ) {
                    Object jsonDataObj;
                    try{
                        jsonDataObj = JSON.parse(jsonData.toString());
                    }catch(Exception e){
                        returnValue = new JSONException(
                                new Exception("Cannot parse uploaded JSON data", e.getCause()));
                        logger.log(Level.FINER, "Cannot parse uploaded JSON data", e.getCause());
                        return;
                    }

                    try{
                        if(!(jsonDataObj instanceof Map)){
                            logger.log(Level.FINER, "Incorrect JSON data. Not convertible to a Map");
                            throw new ReportException("Incorrect JSON data. Not convertible to a Map");
                        }
                        Map jsonDataMap = (Map) jsonDataObj;
                        JsonReportParameterConvertor convertor = JsonReportParameterConvertorFactory.getConvertor(jsonDataMap);
                        Collection<ReportSubmissionClusterBean> clusterBeans =
                                convertor.getReportSubmissions(jsonDataMap, getUser().getLogin());

                        for(ReportSubmissionClusterBean clusterBean: clusterBeans){
                            reportService.enqueueReport(clusterBean.getClusterId(), getUser(), clusterBean.getReportSubmission());
                        }

                        // null for success
                        returnValue = null;
                    }catch(ReportException ex){
                        logger.log(Level.FINER, "Problem running report: " + ex.getMessage(), ex.getCause());
                        returnValue = new JSONException(
                                new Exception("Problem running report: " + ex.getMessage(), ex.getCause()));
                    }
                } else {
                    throw new IllegalArgumentException("jsonData must be either a JSONException or a JSON formatted String");                    
                }
            }
        }));

        add( new StandardReportsManagementPanel("generatedReports") );

        Form form = new Form("form");

        Date now = new Date();
        Date yesterday = new Date(now.getTime() - TimeUnit.DAYS.toMillis(1));
        final YuiDateSelector fromDateField = new YuiDateSelector("fromDate", "absoluteTimePeriodFromDateTextBox", new Model(yesterday), null, now, currentTimeZoneId);
        final YuiDateSelector toDateField = new YuiDateSelector("toDate", "absoluteTimePeriodToDateTextBox", new Model(now), yesterday, now, currentTimeZoneId);

        final Label fromDateJavascript = new Label("fromDateJavascript", buildDateJavascript(true, fromDateField.getDateTextField().getModelObject()));
        fromDateJavascript.setEscapeModelStrings(false);

        final Label toDateJavascript = new Label("toDateJavascript", buildDateJavascript(false, toDateField.getDateTextField().getModelObject()));
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
        final DropDownChoice timeZoneChoice = new DropDownChoice("timezone", new Model(getSession().getTimeZoneId()), zoneIds, new IChoiceRenderer(){
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

        form.add( timeZoneChoice );
        form.add( fromDateField.setOutputMarkupId(true) );
        form.add( toDateField.setOutputMarkupId(true) );

        add( form );
        add( fromDateJavascript.setOutputMarkupId(true) );
        add( toDateJavascript.setOutputMarkupId(true) );

        add( new JsonInteraction("jsonMappings", "jsonMappingUrl", new MappingDataProvider() ) );
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
                            GatewayContext context = gatewayContextFactory.getGatewayContext( getUser(), cluster.getGuid(), cluster.getSslHostName(), cluster.getAdminPort() );
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

    private static final class MappingKey implements JSON.Convertible {
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
}

package com.l7tech.server.ems.pages;

import com.l7tech.server.ems.NavigationPage;
import com.l7tech.server.ems.gateway.GatewayContextFactory;
import com.l7tech.server.ems.gateway.GatewayContext;
import com.l7tech.server.ems.standardreports.*;
import com.l7tech.server.ems.enterprise.JSONException;
import com.l7tech.server.ems.enterprise.SsgClusterManager;
import com.l7tech.server.ems.enterprise.SsgCluster;
import com.l7tech.server.management.api.node.ReportApi;
import com.l7tech.util.TimeUnit;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.RequestCycle;
import org.mortbay.util.ajax.JSON;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Map;
import java.util.Collection;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.TimeZone;
import java.util.Arrays;
import java.text.SimpleDateFormat;

/**
 *
 */
@NavigationPage(page="StandardReports",section="Reports",pageUrl="StandardReports.html")
public class StandardReports extends EmsPage  {
    private static final Logger logger = Logger.getLogger(StandardReports.class.getName());

    @SpringBean
    private ReportService reportService;

    @SpringBean
    private SsgClusterManager ssgClusterManager;

    @SpringBean
    private GatewayContextFactory gatewayContextFactory;

    public StandardReports() {

        add(new JsonPostInteraction("postReportParameters", "generateReportUrl", new JsonDataProvider(){

            Object returnValue = null;

            @Override
            public Object getData() {
                return returnValue;
            }

            @Override
            public void setData(Object jsonData) {

                if(!(jsonData instanceof JSONException || jsonData instanceof String)){
                    throw new IllegalArgumentException("jsonData must be either a JSONException or" +
                            "a JSON formatted String");
                }

                if(jsonData instanceof JSONException){
                    //some exception happened whilst trying to retrieve the payload from upload request
                    returnValue = jsonData;
                    return;
                }

                Object jsonDataObj = null;

                if(jsonData instanceof String){
                    try{
                        jsonDataObj = JSON.parse(jsonData.toString());
                    }catch(Exception e){
                        returnValue = new JSONException(
                                new Exception("Cannot parse uploaded JSON data", e.getCause()));
                        return;
                    }
                }

                try{
                    if(!(jsonDataObj instanceof Map)){
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
                    returnValue = new JSONException(
                            new Exception("Problem running report: " + ex.getMessage(), ex.getCause()));
                }
            }
        }));

        add( new StandardReportsManagementPanel("generatedReports") );

        Form form = new Form("form");

        List<String> zoneIds = Arrays.asList(TimeZone.getAvailableIDs());
        Collections.sort(zoneIds);
        form.add(new DropDownChoice("timezone", zoneIds, new IChoiceRenderer(){
            @Override
            public Object getDisplayValue(Object object) {
                return object;
            }

            @Override
            public String getIdValue(Object object, int index) {
                return object.toString();
            }
        }));

        final Date now = new Date();
        final YuiDateSelector fromDateField = new YuiDateSelector("fromDate", "absoluteTimePeriodFromDateTextBox",
            new Model(new Date(now.getTime() - TimeUnit.DAYS.toMillis(1))), null, now);
        final YuiDateSelector toDateField = new YuiDateSelector("toDate", "absoluteTimePeriodToDateTextBox",
            new Model(new Date(now.getTime())), new Date(now.getTime() - TimeUnit.DAYS.toMillis(1)), now);

        StringBuilder scriptBuilder = new StringBuilder();
        SimpleDateFormat format = new SimpleDateFormat( JsonReportParameterConvertor.DATE_FORMAT );
        scriptBuilder.append( "YAHOO.util.Event.onDOMReady( function(){ " );
        scriptBuilder.append("absoluteTimePeriodFromDate = '").append(format.format(fromDateField.getDateTextField().getModelObject())).append("'; ");
        scriptBuilder.append("absoluteTimePeriodToDate = '").append(format.format(toDateField.getDateTextField().getModelObject())).append("'; ");
        scriptBuilder.append( "} );" );
        Label script = new Label("javascript", scriptBuilder.toString() );
        script.setEscapeModelStrings(false);

        fromDateField.getDateTextField().add(new AjaxFormComponentUpdatingBehavior("onchange") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                // update the hidden form fields with the date in the expected format
                SimpleDateFormat format = new SimpleDateFormat( JsonReportParameterConvertor.DATE_FORMAT );
                target.appendJavascript("absoluteTimePeriodFromDate = '"+format.format(fromDateField.getDateTextField().getModelObject())+"';");

                toDateField.updateJavascriptLabelByDates((Date)fromDateField.getModelObject(), now);
                target.addComponent(toDateField);
            }
        });
        toDateField.getDateTextField().add(new AjaxFormComponentUpdatingBehavior("onchange"){
            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                // update the hidden form fields with the date in the expected format
                SimpleDateFormat format = new SimpleDateFormat( JsonReportParameterConvertor.DATE_FORMAT );
                target.appendJavascript("absoluteTimePeriodToDate = '"+format.format(toDateField.getDateTextField().getModelObject())+"';");

                fromDateField.updateJavascriptLabelByDates(null, (Date)toDateField.getModelObject());
                target.addComponent(fromDateField);
            }
        });

        form.add( fromDateField.setOutputMarkupId(true) );
        form.add( toDateField.setOutputMarkupId(true) );

        add( form );
        add( script );

        add( new JsonInteraction("jsonMappings", "jsonMappingUrl", new MappingDataProvider() ) );
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
                            GatewayContext context = gatewayContextFactory.getGatewayContext( getUser(), cluster.getSslHostName(), cluster.getAdminPort() );
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
                    } catch ( Exception e ) {
                        logger.log( Level.WARNING, "Error getting mapping keys", e );
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
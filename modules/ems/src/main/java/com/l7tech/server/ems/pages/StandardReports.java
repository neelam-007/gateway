package com.l7tech.server.ems.pages;

import com.l7tech.server.ems.NavigationPage;
import com.l7tech.server.ems.standardreports.*;
import com.l7tech.server.ems.enterprise.JSONException;
import com.l7tech.util.TimeUnit;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.mortbay.util.ajax.JSON;

import java.util.logging.Logger;
import java.util.*;
import java.text.SimpleDateFormat;

/**
 *
 */
@NavigationPage(page="StandardReports",section="Reports",pageUrl="StandardReports.html")
public class StandardReports extends EmsPage  {
    private static final Logger logger = Logger.getLogger(StandardReports.class.getName());

    @SpringBean
    private ReportService reportService;

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
                        JSONException jsonException = new JSONException(
                                new Exception("Cannot parse uploaded JSON data", e.getCause()));
                        returnValue = jsonException;
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
                    JSONException jsonException = new JSONException(
                            new Exception("Problem running report: " + ex.getMessage(), ex.getCause()));
                    returnValue = jsonException;
                }
            }
        }));

        add( new StandardReportsManagementPanel("generatedReports") );

        Form form = new Form("form");

        List<String> zoneIds = Arrays.asList(TimeZone.getAvailableIDs());
        Collections.sort(zoneIds);
        form.add(new DropDownChoice("timezone", zoneIds));

        final Date now = new Date();
        final YuiDateSelector fromDateField = new YuiDateSelector("fromDate", new Model(new Date(now.getTime() - TimeUnit.DAYS.toMillis(7))), now, true);
        final YuiDateSelector toDateField = new YuiDateSelector("toDate", new Model(new Date(now.getTime())), now);

        StringBuilder scriptBuilder = new StringBuilder();
        SimpleDateFormat format = new SimpleDateFormat( JsonReportParameterConvertor.DATE_FORMAT );
        scriptBuilder.append( "YAHOO.util.Event.onDOMReady( function(){ " );
        scriptBuilder.append( "absoluteTimePeriodFromDate = '"+format.format(fromDateField.getDateTextField().getModelObject())+"'; " );
        scriptBuilder.append( "absoluteTimePeriodToDate = '"+format.format(toDateField.getDateTextField().getModelObject())+"'; " );
        scriptBuilder.append( "} );" );
        Label script = new Label("javascript", scriptBuilder.toString() );
        script.setEscapeModelStrings(false);

        fromDateField.getDateTextField().setRequired(true);
        fromDateField.getDateTextField().setMarkupId("absoluteTimePeriodFromDateTextBox");
        fromDateField.getDateTextField().add(new AjaxFormSubmitBehavior(form, "onchange"){
            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                // update the hidden form fields with the date in the expected format
                SimpleDateFormat format = new SimpleDateFormat( JsonReportParameterConvertor.DATE_FORMAT );
                target.appendJavascript("absoluteTimePeriodFromDate = '"+format.format(fromDateField.getDateTextField().getModelObject())+"';");
            }
            @Override
            protected void onError(final AjaxRequestTarget target) {
            }
        });
        toDateField.getDateTextField().setRequired(true);
        toDateField.getDateTextField().setMarkupId("absoluteTimePeriodToDateTextBox");
        toDateField.getDateTextField().add(new AjaxFormSubmitBehavior(form, "onchange"){
            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                // update the hidden form fields with the date in the expected format
                SimpleDateFormat format = new SimpleDateFormat( JsonReportParameterConvertor.DATE_FORMAT );
                target.appendJavascript("absoluteTimePeriodToDate = '"+format.format(toDateField.getDateTextField().getModelObject())+"';");
            }
            @Override
            protected void onError(final AjaxRequestTarget target) {
            }
        });

        form.add( fromDateField );
        form.add( toDateField );

        add( form );
        add( script );
    }
}

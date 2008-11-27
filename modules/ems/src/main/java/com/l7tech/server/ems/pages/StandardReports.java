package com.l7tech.server.ems.pages;

import com.l7tech.server.ems.NavigationPage;
import com.l7tech.server.ems.standardreports.*;
import com.l7tech.server.ems.enterprise.JSONException;
import com.l7tech.server.management.api.node.ReportApi;
import com.l7tech.common.io.IOUtils;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WicketEventReference;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.ajax.WicketAjaxReference;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Component;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.protocol.http.WebRequest;
import org.mortbay.util.ajax.JSON;

import javax.servlet.ServletInputStream;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Collection;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.IOException;

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

            public Object getData() {
                return returnValue;
            }

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

    }
}

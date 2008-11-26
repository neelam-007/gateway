package com.l7tech.server.ems.pages;

import com.l7tech.server.ems.NavigationPage;
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
import org.apache.wicket.protocol.http.WebRequest;
import org.mortbay.util.ajax.JSON;

import javax.servlet.ServletInputStream;
import java.util.logging.Logger;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.IOException;

/**
 *
 */
@NavigationPage(page="StandardReports",section="Reports",pageUrl="StandardReports.html")
public class StandardReports extends EmsPage  {
    private static final Logger logger = Logger.getLogger(StandardReports.class.getName());

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

                Object jsonDataMap = null;

                if(jsonData instanceof String){
                    try{
                        jsonDataMap = JSON.parse(jsonData.toString());
                    }catch(Exception e){
                        JSONException jsonException = new JSONException(
                                new Exception("Cannot parse uploaded JSON data", e.getCause()));
                        returnValue = jsonException;
                        return;
                    }
                }

                try{
                    //Start report running process here
                    if(false){
                        throw new ReportApi.ReportException("never going into production like this, compiler now happy");
                    }
                    //report is now being submitted
                    System.out.print("Data successfully submitted and parsed");
                    //report has been submitted and response can be sent to client

                    // null for success
                    returnValue = null;
                }catch(ReportApi.ReportException ex){
                    JSONException jsonException = new JSONException(
                            new Exception("Problem running report", ex.getCause()));
                    returnValue = jsonException;
                }
            }
        }));

    }
}

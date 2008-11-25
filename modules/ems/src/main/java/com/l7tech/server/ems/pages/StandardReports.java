package com.l7tech.server.ems.pages;

import com.l7tech.server.ems.NavigationPage;
import com.l7tech.server.ems.enterprise.JSONException;
import org.apache.wicket.markup.html.form.Form;

import java.util.logging.Logger;

/**
 *
 */
@NavigationPage(page="StandardReports",section="Reports",pageUrl="StandardReports.html")
public class StandardReports extends EmsPage  {
    private static final Logger logger = Logger.getLogger(StandardReports.class.getName());

    public StandardReports() {
        final Form generateReportForm = new JsonDataResponseForm("generateReportForm") {
            @Override
            protected Object getJsonResponseData() {
                logger.info("Received request for standard report generation.");

                try {
                    // TODO Parse HTTP request body into JSON and initiate report generation

                    return null;    // No response object expected if successful.
                } catch (/* TODO specify checked exception here */Exception e) {
                    logger.warning(e.toString());
                    return new JSONException(e);
                }
            }
        };

        add(generateReportForm);
    }
}

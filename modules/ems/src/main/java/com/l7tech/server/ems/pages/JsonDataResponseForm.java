package com.l7tech.server.ems.pages;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.IRequestTarget;
import org.mortbay.util.ajax.JSON;

/**
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Nov 6, 2008
 */
public abstract class JsonDataResponseForm extends Form {
    /**
     * Constructor to create a form to manage json requests from the browser
     * @param id: a wicket component id
     */
    public JsonDataResponseForm(String id) {
        super(id);
    }

    /**
     * Submit a json response with json data.
     */
    protected final void onSubmit() {
        Object data = getJsonResponseData();
        sendResponse(data);
    }

    /**
     * Prepare the json response data.
     * @return the data to be sent back the browser.
     */
    protected abstract Object getJsonResponseData();

    /**
     * Send the json data back to the browser.
     * @param data: the json response data
     */
    protected void sendResponse(final Object data) {
        RequestCycle.get().setRequestTarget(new IRequestTarget() {
            public void detach(RequestCycle requestCycle) {}

            public void respond(RequestCycle requestCycle) {
                // Processing JSON request
                JSON json = new JSON();

                StringBuffer dataBuffer = new StringBuffer(2048);
                json.append(dataBuffer, data);

                requestCycle.getResponse().setContentType("application/json");
                requestCycle.getResponse().write(dataBuffer);
            }
        });
    }
}

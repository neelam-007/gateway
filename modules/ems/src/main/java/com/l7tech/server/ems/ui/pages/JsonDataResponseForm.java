package com.l7tech.server.ems.ui.pages;

import org.apache.wicket.RequestCycle;
import org.apache.wicket.IRequestTarget;
import org.apache.wicket.protocol.http.WebResponse;
import org.mortbay.util.ajax.JSON;
import com.l7tech.gateway.common.security.rbac.AttemptedOperation;

/**
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Nov 6, 2008
 */
public abstract class JsonDataResponseForm extends SecureForm {

    /**
     * Constructor to create a form to manage json requests from the browser
     * @param id: a wicket component id
     */
    public JsonDataResponseForm(String id) {
        super(id);
    }

    /**
     * Constructor to create a form to manage json requests from the browser
     * @param id: a wicket component id
     */
    public JsonDataResponseForm(String id, AttemptedOperation attemptedOperation) {
        super(id, attemptedOperation);
    }

    /**
     * Submit a json response with json data.
     */
    @Override
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
            @Override
            public void detach(RequestCycle requestCycle) {}

            @Override
            public void respond(RequestCycle requestCycle) {
                // Processing JSON request
                JSON json = new JSON();

                StringBuffer dataBuffer = new StringBuffer(2048);
                json.append(dataBuffer, data);

                final WebResponse response = (WebResponse)requestCycle.getResponse();
                response.setContentType("application/json; charset=UTF-8");
                response.setHeader("Expires", "Mon, 26 Jul 1997 05:00:00 GMT");
                response.setHeader("Cache-Control", "no-cache, must-revalidate");
                response.setHeader("Pragma", "no-cache");
                response.write(dataBuffer);
            }
        });
    }
}

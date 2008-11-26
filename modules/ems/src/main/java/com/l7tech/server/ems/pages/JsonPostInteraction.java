package com.l7tech.server.ems.pages;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WicketEventReference;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.ajax.WicketAjaxReference;
import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.IRequestTarget;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.protocol.http.WebRequest;
import org.mortbay.util.ajax.JSON;

import javax.servlet.ServletInputStream;

import com.l7tech.common.io.IOUtils;
import com.l7tech.server.ems.enterprise.JSONException;

import java.io.IOException;
import java.util.Map;

/**
 * JSON Post interaction is a component that provides a JavaScript URL to which JSON data can be uploaded as the body
 * of the request.
 * Note the content type of this request must be application/json this can be set on Yahoo library as follows:
 * <em>
 *      YAHOO.util.Connect.setDefaultPostHeader(false);
 *      YAHOO.util.Connect.initHeader("Content-Type","application/json; charset=utf-8");
 * </em>
 */
public class JsonPostInteraction extends Panel {

    //- PUBLIC

    public JsonPostInteraction( final String id, final String jsonUrlVariable, final JsonDataProvider provider ) {
        super(id);
        this.provider = provider;

        add( new AbstractAjaxBehavior(){
            @Override
            public void renderHead( final IHeaderResponse iHeaderResponse ) {
                super.renderHead( iHeaderResponse );
                iHeaderResponse.renderJavascriptReference(WicketEventReference.INSTANCE);
                iHeaderResponse.renderJavascriptReference(WicketAjaxReference.INSTANCE);
                iHeaderResponse.renderJavascript("var " +jsonUrlVariable+ " = '/"+getCallbackUrl(true)+"';", null);
            }

            @Override
            public void onRequest() {

                WebRequest webRequest = (WebRequest) RequestCycle.get().getRequest();
                try {
                    ServletInputStream inputStream = webRequest.getHttpServletRequest().getInputStream();
                    String input = new String(IOUtils.slurpStream(inputStream, 10000), "utf-8");
                    provider.setData(input);
                } catch (IOException e) {
                    JSONException jsonException = new JSONException(e.getCause());
                    provider.setData(jsonException);
                }

                boolean isPageVersioned = true;
                Page page = getComponent().getPage();
                try {
                    isPageVersioned = page.isVersioned();
                    page.setVersioned(false);

                    RequestCycle.get().setRequestTarget(new IRequestTarget() {
                        @Override
                        public void detach(RequestCycle requestCycle) {}
                        @Override
                        public void respond(RequestCycle requestCycle) {
                            final WebRequest request = (WebRequest)requestCycle.getRequest();
                            final WebResponse response = (WebResponse)requestCycle.getResponse();
                            response.setContentType("application/json");
                            response.setHeader("Expires", "Mon, 26 Jul 1997 05:00:00 GMT");
                            response.setHeader("Cache-Control", "no-cache, must-revalidate");
                            response.setHeader("Pragma", "no-cache");
                            JsonPostInteraction.this.onRequest( request, response );
                        }
                });
                } finally {
                     page.setVersioned(isPageVersioned);
                }
            }
        });
    }

    //- PROTECTED

    @SuppressWarnings({"UnusedDeclaration"})
    protected void onRequest( final WebRequest request, final WebResponse response ) {
        writeJsonResponse( response, provider.getData() );
    }

    protected void writeJsonResponse( final WebResponse response, final Object data ) {
        // Add JSON script to the response
        JSON json = new JSON();

        StringBuffer dataBuffer = new StringBuffer(2048);
        json.append(dataBuffer, data);

        response.write(dataBuffer);
    }

    //- PRIVATE

    private final JsonDataProvider provider;
}
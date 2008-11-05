package com.l7tech.server.ems.pages;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WicketEventReference;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.ajax.WicketAjaxReference;
import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.IRequestTarget;
import org.apache.wicket.Response;
import org.apache.wicket.Request;
import org.mortbay.util.ajax.JSON;

import java.util.logging.Logger;

/**
 * JSON interaction is a component that provides a JavaScript URL for JSON data access.
 */
public class JsonInteraction extends Panel {

    //- PUBLIC

    public JsonInteraction( final String id, final String jsonUrlVariable, final JsonDataProvider provider ) {
        super(id);
        this.provider = provider;

        add( new AbstractAjaxBehavior(){
            public void renderHead( final IHeaderResponse iHeaderResponse ) {
                super.renderHead( iHeaderResponse );
                iHeaderResponse.renderJavascriptReference(WicketEventReference.INSTANCE);
                iHeaderResponse.renderJavascriptReference(WicketAjaxReference.INSTANCE);
                iHeaderResponse.renderJavascript("var " +jsonUrlVariable+ " = '/"+getCallbackUrl(true)+"';", null);
            }

            public void onRequest() {
                boolean isPageVersioned = true;
                Page page = getComponent().getPage();
                try {
                    isPageVersioned = page.isVersioned();
                    page.setVersioned(false);

                    RequestCycle.get().setRequestTarget(new IRequestTarget() {
                        public void detach(RequestCycle requestCycle) {}
                        public void respond(RequestCycle requestCycle) {
                            logger.info("Processing JSON request for enterprise tree.");
                            JsonInteraction.this.onRequest( requestCycle.getRequest(), requestCycle.getResponse() );
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
    protected void onRequest( final Request request, final Response response ) {
        writeJsonResponse( response, provider.getData() );
    }

    protected void writeJsonResponse( final Response response, final Object data ) {
        // Add JSON script to the response
        JSON json = new JSON();

        StringBuffer dataBuffer = new StringBuffer(2048);
        json.append(dataBuffer, data);

        response.setContentType("application/json");
        response.write(dataBuffer);        
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(JsonInteraction.class.getName());

    private final JsonDataProvider provider;
}

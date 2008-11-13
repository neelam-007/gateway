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

/**
 * JSON interaction is a component that provides a JavaScript URL for JSON data access.
 */
public class JsonInteraction extends Panel {

    //- PUBLIC

    public JsonInteraction( final String id, final String jsonUrlVariable, final JsonDataProvider provider ) {
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
                            JsonInteraction.this.onRequest( request, response );
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

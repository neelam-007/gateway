package com.l7tech.server.ems.ui.pages;

import com.l7tech.server.ems.util.JsonUtil;
import org.apache.wicket.IRequestTarget;
import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.WicketAjaxReference;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WicketEventReference;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebResponse;
import com.l7tech.gateway.common.security.rbac.AttemptedOperation;
import com.l7tech.server.ems.ui.SecureComponent;

/**
 * JSON interaction is a component that provides a JavaScript URL for JSON data access.
 */
public class JsonInteraction extends Panel implements SecureComponent {

    //- PUBLIC

    /**
     * Create an unsecured JSON interaction.
     *
     * @param id The wicket component identifier
     * @param jsonUrlVariable The URL variable to create in the containing page
     * @param provider The data provider for the interaction
     */
    public JsonInteraction( final String id,
                            final String jsonUrlVariable,
                            final JsonDataProvider provider ) {
        this( id, jsonUrlVariable, provider, null );
    }

    /**
     * Create a JSON interaction.
     *
     * @param id The wicket component identifier
     * @param jsonUrlVariable The URL variable to create in the containing page
     * @param provider The data provider for the interaction
     * @param attemptedOperation The attempted operation to check or null for no security.
     */
    public JsonInteraction( final String id,
                            final String jsonUrlVariable,
                            final JsonDataProvider provider,
                            final AttemptedOperation attemptedOperation ) {
        super(id);
        this.provider = provider;
        this.attemptedOperation = attemptedOperation;

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
                            response.setContentType("application/json; charset=UTF-8");
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

    @Override
    public final AttemptedOperation getAttemptedOperation() {
        return attemptedOperation;
    }

    //- PROTECTED

    @SuppressWarnings({"UnusedDeclaration"})
    protected void onRequest( final WebRequest request, final WebResponse response ) {
        writeJsonResponse( response, provider.getData() );
    }

    protected void writeJsonResponse( final WebResponse response, final Object data ) {
        // Add JSON script to the response
        StringBuffer dataBuffer = new StringBuffer(2048);
        JsonUtil.writeJson( data, dataBuffer );
        response.write(dataBuffer);        
    }

    //- PRIVATE

    private final JsonDataProvider provider;
    private final AttemptedOperation attemptedOperation;
}

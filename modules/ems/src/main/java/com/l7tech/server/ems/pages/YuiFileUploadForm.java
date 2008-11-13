package com.l7tech.server.ems.pages;

import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.WicketAjaxReference;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.protocol.http.WebRequest;

/**
 * Form for AJAX style file uploads.
 *
 * @author steve
 */
public class YuiFileUploadForm extends Form {

    //- PUBLIC

    public YuiFileUploadForm( final String id, final IModel model ) {
        super( id, model );
        init();
    }

    public YuiFileUploadForm( final String id ) {
        super(id);
        init();
    }

    @Override
    public void renderHead( final HtmlHeaderContainer htmlHeaderContainer ) {
        super.renderHead(htmlHeaderContainer);
        String actionJs =
            "  var onUploadButtonClick = function(e){\n" +
            "   " + getSubmitJavascript() +
            "   YAHOO.util.Event.stopEvent(e); };\n" +
            "  YAHOO.util.Event.on('"+getMarkupId()+"', 'submit', onUploadButtonClick);";

        htmlHeaderContainer.getHeaderResponse().renderOnDomReadyJavascript(actionJs);
    }

    public String getSubmitJavascript() {
        return
            " YAHOO.util.Connect.setForm('"+getMarkupId()+"', true);\n" +
            " var uploadHandler = { upload: function(o) {  new Wicket.Ajax.Call().loadedCallback(o.responseXML); } };\n" +
            " YAHOO.util.Connect.asyncRequest('POST', document.getElementById('"+getMarkupId()+"').action + '&yui=true', uploadHandler);\n";        
    }

    //- PROTECTED

    @Override
    protected final void onError() {
        if ( isAjax() ) {
            onError( retarget() );
        } else {
            onError( null );
        }
    }

    /**
     * Handle form submission.
     *
     * @param target The AjaxRequestTarget or null if not an AJAX submission
     */
    @SuppressWarnings({"UnusedDeclaration"})
    protected void onError( final AjaxRequestTarget target ) {
    }

    @Override
    protected final void onSubmit() {
        if ( isAjax() ) {
            onSubmit( retarget() );
        } else {
            onSubmit( null );
        }
    }

    /**
     * Handle form submission.
     *
     * @param target The AjaxRequestTarget or null if not an AJAX submission
     */
    @SuppressWarnings({"UnusedDeclaration"})
    protected void onSubmit( final AjaxRequestTarget target ) {
    }

    //- PRIVATE

    private void init() {
        setOutputMarkupId( true );
        setMultiPart( true );
        add( HeaderContributor.forJavaScript( WicketAjaxReference.INSTANCE ) );
        add( HeaderContributor.forJavaScript( YuiCommon.RES_JS_DOM_EVENT ) );
        add( HeaderContributor.forJavaScript( YuiCommon.RES_JS_CONNECTION ) );        
    }

    private boolean isAjax() {
        return Boolean.valueOf(((WebRequest)RequestCycle.get().getRequest()).getHttpServletRequest().getParameter("yui"));
    }

    private AjaxRequestTarget retarget() {
        AjaxRequestTarget target = new AjaxRequestTarget(this.getPage());
        RequestCycle.get().setRequestTarget(target);
        return target;
    }
}
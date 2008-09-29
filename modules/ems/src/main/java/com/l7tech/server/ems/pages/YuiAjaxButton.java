package com.l7tech.server.ems.pages;

import org.apache.wicket.Component;
import org.apache.wicket.IComponentBorder;
import org.apache.wicket.Response;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.markup.html.form.Form;

/**
 * Extension of AjaxButton that has YUI look and feel.
 * 
 * <p>There is no functional difference between this button and
 * AjaxButton.</p>
 * 
 * @author steve
 */
public abstract class YuiAjaxButton extends AjaxButton {
    
    //- PUBLIC
    
    public YuiAjaxButton( final String id ) {
        super(id);        
        init();
    }
    
    public YuiAjaxButton( final String id, final Form form ) {
        super(id, form);        
        init();
    }
    
    //- PRIVATE
    
    /**
     * Initialize this component.
     */
    private void init() {
        // Add CSS / JS header contributions
        add( HeaderContributor.forCss( YuiCommon.RES_CSS_SAM_BUTTON ) );
        add( HeaderContributor.forJavaScript( YuiCommon.RES_JS_DOM_EVENT ) );
        add( HeaderContributor.forJavaScript( YuiCommon.RES_JS_ELEMENT ) );
        add( HeaderContributor.forJavaScript( YuiCommon.RES_JS_BUTTON ) );

        // ID is required to locate the button using JS
        setOutputMarkupId(true);
        setComponentBorder(new YuiButtonScriptComponentBorder());        
    }
    
    /**
     * IComponentBorder that adds a script to initialize the YUI Button and
     * to copy the original buttons onclick javascript handler to the new
     * YUI buttons onclick function.
     */
    private static final class YuiButtonScriptComponentBorder implements IComponentBorder {
        public void renderBefore( final Component component ) {}
        public void renderAfter( final Component component ) {
            final Response response = component.getResponse();
            final StringBuilder builder = new StringBuilder();
            final String markupId = component.getMarkupId();
            
            builder.append( "<script>var onButtonClick = document.getElementById('");
            builder.append( markupId );
            builder.append( "').onclick; new YAHOO.widget.Button(\"" );
            builder.append( markupId );
            builder.append( "\", { onclick: { fn: onButtonClick } });</script>" );

            response.write( builder.toString() );
        }        
    }
}

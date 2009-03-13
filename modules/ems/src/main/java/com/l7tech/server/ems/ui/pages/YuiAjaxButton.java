package com.l7tech.server.ems.ui.pages;

import org.apache.wicket.Component;
import org.apache.wicket.IComponentBorder;
import org.apache.wicket.Response;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxCallDecorator;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.markup.html.form.Form;
import com.l7tech.gateway.common.security.rbac.AttemptedOperation;
import com.l7tech.server.ems.ui.SecureComponent;

/**
 * Extension of AjaxButton that has YUI look and feel.
 * 
 * <p>There is no functional difference between this button and
 * AjaxButton.</p>
 * 
 * @author steve
 */
public abstract class YuiAjaxButton extends AjaxButton implements SecureComponent {
    
    //- PUBLIC
    
    public YuiAjaxButton( final String id ) {
        super(id);        
        init();
    }
    
    public YuiAjaxButton( final String id, final Form form ) {
        super(id, form);        
        init();
    }
    
    @Override
    public final AttemptedOperation getAttemptedOperation() {
        return attemptedOperation;
    }

    public YuiAjaxButton add( final AttemptedOperation attemptedOperation ) {
        this.attemptedOperation = attemptedOperation;
        return this;
    }

    //- PROTECTED

    @Override
    protected IAjaxCallDecorator getAjaxCallDecorator() {
        return new AjaxCallDecorator(){
            @Override
            public CharSequence decorateOnFailureScript(final CharSequence script) {
                return "l7.Dialog.showErrorDialog(null,'Enterprise Service Manager server is not available.',null); " + script;
            }
        };
    }

    //- PRIVATE
    
    private AttemptedOperation attemptedOperation;

    /**
     * Initialize this component.
     */
    private void init() {
        // Add CSS / JS header contributions
        add( HeaderContributor.forCss( YuiCommon.RES_CSS_SAM_BUTTON ) );
        add( HeaderContributor.forCss( YuiCommon.RES_CSS_SAM_CONTAINER ) );        
        add( HeaderContributor.forJavaScript( YuiCommon.RES_JS_DOM_EVENT ) );
        add( HeaderContributor.forJavaScript( YuiCommon.RES_JS_ELEMENT ) );
        add( HeaderContributor.forJavaScript( YuiCommon.RES_JS_BUTTON ) );
        add( HeaderContributor.forJavaScript( YuiCommon.RES_JS_CONTAINER ) );

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
        @Override
        public void renderBefore( final Component component ) {}
        @Override
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

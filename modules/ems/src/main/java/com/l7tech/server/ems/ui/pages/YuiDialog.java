package com.l7tech.server.ems.ui.pages;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.JavascriptPackageResource;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;

/**
 * YUI dialog component.
 *
 * @author steve
 */
public class YuiDialog extends Panel {

    //- PUBLIC

    /**
     * Buttons for use as dialog options.
     */
    public static enum Button {
        OK(true), CANCEL(false), YES(true), NO(false), CLOSE(false);

        Button( final boolean formSubmit ) {
            this.formSubmit = formSubmit;
        }   
        
        boolean isFormSubmit() { return formSubmit; }
        String getComponentId() { return this.toString().toLowerCase() + "Button"; }
        
        private final boolean formSubmit;
    }
    
    /**
     * Dialog styles for use in common cases.
     */
    public static enum Style {
        OK_CANCEL(Button.OK, Button.OK, Button.CANCEL), 
        YES_NO(Button.YES, Button.YES, Button.NO), 
        YES_NO_CANCEL(Button.YES, Button.YES, Button.NO, Button.CANCEL), 
        CLOSE(Button.CLOSE, Button.CLOSE);  
        
        Style( final Button defaultButton, final Button... buttons ) {
            this.defaultButton = defaultButton;
            this.buttons = buttons;
        }
        
        public Button getDefaultButton(){ return defaultButton; }
        public Button[] getButtons(){ return buttons; }
        
        private final Button defaultButton;
        private final Button[] buttons;
    }
    
    /**
     * Create a dialog with the given style.
     *
     * <p>You'll need to set the content of the dialog before use.</p>
     */
    public YuiDialog( final String id,
                      final String title,
                      final Style style,
                      final OkCancelCallback callback ) {
        this( id, title, style, null, callback );
    }
    
    /**
     * Create a dialog with the given style and content.
     */
    public YuiDialog( final String id,
                      final String title,
                      final Style style,
                      final Component content,
                      final OkCancelCallback callback ) {
        this( id, title, content, callback, style.getDefaultButton(), style.getButtons() );    
    }

    /**
     * Create a dialog with the given style and content.
     */
    public YuiDialog( final String id,
                      final String title,
                      final Style style,
                      final Component content,
                      final OkCancelCallback callback,
                      final String width ) {
        this( id, title, content, callback, style.getDefaultButton(), style.getButtons(), width );
    }

    /**
     * Create a dialog with the given style and content.
     */
    public YuiDialog( final String id,
                      final String title,
                      final Style style,
                      final Component content ) {
        this( id, title, content, null, style.getDefaultButton(), style.getButtons() );
    }

    /**
     * Create a dialog with the given buttons and content.
     */
    public YuiDialog( final String id,
                      final String title,
                      final Component content,
                      final OkCancelCallback callback,
                      final Button defaultButton,
                      final Button[] buttons ) {
        this( id, title, content, callback, defaultButton, buttons, "40em" );
    }

    /**
     * Create a dialog with the given buttons and content.
     */
    public YuiDialog( final String id,
                      final String title,
                      final Component content,
                      final OkCancelCallback callback,
                      final Button defaultButton,
                      final Button[] buttons,
                      final String width ) {
        super( id );
        this.callback = callback;

        add( CSSPackageResource.getHeaderContribution( YuiCommon.RES_CSS_SAM_BUTTON ) );
        add( CSSPackageResource.getHeaderContribution( YuiCommon.RES_CSS_SAM_CONTAINER ) );

        add( JavascriptPackageResource.getHeaderContribution( YuiCommon.RES_JS_DOM_EVENT ) );
        add( JavascriptPackageResource.getHeaderContribution( YuiCommon.RES_JS_ELEMENT ) );
        add( JavascriptPackageResource.getHeaderContribution( YuiCommon.RES_JS_BUTTON ) );
        add( JavascriptPackageResource.getHeaderContribution( YuiCommon.RES_JS_DRAGDROP ) );
        add( JavascriptPackageResource.getHeaderContribution( YuiCommon.RES_JS_CONTAINER ) );

        add( JavascriptPackageResource.getHeaderContribution( "js/dialog.js" ) );

        final Component contentComponent = content!=null ? content : new WebMarkupContainer("content");
        contentComponent.setOutputMarkupId(true);
        final FeedbackPanel feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        
        WebMarkupContainer dialog = new WebMarkupContainer("dialog");
        dialog.setOutputMarkupId(true);
        dialog.add( new Label("title", title) );
        dialog.add( feedback );
        dialog.add( contentComponent );

        Form defaultForm = new Form("dialogDefaultForm");
        Form targetForm = null;
        if ( content instanceof MarkupContainer ) {
            MarkupContainer container = (MarkupContainer) content;
            targetForm = (Form) container.visitChildren(Form.class, new Component.IVisitor<Form>(){
                @Override
                public Object component(Form component) { return component; }
            });
        }
        boolean isFileUpload = false;
        if ( targetForm == null ) {
            targetForm = defaultForm;
        } else if ( targetForm instanceof YuiFileUploadForm ) {
            isFileUpload = true;
        }
        org.apache.wicket.markup.html.form.Button[] ajaxButtons = buildButtons( targetForm, feedback, isFileUpload );

        String initScript = buildInitJavascript(dialog.getMarkupId(), ajaxButtons, defaultButton, buttons, width);
        Component script = new Label("javascript", initScript).setEscapeModelStrings(false);

        for ( Component ajaxButton : ajaxButtons ) {
            defaultForm.add( ajaxButton );
        }

        add( dialog );
        add( defaultForm );
        add( script );
    }

    /**
     * Get the id to use for the content component.
     *
     * @return The identifier
     */
    public static String getContentId() {
        return "content";
    }

    /**
     * Set the content for the dialog.
     *
     * @param component The content to display
     */
    public void setContent( final Component component ) {
        ((WebMarkupContainer)get("dialog")).replace(component);
    }

    public String getSuccessScript() {
        return successScript;
    }

    /**
     * Create a listener for dialog actions.
     */
    public interface OkCancelCallback extends Serializable {
        void onAction( YuiDialog dialog, AjaxRequestTarget target, Button button );
    }

    //- PRIVATE
    
    private final OkCancelCallback callback;
    private String successScript;

    /**
     * Create the wicket buttons, note that this creates all buttons even though some will not be used. 
     */
    private org.apache.wicket.markup.html.form.Button[] buildButtons( final Form targetForm, final Component feedback, final boolean isFileUpload ) {
        Collection<org.apache.wicket.markup.html.form.Button> ajaxButtons = new ArrayList<org.apache.wicket.markup.html.form.Button>();
        
        for ( Button button : Button.values() ) {
            final Button result = button;
            final String buttonId = button.getComponentId();
            org.apache.wicket.markup.html.form.Button ajaxButton;
            if ( button.isFormSubmit() ) {
                if ( isFileUpload ) {
                    ajaxButton = new org.apache.wicket.markup.html.form.Button(buttonId){
                        @Override
                        protected String getOnClickScript() {
                            return ((YuiFileUploadForm)targetForm).getSubmitJavascript();
                        }
                    };
                } else {
                    ajaxButton = new AjaxButton(buttonId, targetForm){
                        @Override
                        protected void onSubmit(AjaxRequestTarget target, Form form) {
                            if ( callback != null ) callback.onAction(YuiDialog.this, target, result);
                            String script = "{var button = document.getElementById('"+getMarkupId()+"'); if (button.wicketSuccessCallback) button.wicketSuccessCallback();}";
                            target.prependJavascript(script);
                        }
                        @Override
                        protected void onError(AjaxRequestTarget target, Form form) {
                            target.addComponent(feedback);
                        }
                    };
                }
                if ( successScript == null ) {
                    successScript = "{var button = document.getElementById('"+ajaxButton.getMarkupId()+"'); if (button.wicketSuccessCallback) button.wicketSuccessCallback();}";
                }
            } else {
                ajaxButton = new AjaxButton(buttonId, targetForm){
                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form form) {
                        if ( callback != null ) callback.onAction(YuiDialog.this, target, result);
                    }            
                };
            }

            ajaxButton.setOutputMarkupId(true);
            ajaxButton.setDefaultFormProcessing( button.isFormSubmit() );
            ajaxButtons.add(ajaxButton);
        }

        return ajaxButtons.toArray(new org.apache.wicket.markup.html.form.Button[ajaxButtons.size()]);
    }

    /**
     * Generate the component specific javascript. 
     */
    private String buildInitJavascript( final String dialogId, 
                                        final org.apache.wicket.markup.html.form.Button[] ajaxButtons,
                                        final Button defaultButton,
                                        final Button[] buttons,
                                        final String width ) {
        StringBuilder scriptBuilder = new StringBuilder();
        scriptBuilder.append("YAHOO.util.Event.onDOMReady( function(){ showDialog('");
        scriptBuilder.append( dialogId );
        scriptBuilder.append("', [");
        boolean first = true;
        for ( Button button : buttons ) {
            if (first) first = false;
            else scriptBuilder.append( ", " );
            
            scriptBuilder.append( " {id:\"" );
            scriptBuilder.append( getButtonMarkupId(ajaxButtons, button) );
            scriptBuilder.append( "\",callback:" );
            scriptBuilder.append( button.isFormSubmit() );
            scriptBuilder.append( ",isDefault:" );
            scriptBuilder.append( button == defaultButton );
            scriptBuilder.append( "}" );
        }
        scriptBuilder.append("], '");
        scriptBuilder.append(width);
        scriptBuilder.append("' ); });");
        
        return scriptBuilder.toString();
    }
    
    private String getButtonMarkupId( final org.apache.wicket.markup.html.form.Button[] ajaxButtons,
                                      final Button button ) {
        String id = "";
        
        for ( Component ajaxButton : ajaxButtons ) {
            if ( ajaxButton.getId().equals(button.getComponentId()) ) {
                id = ajaxButton.getMarkupId();
                break;
            }
        }
        
        return id;
    }
}

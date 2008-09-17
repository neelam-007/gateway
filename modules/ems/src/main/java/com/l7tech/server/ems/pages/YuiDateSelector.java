package com.l7tech.server.ems.pages;

import org.apache.wicket.extensions.markup.html.form.DateTextField;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.ResourceReference;

/**
 * Panel with a text input field with an associated pop-up YUI calendar.
 *
 * @author steve
 */
public class YuiDateSelector extends Panel {

    //- PUBLIC

    /**
     * Create a DateSelector with the given model.
     * 
     * @param id The component identifier.
     * @param model The {@link java.util.Date Date} model.
     */
    public YuiDateSelector( final String id, final Model model ) {
        super( id, model );

        add( HeaderContributor.forCss( YuiCommon.RES_CSS_SAM_CONTAINER ) );
        add( HeaderContributor.forCss( YuiCommon.RES_CSS_SAM_BUTTON ) );
        add( HeaderContributor.forCss( YuiCommon.RES_CSS_SAM_CALENDAR ) );

        add( HeaderContributor.forJavaScript( YuiCommon.RES_JS_DOM_EVENT ) );
        add( HeaderContributor.forJavaScript( YuiCommon.RES_JS_DRAGDROP ) );
        add( HeaderContributor.forJavaScript( YuiCommon.RES_JS_ELEMENT ) );
        add( HeaderContributor.forJavaScript( YuiCommon.RES_JS_BUTTON ) );
        add( HeaderContributor.forJavaScript( YuiCommon.RES_JS_CONTAINER ) );
        add( HeaderContributor.forJavaScript( YuiCommon.RES_JS_CALENDAR ) );

        add( HeaderContributor.forJavaScript( new ResourceReference( YuiDataTable.class, "../resources/js/dateSelector.js" ) ) );

        DateTextField textField = new DateTextField("date", model);
        textField.setRequired(true);
        Button showButton = new Button("show");
        WebMarkupContainer calendarDiv = new WebMarkupContainer("calendar");
        WebMarkupContainer calendarBody = new WebMarkupContainer("calendar-body");        
                
        textField.setOutputMarkupId(true);
        showButton.setOutputMarkupId(true);
        calendarDiv.setOutputMarkupId(true);
        calendarBody.setOutputMarkupId(true);
                
        add( textField );
        add( showButton );
        add( calendarDiv );
        calendarDiv.add( calendarBody );
        
        StringBuilder scriptBuilder = new StringBuilder();
        scriptBuilder.append("YAHOO.util.Event.onDOMReady( function(){ initDateSelector('");
        scriptBuilder.append( textField.getMarkupId() );
        scriptBuilder.append("', '");
        scriptBuilder.append( showButton.getMarkupId() );
        scriptBuilder.append("', '");
        scriptBuilder.append( calendarDiv.getMarkupId() );
        scriptBuilder.append("', '");
        scriptBuilder.append( calendarBody.getMarkupId() );
        scriptBuilder.append("'); } );");
        
        Label label = new Label("javascript", scriptBuilder.toString());
        label.setEscapeModelStrings(false);
        
        add( label );

        this.dateTextField = textField;
    }

    /**
     * Access the underlying date text field to allow configuration of validation options.
     *
     * @return The date text field.
     */
    public DateTextField getDateTextField() {
        return dateTextField;
    }

    //- PRIVATE

    private final DateTextField dateTextField;
}

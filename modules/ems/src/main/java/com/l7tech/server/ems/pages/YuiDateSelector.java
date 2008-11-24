package com.l7tech.server.ems.pages;

import org.apache.wicket.extensions.markup.html.form.DateTextField;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.util.convert.IConverter;
import com.l7tech.server.ems.EmsSession;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.TimeZone;
import java.util.Locale;
import java.util.Date;

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
     * @param maxDate Maximumn permitted date (may be null)
     */
    public YuiDateSelector( final String id, final Model model, final Date maxDate ) {
        this( id, model, maxDate, false ) ;
    }

    /**
     * Create a DateSelector with the given model.
     * 
     * @param id The component identifier.
     * @param model The {@link java.util.Date Date} model.
     * @param maxDate Maximumn permitted date (may be null)
     * @param ignoreFirstSelect True to not pop-up on first select (useful if component is the form focus)
     */
    public YuiDateSelector( final String id, final Model model, final Date maxDate, final boolean ignoreFirstSelect ) {
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

        EmsSession session = ((EmsSession) RequestCycle.get().getSession());
        final String timeZoneId = session.getTimeZoneId();

        DateTextField textField = new DateTextField("date", model) {
            @Override
            public IConverter getConverter(final Class aClass) {
                return new IConverter() {
                    public SimpleDateFormat getDateFormat() {
                        SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
                        if ( timeZoneId != null ) {
                            format.setTimeZone( TimeZone.getTimeZone( timeZoneId ) );
                        }
                        format.setLenient( false );
                        return format;
                    }

                    @Override
                    public Object convertToObject(String s, Locale locale) {
                        Date value = new Date(0);
                        try {
                            value = getDateFormat().parse(s);
                        } catch ( ParseException pe ) {
                            // not of interest
                        }
                        return value;
                    }

                    @Override
                    public String convertToString(Object o, Locale locale) {
                        return getDateFormat().format((Date)o);
                    }
                };
            }
        };
        textField.setRequired(true);
        WebMarkupContainer calendarDiv = new WebMarkupContainer("calendar");
        WebMarkupContainer calendarBody = new WebMarkupContainer("calendar-body");        
                
        textField.setOutputMarkupId(true);
        calendarDiv.setOutputMarkupId(true);
        calendarBody.setOutputMarkupId(true);
                
        add( textField );
        add( calendarDiv );
        calendarDiv.add( calendarBody );
        
        StringBuilder scriptBuilder = new StringBuilder();
        scriptBuilder.append("YAHOO.util.Event.onDOMReady( function(){ initDateSelector('");
        scriptBuilder.append( textField.getMarkupId() );
        scriptBuilder.append("', '");
        scriptBuilder.append( calendarDiv.getMarkupId() );
        scriptBuilder.append("', '");
        scriptBuilder.append( calendarBody.getMarkupId() );
        scriptBuilder.append("'");
        if ( maxDate != null ) {
            scriptBuilder.append(", '");
            scriptBuilder.append( YuiCommon.toYuiDate(maxDate, timeZoneId) );
            scriptBuilder.append("'");
        } else {
            scriptBuilder.append(", null");
        }
        scriptBuilder.append(", ");
        scriptBuilder.append( ignoreFirstSelect );                
        scriptBuilder.append("); });");
        
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

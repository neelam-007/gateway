package com.l7tech.server.ems.pages;

import org.apache.wicket.extensions.markup.html.form.DateTextField;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.util.convert.IConverter;
import com.l7tech.server.ems.EmsSession;
import com.l7tech.server.ems.EmsApplication;

import java.text.ParseException;
import java.text.DateFormat;
import java.util.Locale;
import java.util.Date;
import java.util.TimeZone;
import java.io.Serializable;

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
        this(id, null, model, null, maxDate, false) ;
    }

    /**
     * Create a DateSelector with the given model.
     *
     * @param id The component identifier.
     * @param model The {@link java.util.Date Date} model.
     * @param maxDate Maximumn permitted date (may be null)
     * @param ignoreFirstSelect True to not pop-up on first select (useful if component is the form focus)
     */
    public YuiDateSelector( final String id, final Model model, final Date maxDate, final boolean ignoreFirstSelect) {
        this(id, null, model, null, maxDate, ignoreFirstSelect);
    }

    /**
     * Create a DateSelector with the given model.
     *
     * @param id The component identifier.
     * @param dateFieldMarkupId The html markup id of the date text field (may be null)
     * @param model The {@link java.util.Date Date} model.
     * @param minDate Minimum permitted date (may be null)
     * @param maxDate Maximumn permitted date (may be null)
     */
    public YuiDateSelector( final String id, final String dateFieldMarkupId, final Model model, final Date minDate, final Date maxDate, final String newTimeZoneUsed) {
        this(id, dateFieldMarkupId, model, minDate, maxDate, false, newTimeZoneUsed);
    }

    /**
     * Create a DateSelector with the given model.
     *
     * @param id The component identifier.
     * @param dateFieldMarkupId The html markup id of the date text field (may be null)
     * @param model The {@link java.util.Date Date} model.
     * @param minDate Minimum permitted date (may be null)
     * @param maxDate Maximumn permitted date (may be null)
     * @param ignoreFirstSelect True to not pop-up on first select (useful if component is the form focus)
     */
    public YuiDateSelector(final String id, final String dateFieldMarkupId, final Model model, final Date minDate, final Date maxDate, final boolean ignoreFirstSelect) {
        this(id, dateFieldMarkupId, model, minDate, maxDate, ignoreFirstSelect, null);
    }

    /**
     * Create a DateSelector with the given model.
     *
     * @param id The component identifier.
     * @param dateFieldMarkupId The html markup id of the date text field (may be null)
     * @param model The {@link java.util.Date Date} model.
     * @param minDate Minimum permitted date (may be null)
     * @param maxDate Maximumn permitted date (may be null)
     * @param ignoreFirstSelect True to not pop-up on first select (useful if component is the form focus)
     * @param newTimeZoneUsed The new time zone used by the date selector.
     */
    public YuiDateSelector(final String id, final String dateFieldMarkupId, final Model model, final Date minDate, final Date maxDate, final boolean ignoreFirstSelect, final String newTimeZoneUsed) {
        super(id, model);
        this.ignoreFirstSelect = ignoreFirstSelect;
        this.newTimeZoneUsed = newTimeZoneUsed;

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

        dateTextField = new DateTextField("date", model) {
            @Override
            public IConverter getConverter(final Class aClass) {
                return new IConverter() {
                    public DateFormat getDateFormat() {
                        EmsSession session = ((EmsSession) RequestCycle.get().getSession());
                        DateFormat format = session.buildDateFormat( false );
                        if (YuiDateSelector.this.newTimeZoneUsed != null) {
                            format.setTimeZone(TimeZone.getTimeZone(YuiDateSelector.this.newTimeZoneUsed));
                        }
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
        if (dateFieldMarkupId != null && !dateFieldMarkupId.isEmpty()) {
            dateTextField.setMarkupId(dateFieldMarkupId);
        }
        dateTextField.setRequired(true);
        calendarDiv = new WebMarkupContainer("calendar");
        calendarBody = new WebMarkupContainer("calendar-body");

        dateTextField.setOutputMarkupId(true);
        calendarDiv.setOutputMarkupId(true);
        calendarBody.setOutputMarkupId(true);

        add( dateTextField );
        add( calendarDiv );
        calendarDiv.add( calendarBody );

        javascriptLabel = new Label("javascript", new PropertyModel(new DateSelectorModel(minDate, maxDate), "script"));
        javascriptLabel.setEscapeModelStrings(false);

        add( javascriptLabel.setOutputMarkupId(true) );
    }

    /**
     * Access the underlying date text field to allow configuration of validation options.
     *
     * @return The date text field.
     */
    public DateTextField getDateTextField() {
        return dateTextField;
    }

    public Label getJavascriptLabel() {
        return javascriptLabel;
    }

    public void setNewTimeZoneUsed(String newTimeZoneUsed) {
        this.newTimeZoneUsed = newTimeZoneUsed;
    }

    public void setDateSelectorModel(Date minDate, Date maxDate) {
        javascriptLabel.setModel(new PropertyModel(new DateSelectorModel(minDate, maxDate), "script"));
    }

    //- PRIVATE

    private final DateTextField dateTextField;
    private final Label javascriptLabel;
    private WebMarkupContainer calendarDiv;
    private WebMarkupContainer calendarBody;
    private boolean ignoreFirstSelect;
    private String newTimeZoneUsed;

    /**
     * A model to store a javascript of the date selector settings based on min date and max date.
     */
    private class DateSelectorModel implements Serializable {
        private String script;
        private Date minDate;
        private Date maxDate;

        DateSelectorModel(Date minDate, Date maxDate) {
            this.minDate = minDate;
            this.maxDate = maxDate;
            script = buildDateSelectorJavascript(minDate, maxDate);
        }

        public Date getMaxDate() {
            return maxDate;
        }

        public void setMaxDate(Date maxDate) {
            this.maxDate = maxDate;
            script = buildDateSelectorJavascript(minDate, maxDate);
        }

        public Date getMinDate() {
            return minDate;
        }

        public void setMinDate(Date minDate) {
            this.minDate = minDate;
            script = buildDateSelectorJavascript(minDate, maxDate);
        }

        public String getScript() {
            return script;
        }

        public void setScript(String script) {
            this.script = script;
        }
    }

    /**
     * Create a javascript with the date-selector setting
     *
     * @param minDate: the min date in the date selector.
     * @param maxDate: the max date in the date selector.
     * @return
     */
    private String buildDateSelectorJavascript(Date minDate, Date maxDate) {
        EmsSession session = ((EmsSession) RequestCycle.get().getSession());
        String timeZoneId = newTimeZoneUsed == null? session.getTimeZoneId() : newTimeZoneUsed;

        StringBuilder scriptBuilder = new StringBuilder();
        scriptBuilder.append("YAHOO.util.Event.onDOMReady( function(){ initDateSelector('");
        scriptBuilder.append( dateTextField.getMarkupId() );
        scriptBuilder.append("', '");
        scriptBuilder.append( calendarDiv.getMarkupId() );
        scriptBuilder.append("', '");
        scriptBuilder.append( calendarBody.getMarkupId() );
        scriptBuilder.append("'");

        // date format (true for long format)
        scriptBuilder.append(", ");
        scriptBuilder.append( !EmsApplication.DEFAULT_DATE_FORMAT.equals(session.getDateFormatPattern()) );

        // date selected
        scriptBuilder.append(", '");
        scriptBuilder.append( YuiCommon.toYuiDate(((Date)dateTextField.getModelObject()), timeZoneId) );
        scriptBuilder.append("'");

        // min date if any
        if ( minDate != null ) {
            scriptBuilder.append(", '");
            scriptBuilder.append( YuiCommon.toYuiDate(minDate, timeZoneId) );
            scriptBuilder.append("'");
        } else {
            scriptBuilder.append(", null");
        }

        // max date if any
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

        return scriptBuilder.toString();
    }
}

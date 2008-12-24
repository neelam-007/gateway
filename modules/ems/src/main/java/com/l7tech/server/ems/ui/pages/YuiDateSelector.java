package com.l7tech.server.ems.ui.pages;

import org.apache.wicket.extensions.markup.html.form.DateTextField;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.util.convert.IConverter;
import com.l7tech.server.ems.ui.EsmSession;
import com.l7tech.server.ems.ui.EsmApplication;

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
     * @param minDate Minimum permitted date (may be null)
     * @param maxDate Maximumn permitted date (may be null)
     */
    public YuiDateSelector( final String id, final Model model, final Date minDate, final Date maxDate ) {
        this(id, null, model, minDate, maxDate, false, null) ;
    }

    /**
     * Create a DateSelector with the given model.
     *
     * @param id The component identifier.
     * @param dateFieldMarkupId The html markup id of the date text field (may be null)
     * @param model The {@link java.util.Date Date} model.
     * @param minDate Minimum permitted date (may be null)
     * @param maxDate Maximumn permitted date (may be null)
     * @param newTimeZoneUsed The new time zone used by the date selector.
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

        add( HeaderContributor.forJavaScript( "js/dateSelector.js" ) );

        dateTextField = new DateTextField("date", model) {
            @Override
            public IConverter getConverter(final Class aClass) {
                return new IConverter() {
                    public DateFormat getDateFormat() {
                        EsmSession session = ((EsmSession) RequestCycle.get().getSession());
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

    /**
     * Add interaction between this date selector and other date selector.  When a selected date is changed in this date
     * selector, the other date selector will be updated as well.
     *
     * @param otherDateSelector The other date selector, which this date selector interacts with.
     * @param isFromDateSelector A flag indicating if the other date selector is a calendar to choose a "from" date rather than a "to" date.
     * @param tasker The tasker to update the other date selector.
     */
    public void addInteractionWithOtherDateSelector(final YuiDateSelector otherDateSelector, final boolean isFromDateSelector, final InteractionTasker tasker) {
        otherDateSelector.setOutputMarkupId(true);

        dateTextField.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            @Override
            protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
                tasker.doBasicTask(YuiDateSelector.this, otherDateSelector, isFromDateSelector, ajaxRequestTarget);
                tasker.doExtraTask(ajaxRequestTarget);
            }
        });
    }

    /**
     * This class defines how many tasks should be done for two date selectors interaction.
     */
    public static class InteractionTasker implements Serializable {

        /**
         * Update the other date selector when the current date selector has a date change.
         *
         * @param currentDateSelector The current date selector, which has a date change.
         * @param otherDateSelector The other date selector, which will be updated.
         * @param isFromDateSelector A flag indicating if the other date selector is a calendar to choose a "from" date rather than a "to" date.
         * @param ajaxRequestTarget
         */
        private void doBasicTask(YuiDateSelector currentDateSelector, YuiDateSelector otherDateSelector, boolean isFromDateSelector, AjaxRequestTarget ajaxRequestTarget) {
            if (isFromDateSelector) {
                otherDateSelector.setDateSelectorModel(null, (Date) currentDateSelector.getModelObject());
            } else {
                otherDateSelector.setDateSelectorModel((Date) currentDateSelector.getModelObject(), new Date());
            }
            ajaxRequestTarget.addComponent(otherDateSelector);
        }

        /**
         * Perform other task besides the basic task when the interaction occurs.
         * Note: normally this mehtod is overriden/redefined in the subclass. 
         *
         * @param ajaxRequestTarget
         */
        protected void doExtraTask(AjaxRequestTarget ajaxRequestTarget) {
        }
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
        EsmSession session = ((EsmSession) RequestCycle.get().getSession());
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
        scriptBuilder.append( !EsmApplication.DEFAULT_DATE_FORMAT.equals(session.getDateFormatPattern()) );

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

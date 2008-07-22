/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.util.jcalendar;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

/**
 * A GUI widget to choose a date-time range.
 *
 * <p>Property change to listen for are "timeZone", "startTime" and "endTime".
 *
 * <p>Run TimeRangePickerTest for a demo.
 *
 * @author rmak
 * @since SecureSpan 4.2
 */
public class TimeRangePicker extends JComponent implements PropertyChangeListener {

    /**
     * Combo box item for a time zone. Time zone IDs are used in the display
     * because they are more distinguishable then the Time zone display names.
     */
    private static class TimeZoneItem {
        private final TimeZone _timeZone;
        private final String _displayString;
        private final String _symbolicString;

        public TimeZoneItem(final TimeZone timeZone) {
            _timeZone = timeZone;
            _displayString = timeZone.getID();
            final StringBuilder sb = new StringBuilder(" (GMT");
            sb.append(formatTimeZoneOffset(timeZone.getRawOffset()));
            if (timeZone.useDaylightTime()) {
                sb.append("/");
                sb.append(formatTimeZoneOffset(timeZone.getRawOffset() + timeZone.getDSTSavings()));
            }
            sb.append(")");
            _symbolicString = sb.toString();
        }

        public TimeZone getTimeZone() {
            return _timeZone;
        }

        public String toString() {
            return _displayString;
        }

        public String getSymbolicString() {
            return _symbolicString;
        }

        public static String formatTimeZoneOffset(final int offset) {
            int tmp = Math.abs(offset);
            final int hours = tmp / 3600000;
            final int minutes = (tmp - hours * 3600000) / 60000;
            return String.format("%c%02d:%02d", (offset >= 0 ? '+' : '-'), hours, minutes);
        }
    }

    /** Time zone items for all available time zone IDs; sorted. */
    private static final Map<TimeZone, TimeZoneItem> TIME_ZONE_ITEMS;
    static {
        final String[] ids = TimeZone.getAvailableIDs();
        Arrays.sort(ids);
        TIME_ZONE_ITEMS = new LinkedHashMap<TimeZone, TimeZoneItem>(ids.length);
        for (String id : ids) {
            final TimeZone timeZone = TimeZone.getTimeZone(id);
            TIME_ZONE_ITEMS.put(timeZone, new TimeZoneItem(timeZone));
        }
    }

    public JPanel mainPanel;
    private JDateTimeChooser _startChooser;
    private JDateTimeChooser _endChooser;
    private JComboBox _timeZoneComboBox;
    private JLabel _timeZoneLabel;

    private Date _startDate;
    private Date _endDate;
    private TimeZone _timeZone;
    private Locale _locale;

    public TimeRangePicker() {
        final Date now = new Date();
        init(now, now, TimeZone.getDefault(), Locale.getDefault());
    }

    public TimeRangePicker(final Date startDate, final Date endDate) {
        init(startDate, endDate, TimeZone.getDefault(), Locale.getDefault());
    }

    public TimeRangePicker(final Date startDate, final Date endDate, final TimeZone timeZone) {
        init(startDate, endDate, timeZone, Locale.getDefault());
    }

    public TimeRangePicker(final Date startDate, final Date endDate, final TimeZone timeZone, final Locale locale) {
        init(startDate, endDate, timeZone, locale);
    }

    protected void init(final Date startDate, final Date endDate, final TimeZone timeZone, final Locale locale) {
        initComponents();
        setLocale(locale);
        setTimeZone(timeZone, false);
        setStartTime(startDate);
        setEndTime(endDate);
    }


    protected void initComponents() {
        _startChooser.getJCalendar().setDecorationBackgroundVisible(true);
        _startChooser.getJCalendar().setDecorationBordersVisible(false);
        _startChooser.getJCalendar().setWeekOfYearVisible(false);
        _startChooser.addPropertyChangeListener("date", this);

        _endChooser.getJCalendar().setDecorationBackgroundVisible(true);
        _endChooser.getJCalendar().setDecorationBordersVisible(false);
        _endChooser.getJCalendar().setWeekOfYearVisible(false);
        _endChooser.addPropertyChangeListener("date", this);

        _timeZoneComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final TimeZone timeZone = ((TimeZoneItem)_timeZoneComboBox.getSelectedItem()).getTimeZone();
                setTimeZone(timeZone, true);
            }
        });
    }

    public Date getStartTime() {
        return _startDate;
    }

    /**
     * Sets the end time. Also fires "startTime" property change event.
     *
     * @param date  the new start time
     */
    public void setStartTime(final Date date) {
        _startChooser.setDate(date);
        // _startChooser will fire "date" property change event; which causes
        // this.propertyChange() to assign _startDate and fires "startTime"
        // property change event.
    }

    public Date getEndTime() {
        return _endDate;
    }

    /**
     * Sets the end time. Also fires "endTime" property change event.
     *
     * @param date  the new end time
     */
    public void setEndTime(final Date date) {
        _endChooser.setDate(date);
        // _endChooser will fire "date" property change event; which causes
        // this.propertyChange() to assign _endDate and fires "endTime"
        // property change event.
    }

    public TimeZone getTimeZone() {
        return _timeZone;
    }

    /**
     * Sets the time zone while preserving the numeric year, month, day, hour,
     * minute, second, and millisecond. Optionally fires "timeZone" property change.
     *
     * @param timeZone the new time zone
     * @param firePropertyChange    whether to fire the "timeZone" property change event
     * @throws IllegalArgumentException if <code>timeZone</code> does not match any
     *                                  of the avaiable time zones listed in the combo
     *                                  box using the {@link TimeZone#equals} method.
     */
    public void setTimeZone(final TimeZone timeZone, final boolean firePropertyChange) {
        final TimeZone oldZone = _timeZone;
        final TimeZoneItem tzItem = TIME_ZONE_ITEMS.get(timeZone);
        if (tzItem == null) {
            throw new IllegalArgumentException("Not an available time zone.");
        }
        _timeZone = tzItem.getTimeZone();
        _startChooser.setTimeZone(_timeZone);
        _endChooser.setTimeZone(_timeZone);
        _timeZoneComboBox.setSelectedItem(tzItem);
        _timeZoneLabel.setText(tzItem.getSymbolicString());
        if (firePropertyChange) {
            firePropertyChange("timeZone", oldZone, _timeZone);
        }
    }

    public Locale getLocale() {
        return _locale;
    }

    public void setLocale(final Locale locale) {
        _locale = locale;
        _startChooser.setLocale(locale);
        _endChooser.setLocale(locale);
    }

    @Override
    public void setEnabled(boolean b) {
        _startChooser.setEnabled(b);
        _endChooser.setEnabled(b);
        _timeZoneComboBox.setEnabled(b);
        _timeZoneLabel.setEnabled(b);

    }

    // Implements PropertyChangeListener.
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() == _startChooser) {
            final Date oldDate = _startDate;
            _startDate = _startChooser.getDate();
            _endChooser.setMinSelectableDate(_startDate);
            firePropertyChange("startTime", oldDate, _startDate);
        } else if (evt.getSource() == _endChooser) {
            final Date oldDate = _endDate;
            _endDate = _endChooser.getDate();
            _startChooser.setMaxSelectableDate(_endDate);
            firePropertyChange("endTime", oldDate, _endDate);
        }
    }

    /**
     * Instantiates IDEA form components marked as "Custom Create".
     *
     * <p><b>Note:</b>
     * In IDEA 6, createUIComponents() is invoked before constuctor.
     * In IDEA 7, createUIComponents() is invoked after field assignments in constuctor.
     * To work both ways, this method must not depend on class members initialized
     * by class constructor(s).
     */
    private void createUIComponents() {
        _timeZoneComboBox = new JComboBox(TIME_ZONE_ITEMS.values().toArray());
    }
}

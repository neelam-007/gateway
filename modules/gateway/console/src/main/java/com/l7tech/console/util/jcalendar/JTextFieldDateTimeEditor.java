/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.util.jcalendar;

import com.toedter.calendar.JTextFieldDateEditor;

import javax.swing.event.CaretEvent;
import java.awt.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Extension of {@link JTextFieldDateEditor} with time fields in addition to date fields;
 * plus time zone setter/getter.
 *
 * @author rmak
 * @since SecureSpan 4.2
 */
public class JTextFieldDateTimeEditor extends JTextFieldDateEditor implements IDateEditorEx {

    private static final long serialVersionUID = 1001181351142803224L;

    protected TimeZone _timeZone = TimeZone.getDefault();

    // Replaces the use of private member in superclass.
    protected int _hours;

    // Replaces the use of private member in superclass.
    protected int _minutes;

    // Replaces the use of private member in superclass.
    protected int _seconds;

    // Replaces the use of private member in superclass.
    protected int _millis;

    // Replaces the use of private member in superclass.
    protected Calendar _calendar;

    // Replaces the use of private member in superclass.
    protected boolean _ignoreDatePatternChange;

    // Replaces the use of dateUtil in superclass.
    protected Date _minSelectableDate;

    // Replaces the use of dateUtil in superclass.
    protected Date _maxSelectableDate;

    public JTextFieldDateTimeEditor() {
        this(false, null, null, ' ');
    }

    public JTextFieldDateTimeEditor(boolean showMask, String datePattern, String maskPattern, char placeholder) {
        super(showMask, datePattern, maskPattern, placeholder);
        dateFormatter = (SimpleDateFormat) DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
        dateFormatter.setLenient(false);
        if (datePattern != null) {
            _ignoreDatePatternChange = true;
        }
        _calendar = Calendar.getInstance();
    }

    // Implements IDateEditorEx.
    public TimeZone getTimeZone() {
        return _timeZone;
    }

    // Implements IDateEditorEx.
    public void setTimeZone(TimeZone timeZone) {
        dateFormatter.setTimeZone(timeZone);
        _calendar.setTimeZone(timeZone);
        _timeZone = timeZone;
        if (date != null) {
            setDate(date, false);
        }
    }

    @Override
    public Date getDate() {
        try {
            _calendar.setTime(dateFormatter.parse(getText()));
            _calendar.set(Calendar.HOUR_OF_DAY, _hours);
            _calendar.set(Calendar.MINUTE, _minutes);
            _calendar.set(Calendar.SECOND, _seconds);
            _calendar.set(Calendar.MILLISECOND, _millis);
            date = _calendar.getTime();
        } catch (ParseException e) {
            date = null;
        }
        return date;
    }

    @Override
    protected void setDate(Date date, boolean firePropertyChange) {
        Date oldDate = this.date;
        this.date = date;

        if (date == null) {
            setText("");
        } else {
            _calendar.setTime(date);
            _hours = _calendar.get(Calendar.HOUR_OF_DAY);
            _minutes = _calendar.get(Calendar.MINUTE);
            _seconds = _calendar.get(Calendar.SECOND);
            _millis = _calendar.get(Calendar.MILLISECOND);

            setText(dateFormatter.format(date));
        }
        if (date != null && checkDate(date)) {
            setForeground(Color.BLACK);
        }

        if (firePropertyChange) {
            firePropertyChange("date", oldDate, date);
        }
    }

    @Override
    public void setDateFormatString(String dateFormatString) {
        if (_ignoreDatePatternChange) {
            return;
        }

        try {
            dateFormatter.applyPattern(dateFormatString);
        } catch (RuntimeException e) {
            dateFormatter = (SimpleDateFormat) DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
            dateFormatter.setLenient(false);
        }
        dateFormatter.setTimeZone(_timeZone);

        this.datePattern = dateFormatter.toPattern();
        setToolTipText(this.datePattern);
        setDate(date, false);
    }

    @Override
    public void setLocale(Locale locale) {
        if (locale == getLocale() || _ignoreDatePatternChange) {
            return;
        }

        super.setLocale(locale);
        dateFormatter = (SimpleDateFormat) DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale);
        dateFormatter.setTimeZone(_timeZone);
        setToolTipText(dateFormatter.toPattern());

        setDate(date, false);
        doLayout();
    }

    @Override
    public void caretUpdate(CaretEvent event) {
        String text = getText().trim();
        String emptyMask = maskPattern.replace('#', placeholder);

        if (text.length() == 0 || text.equals(emptyMask)) {
            setForeground(Color.BLACK);
            return;
        }

        try {
            Date date = dateFormatter.parse(getText());
            if (checkDate(date)) {
                setForeground(darkGreen);
            } else {
                setForeground(Color.RED);
            }
        } catch (Exception e) {
            setForeground(Color.RED);
        }
    }

    @Override
    public Date getMaxSelectableDate() {
        return _maxSelectableDate;
    }

    @Override
    public Date getMinSelectableDate() {
        return _minSelectableDate;
    }

    @Override
    public void setMaxSelectableDate(Date max) {
        _maxSelectableDate = max;
        try {
            Date date = dateFormatter.parse(getText());
            setDate(date, true);
        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    public void setMinSelectableDate(Date min) {
        _minSelectableDate = min;
        try {
            Date date = dateFormatter.parse(getText());
            setDate(date, true);
        } catch (Exception e) {
            // ignore
        }
    }

    protected boolean checkDate(Date date) {
        return (_minSelectableDate == null || _minSelectableDate.getTime() <= date.getTime()) &&
               (_maxSelectableDate == null || _maxSelectableDate.getTime() >= date.getTime());
    }
}

/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.util.jcalendar;

import com.toedter.calendar.JDateChooser;

import java.util.Date;
import java.util.TimeZone;

/**
 * Extension of {@link JDateChooser} bean with time fields in addition to date fields;
 * plus time zone setter/getter.
 *
 * @rmak
 * @since SecureSpan 4.2
 */
public class JDateTimeChooser extends JDateChooser {

    public JDateTimeChooser() {
        this(null, null, null, null);
    }

    public JDateTimeChooser(JCalendarEx jCalendarEx, Date date, String dateFormatString, IDateEditorEx dateEditor) {
        super(jCalendarEx == null ? new JCalendarEx(date) : jCalendarEx,
              date,
              dateFormatString,
              dateEditor == null ? new JTextFieldDateTimeEditor() : dateEditor);

        setName("JDateChooserEx");
    }

    public TimeZone getTimeZone() {
        return ((JCalendarEx)jcalendar).getTimeZone();
    }

    public void setTimeZone(TimeZone timeZone) {
        ((JCalendarEx)jcalendar).setTimeZone(timeZone);
        ((IDateEditorEx)dateEditor).setTimeZone(timeZone);
    }
}

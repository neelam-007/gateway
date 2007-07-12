/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.util.jcalendar;

import com.toedter.calendar.IDateEditor;

import java.util.TimeZone;

/**
 * Extension of {@link IDateEditor} interface with addition of time zone setter/getter.
 *
 * @rmak
 * @since SecureSpan 4.1
 */
public interface IDateEditorEx extends IDateEditor {

    public TimeZone getTimeZone();

    /**
     * Sets the time zone; while preserving the numeric year, month, day, hour,
     * minute, second and milliseconds.
     *
     * <p>This is the whole purpose of this subclassing.
     *
     * @param timeZone the time zone
     */
    public void setTimeZone(TimeZone timeZone);
}

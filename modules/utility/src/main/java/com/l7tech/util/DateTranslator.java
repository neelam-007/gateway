/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.util;

import java.util.Date;

/**
 * Interface implemented by utilities that translate dates.
 */
public abstract class DateTranslator {
    /**
     * Translate a date from the source clock into the destination clock.
     *
     * @param source the source date.  Might be null.
     * @return the translated date, or null if the source date was null.
     */
    public Date translate(Date source) {
        if (source == null)
            return null;
        final long timeOffset = getOffset();
        if (timeOffset == 0)
            return source;
        final Date result = new Date(source.getTime() + timeOffset);
        log(source.getTime(), result);
        return result;
    }

    /**
     * Translate a date from the source clock into the destination clock.
     *
     * @param source the source date.
     * @return the translated date.  Never null.
     */
    public Date translate(long source) {
        final long timeOffset = getOffset();
        if (timeOffset == 0)
            return new Date(source);
        final Date result = new Date(source + timeOffset);
        log(source, result);
        return result;
    }

    protected abstract long getOffset();

    /**
     * Do any logging of a converstion.  This method does nothing.  Subclasses can take some action if they wish.
     *
     * @param source  the input date
     * @param result  the resulting output date
     */
    protected void log(long source, Date result) {
        // Take no action by default
    }
}

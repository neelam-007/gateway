/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.util;

import java.util.Date;

/**
 * Interface implemented by utilities that translate dates.
 */
public interface DateTranslator {
    /**
     * Translate a date from the source clock into the destination clock.
     *
     * @param source the source date.  Might be null.
     * @return the translated date, or null if the source date was null.
     */
    Date translate(Date source);
}

/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.audit;

import java.util.logging.Level;

/**
 * Message catalog for system componenets such as {@link com.l7tech.server.GatewayLicenseManager}.
 * The ID range 2000-2999 (inclusive) is reserved for these messages.
 */
public class SystemMessages extends Messages {
    public static final M DATABASE_ERROR                = m(2000, Level.WARNING, "Database error");
    public static final M DATABASE_ERROR_WITH_MORE_INFO = m(2001, Level.WARNING, "{0}. Database error");

    public static final M LICENSE_DB_ERROR_RETRY        = m(2010, Level.WARNING, "Database error reading license file.  Will keep current license and retry.");
    public static final M LICENSE_DB_ERROR_GAVEUP       = m(2011, Level.WARNING, "Database error reading license file.  Current license was too stale to keep.  Will keep trying.");
    public static final M LICENSE_NO_LICENSE            = m(2012, Level.WARNING, "No valid license is installed.  Some product features may be disabled.");
    public static final M LICENSE_FOUND                 = m(2013, Level.INFO,  "Valid license found");
    public static final M LICENSE_INVALID               = m(2014, Level.WARNING, "License file is not valid");
    public static final M LICENSE_UPDATED               = m(2015, Level.INFO, "License updated");

    // MAX -                                               m(2999
}

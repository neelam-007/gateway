/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security;

import java.util.logging.Level;

/**
 * @author alex
 * @version $Revision$
 */
public class CertificateExpiry {
    public CertificateExpiry(int days) {
        this.days = days;
    }

    public int getDays() {
        return days;
    }

    public Level getSeverity() {
        if ( days <= INFO_DAYS ) {
            return Level.INFO;
        } else if ( days <= WARNING_DAYS ) {
            return Level.WARNING;
        } else if ( days <= SEVERE_DAYS ) {
            return Level.SEVERE;
        } else {
            return Level.FINE;
        }
    }

    private int days;
    public static final int FINE_DAYS = 30;
    public static final int INFO_DAYS = 14;
    public static final int WARNING_DAYS = 7;
    public static final int SEVERE_DAYS = 2;
}

/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.util;

/**
 * Exception utilities.
 *
 * User: mike
 * Date: Sep 5, 2003
 * Time: 12:03:26 PM
 */
public class ExceptionUtils {
    /**
     * Return true iff. a throable assignable to cause appears within suspect's getCase() chain.
     *
     * @param suspect   The exception you wish to examine.
     * @param cause     The cause you wish to search for, which should be some Throwable class.
     * @return          True if the exception was caused, directly or indirectly, by an instance of the cause class;
     *                  false otherwise.
     */
    public static boolean causedBy(Throwable suspect, Class cause) {
        while (suspect != null) {
            if (cause.isAssignableFrom(suspect.getClass()))
                return true;
            suspect = suspect.getCause();
        }
        return false;
    }

}

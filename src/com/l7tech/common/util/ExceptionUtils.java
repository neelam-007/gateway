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
     * Get the cause of this exception if it was caused by an instnace of class "cause", or null
     * otherwise.
     *
     * @param suspect   The exception to examine.
     * @param cause     The cause you wish to search for, which should be some Throwable class.
     * @return          An instance of the cause class if it was a cause of suspect; otherwise null.
     */
    public static Throwable getCauseIfCausedBy(Throwable suspect, Class cause) {
        while (suspect != null) {
            if (cause.isAssignableFrom(suspect.getClass()))
                return suspect;
            suspect = suspect.getCause();
        }
        return null;
    }

    /**
     * Return true iff. a throwable assignable to cause appears within suspect's getCase() chain.
     * Example:  <code>if (e instanceof SSLException && ExceptionUtils.causedBy(e, IOException.class)
     *              dealWithIOException(...);</code>
     *
     * @param suspect   The exception you wish to examine.
     * @param cause     The cause you wish to search for, which should be some Throwable class.
     * @return          True if the exception was caused, directly or indirectly, by an instance of the cause class;
     *                  false otherwise.
     */
    public static boolean causedBy(Throwable suspect, Class cause) {
        return getCauseIfCausedBy(suspect, cause) != null;
    }

    /**
     * Unnest a throwable to the root <code>Throwable</code>.
     * If no nested exception exist, same Throwable is returned.
     * 
     * @param exception the throwable to unnest
     * @return the root Throwable
     */
    public static Throwable unnestToRoot(Throwable exception) {
        Throwable nestedException = exception.getCause();
        return nestedException == null ? exception : unnestToRoot(nestedException);
    }

}

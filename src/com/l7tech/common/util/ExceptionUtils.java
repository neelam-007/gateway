/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * 
 */

package com.l7tech.common.util;

import java.net.UnknownHostException;

/**
 * Exception utilities.
 * <p/>
 * User: mike
 * Date: Sep 5, 2003
 * Time: 12:03:26 PM
 */
public class ExceptionUtils {

    /**
     * Get the cause of this exception if it was caused by an instnace of class "cause", or null
     * otherwise.
     *
     * @param suspect The exception to examine.
     * @param cause   The cause you wish to search for, which should be some Throwable class.
     * @return An instance of the cause class if it was a cause of suspect; otherwise null.
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
     * dealWithIOException(...);</code>
     *
     * @param suspect The exception you wish to examine.
     * @param cause   The cause you wish to search for, which should be some Throwable class.
     * @return True if the exception was caused, directly or indirectly, by an instance of the cause class;
     *         false otherwise.
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

    /**
     * Wrap another exeception in a RuntimeException.
     */
    public static RuntimeException wrap(Throwable t) {
        if (t instanceof RuntimeException) return (RuntimeException)t;
        return new RuntimeException(t);
    }

    /**
     * Get the message for the specified exception that is at least 2 characters long.
     * If the exception itself has a null message or it is too short,
     * checks for a message in its cause.  If all causes have been exhaused, returns the
     * classname of the original exception.
     *
     * @param t the Throwable to examine.  Must not be null.
     * @return a diagnostic message that can be displayed.  Never null.
     */
    public static String getMessage(final Throwable t) {
        return getMessage(t, 2);
    }

    /**
     * Get the message for the specified exception that is at least n characters long.
     * If the exception has a null message, or its message is shorter than n character, checks
     * for a message in its cause.  If all causes have been exhausted, returns the classname of the original
     * exception.
     *
     * @param t the Throwable to examine.  Must not be null.
     * @param n the minimum length of message that is acceptable, or 0 to accept any non-null message.
     *          For example, set to 2 to disallow the exception message "0".
     * @return a diagnostic message that can be displayed.  Never null.
     */
    public static String getMessage(final Throwable t, int n) {
        if (t == null)
            return "null";

        Throwable current = t;
        while (current != null) {
            String msg = current.getMessage();

            // Special case for array IndexOutOfBounds, which often uses just the bad index as its message
            if (current instanceof IndexOutOfBoundsException && isLong(msg))
                msg = "Index out of bounds: " + msg;

            if (t instanceof UnknownHostException) {
                UnknownHostException o = (UnknownHostException)t;
                msg = "Unknown host: " + o.getMessage();
            }

            if (msg != null && (n < 1 || msg.length() >= n))
                return msg;
            current = current.getCause();
        }

        return t.getClass().getName();
    }

    private static boolean isLong(String s) {
        try {
            Long.valueOf(s);
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }
}

package com.l7tech.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Exception utilities.
 * <p/>
 * User: mike
 * Date: Sep 5, 2003
 * Time: 12:03:26 PM
 */
public class ExceptionUtils {
    private static final String MYSQL_COMMUNICATIONS_EXCEPTION_CLASSNAME = "com.mysql.jdbc.CommunicationsException";

    //- PUBLIC

    /**
     * Get the cause of this exception if it was caused by an instnace of class "cause", or null
     * otherwise.
     *
     * @param suspect The exception to examine.
     * @param cause   The cause you wish to search for, which should be some Throwable class.
     * @return An instance of the cause class if it was a cause of suspect; otherwise null.
     */
    public static <T extends Throwable> T getCauseIfCausedBy(Throwable suspect, Class<T> cause) {
        while (suspect != null) {
            if (cause.isAssignableFrom(suspect.getClass()))
                return (T) suspect;
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
     * Wrap another exception in a RuntimeException.
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
     * @param t the Throwable to examine.  May be null.
     * @return a diagnostic message that can be displayed.  Never null.
     */
    @NotNull
    public static String getMessage(final Throwable t) {
        return getMessage(t, 2, null);
    }

    /**
     * Get the message for the specified exception that is at least 2 characters long.
     * If the exception itself has a null message or it is too short,
     * checks for a message in its cause.  If all causes have been exhaused, returns the
     * classname of the original exception.
     *
     * If the ultimate cause of the error is not the resulting message then the error
     * message is enhanced by adding " Caused by: " followed by the message for the ultimate
     * cause.
     *
     * @param t the Throwable to examine.  Must not be null.
     * @return a diagnostic message that can be displayed.  Never null.
     */
    public static String getMessageWithCause(final Throwable t) {
        final String message = getMessage(t, 2, null);
        final Throwable cause = unnestToRoot( t );
        if ( cause != t ) {
            final String causeMessage = getMessage( cause, 2, null );
            if ( !message.equals( causeMessage ) ) {
                final String messageTerminator = message.endsWith( "." ) ? "" : ".";
                return message + messageTerminator + " Caused by: " + causeMessage;
            }
        }
        return message;
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
    public static String getMessage(final Throwable t, final String defaultMessage) {
        return getMessage(t, 2, defaultMessage);
    }

    /**
     * Get the message for the specified exception that is at least n characters long.
     * If the exception has a null message, or its message is shorter than n character, checks
     * for a message in its cause.  If all causes have been exhausted, returns the classname of the original
     * exception.
     *
     * @param t the Throwable to examine.  May be null.
     * @param n the minimum length of message that is acceptable, or 0 to accept any non-null message.
     *          For example, set to 2 to disallow the exception message "0".
     * @param defaultMessage last-resort message to use if no good message could be found, or null to use the classname
     *                       of the original exception if nothing better could be unearthed.
     * @return a diagnostic message that can be displayed.  Never null.
     */
    public static String getMessage(@Nullable final Throwable t, int n, @Nullable String defaultMessage) {
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

            //this is a special case where the exception came from the MySQL driver and the message contains a
            //a stack trace. In this case, we don't display the stack trace part of the message.
            if (MYSQL_COMMUNICATIONS_EXCEPTION_CLASSNAME.equals(t.getClass().getName())) {
                msg = "Error communicating with the database. Ensure the database is running and the correct credentials are supplied";
            }

            if (msg != null && (n < 1 || msg.length() >= n))
                return msg;
            current = current.getCause();
        }

        return defaultMessage != null ? defaultMessage : t.getClass().getName();
    }

    /**
     * Return the given throwable if debug is on.
     *
     * <p>This is for use when logging stacktraces that could be useful but
     * generally aren't.</p>
     *
     * @param throwable The throwable to return
     * @return null, unless debugging is enabled.
     */
    public static <T extends Throwable> T getDebugException(T throwable) {
        return JdkLoggerConfigurator.debugState() ? throwable : null;
    }

    /**
     * Filter the given Throwable by removing any Throwables in the given list.
     *
     * <p>This is useful when passing Throwables to external systems (e.g. over RMI).</p>
     *
     * <p>The filter will process each cause in turn until an unsupported cause is
     * reached. It will then relace the unsupported exception and its children with
     * a text version of the stack trace.</p>
     *
     * @param throwable The throwable to filter
     * @param remove The unsupported throwables to be removed
     * @param substituteStackAsText True to add the unsupported throwables stack trace as message text
     * @return the given throwable or a copy that has unsupported throwable causes removed.
     */
    public static Throwable filter(final Throwable throwable, final Class[] remove, final boolean substituteStackAsText) {
        Throwable result = throwable;

        List processedThrowables = new ArrayList();
        Throwable current = throwable;

        while (current != null) {
            if (isInstanceOfAny(current, remove)) {
                // The we need to remove this throwable. We need to reconstruct all parent
                // Throwables and textualize either this one or the first non-constructable parent.
                if (processedThrowables.isEmpty()) {
                    result = textReplace(current, substituteStackAsText);
                } else {
                    result = rebuildThrowable(processedThrowables, substituteStackAsText);

                }
                break;
            }
            else {
                processedThrowables.add(current);
                current = current.getCause();
            }
        }

        if (result == null && throwable != null) {
            logger.warning("Replacement exception is null!");
        }

        return result;
    }
    
    /**
     * Replace the given (unsupported) exception with a generic Exception for which
     * the message text is the stack trace.
     *
     * @param throwable The throwable to be replaced
     * @return An Exception with a message that is the stacktrace / message for the original throwable.
     */
    public static Throwable textReplace(Throwable throwable, boolean stackAsText) {
        StringWriter exceptionWriter = new StringWriter();
        if (stackAsText)
            throwable.printStackTrace(new PrintWriter(exceptionWriter));
        return new Exception("Replaced exception of type '"+throwable.getClass().getName()+"', with message '"+
                throwable.getMessage()+"'"  +
                (stackAsText ? ", original stack was:\n" +
                exceptionWriter.getBuffer().toString() : ""));
    }

    /**
     * A deep version of {@link Throwable#toString()} that includes detail
     * messages from all chained exception causes.

     * @param throwable     the throwable to examine
     * @param multiline     whether to print the chained causes on separate lines
     * @return a string of the form:<br/><i>class name</i>: <i>detail message</i><br/>Caused by: <i>class name</i>: <i>detail message</i><br/>Caused by: <i>and so on ...</i>
     */
    public static String toStringDeep(final Throwable throwable, final boolean multiline) {
        final StringBuilder sb = new StringBuilder();
        Throwable t = throwable;
        while (t != null) {
            if (t != throwable) {   // not the top
                if (multiline) {
                    sb.append(NEWLINE);
                } else {
                    sb.append(" ");
                }
                sb.append("Caused by: ");
            }
            sb.append(t.getClass().getName());
            if (t.getMessage() != null) {
                sb.append(": ");
                sb.append(t.getMessage());
            }
            t = t.getCause();
        }
        return sb.toString();
    }

    /**
     * Returns a String containing the characters that would be output be calling printStackTrace() on the specified
     * Throwable.
     *
     * @param t the Throwable whos stack trace to collect.  Required.
     * @return the stack trace as a String.  Never null.
     */
    public static String getStackTraceAsString(Throwable t) {
        StringWriter sw = new StringWriter(4096);
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ExceptionUtils.class.getName());
    private static final String NEWLINE = SyspropUtil.getProperty( "line.separator" );

    /**
     * Check if the throwable argument is of any of the given types.
     *
     * @param throwable      The throwable to check the type of.
     * @param throwableTypes The types to check for.
     * @return true if the given throwable is one of the given types.
     */
    private static boolean isInstanceOfAny(Throwable throwable, Class[] throwableTypes) {
        boolean isInstance = false;

        for (int i = 0; i < throwableTypes.length; i++) {
            Class throwableType = throwableTypes[i];
            if (throwableType.isInstance(throwable)) {
                isInstance = true;
                break;
            }
        }

        return isInstance;
    }

    /**
     * Rebuild the given list of Throwables with each being caused by the next.
     *
     * <p>The final item in the list will have its cause (if any) converted to a text version.</p>
     *
     * @param parents The list of throwables.
     * @param substituteStackAsText True to add the unsupported throwables stack trace as message text
     * @return the replacement
     */
    private static Throwable rebuildThrowable(final List parents, final boolean substituteStackAsText) {
        Throwable constructed = null;

        for (Iterator parentIter=parents.iterator(); parentIter.hasNext();) {
            Throwable parent = (Throwable) parentIter.next();

            boolean added = false;
            try {
                Constructor messageConstructor = findConstructor(parent, new Class[]{String.class});
                if (messageConstructor != null) {
                    Throwable replacement = (Throwable) messageConstructor.newInstance(new Object[]{parent.getMessage()});
                    replacement.setStackTrace(parent.getStackTrace());
                    constructed = addCause(constructed, replacement);
                    added = true;
                }
                else { // then try for a constructor without the message
                    Constructor emptyConstructor = findConstructor(parent, new Class[]{});
                    if (emptyConstructor != null) {
                        Throwable replacement = (Throwable) emptyConstructor.newInstance(new Object[]{});
                        replacement.setStackTrace(parent.getStackTrace());
                        constructed = addCause(constructed, replacement);
                        added = true;
                    }
                }
            }
            catch(InstantiationException ie) {
                logger.log(Level.INFO, "Could not create replacement exception", ie);
            }
            catch(IllegalAccessException iae) {
                logger.log(Level.INFO, "Could not create replacement exception", iae);
            }
            catch(InvocationTargetException ite) {
                logger.log(Level.INFO, "Could not create replacement exception", ite);
            }

            if (!added) {
                // end of the line
                if (substituteStackAsText) {
                    constructed = addCause(constructed, textReplace(parent, substituteStackAsText));
                }
                break;
            }

            if (!parentIter.hasNext() && parent.getCause()!=null && substituteStackAsText) {
                // make the cause of the current exception the textualized child
                constructed = addCause(constructed, textReplace(parent.getCause(), substituteStackAsText));
            }
        }

        return constructed;
    }

    /**
     * Add the given cause as the child of the given target (and its children)
     *
     * @param target The exception being built
     * @param cause The exception to add
     * @return The cause or the target with the cause added.
     */
    private static Throwable addCause(Throwable target, Throwable cause) {
        Throwable addTo = target;
        while (addTo != null) {
            Throwable current = addTo;
            addTo = addTo.getCause();
            if (addTo==null) {
                current.initCause(cause);
                return target;
            }
        }

        return cause;
    }

    /**
     * Find a constructor for the throwable.
     */
    private static Constructor findConstructor(Throwable throwable, Class[] constructorArgs) {
        Constructor constructor = null;

        Class throwableClass = throwable.getClass();
        Constructor[] throwableConstructors = throwableClass.getConstructors();
        for (int c=0; c<throwableConstructors.length; c++) {
            Constructor currentConstructor = throwableConstructors[c];
            if (Arrays.equals(currentConstructor.getParameterTypes(), constructorArgs)) {
                constructor = currentConstructor;
                break;
            }
        }

        return constructor;
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

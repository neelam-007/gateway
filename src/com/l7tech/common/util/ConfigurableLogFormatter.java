package com.l7tech.common.util;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.Handler;
import java.util.Date;
import java.util.Enumeration;
import java.util.IllegalFormatException;
import java.util.Formattable;
import java.util.FormattableFlags;
import java.text.MessageFormat;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.IOException;

/**
 * Log formatter that doesn't totally suck.
 *
 * <p>This is for Java 5, so don't try and use it with anything earlier ...</p>
 *
 * <p>The available format arguments are:</p>
 *
 * <ol>
 *   <li>Time</li>
 *   <li>Level</li>
 *   <li>Logger Name</li>
 *   <li>Message</li>
 *   <li>Thread Id</li>
 *   <li>Exception message</li>
 * </ol>
 *
 * <p>Below is an example of using this formatter with a ConsoleHandler.</p>
 *
 * <pre>
 *  java.util.logging.ConsoleHandler.level = FINEST
 *  java.util.logging.ConsoleHandler.formatter = com.l7tech.common.util.ConfigurableLogFormatter
 *  java.util.logging.ConsoleHandler.formatter.format = [%1$tY/%1$tm/%1$td-%1$tH:%1$tM:%1$tS.%1$tL]-[%2$-7s]-[%5$d]-[%3$#2s]-%4$s%n
 *  java.util.logging.ConsoleHandler.formatter.exceptionFormat = [%1$tY/%1$tm/%1$td-%1$tH:%1$tM:%1$tS.%1$tL]-[%2$-7s]-[%5$d]-[%3$#2s]-%4$s%n%6$s%
 * </pre>
 *
 * @author $Author$
 * @version $Revision$
 */
public class ConfigurableLogFormatter extends Formatter {

    // - PUBLIC

    /**
     *
     */
    public ConfigurableLogFormatter() {
        configured = false;
        format = DEFAULT_FORMAT;
        exceptFormat = DEFAULT_FORMAT_THROWN;
    }

    /**
     * Format the given log record.
     *
     * @param record the log record
     * @return the formatted message
     */
    public String format(final LogRecord record) {
        initConfig();

        String name = record.getLoggerName();
        long time = record.getMillis();
        Level level = record.getLevel();
        String message = record.getMessage();
        Object[] params = record.getParameters();
        Throwable thrown = record.getThrown();
        int threadId = record.getThreadID();

        if(params!=null) {
            try {
                message = MessageFormat.format(message, params);
            }
            catch(IllegalArgumentException iae) {
                // if they don't use a valid pattern just log the raw message
            }
        }
        Object[] formatArgs =  new Object[]{new Long(time), level.getName(), new FormattableLoggerName(name), message, new Integer(threadId), toString(thrown)};

        String selectedFormat = null;
        boolean standardFormat = true;
        if(thrown == null) {
            selectedFormat = format;
        }
        else {
            selectedFormat = exceptFormat;
            standardFormat = false;
        }

        String formattedString = null;
        try {
            formattedString = String.format(selectedFormat, formatArgs);
        }
        catch(IllegalFormatException ife) {
            ife.printStackTrace();

            if(standardFormat) {
                format = DEFAULT_FORMAT;
                selectedFormat = DEFAULT_FORMAT;
            }
            else {
                exceptFormat = DEFAULT_FORMAT_THROWN;
                selectedFormat = DEFAULT_FORMAT_THROWN;
            }

            formattedString = String.format(selectedFormat, formatArgs);
        }

        return formattedString;
    }

    //- PRIVATE

    /**
     * The standard message formats, e.g.
     *
     *   [2006/03/02-13:26:21,508]-[FINEST ]-[16]-[com.l7tech.server.ServerConfig]-Using default value 131071
     *
     */
    private static final String DEFAULT_FORMAT = "[%1$tY/%1$tm/%1$td-%1$tH:%1$tM:%1$tS,%1$tL]-[%2$-7s]-[%5$d]-[%3$s]-%4$s%n";
    private static final String DEFAULT_FORMAT_THROWN = "[%1$tY/%1$tm/%1$td-%1$tH:%1$tM:%1$tS,%1$tL]-[%2$-7s]-[%5$d]-[%3$s]-%4$s%n%6$s%n";

    /**
     *
     */
    private boolean configured;
    private String format;
    private String exceptFormat;

    /**
     * Read the configuration for the format
     */
    private void initConfig() {
        configured = true;
        LogManager manager = LogManager.getLogManager();
        Enumeration nameEnum = manager.getLoggerNames();
        boolean found = false;
        // Loop through the Loggers
        while(nameEnum.hasMoreElements() && !found) {
            String loggerName = (String) nameEnum.nextElement();
            Logger logger = manager.getLogger(loggerName);

            // Check Handlers for this Logger
            Handler[] handlers = logger.getHandlers();
            for (int i = 0; i < handlers.length; i++) {
                Handler handler = handlers[i];
                String className = handler.getClass().getName();

                // See if we are the formatter for this Handler
                Formatter formatter = handler.getFormatter();
                if(formatter == this) {
                    // Check for customized format pattern for exceptions and std messages
                    String prefix = className + ".formatter";
                    String configFormat = manager.getProperty(prefix + ".format");
                    String configExceptFormat = manager.getProperty(prefix + ".exceptionFormat");

                    if(configFormat!=null) format = configFormat;
                    if(configExceptFormat!=null) exceptFormat = configExceptFormat;

                    found = true;
                    break;
                }
            }
        }
    }

    /**
     * Turn a throwable into a huge stacktrace.
     *
     * @param t the throwable
     * @return the formatted string
     */
    private String toString(Throwable t) {
        String formatted = null;

        if(t!=null) {
            StringWriter stackWriter = new StringWriter(512);
            t.printStackTrace(new PrintWriter(stackWriter));
            formatted = stackWriter.toString();
        }

        return formatted;
    }

    /**
     * Formattable wrapper for class/logger names with an alternative format that
     * allows the use of width to specify the number of name sections that are written out.
     *
     * e.g for com.l7tech.server.BootProcess with width=2 you would get server.BootProcess
     */
    private static final class FormattableLoggerName implements Formattable {
        private final String name;

        private FormattableLoggerName(String name) {
            this.name = name;
        }

        public void formatTo(java.util.Formatter formatter, int flags, int width, int precision) {
            String formatted = name;

            if(formatted==null) formatted = ""; // anonymous logger

            // handle uppercase flag
            if((FormattableFlags.UPPERCASE & flags) > 0) {
                formatted = formatted.toUpperCase();
            }

            Appendable out = formatter.out();
            try {
                // Alternative format that uses width for specification of name parts
                if((FormattableFlags.ALTERNATE & flags) > 0) {
                    int cutIndex = width==0 ? 0 : formatted.length();
                    for(int w=0; w<width; w++) {
                        int newCutIndex = formatted.lastIndexOf('.', cutIndex);
                        if(newCutIndex>0) {
                            cutIndex = newCutIndex -1;
                        }
                        else { // not more dots
                            cutIndex = 0;
                            break;
                        }
                    }
                    if(cutIndex > 0) {
                        out.append(formatted,cutIndex+2,formatted.length());
                    }
                    else {
                        out.append(formatted);
                    }
                }
                else { // Not FormattableFlags.ALTERNATE
                    if(formatted.length() < width) {
                        int append = width - formatted.length();
                        if((FormattableFlags.LEFT_JUSTIFY & flags) > 0) {
                            out.append(formatted);
                            for(int a=0; a<append; a++) out.append(' ');
                        }
                        else {
                            for(int a=0; a<append; a++) out.append(' ');
                            out.append(formatted);
                        }
                    }
                    else {
                        out.append(formatted);
                    }
                }
            }
            catch(IOException ioe) {
            }
        }
    }
}

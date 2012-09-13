package com.l7tech.server.log.syslog;

import com.l7tech.util.Charsets;

import java.io.Closeable;
import java.nio.charset.Charset;
import java.text.*;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Managed syslog class
 *
 * @author Steve Jones
 */
public abstract class ManagedSyslog implements Closeable {

    //- PROTECTED

    protected abstract void init();

    /**
     * Log a message to syslog.
     *
     * @param format   The format to use when sending the message
     * @param facility The facility part of the priority
     * @param severity The severity part of the priority
     * @param host     The host to log as (may be null)
     * @param process  The process to log
     * @param threadId The identifier for the log message
     * @param time     The time for the log message
     * @param message  The log message
     */
    protected abstract void log(SyslogFormat format,
                                int facility,
                                SyslogSeverity severity,
                                String host,
                                String process,
                                long threadId,
                                long time,
                                String message);

    /**
     * Get the SyslogConnectionListener for this ManagedSyslog.
     *
     * @return the SyslogConnectionListener (never null)
     */
    protected SyslogConnectionListener getSyslogConnectionListener() {
        return manager.getSyslogConnectionListener();
    }

    /**
     * Format the given syslog message for transmission.
     *
     * This should only be called from a single thread.
     */
    protected static String formatMessage(final SyslogFormat format,
                                          final int facility,
                                          final int severity,
                                          final int priority,
                                          final String host,
                                          final String process,
                                          final long threadId,
                                          final long time,
                                          final String message) {
        //  populate the given format argument array from the supplied message.
        //
        //  0 - facility
        //  1 - severity
        //  2 - priority
        //  3 - date (Oct 11 22:14:15)
        //  4 - date (format not specified)
        //  5 - host (domain removed)
        //  6 - FQDN
        //  7 - process
        //  8 - thread id
        //  9 - message
        // 10 - LogSink name
        Object[] formatArguments = new Object[11];
        formatArguments[0] = Integer.valueOf(facility);
        formatArguments[1] = Integer.valueOf(severity);
        formatArguments[2] = Integer.valueOf(priority);
        formatArguments[3] = new Object[]{format.timeZone, Long.valueOf(time)};
        formatArguments[4] = Long.valueOf(time);
        formatArguments[5] = host; // same value, different formats
        formatArguments[6] = host;
        formatArguments[7] = process;
        formatArguments[8] = Long.valueOf(threadId);
        formatArguments[9] = message;

        MessageFormat messageFormat = format.format;

        // Set zone if possible
        Format[] formats = messageFormat.getFormatsByArgumentIndex();
        if ( formats[5] instanceof DateFormat) {
            ((DateFormat)formats[5]).setTimeZone(format.timeZone);
        }

        return escapeMessage(messageFormat.format(formatArguments), format.delimiter);
    }

    /**
     * Format information for syslog messages, includes encoding and delimiter.
     */
    protected static final class SyslogFormat {
        private final MessageFormat format;
        private final TimeZone timeZone;
        private final Charset charset;
        private final String delimiter;
        private final int maxLength;

        protected SyslogFormat(final String format,
                               final String timeZone,
                               final String charset,
                               final String delimiter,
                               final int maxLength){
            this.format = new MessageFormat(format == null ? DEFAULT_LOG_PATTERN : format);
            this.timeZone = timeZone == null ? TimeZone.getDefault() : TimeZone.getTimeZone(timeZone);
            // bug #6564 - bad charset name for LATIN-1
            if ("LATIN-1".equalsIgnoreCase(charset)) {
                this.charset = Charsets.ISO8859;
            } else {
                this.charset = charset == null ? Charsets.UTF8 : Charset.forName(charset);
            }
            this.delimiter = delimiter == null ? "\n" : delimiter;
            this.maxLength = maxLength;
            installFormats(this.format);
        }

        public Charset getCharset() {
            return charset;
        }

        public String getDelimiter() {
            return delimiter;
        }

        public int getMaxLength() {
            return maxLength;
        }
    }

    //- PACKAGE

    void reference() {
        referenceCount.incrementAndGet();
    }

    void dereference() {
        int references = referenceCount.decrementAndGet();
        if ( references <= 0 ) {
            manager.destroySyslog(this);
        }
    }

    boolean isReferenced() {
        return referenceCount.get() > 0;
    }

    //- PROTECTED

    protected void init(final SyslogManager manager) {
        this.manager = manager;
        init();
    }

    protected Syslog getSylog(final SyslogFormat format,
                    final int facility,
                    final String host) {
        return new SyslogHandle(this, format, facility, host);
    }

    //- PRIVATE

    private static final String DEFAULT_LOG_PATTERN = "<{2}>{3} {5} {7}[{8}]: {9}";
    private static final String SYSLOG_DATE_FORMAT_PATTERN = "MMM d HH:mm:ss";

    private final AtomicInteger referenceCount = new AtomicInteger(0);
    private SyslogManager manager;

    /**
     * Remove any delimiter or partial delimiter occurances. 
     */
    private static String escapeMessage(final String message, final String delimiter) {
        String result = message;

        // process full value
        if ( delimiter.length() > 1 ) {
            result = message.replace(delimiter, " ");
        }

        // process parts
        for ( int i=0; i<delimiter.length(); i++ ) {
            result = message.replace(delimiter.charAt(i), ' ');
        }

        return result;
    }

    /**
     * Install format customizers
     */
    private static void installFormats(final MessageFormat format) {
        // install format for date with timezone in syslog format
        format.setFormatByArgumentIndex(3, new Format(){
            private final SimpleDateFormat format = new SimpleDateFormat(SYSLOG_DATE_FORMAT_PATTERN);
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                Object[] dateAndTimeZone = (Object[]) obj;
                toAppendTo.append(formatForZone(format, (TimeZone)dateAndTimeZone[0], (Long)dateAndTimeZone[1]));
                return toAppendTo;
            }
            public Object parseObject(String source, ParsePosition pos) { return null; }
        });
        // install format for host from FQDN
        format.setFormatByArgumentIndex(5, new Format(){
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                String fqdn = obj.toString();
                int dotIndex = fqdn.indexOf('.');
                String host;
                if ( dotIndex == -1 ) {
                    host = fqdn;
                } else {
                    host = fqdn.substring(0, dotIndex);
                }
                toAppendTo.append(host);
                return toAppendTo;
            }
            public Object parseObject(String source, ParsePosition pos) { return null; }
        });
    }

    /**
     * Format the given time for the specifed zone in Syslog format.
     *
     *   Oct 11 22:14:15
     *   Oct  1 02:54:55
     */
    private static String formatForZone(final SimpleDateFormat format, final TimeZone timeZone, final long time) {
        //
        format.setTimeZone(timeZone);
        String formatted = format.format(time);

        if ( formatted.length()==14 ) { // extra padding for date ...
            formatted = formatted.substring(0,3) + " " + formatted.substring(3,14);
        }

        return formatted;
    }

    /**
     * Handle for a ManagedSyslog that implements Syslog.
     *
     * This holds log parameters that don't change for each Syslog and also
     * allows active use of the underlying ManagedSyslog to be tracked for
     * resource cleanup. 
     */
    private static final class SyslogHandle implements Syslog {
        private final ManagedSyslog managedSyslog;
        private final SyslogFormat format;
        private final int facility;
        private final String host;

        private SyslogHandle(final ManagedSyslog managedSyslog,
                             final SyslogFormat format,
                             final int facility,
                             final String host) {
            this.managedSyslog = managedSyslog;
            this.format = format;
            this.facility = facility;
            this.host = host;
        }

        public void log(SyslogSeverity severity, String process, long threadId, long time, String message) {
            managedSyslog.log(format, facility, severity, host, process, threadId, time, message);
        }

        public void close() {
            managedSyslog.dereference();
        }
    }
}

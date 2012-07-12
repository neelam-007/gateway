package com.l7tech.server.log;

import com.l7tech.gateway.common.log.SinkConfiguration;

import java.io.FileOutputStream;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.logging.*;

/**
 * <p>An implementation of the {@link java.util.logging.Handler} to roll log files based on a time interval.
 * The supported intervals are defined in {@link com.l7tech.gateway.common.log.SinkConfiguration.RollingInterval}</p>
 *
 * <p>This class is based on the @{link java.util.logging.FileHandler}.</p>
 * @author KDiep
 */
public class RollingFileHandler extends StreamHandler implements StartupAwareHandler {

    private final SinkConfiguration.RollingInterval interval;
    private final String prefix;

    private long nextRollOver;

    /**
     * Construct a new handler with the given filename prefix and the rolling interval.
     *
     * @param prefix   the filename prefix (this may include the full path and portion of the filename).  IE: \log\ssg, the timestamp and filename suffix will be appended to this prefix to form \log\ssg.2012-07-11.log.
     * @param interval the interval which the log file will be rolled on.
     * @throws IOException if any I/O errors occur during the creation of the log file(s).
     */
    public RollingFileHandler(final String prefix, final SinkConfiguration.RollingInterval interval) throws IOException {
        this.prefix = prefix;
        this.interval = interval;
        open();
    }

    @Override
    public synchronized void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }
        if (System.currentTimeMillis() >= nextRollOver) {
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                public Object run() {
                    rotate();
                    return null;
                }
            });
        }
        super.publish(record);
        flush();
    }

    private synchronized void open() throws IOException {
        LogManager.getLogManager().checkAccess();
        final SimpleDateFormat sdf = new SimpleDateFormat(interval.getPattern());

        final Calendar currentTime = Calendar.getInstance();
        final String dateString = sdf.format(currentTime.getTime());
        final String fname = prefix + "." + dateString + ".log";

        //clear unused time fields
        final Calendar current = new GregorianCalendar(
                currentTime.get(Calendar.YEAR),
                currentTime.get(Calendar.MONTH),
                currentTime.get(Calendar.DATE)
        );

        switch (interval){
            case HOURLY:
                current.set(Calendar.HOUR_OF_DAY, currentTime.get(Calendar.HOUR_OF_DAY));
                current.add(Calendar.HOUR_OF_DAY, 1);
                break;
            case DAILY:
                current.add(Calendar.DATE, 1);
                break;
        }
        nextRollOver = current.getTimeInMillis();

        //always append
        setOutputStream(new FileOutputStream(fname, true));
    }

    private synchronized void rotate() {
        Level oldLevel = getLevel();
        setLevel(Level.OFF);
        super.close();
        try {
            open();
        } catch (IOException ix) {
            // We don't want to throw an exception here, but we
            // report the exception to any registered ErrorManager.
            reportError(null, ix, ErrorManager.OPEN_FAILURE);
        }
        setLevel(oldLevel);
    }

}

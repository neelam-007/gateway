package com.l7tech.adminws.logging;

import com.l7tech.logging.LogManager;
import com.l7tech.common.util.UptimeMetrics;
import com.l7tech.common.util.UptimeMonitor;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.FileNotFoundException;

/**
 * Layer 7 technologies, inc.
 * User: flascell
 * Date: Jul 3, 2003
 * Time: 12:45:24 PM
 *
 * AdminWS for consulting the server system log
 */
public class Service implements Log {
    public static final String SERVICE_DEPENDENT_URL_PORTION = "/services/loggingAdmin";

    public String[] getSystemLog(int offset, int size) throws RemoteException {
        LogRecord[] records = LogManager.getInstance().getRecorded(offset, size);
        return logRecordsToStrings(records);
    }

    /**
     * Retrieve the system logs in between the startMsgNumber and endMsgNumber specified
     * up to the specified size.
     * NOTE: the log messages whose message number equal to startMsgNumber and endMsgNumber
     * are not returned.
     *
     * @param startMsgNumber the message number to locate the start point.
     *                       Start from beginning of the message buffer if it equals to -1.
     * @param endMsgNumber   the message number to locate the end point.
     *                       Retrieve messages until the end of the message buffer is hit if it equals to -1.
     * @param size  the max. number of messages retrieved
     * @return String[] the array of messages retrieved
     * @throws RemoteException
     */
    public String[] getSystemLog(long startMsgNumber, long endMsgNumber, int size) throws RemoteException {
        LogRecord[] records = LogManager.getInstance().getRecorded(startMsgNumber, endMsgNumber, size);
        return logRecordsToStrings(records);
    }

    public UptimeMetrics getUptime() throws RemoteException {
        try {
            return UptimeMonitor.getLastUptime();
        } catch (FileNotFoundException e) {
            String msg = "cannot retrieve uptime";
            logger.log(Level.WARNING, msg, e);
            throw new RemoteException(msg, e);
        } catch (IllegalStateException e) {
            String msg = "cannot retrieve uptime";
            logger.log(Level.WARNING, msg, e);
            throw new RemoteException(msg, e);
        }
    }

    // ************************************************
    // PRIVATES
    // ************************************************
    private String[] logRecordsToStrings(LogRecord[] logs) {
        String[] output = new String[logs.length];
        String delimiter = "|";
        SimpleDateFormat sdf = new SimpleDateFormat( "yyyyMMdd HH:mm:ss.SSS" );
        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < logs.length; i++) {
            cal.setTimeInMillis( logs[i].getMillis() );
            output[i] = logs[i].getSequenceNumber() + delimiter +
                        sdf.format( cal.getTime() ) + delimiter +
                        logs[i].getLevel().toString() + delimiter +
                        logs[i].getSourceClassName() + delimiter +
                        logs[i].getSourceMethodName() + delimiter +
                        logs[i].getMessage();
            if (logs[i].getThrown() != null)
                        output[i] += " Exception: " + logs[i].getThrown().getClass().getName() + " " + logs[i].getThrown().getMessage();
        }
        return output;
    }

    private Logger logger = LogManager.getInstance().getSystemLogger();
}

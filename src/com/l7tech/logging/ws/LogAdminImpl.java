package com.l7tech.logging.ws;

import com.l7tech.logging.LogAdmin;
import com.l7tech.logging.LogManager;
import com.l7tech.logging.SSGLogRecord;
import com.l7tech.objectmodel.PersistenceContext;

import java.rmi.RemoteException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * AdminWS for consulting the server system log.
 *
 * <br/><br/>
 * Layer 7 technologies, inc.<br/>
 * User: flascell<br/>
 * Date: Jul 3, 2003<br/>
 */
public class LogAdminImpl implements LogAdmin {
    public static final String SERVICE_DEPENDENT_URL_PORTION = "/services/loggingAdmin";

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
        try {
            LogRecord[] records = LogManager.getInstance().getRecorded(startMsgNumber, endMsgNumber, size);
            return logRecordsToStrings(records);
        } finally {
            try {
                PersistenceContext.getCurrent().close();
            } catch (SQLException e) {
                logger.fine("error closing context");
            }
        }
    }

    public SSGLogRecord[] getSystemLog(String nodeid, long startMsgNumber, long endMsgNumber, int size) throws RemoteException {
        try {
            return LogManager.getInstance().getRecorded(nodeid, startMsgNumber, endMsgNumber, size);
        } finally {
            try {
                PersistenceContext.getCurrent().close();
            } catch (SQLException e) {
                logger.fine("error closing context");
            }
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

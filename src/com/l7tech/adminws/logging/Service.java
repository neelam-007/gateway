package com.l7tech.adminws.logging;

import com.l7tech.logging.LogManager;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.LogRecord;

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

    // ************************************************
    // PRIVATES
    // ************************************************
    private String[] logRecordsToStrings(LogRecord[] logs) {
        String[] output = new String[logs.length];
        SimpleDateFormat sdf = new SimpleDateFormat( "yyyyMMdd HH:mm:ss.SSS" );
        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < logs.length; i++) {
            cal.setTimeInMillis( logs[i].getMillis() );
            output[i] = sdf.format( cal.getTime() ) + " - " +
                        logs[i].getLevel().toString() +  " - " +
                        logs[i].getSourceClassName() +  " - " +
                        logs[i].getSourceMethodName() +  " - " +
                        logs[i].getMessage();
            if (logs[i].getThrown() != null)
                        output[i] += " Exception: " + logs[i].getThrown().getClass().getName() + " " + logs[i].getThrown().getMessage();
        }
        return output;
    }
}

package com.l7tech.logging.ws;

import com.l7tech.logging.LogAdmin;
import com.l7tech.logging.LogManager;
import com.l7tech.logging.SSGLogRecord;
import com.l7tech.objectmodel.PersistenceContext;

import java.rmi.RemoteException;
import java.sql.SQLException;
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

    private Logger logger = LogManager.getInstance().getSystemLogger();
}

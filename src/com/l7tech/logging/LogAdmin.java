package com.l7tech.logging;

import com.l7tech.common.util.UptimeMetrics;

import java.rmi.RemoteException;
import java.rmi.Remote;

/**
 * Insert comments here.
 *
 * @author  <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version $Revision$, $Date$
 */
public interface LogAdmin extends Remote {
    String[] getSystemLog(int offset, int size) throws RemoteException;

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
    String[] getSystemLog(long startMsgNumber, long endMsgNumber, int size) throws RemoteException;

    UptimeMetrics getUptime() throws RemoteException;
}

/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.logging;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author mike
 */
public interface GenericLogAdmin extends Remote {
    /**
     * Retrieve the system logs in between the startMsgNumber and endMsgNumber specified
     * up to the specified size. This version of getSystemLog is used when the ssm connects
     * to a ssg cluster that has more than one server. Calls to ClusterStatusAdmin.getClusterStatus
     * tell the ssm how nodes exist and what their nodeid is.
     *
     * @param nodeid the id of the node for which the logs should be retrieved.
     * @param startMsgNumber the message number to locate the start point.
     *                       Start from beginning of the message buffer if it equals to -1.
     * @param endMsgNumber   the message number to locate the end point.
     *                       Retrieve messages until the end of the message buffer is hit if it equals to -1.
     * @param size  the max. number of messages retrieved
     * @return SSGLogRecord[] the array of messages retrieved
     */
    SSGLogRecord[] getSystemLog(String nodeid, long startMsgNumber, long endMsgNumber, int size) throws RemoteException;
}

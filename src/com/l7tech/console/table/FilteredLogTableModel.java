package com.l7tech.console.table;

import com.l7tech.logging.LogMessage;
import com.l7tech.console.panels.LogPanel;
import com.l7tech.cluster.GatewayStatus;

import java.util.Vector;
import java.util.Hashtable;
import java.util.Iterator;

/*
 * This class encapsulates the table model for filtered logs.
 *
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class FilteredLogTableModel extends FilteredDefaultTableModel {

    public static final int MAX_MESSAGE_BLOCK_SIZE = 600;
    public static final int MAX_NUMBER_OF_LOG_MESSGAES = 4096;

    protected Hashtable rawLogCache = new Hashtable();
    protected Vector filteredLogCache = new Vector();
    private int filterLevel = LogPanel.MSG_FILTER_LEVEL_WARNING;
    protected Hashtable currentNodeList;

    /**
     * Set the filter level.
     *
     * @param filterLevel  The filter level to be applied.
     */
    public void setMsgFilterLevel(int filterLevel) {
        this.filterLevel = filterLevel;
    }

    /**
     * Check if the message should be filtered out or not.
     *
     * @param logMsg  The message to be examined.
     * @return  true if the message is filtered out, false otherwise.
     */
    private boolean isFilteredMsg(LogMessage logMsg) {

        if ((((logMsg.getSeverity().toString().equals("FINEST")) ||
                (logMsg.getSeverity().toString().equals("FINER")) ||
                (logMsg.getSeverity().toString().equals("FINE"))) && (filterLevel >= LogPanel.MSG_FILTER_LEVEL_ALL)) ||
                (logMsg.getSeverity().toString().equals("INFO") && (filterLevel >= LogPanel.MSG_FILTER_LEVEL_INFO)) ||
                (logMsg.getSeverity().toString().equals("WARNING") && (filterLevel >= LogPanel.MSG_FILTER_LEVEL_WARNING)) ||
                (logMsg.getSeverity().toString().equals("SEVERE") && (filterLevel >= LogPanel.MSG_FILTER_LEVEL_SEVERE))) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Filtering messages with the specified filter level.
     *
     * @param msgFilterLevel
     */
    protected void filterData(int msgFilterLevel) {

        setMsgFilterLevel(msgFilterLevel);

        // initialize the cache
        filteredLogCache = new Vector();

        for (Iterator i = rawLogCache.keySet().iterator(); i.hasNext(); ) {
            Object node = i.next();
            filterData((String) node);
        }
    }

    /**
     * Filtering messages of the given node.
     *
     * @param nodeId  The Id of the node whose messages will be filtered.
     */
    private void filterData(String nodeId) {

        Object node = null;
        Vector logs;
        Object nodeName = null;

        if((nodeName = currentNodeList.get(nodeId)) == null) {
            return;
        }

        if ((node = rawLogCache.get(nodeId)) != null) {
            logs = (Vector) node;

            for (int i = 0; i < logs.size(); i++) {

                LogMessage logMsg = (LogMessage) logs.elementAt(i);

                if (isFilteredMsg(logMsg)) {
                    logMsg.setNodeName(((GatewayStatus) nodeName).getName());
                    filteredLogCache.add(logMsg);
                }
            }
        }
    }

}

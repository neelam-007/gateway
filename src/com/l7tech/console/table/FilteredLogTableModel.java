package com.l7tech.console.table;

import com.l7tech.logging.LogMessage;
import com.l7tech.console.panels.LogPanel;
import com.l7tech.cluster.GatewayStatus;
import com.l7tech.common.audit.MessageSummaryAuditRecord;

import java.util.Map;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Collection;

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

    protected Map rawLogCache = new HashMap();
    protected List filteredLogCache = new ArrayList();
    private int filterLevel = LogPanel.MSG_FILTER_LEVEL_WARNING;
    private String filterNodeName = "";
    private String filterService = "";
    private String filterThreadId = "";
    private String filterMessage = "";
    protected Map currentNodeList;

    /**
     * Set the filter level.
     *
     * @param filterLevel  The filter level to be applied.
     */
    public void setMsgFilterLevel(int filterLevel) {
        this.filterLevel = filterLevel;
    }

    public void setMsgFilterNodeName(String filterNodeName) {
        this.filterNodeName = filterNodeName;
    }

    public void setMsgFilterService(String filterService) {
        this.filterService = filterService;
    }

    public void setMsgFilterThreadId(String filterThreadId) {
        this.filterThreadId = filterThreadId;
    }

    public void setMsgFilterMessage(String filterMessage) {
        this.filterMessage = filterMessage;
    }

    /**
     * Check if the message should be filtered out or not.
     *
     * @param logMsg  The message to be examined.
     * @return  true if the message is filtered out, false otherwise.
     */
    private boolean isFilteredMsg(LogMessage logMsg) {

        String logSeverity = logMsg.getSeverity().toString();

        if ((((logSeverity.equals("FINEST")) ||
              (logSeverity.equals("FINER")) ||
              (logSeverity.equals("FINE"))) && (filterLevel >= LogPanel.MSG_FILTER_LEVEL_ALL)) ||
             (logSeverity.equals("INFO")    && (filterLevel >= LogPanel.MSG_FILTER_LEVEL_INFO)) ||
             (logSeverity.equals("WARNING") && (filterLevel >= LogPanel.MSG_FILTER_LEVEL_WARNING)) ||
             (logSeverity.equals("SEVERE")  && (filterLevel >= LogPanel.MSG_FILTER_LEVEL_SEVERE))) {

            String name = logMsg.getNodeName();
            String message = logMsg.getMsgDetails();
            String threadId = Integer.toString(logMsg.getSSGLogRecord().getThreadID());
            String service = getServiceName(logMsg);

            return matches(filterNodeName, name) &&
                   matches(filterService, service) &&
                   matches(filterThreadId, threadId) &&
                   matches(filterMessage, message);
        } else {
            return false;
        }
    }

    private static boolean matches(String pattern, String data) {
        boolean matches = true;

        if(pattern!=null && pattern.trim().length()>0) {
            if(pattern.equals("*")) {
                matches = true;
            }
            else if(data==null) {
                matches = false;
            }
            else {
                String lpattern = pattern.toLowerCase();
                String ldata = data.toLowerCase();

                String[] patterns = lpattern.split("\\*");
                boolean wildStart = lpattern.startsWith("*");
                boolean wildEnd = lpattern.endsWith("*");

                int offset = 0;
                for (int i = 0; i < patterns.length; i++) {
                    String pat = patterns[i];
                    if(i==0 && !wildStart && !wildEnd && patterns.length==1 && !ldata.equals(pat)) {
                        matches = false;
                        break;
                    }
                    else if(i==0 && !wildStart && !ldata.startsWith(pat)) {
                        matches = false;
                        break;
                    }
                    else if(i==patterns.length-1 && !wildEnd && !ldata.endsWith(pat)) {
                        matches = false;
                        break;
                    }
                    else {
                        if(pat.length()==0) continue;
                        int patIndex = ldata.indexOf(pat, offset);
                        if(patIndex<0) {
                            matches = false;
                            break;
                        }
                        else {
                            offset = patIndex + pat.length();
                        }
                    }
                }
            }
        }

        return matches;
    }

    /**
     * Filtering messages with the specified filter level.
     *
     * @param msgFilterLevel
     */
    protected void filterData(int msgFilterLevel,
                              String msgFilterNodeName,
                              String msgFilterService,
                              String msgFilterThreadId,
                              String msgFilterMessage) {

        setMsgFilterLevel(msgFilterLevel);
        setMsgFilterNodeName(msgFilterNodeName);
        setMsgFilterService(msgFilterService);
        setMsgFilterThreadId(msgFilterThreadId);
        setMsgFilterMessage(msgFilterMessage);

        // initialize the cache
        filteredLogCache = new ArrayList();

        for (Iterator i = rawLogCache.keySet().iterator(); i.hasNext(); ) {
            Object node = i.next();
            filterData((String) node);
        }
    }

    protected String getServiceName(LogMessage msg) {
        return msg.getSSGLogRecord() instanceof MessageSummaryAuditRecord
                        ? ((MessageSummaryAuditRecord)msg.getSSGLogRecord()).getName()
                        : "";
    }

    /**
     * Filtering messages of the given node.
     *
     * @param nodeId  The Id of the node whose messages will be filtered.
     */
    private void filterData(String nodeId) {

        Object node = null;
        Collection logs;
        Object nodeName = null;

        if((nodeName = currentNodeList.get(nodeId)) == null) {
            return;
        }

        if ((node = rawLogCache.get(nodeId)) != null) {
            logs = (Collection) node;

            for (Iterator iterator = logs.iterator(); iterator.hasNext();) {
                LogMessage logMsg = (LogMessage) iterator.next();

                if (isFilteredMsg(logMsg) && !filteredLogCache.contains(logMsg)) {
                    logMsg.setNodeName(((GatewayStatus) nodeName).getName());
                    filteredLogCache.add(logMsg);
                }
            }
        }
    }

}

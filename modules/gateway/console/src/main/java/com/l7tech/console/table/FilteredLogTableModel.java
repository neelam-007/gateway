package com.l7tech.console.table;

import com.l7tech.console.util.LogMessage;
import com.l7tech.gateway.common.cluster.GatewayStatus;
import com.l7tech.console.panels.LogPanel;

import java.util.*;
import java.util.logging.Level;

/*
 * This class encapsulates the table model for filtered logs.
 *
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class FilteredLogTableModel extends FilteredDefaultTableModel {

    public static final int MAX_MESSAGE_BLOCK_SIZE = 1024;
    public static final int MAX_NUMBER_OF_LOG_MESSAGES = 131072;//2^17
    protected Map<Long, LogMessage> rawLogCache = new HashMap<Long, LogMessage>();
    protected List<LogMessage> filteredLogCache = new ArrayList<LogMessage>();
    protected Map<String, GatewayStatus> currentNodeList;
    private LogPanel.LogLevelOption filterLevel = LogPanel.LogLevelOption.WARNING;

    private String filterThreadId = "";
    private String filterMessage = "";

    /**
     * Set the filter level.
     *
     * @param filterLevel  The filter level to be applied.
     */
    public void setMsgFilterLevel(LogPanel.LogLevelOption filterLevel) {
        this.filterLevel = filterLevel;
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
     * @param logMessage  The message to be examined.
     * @return  true if the message is filtered out, false otherwise.
     */
    private boolean isFilteredMsg(LogMessage logMessage) {

        String logSeverity = logMessage.getSeverity();

        if ((((logSeverity.equals("FINEST")) ||
              (logSeverity.equals("FINER")) ||
              (logSeverity.equals("FINE"))) && (filterLevel.getLevel().intValue() <= Level.ALL.intValue())) ||
             (logSeverity.equals("INFO")    && (filterLevel.getLevel().intValue() <= Level.INFO.intValue()))||
             (logSeverity.equals("WARNING") && (filterLevel.getLevel().intValue() <= Level.WARNING.intValue())) ||
             (logSeverity.equals("SEVERE")  && (filterLevel.getLevel().intValue() <= Level.SEVERE.intValue()))) {

            String message = logMessage.getMsgDetails();
            String threadId = Integer.toString(logMessage.getThreadID());

            return matches(filterThreadId, threadId) &&
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

    /*
     * Filtering messages with the specified filter level.
     */
    protected void filterData(LogPanel.LogLevelOption msgFilterLevel,
                              String msgFilterThreadId,
                              String msgFilterMessage) {

        setMsgFilterLevel(msgFilterLevel);
        setMsgFilterThreadId(msgFilterThreadId);
        setMsgFilterMessage(msgFilterMessage);

        // initialize the cache
        filteredLogCache = new ArrayList<LogMessage>();

        Collection<LogMessage> rawLogCacheCollection = rawLogCache.values();

        for (LogMessage logMessage : rawLogCacheCollection) {
            if (isFilteredMsg(logMessage) && !filteredLogCache.contains(logMessage)) {
                filteredLogCache.add(logMessage);
            }
        }
    }
}

package com.l7tech.console.table;

import com.l7tech.common.util.Locator;
import com.l7tech.logging.LogMessage;
import com.l7tech.logging.LogAdmin;
import com.l7tech.console.panels.LogPanel;
import com.l7tech.console.util.LogsWorker;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: fpang
 * Date: Sep 19, 2003
 * Time: 9:38:30 AM
 * To change this template use Options | File Templates.
 */
public class FilteredLogTableModel extends FilteredDefaultTableModel{

     public static final int MAX_MESSAGE_BLOCK_SIZE = 100;
     public static final int MAX_NUMBER_OF_LOG_MESSGAES = 4096;
     private LogAdmin logService = null;

     private Vector logsCache = new Vector();

     public void applyNewMsgFilter(int newFilterLevel){

         while (realModel.getRowCount() > 0) {
              realModel.removeRow(0);
         }
         appendLogsToTable(logsCache, newFilterLevel);
         realModel.fireTableDataChanged();
     }

     public void clearTable(){

         while (realModel.getRowCount() > 0) {
             realModel.removeRow(0);
         }

         realModel.fireTableDataChanged();

         // clear the logsCache too
         logsCache.removeAllElements();
     }

    public void onConnect() {
        logService = (LogAdmin) Locator.getDefault().lookup(LogAdmin.class);
        if (logService == null) throw new IllegalStateException("cannot obtain LogAdmin remote reference");
    }

    public void onDisconnect(){
        logService = null;
    }

    public void refreshLogs(final int msgFilterLevel, final LogPanel logPane, final String msgNumSelected, final boolean restartTimer) {

        long startMsgNumber = -1;
        long endMsgNumber = -1;

        if (logsCache.size() > 0) {
            endMsgNumber = ((LogMessage) logsCache.firstElement()).getMsgNumber();
        }

        // create a worker thread to retrieve the Service statistics
        final LogsWorker logsWorker = new LogsWorker(logService, startMsgNumber, endMsgNumber) {
            public void finished() {
                updateLogsTable(getNewLogs(), msgFilterLevel);
                logPane.updateMsgTotal();
                logPane.setSelectedRow(msgNumSelected);

                if (restartTimer) {
                    logPane.getLogsRefreshTimer().start();
                }
            }
        };

        logsWorker.start();
    }

    private void updateLogsTable(Vector newLogs, int msgFilterLevel) {
        boolean cleanUp = true;

        if (newLogs.size() > 0) {

            // table clean up required only for the first time
            if (cleanUp) {
                while (realModel.getRowCount() > 0) {
                    realModel.removeRow(0);
                }
                cleanUp = false;
            }

            // populate the new logs to the table
            appendLogsToTable(newLogs, msgFilterLevel);

            while (logsCache.size() + newLogs.size() > MAX_NUMBER_OF_LOG_MESSGAES) {
                // remove the last element
                logsCache.remove(logsCache.size() - 1);
            }

            // append the old logs to the table
            appendLogsToTable(logsCache, msgFilterLevel);

            realModel.fireTableDataChanged();

            // append the old logs to the cache
            newLogs.addAll(logsCache);

            logsCache = newLogs;
        }
    }


     private boolean isFilteredMsg(LogMessage logMsg, int msgFilterLevel) {

         if ((((logMsg.getSeverity().toString().equals("FINEST")) ||
               (logMsg.getSeverity().toString().equals("FINER")) ||
               (logMsg.getSeverity().toString().equals("FINE"))) && (msgFilterLevel >= LogPanel.MSG_FILTER_LEVEL_ALL)) ||
                 (logMsg.getSeverity().toString().equals("INFO") && (msgFilterLevel >= LogPanel.MSG_FILTER_LEVEL_INFO)) ||
                 (logMsg.getSeverity().toString().equals("WARNING") && (msgFilterLevel >= LogPanel.MSG_FILTER_LEVEL_WARNING)) ||
                 (logMsg.getSeverity().toString().equals("SEVERE") && (msgFilterLevel >= LogPanel.MSG_FILTER_LEVEL_SEVERE))) {
             return true;
         } else {
             return false;
         }
     }

    private void appendLogsToTable(Vector logs, int msgFilterLevel){

         for (int i = 0; i < logs.size(); i++) {

             LogMessage logMsg = (LogMessage) logs.elementAt(i);

             if (isFilteredMsg(logMsg, msgFilterLevel)) {
                 Vector newRow = new Vector();

                 newRow.add(Long.toString(logMsg.getMsgNumber()));
                 newRow.add(logMsg.getTime());
                 newRow.add(logMsg.getSeverity());
                 newRow.add(logMsg.getMessageDetails());
                 newRow.add(logMsg.getMessageClass());
                 newRow.add(logMsg.getMessageMethod());
                 realModel.addRow(newRow);
             }
         }
    }
}

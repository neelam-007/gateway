package com.l7tech.console.table;

import com.l7tech.adminws.logging.Log;
import com.l7tech.common.util.Locator;
import com.l7tech.logging.LogMessage;
import com.l7tech.console.panels.LogPanel;

import java.util.Vector;
import java.rmi.RemoteException;

/**
 * Created by IntelliJ IDEA.
 * User: fpang
 * Date: Sep 19, 2003
 * Time: 9:38:30 AM
 * To change this template use Options | File Templates.
 */
public class FilteredLogTableModel extends FilteredDefaultTableModel{

     private static final int MAX_MESSAGE_BLOCK_SIZE = 100;
     private static final int MAX_NUMBER_OF_LOG_MESSGAES = 4096;

     private Vector logsCache = new Vector();

     public void applyNewMsgFilter(int newFilterLevel){

         while (realModel.getRowCount() > 0) {
              realModel.removeRow(0);
         }
         updateLogTable(logsCache, newFilterLevel);
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

     public void refreshLogs(int msgFilterLevel){

         Log log = (Log) Locator.getDefault().lookup(Log.class);
         if (log == null) throw new IllegalStateException("cannot obtain log remote reference");

         String[] rawLogs = new String[]{};
         long startMsgNumber = -1;
         long endMsgNumber = -1;
         boolean cleanUp = true;
         Vector newLogsCache = new Vector();
         int accumulatedNewLogs = 0;

         if(logsCache.size() > 0){
             endMsgNumber = ((LogMessage) logsCache.firstElement()).getMsgNumber();
         }

         LogMessage logMsg = null;

         do {
             try {
                 rawLogs = log.getSystemLog(startMsgNumber, endMsgNumber, MAX_MESSAGE_BLOCK_SIZE);

                 if (rawLogs.length > 0) {

                     // table clean up required only for the first time
                     if (cleanUp) {
                         while (realModel.getRowCount() > 0) {
                             realModel.removeRow(0);
                         }
                         cleanUp = false;
                     }

                     Vector newLogs = new Vector();
                     for (int i = 0; i < rawLogs.length; i++) {
                         logMsg = new LogMessage(rawLogs[i]);
                         newLogs.add(logMsg);
                     }


                     if (accumulatedNewLogs + rawLogs.length <= MAX_NUMBER_OF_LOG_MESSGAES) {
                         // update the startMsgNumber
                         startMsgNumber = logMsg.getMsgNumber();

                         newLogsCache.addAll(newLogs);
                         updateLogTable(newLogs, msgFilterLevel);
                         realModel.fireTableDataChanged();

                         accumulatedNewLogs += rawLogs.length;
                     } else {
                         for (int i = 0; accumulatedNewLogs < MAX_NUMBER_OF_LOG_MESSGAES; i++) {
                             newLogsCache.add(newLogs.get(i));
                             accumulatedNewLogs++;

                             updateLogTable(newLogs, msgFilterLevel);
                             realModel.fireTableDataChanged();
                         }
                         break;
                     }
                 }

             } catch (RemoteException e) {
                 System.err.println("Unable to retrieve logs from server");
             }
         } while (rawLogs.length == MAX_MESSAGE_BLOCK_SIZE);    // may be more messages for retrieval

         // append the old logs to the new logs
         if (accumulatedNewLogs > 0) {

             while (logsCache.size() + newLogsCache.size() > MAX_NUMBER_OF_LOG_MESSGAES) {
                 // remove the last element
                 logsCache.remove(logsCache.size() - 1);
             }
             updateLogTable(logsCache, msgFilterLevel);
             realModel.fireTableDataChanged();
             newLogsCache.addAll(logsCache);

             logsCache = newLogsCache;
         }
     }


     private boolean isFilteredMsg(LogMessage logMsg, int msgFilterLevel) {

         if ((logMsg.getSeverity().toString().equals("INFO") && (msgFilterLevel >= LogPanel.MSG_FILTER_LEVEL_INFO)) ||
                 (logMsg.getSeverity().toString().equals("WARNING") && (msgFilterLevel >= LogPanel.MSG_FILTER_LEVEL_WARNING)) ||
                 (logMsg.getSeverity().toString().equals("SEVERE") && (msgFilterLevel >= LogPanel.MSG_FILTER_LEVEL_SEVERE))) {
             return true;
         } else {
             return false;
         }
     }

    private void updateLogTable(Vector logs, int msgFilterLevel){

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

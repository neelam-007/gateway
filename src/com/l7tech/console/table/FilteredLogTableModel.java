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
     }

     public void refreshLogs(int msgFilterLevel){

         Log log = (Log) Locator.getDefault().lookup(Log.class);
         if (log == null) throw new IllegalStateException("cannot obtain log remote reference");

         String[] rawLogs = new String[]{};
         long startMsgNumber = -1;
         long endMsgNumber;
         boolean cleanUp = true;
         Vector newLogsCache = new Vector();

         if(logsCache.size() <= 0){
             // retrieve all logs
             getLogs(msgFilterLevel);
             return;
         }
         else{
             if (cleanUp) {
                // logsCache = new Vector();
                 while (realModel.getRowCount() > 0) {
                     realModel.removeRow(0);
                 }
                 cleanUp = false;
             }

             endMsgNumber = ((LogMessage) logsCache.firstElement()).getMsgNumber();

             LogMessage logMsg = null;

             do {
                 try {
                     rawLogs = log.getSystemLog(startMsgNumber, endMsgNumber, MAX_MESSAGE_BLOCK_SIZE);

                     if (rawLogs.length > 0) {
                         Vector newLogs = new Vector();
                         for (int i = 0; i < rawLogs.length; i++) {
                             logMsg = new LogMessage(rawLogs[i]);
                             newLogs.add(logMsg);
                         }

                         // update the startMsgNumber
                         startMsgNumber = logMsg.getMsgNumber();

                         newLogsCache.addAll(newLogs);
                         updateLogTable(newLogs, msgFilterLevel);
                         realModel.fireTableDataChanged();
                     }

                 } catch (RemoteException e) {
                     System.err.println("Unable to retrieve logs from server");
                 }
             } while (rawLogs.length == MAX_MESSAGE_BLOCK_SIZE);    // may be more messages for retrieval

             updateLogTable(logsCache, msgFilterLevel);
             realModel.fireTableDataChanged();
             newLogsCache.addAll(logsCache);

             logsCache = newLogsCache;
         }
     }

     public void getLogs(int msgFilterLevel) {

          Log log = (Log) Locator.getDefault().lookup(Log.class);
          if (log == null) throw new IllegalStateException("cannot obtain log remote reference");

          String[] rawLogs = new String[]{};
          int numOfMsgReceived = 0;
          boolean cleanUp = true;
          do {
              try {
                  rawLogs = log.getSystemLog(numOfMsgReceived, MAX_MESSAGE_BLOCK_SIZE);

                  numOfMsgReceived += rawLogs.length;

                  if (cleanUp) {
                      logsCache = new Vector();
                      while (realModel.getRowCount() > 0) {
                          realModel.removeRow(0);
                      }
                      cleanUp = false;
                  }

                  Vector newLogs = new Vector();
                   for (int i = 0; i < rawLogs.length; i++) {

                      LogMessage logMsg = new LogMessage(rawLogs[i]);
                      newLogs.add(logMsg);

                  }

                  logsCache.addAll(newLogs);
                  updateLogTable(newLogs, msgFilterLevel);
                  realModel.fireTableDataChanged();

              } catch (RemoteException e) {
                  System.err.println("Unable to retrieve logs from server");
              }
          } while (rawLogs.length == MAX_MESSAGE_BLOCK_SIZE);    // may be more messages for retrieval
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

package com.l7tech.console.util;


import com.l7tech.adminws.logging.Log;
import com.l7tech.logging.LogMessage;
import com.l7tech.console.table.FilteredLogTableModel;
import com.l7tech.console.panels.LogPanel;

import javax.swing.*;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: fpang
 * Date: Oct 14, 2003
 * Time: 10:26:33 AM
 * To change this template use Options | File Templates.
 */
public class LogsWorker  extends SwingWorker {

    private Log log = null;
    private long startMsgNumber = -1;
    private long endMsgNumber = -1;
    private Vector newLogs;
    static Logger logger = Logger.getLogger(LogsWorker.class.getName());

    public LogsWorker(Log logService, long startNumber, long endNumber){
        log = logService;
        startMsgNumber = startNumber;
        endMsgNumber = endNumber;
        newLogs = new Vector();
    }

    public Vector getNewLogs(){
        return newLogs;
    }

    public Object construct() {

        String[] rawLogs = new String[]{};
        int accumulatedNewLogs = 0;

        do {
            try {
                rawLogs = log.getSystemLog(startMsgNumber, endMsgNumber, FilteredLogTableModel.MAX_MESSAGE_BLOCK_SIZE);

                LogMessage logMsg = null;

                if (rawLogs.length > 0) {

                    for (int i = 0; i < rawLogs.length; i++) {
                        logMsg = new LogMessage(rawLogs[i]);
                        newLogs.add(logMsg);
                    }

                    if (accumulatedNewLogs + rawLogs.length <= FilteredLogTableModel.MAX_NUMBER_OF_LOG_MESSGAES) {
                        // update the startMsgNumber
                        startMsgNumber = logMsg.getMsgNumber();

                        accumulatedNewLogs += rawLogs.length;
                    } else {

                        // done
                        break;
                    }
                }

            } catch (RemoteException e) {
                logger.log(Level.SEVERE, "Unable to retrieve logs from server", e);
            }
        } while (rawLogs.length == FilteredLogTableModel.MAX_MESSAGE_BLOCK_SIZE);    // may be more messages for retrieval

        return newLogs;
    }

}

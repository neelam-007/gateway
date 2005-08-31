package com.l7tech.server.config;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Level;
import java.util.ArrayList;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 30, 2005
 * Time: 2:36:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class ListHandler extends Handler {
    private static ArrayList severeLogList = new ArrayList();
    private static ArrayList warningLogList = new ArrayList();
    private static ArrayList infoLogList = new ArrayList();


    public synchronized void publish(LogRecord logRecord) {
        if (logRecord != null) {
            Level level = logRecord.getLevel();
            if (level == Level.SEVERE) {
                severeLogList.add(level + ": " + logRecord.getMessage());
                Throwable t = logRecord.getThrown();
                if (t != null)  {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    PrintStream ps = new PrintStream(baos);
                    t.printStackTrace(ps);
                    ps.close();
                    severeLogList.add(baos.toString());
                }
            }
            else if (level == Level.WARNING) {
                warningLogList.add(level + ": " + logRecord.getMessage());
            }
            else { //(level == Level.INFO) or other wise
                infoLogList.add(level + ": " + logRecord.getMessage());
            }
        }
    }

    public void flush() {
        severeLogList.clear();
        warningLogList.clear();
        infoLogList.clear();
    }

    public void close() throws SecurityException {
    }

    public static ArrayList getSevereLogList() {
        return severeLogList;
    }

    public static ArrayList getWarningLogList() {
        return warningLogList;
    }

    public static ArrayList getInfoLogList() {
        return infoLogList;
    }
}

package com.l7tech.server.config;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.List;
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
    private static List<String> logList = new ArrayList<String>();

    public synchronized void publish(LogRecord logRecord) {
        if (logRecord != null) {
            Level level = logRecord.getLevel();
            if (level == Level.SEVERE) {
                logList.add(level + ": " + logRecord.getMessage());
                Throwable t = logRecord.getThrown();
                if (t != null)  {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    PrintStream ps = new PrintStream(baos);
                    t.printStackTrace(ps);
                    ps.close();
                    logList.add(baos.toString());
                }
            }
            else { 
                logList.add(level + ": " + logRecord.getMessage());
            }
        }
    }

    public void flush() {
        logList.clear();
    }

    public void close() throws SecurityException {
    }

    public static List<String> getLogList() {
        return logList;
    }
}

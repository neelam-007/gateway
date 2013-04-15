package com.l7tech.gateway.common.log;

import com.l7tech.util.Functions;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static com.l7tech.util.Functions.reduce;


public class TestHandler extends Handler {
    private static List<String> logList = new ArrayList<String>();

    public synchronized void publish(LogRecord logRecord) {
        if (logRecord != null) {
            Level level = logRecord.getLevel();
            String logMessage = MessageFormat.format(logRecord.getMessage(),logRecord.getParameters());
            if (level == Level.SEVERE) {
                logList.add(level + ": " + logMessage);
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
                logList.add(level + ": " + logMessage);
            }
        }
    }

    public void flush() {
        logList.clear();
    }

    public void close() throws SecurityException {
    }

    static public boolean isAuditPresentContaining( final String text ) {
       return reduce(logList, Boolean.FALSE, new Functions.Binary<Boolean, Boolean, String>() {
            @Override
            public Boolean call(final Boolean aBoolean, final String log) {
                return aBoolean || log.contains(text);
            }
        });
    }
}

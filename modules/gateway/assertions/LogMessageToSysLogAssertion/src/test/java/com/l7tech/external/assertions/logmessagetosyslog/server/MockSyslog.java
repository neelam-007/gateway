package com.l7tech.external.assertions.logmessagetosyslog.server;

import com.l7tech.server.log.syslog.Syslog;
import com.l7tech.server.log.syslog.SyslogSeverity;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: spreibisch
 * Date: 2/29/12
 * Time: 4:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class MockSyslog implements Syslog {

    private String logMessage;
    private SyslogSeverity severity;
    private String process;
    private long threadId;
    private long time;

    @Override
    public void log(SyslogSeverity severity, String process, long threadId, long time, String message) {
        this.severity = severity;
        this.process = process;
        this.threadId = threadId;
        this.time = time;
        logMessage = message;
    }

    @Override
    public void close() throws IOException {
        // ignore
    }

    public String getLogMessage() {
        return logMessage;
    }

    public SyslogSeverity getSeverity() {
        return severity;
    }

    public String getProcess() {
        return process;
    }

    public long getThreadId() {
        return threadId;
    }

    public long getTime() {
        return time;
    }
}
package com.l7tech.external.assertions.logmessagetosyslog.server;

import com.l7tech.server.log.syslog.Syslog;
import com.l7tech.server.log.syslog.SyslogProtocol;
import com.l7tech.server.log.syslog.TestingSyslogManager;

/**
 * Created by IntelliJ IDEA.
 * User: spreibisch
 * Date: 2/29/12
 * Time: 4:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class MockSyslogManager extends TestingSyslogManager {

    private MockSyslog syslogMock;

    @Override
    public Syslog getSyslog(SyslogProtocol protocol, String[][] syslogHosts, String format, String timeZone, int facility, String host, String charset, String lineDelimiter, String sslKeystoreAlias, String sslKeystoreId) {
        syslogMock = new MockSyslog();
        return syslogMock;
    }

    public MockSyslog getSyslogMock() {
        return syslogMock;
    }
}
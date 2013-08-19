package com.l7tech.server.log.syslog;

import com.l7tech.objectmodel.Goid;
import com.l7tech.server.log.syslog.impl.MinaManagedSyslog;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;

/**
 * SyslogManager subclass used only for testing a Syslog sink configuration.  The main difference is that
 * the getSyslog() will create a new
 * ManagedSyslog each time
 *
 * User: vchan
 */
public class TestingSyslogManager extends SyslogManager {

    private final ArrayList<Syslog> syslogList;
    private int testingHost;

    public TestingSyslogManager() {
        super();
        this.syslogList = new ArrayList<Syslog>(10);
    }

    @Override
    Syslog getSyslog(SyslogProtocol protocol, SocketAddress[] addresses, String format, String timeZone, int facility, String host, String charset, String delimiter, String sslKeystoreAlias, Goid sslKeystoreId) {

        if (protocol == null) throw new IllegalArgumentException("protocol must not be null");

        // since we're testing each individual address, only use the specified address one
        SocketAddress[] testAddress = new SocketAddress[] { addresses[testingHost] };

        // create the syslog
        ManagedSyslog msyslog = new MinaManagedSyslog(protocol, testAddress, sslKeystoreAlias, sslKeystoreId);
        msyslog.init(this);
        msyslog.reference();

        // add to list to track the resource
        Syslog result = msyslog.getSylog(new ManagedSyslog.SyslogFormat(format, timeZone, charset, delimiter, 1024),
                                facility,
                                host);
        syslogList.add(result);
        return result;
    }

    public void setTestingHost(int newIndex) {
        this.testingHost = newIndex;
    }

    @Override
    public void close() {
        for (Syslog msyslog : syslogList) {
            try {
                msyslog.close();
            } catch (IOException ioe) {
                // ignore + continue
            }
        }
        super.close();
    }
}

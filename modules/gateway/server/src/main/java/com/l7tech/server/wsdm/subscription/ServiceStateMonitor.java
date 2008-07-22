package com.l7tech.server.wsdm.subscription;

/**
 * Callback definitions for objects monitoring service states
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 4, 2008<br/>
 */
public interface ServiceStateMonitor {
    void onServiceDisabled(long serviceoid);

    void onServiceEnabled(long serviceoid);

    void onServiceCreated(long serviceoid);

    void onServiceDeleted(long serviceoid);
}

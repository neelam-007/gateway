package com.l7tech.server.wsdm.subscription;

import com.l7tech.objectmodel.Goid;

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
    void onServiceDisabled(Goid serviceGoid);

    void onServiceEnabled(Goid serviceGoid);

    void onServiceCreated(Goid serviceGoid);

    void onServiceDeleted(Goid serviceGoid);
}

package com.l7tech.external.assertions.snmpagent.server;

import com.l7tech.gateway.common.mapping.MessageContextMapping;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.service.ServiceMetricsServices;

import java.util.List;

/**
 * User: rseminoff
 * Date: 5/10/12
 */
public class MockServiceMetricsServices implements ServiceMetricsServices {

    @Override
    public boolean isEnabled() {
        System.out.println("*** CALL *** MockServiceMetricsServices: isEnabled()");
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void trackServiceMetrics(Goid serviceGoid) {
        System.out.println("*** CALL *** MockServiceMetricsServices: trackServiceMetrics()");
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addRequest(Goid serviceGoid, String operation, User authorizedUser, List<MessageContextMapping> mappings, boolean authorized, boolean completed, int frontTime, int backTime) {
        System.out.println("*** CALL *** MockServiceMetricsServices: addRequest()");
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getFineInterval() {
        return MockSnmpValues.FINE_MS_VALUE;  // In milliseconds = 10 minutes.
    }
}

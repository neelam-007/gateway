package com.l7tech.server.uddi;

import com.l7tech.gateway.common.uddi.UDDIServiceControl;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.EntityManagerStub;

import java.util.Collection;

/**
 * This was created: 11/1/13 as 1:43 PM
 *
 * @author Victor Kazakov
 */
public class UDDIServiceControlManagerStub extends EntityManagerStub<UDDIServiceControl, EntityHeader> implements UDDIServiceControlManager {
    @Override
    public UDDIServiceControl findByPublishedServiceGoid(Goid serviceGoid) throws FindException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<UDDIServiceControl> findByUDDIRegistryGoid(Goid registryGoid) throws FindException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<UDDIServiceControl> findByUDDIRegistryAndServiceKey(Goid registryGoid, String serviceKey, Boolean uddiControlled) throws FindException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<UDDIServiceControl> findByUDDIRegistryAndMetricsState(Goid registryGoid, boolean metricsEnabled) throws FindException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<UDDIServiceControl> findByUDDIServiceKey(String serviceKey) throws FindException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.server.uddi;

import com.l7tech.gateway.common.uddi.UDDIPublishStatus;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.HibernateEntityManager;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class UDDIPublishStatusManagerImpl extends HibernateEntityManager<UDDIPublishStatus, EntityHeader> implements UDDIPublishStatusManager  {

    @Override
    public Class<? extends Entity> getImpClass() {
        return UDDIPublishStatus.class;
    }

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.OTHER;
    }

    @Override
    protected Collection<Map<String, Object>> getUniqueConstraints(UDDIPublishStatus entity) {
        Map<String,Object> serviceOidMap = new HashMap<String, Object>();
        serviceOidMap.put("uddiProxiedServiceInfoGoid", entity.getUddiProxiedServiceInfoGoid());
        return Arrays.asList(serviceOidMap);
    }

    @Override
    public UDDIPublishStatus findByProxiedSerivceInfoGoid(Goid proxiedServiceGoid) throws FindException {
        return findByUniqueKey( "uddiProxiedServiceInfoGoid", proxiedServiceGoid );
    }
}

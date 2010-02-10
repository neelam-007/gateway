/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.server.uddi;

import com.l7tech.gateway.common.uddi.UDDIServiceControlRuntime;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.HibernateEntityManager;

import java.util.Map;
import java.util.Collection;
import java.util.HashMap;
import java.util.Arrays;

public class UDDIServiceControlRuntimeManagerImpl extends HibernateEntityManager<UDDIServiceControlRuntime,EntityHeader> implements UDDIServiceControlRuntimeManager {

    @Override
    public Class<? extends Entity> getImpClass() {
        return UDDIServiceControlRuntime.class;
    }

    @Override
    public UDDIServiceControlRuntime findByServiceControlOid( final long uddiServiceControlOid) throws FindException {
        return findByUniqueKey("uddiServiceControlOid", uddiServiceControlOid);
    }

    //- PROTECTED

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.OTHER;
    }

    @Override
    protected Collection<Map<String, Object>> getUniqueConstraints( final UDDIServiceControlRuntime controlMonitorRuntime ) {
        Map<String,Object> serviceOidMap = new HashMap<String, Object>();
        serviceOidMap.put( "uddiServiceControlOid", controlMonitorRuntime.getUddiServiceControlOid() );
        return Arrays.asList(serviceOidMap);
    }

}

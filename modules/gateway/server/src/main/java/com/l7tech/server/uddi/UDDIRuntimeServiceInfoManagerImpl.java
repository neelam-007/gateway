package com.l7tech.server.uddi;

import com.l7tech.server.HibernateEntityManager;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Entity;

import java.util.Map;
import java.util.Collection;
import java.util.HashMap;
import java.util.Arrays;

/**
 *
 */
public class UDDIRuntimeServiceInfoManagerImpl extends HibernateEntityManager<UDDIRuntimeServiceInfo, EntityHeader> implements UDDIRuntimeServiceInfoManager {

    //- PUBLIC

    @Override
    public Class<? extends Entity> getImpClass() {
        return UDDIRuntimeServiceInfo.class;
    }

    //- PROTECTED

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.OTHER;
    }

    @Override
    protected Collection<Map<String, Object>> getUniqueConstraints( final UDDIRuntimeServiceInfo uddiRuntimeServiceInfo ) {
        Map<String,Object> serviceOidMap = new HashMap<String, Object>();
        serviceOidMap.put( "publishedServiceOid", uddiRuntimeServiceInfo.getPublishedServiceOid() );
        return Arrays.asList(serviceOidMap);
    }

}


package com.l7tech.server.uddi;

import com.l7tech.gateway.common.uddi.UDDIProxiedService;
import com.l7tech.objectmodel.*;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.common.uddi.guddiv3.BusinessService;
import com.l7tech.common.uddi.guddiv3.TModel;
import com.l7tech.uddi.UDDIClient;
import com.l7tech.uddi.UDDIException;
import com.l7tech.uddi.BusinessServicePublisher;

import java.util.*;
import java.util.logging.Logger;

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
public class UDDIProxiedServiceManagerImpl extends HibernateEntityManager<UDDIProxiedService, EntityHeader>
        implements UDDIProxiedServiceManager{

    private static final Logger logger = Logger.getLogger(UDDIProxiedServiceManagerImpl.class.getName());

    public UDDIProxiedServiceManagerImpl() {
    }

    @Override
    public String getTableName() {
        return "uddi_proxied_service";
    }
    
    @Override
    public Class<? extends Entity> getInterfaceClass() {
        return UDDIProxiedService.class;
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return UDDIProxiedService.class;
    }

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.NONE;
    }

    @Override
    protected Collection<Map<String, Object>> getUniqueConstraints(final UDDIProxiedService uddiProxiedService) {
        Map<String,Object> serviceOidMap = new HashMap<String, Object>();
        serviceOidMap.put("published_service_oid", uddiProxiedService.getServiceOid());

        Map<String,Object> keywordMap = new HashMap<String, Object>();
        keywordMap.put("general_keyword", uddiProxiedService.getGeneralKeywordServiceIdentifier());
        
        return Arrays.asList(serviceOidMap, keywordMap);


    }

    @Override
    public long saveUDDIProxiedService(final UDDIProxiedService uddiProxiedService,
                                       final UDDIClient uddiClient,
                                       final List<BusinessService> businessServices,
                                       final Map<String, TModel> dependentTModels)
            throws SaveException, VersionException, UDDIException {

        BusinessServicePublisher businessServicePublisher = new BusinessServicePublisher();
        businessServicePublisher.publishServicesToUDDIRegistry(uddiClient, businessServices, dependentTModels);

        return super.save(uddiProxiedService);
    }

    @Override
    public void updateUDDIProxiedService(final UDDIProxiedService uddiProxiedService,
                                         final UDDIClient uddiClient,
                                         final List<BusinessService> businessServices,
                                         final Map<String, TModel> dependentTModels)
            throws UpdateException, VersionException {

        //todo implement according to http://sarek.l7tech.com/mediawiki/index.php?title=CentraSite_ActiveSOA_Design#Strategy_for_publishing_and_updating_BusinessServices_to_UDDI
        super.update(uddiProxiedService);
    }
}

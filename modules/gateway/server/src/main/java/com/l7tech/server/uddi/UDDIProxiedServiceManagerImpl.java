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

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
public class UDDIProxiedServiceManagerImpl extends HibernateEntityManager<UDDIProxiedService, EntityHeader>
        implements UDDIProxiedServiceManager{

    public UDDIProxiedServiceManagerImpl() {
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return UDDIProxiedService.class;
    }

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.OTHER;
    }

    @Override
    protected Collection<Map<String, Object>> getUniqueConstraints(final UDDIProxiedService uddiProxiedService) {
        Map<String,Object> serviceOidMap = new HashMap<String, Object>();
        serviceOidMap.put("serviceOid", uddiProxiedService.getServiceOid());

        Map<String,Object> keywordMap = new HashMap<String, Object>();
        keywordMap.put("generalKeywordServiceIdentifier", uddiProxiedService.getGeneralKeywordServiceIdentifier());
        
        return Arrays.asList(serviceOidMap, keywordMap);


    }

    @Override
    public UDDIProxiedService findByPublishedServiceOid( final long publishedServiceOid ) throws FindException {
        return findByUniqueKey( "serviceOid", publishedServiceOid );
    }

    @Override
    public long saveUDDIProxiedService(final UDDIProxiedService uddiProxiedService,
                                       final UDDIClient uddiClient,
                                       final List<BusinessService> businessServices,
                                       final Map<String, TModel> dependentTModels,
                                       final String generalKeyword)
            throws SaveException, VersionException, UDDIException {

        BusinessServicePublisher businessServicePublisher = new BusinessServicePublisher();
        businessServicePublisher.publishServicesToUDDIRegistry(uddiClient, businessServices, dependentTModels, generalKeyword);

        return super.save(uddiProxiedService);
    }

    /**
     * See http://sarek.l7tech.com/mediawiki/index.php?title=CentraSite_ActiveSOA_Design#Strategy_for_publishing_and_updating_BusinessServices_to_UDDI
     *
     */
    @Override
    public void updateUDDIProxiedService(final UDDIProxiedService uddiProxiedService,
                                         final UDDIClient uddiClient,
                                         final List<BusinessService> wsdlBusinessServices,
                                         final Map<String, TModel> wsdlDependentTModels,
                                         final List<BusinessService> uddiBusinessServices,
                                         final Map<String, TModel> uddiDependentTModels, String generalKeyword)
            throws UpdateException, VersionException, UDDIException {

        BusinessServicePublisher businessServicePublisher = new BusinessServicePublisher();
        businessServicePublisher.updateServicesToUDDIRegistry(
                uddiClient, wsdlBusinessServices, wsdlDependentTModels, uddiBusinessServices, uddiDependentTModels, generalKeyword);

        super.update(uddiProxiedService);
    }
}

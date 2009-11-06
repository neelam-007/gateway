/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.server.uddi;

import com.l7tech.gateway.common.uddi.UDDIProxiedServiceInfo;
import com.l7tech.gateway.common.uddi.UDDIProxiedService;
import com.l7tech.objectmodel.*;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.uddi.*;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UDDIProxiedServiceInfoManagerImpl extends HibernateEntityManager<UDDIProxiedServiceInfo, EntityHeader>
implements UDDIProxiedServiceInfoManager{

    //- PUBLIC

    public UDDIProxiedServiceInfoManagerImpl( final UDDIHelper uddiHelper ) {
        this.uddiHelper = uddiHelper;
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return UDDIProxiedServiceInfo.class;
    }

    @Override
    public void saveUDDIProxiedServiceInfo(final UDDIProxiedServiceInfo uddiProxiedServiceInfo)
            throws SaveException{

        //first thing to do - persit the entity
        //todo set the status to publishing
        save(uddiProxiedServiceInfo);
    }

    @Override
    public UDDIProxiedServiceInfo findByPublishedServiceOid( final long publishedServiceOid ) throws FindException {
        return findByUniqueKey( "publishedServiceOid", publishedServiceOid );
    }

    @Override
    public Collection<UDDIProxiedServiceInfo> findByUDDIRegistryAndMetricsState( final long registryOid,
                                                                             final boolean metricsEnabled ) throws FindException {
        final Map<String,Object> matchMap = new HashMap<String,Object>();
        matchMap.put( "uddiRegistryOid", registryOid );
        matchMap.put( "metricsEnabled", metricsEnabled );
        return findMatching( Collections.singletonList( matchMap ) );
    }

    @Override
    public void updateUDDIProxiedService(final UDDIProxiedServiceInfo uddiProxiedServiceInfo,
                                         final Wsdl wsdl,
                                         final UDDIClientConfig uddiClientConfig)
            throws UpdateException, VersionException, UDDIException, FindException, WsdlToUDDIModelConverter.MissingWsdlReferenceException {

        if(uddiProxiedServiceInfo == null) throw new NullPointerException("uddiProxiedServiceInfo cannot be null");
        if(uddiClientConfig == null) throw new NullPointerException("uddiClientConfig cannot be null");
        if(wsdl == null) throw new NullPointerException("wsdl cannot be null");

        final String protectedServiceExternalURL = uddiHelper.getExternalUrlForService(uddiProxiedServiceInfo.getPublishedServiceOid());
        //protected service gateway external wsdl url
        final String protectedServiceWsdlURL = uddiHelper.getExternalWsdlUrlForService(uddiProxiedServiceInfo.getPublishedServiceOid());

        //Get existing services
        Set<UDDIProxiedService> allProxiedServices = uddiProxiedServiceInfo.getProxiedServices();
        Set<String> serviceKeys = new HashSet<String>();
        for(UDDIProxiedService ps: allProxiedServices){
            serviceKeys.add(ps.getUddiServiceKey());
        }

        BusinessServicePublisher businessServicePublisher = new BusinessServicePublisher(wsdl, uddiProxiedServiceInfo.getPublishedServiceOid(), uddiClientConfig);

        final Pair<Set<String>, Set<UDDIBusinessService>> deletedAndNewServices = businessServicePublisher.updateServicesToUDDIRegistry(
                protectedServiceExternalURL, protectedServiceWsdlURL, uddiProxiedServiceInfo.getUddiBusinessKey(), serviceKeys);

        //now manage db entities
        Set<String> deleteSet = deletedAndNewServices.left;
        //update all child entities
        Set<UDDIProxiedService> removeSet = new HashSet<UDDIProxiedService>();
        for(String deleteServiceKey: deleteSet){
            for(UDDIProxiedService proxiedService: allProxiedServices){
                if(proxiedService.getUddiServiceKey().equals(deleteServiceKey)){
                    removeSet.add(proxiedService);
                    logger.log(Level.INFO, "Deleting UDDIProxiedService for serviceKey: " + deleteServiceKey);
                }
            }
        }

        uddiProxiedServiceInfo.getProxiedServices().removeAll(removeSet);

        //create required new UDDIProxiedServices
        Set<UDDIBusinessService> newlyCreatedServices = deletedAndNewServices.right;
        for(UDDIBusinessService bs: newlyCreatedServices){
            final UDDIProxiedService proxiedService =
                    new UDDIProxiedService(bs.getServiceKey(), bs.getServiceName(), bs.getWsdlServiceName());
            proxiedService.setUddiProxiedServiceInfo(uddiProxiedServiceInfo);
            uddiProxiedServiceInfo.getProxiedServices().add(proxiedService);
        }

        try {
            update(uddiProxiedServiceInfo);
        } catch (UpdateException e) {
            //rollback UDDI updates
            //this is difficult as we may have just made any number of changes. The initial save case is easy
            //as we delete everything.
            //todo audit
            logger.log(Level.WARNING, "Could not update UDDiProxiedServiceInfo. Gateway may be out of sync with UDDI: "
                    + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            throw e;
        }
    }

    //- PROTECTED

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.OTHER;
    }

    @Override
    protected Collection<Map<String, Object>> getUniqueConstraints(final UDDIProxiedServiceInfo proxiedServiceInfo) {
        Map<String,Object> serviceOidMap = new HashMap<String, Object>();
        serviceOidMap.put("publishedServiceOid", proxiedServiceInfo.getPublishedServiceOid());
        return Arrays.asList(serviceOidMap);
    }

    //- PRIVATE

    private  static final Logger logger = Logger.getLogger(UDDIProxiedServiceInfoManagerImpl.class.getName());

    private final UDDIHelper uddiHelper;

}

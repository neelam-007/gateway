/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.server.uddi;

import com.l7tech.gateway.common.uddi.UDDIProxiedServiceInfo;
import com.l7tech.gateway.common.uddi.UDDIProxiedService;
import com.l7tech.gateway.common.admin.UDDIRegistryAdmin;
import com.l7tech.objectmodel.*;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.uddi.*;
import com.l7tech.common.uddi.guddiv3.BusinessService;
import com.l7tech.common.uddi.guddiv3.TModel;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.util.Pair;
import com.l7tech.util.ExceptionUtils;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UDDIProxiedServiceInfoManagerImpl extends HibernateEntityManager<UDDIProxiedServiceInfo, EntityHeader>
implements UDDIProxiedServiceInfoManager{

    protected static final Logger logger = Logger.getLogger(UDDIProxiedServiceInfoManagerImpl.class.getName());

    public UDDIProxiedServiceInfoManagerImpl(final UDDIHelper uddiHelper) {
        this.uddiHelper = uddiHelper;
    }

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

    @Override
    public Class<? extends Entity> getImpClass() {
        return UDDIProxiedServiceInfo.class;
    }

    @Override
    public long saveUDDIProxiedServiceInfo(final UDDIProxiedServiceInfo uddiProxiedServiceInfo,
                                           final UDDIClient uddiClient,
                                           final Wsdl wsdl)
            throws SaveException, VersionException, UDDIException, FindException, WsdlToUDDIModelConverter.MissingWsdlReferenceException {
        WsdlToUDDIModelConverter modelConverter = getConvertedWsdlToUDDIModel(wsdl,
                uddiProxiedServiceInfo.getPublishedServiceOid(), uddiProxiedServiceInfo.getUddiBusinessKey());

        final List<BusinessService> wsdlBusinessServices = modelConverter.getBusinessServices();
        final Map<String, TModel> wsdlDependentTModels = modelConverter.getKeysToPublishedTModels();
        final Map<BusinessService, String> serviceToWsdlServiceName = modelConverter.getServiceKeyToWsdlServiceNameMap();

        BusinessServicePublisher businessServicePublisher = new BusinessServicePublisher();
        businessServicePublisher.publishServicesToUDDIRegistry(uddiClient, wsdlBusinessServices, wsdlDependentTModels);

        //Create all required UDDIProxiedService
        for(BusinessService bs: wsdlBusinessServices){
            final UDDIProxiedService proxiedService = new UDDIProxiedService(bs.getServiceKey(),
                    bs.getName().get(0).getValue(), serviceToWsdlServiceName.get(bs));
            //both parent and child records must be set before save
            uddiProxiedServiceInfo.getProxiedServices().add(proxiedService);
            proxiedService.setProxiedServiceInfo(uddiProxiedServiceInfo);
        }

        try {
            return save(uddiProxiedServiceInfo);
        } catch (SaveException e) {
            logger.log(Level.WARNING, "Could not save UDDIProxiedServiceInfo: " + e.getMessage());
            try {
                logger.log(Level.WARNING, "Attempting to rollback UDDI updates");
                //Attempt to roll back UDDI updates
                uddiClient.deleteBusinessServices(wsdlBusinessServices);
                logger.log(Level.WARNING, "UDDI updates rolled back successfully");
            } catch (UDDIException e1) {
                logger.log(Level.WARNING, "Could not rollback UDDI updates: " + e1.getMessage());
            }
            throw e;
        }
    }

    @Override
    public UDDIProxiedServiceInfo findByPublishedServiceOid( final long publishedServiceOid ) throws FindException {
        return findByUniqueKey( "publishedServiceOid", publishedServiceOid );
    }

    @Override
    public void updateUDDIProxiedService(final UDDIProxiedServiceInfo uddiProxiedServiceInfo,
                                         final UDDIClient uddiClient,
                                         final Wsdl wsdl)
            throws UpdateException, VersionException, UDDIException, FindException, WsdlToUDDIModelConverter.MissingWsdlReferenceException {

        if(uddiProxiedServiceInfo == null) throw new NullPointerException("uddiProxiedServiceInfo cannot be null");
        if(uddiClient == null) throw new NullPointerException("uddiClient cannot be null");
        if(wsdl == null) throw new NullPointerException("wsdl cannot be null");

        WsdlToUDDIModelConverter modelConverter = getConvertedWsdlToUDDIModel(wsdl,
                uddiProxiedServiceInfo.getPublishedServiceOid(), uddiProxiedServiceInfo.getUddiBusinessKey());

        final List<BusinessService> wsdlBusinessServices = modelConverter.getBusinessServices();
        final Map<String, TModel> wsdlDependentTModels = modelConverter.getKeysToPublishedTModels();
        final Map<BusinessService, String> serviceToWsdlServiceName = modelConverter.getServiceKeyToWsdlServiceNameMap();

        //Get existing services
        Set<UDDIProxiedService> allProxiedServices = uddiProxiedServiceInfo.getProxiedServices();
        Set<String> serviceKeys = new HashSet<String>();
        for(UDDIProxiedService ps: allProxiedServices){
            serviceKeys.add(ps.getUddiServiceKey());
        }

        BusinessServicePublisher businessServicePublisher = new BusinessServicePublisher();
        //update uddi
        businessServicePublisher.updateServicesToUDDIRegistry(
                uddiClient, serviceKeys, wsdlBusinessServices, wsdlDependentTModels);

        //now manage db entities
        Set<BusinessService> deleteSet = businessServicePublisher.getServiceDeleteSet();
        //update all child entities
        Set<UDDIProxiedService> removeSet = new HashSet<UDDIProxiedService>();
        for(BusinessService bs: deleteSet){
            for(UDDIProxiedService proxiedService: allProxiedServices){
                if(proxiedService.getUddiServiceKey().equals(bs.getServiceKey())){
                    removeSet.add(proxiedService);
                    logger.log(Level.INFO, "Deleting UDDIProxiedService for serviceKey: " + bs.getServiceKey());
                }
            }
        }

        uddiProxiedServiceInfo.getProxiedServices().removeAll(removeSet);

        //create required new UDDIProxiedServices
        List<BusinessService> newlyCreatedServices = businessServicePublisher.getNewlyPublishedServices();
        for(BusinessService bs: newlyCreatedServices){
            final UDDIProxiedService proxiedService =
                    new UDDIProxiedService(bs.getServiceKey(), bs.getName().get(0).getValue(), serviceToWsdlServiceName.get(bs));
            proxiedService.setProxiedServiceInfo(uddiProxiedServiceInfo);
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

    private WsdlToUDDIModelConverter getConvertedWsdlToUDDIModel( final Wsdl wsdl,
                                                                  final long publishedServiceOid,
                                                                  final String businessKey)
            throws WsdlToUDDIModelConverter.MissingWsdlReferenceException {
        final String protectedServiceExternalURL = uddiHelper.getExternalUrlForService(publishedServiceOid);
        //protected service gateway external wsdl url
        final String protectedServiceWsdlURL = uddiHelper.getExternalWsdlUrlForService(publishedServiceOid);

        WsdlToUDDIModelConverter modelConverter = new WsdlToUDDIModelConverter(wsdl, protectedServiceWsdlURL,
                protectedServiceExternalURL, businessKey, publishedServiceOid);
        modelConverter.convertWsdlToUDDIModel();
        return modelConverter;
    }

    //PRIVATE

    final private UDDIHelper uddiHelper;
}

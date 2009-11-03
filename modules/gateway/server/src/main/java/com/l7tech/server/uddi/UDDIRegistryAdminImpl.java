/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Oct 14, 2009
 * Time: 12:42:53 PM
 */
package com.l7tech.server.uddi;

import com.l7tech.gateway.common.admin.UDDIRegistryAdmin;
import com.l7tech.gateway.common.uddi.UDDIRegistry;
import com.l7tech.gateway.common.uddi.UDDIProxiedService;
import com.l7tech.gateway.common.uddi.UDDIServiceControl;
import com.l7tech.gateway.common.uddi.UDDIProxiedServiceInfo;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.uddi.*;
import com.l7tech.objectmodel.*;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.ServerConfig;
import com.l7tech.wsdl.Wsdl;

import javax.wsdl.WSDLException;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

public class UDDIRegistryAdminImpl implements UDDIRegistryAdmin {
    protected static final Logger logger = Logger.getLogger(UDDIRegistryAdminImpl.class.getName());

    final private UDDIRegistryManager uddiRegistryManager;
    final private UDDIProxiedServiceInfoManager uddiProxiedServiceInfoManager;
    final private UDDIServiceControlManager uddiServiceControlManager;
    final private UDDICoordinator uddiCoordinator;
    final private ServiceCache serviceCache;
    final private ServiceManager serviceManager;

    public UDDIRegistryAdminImpl(final UDDIRegistryManager uddiRegistryManager,
                                 final UDDIServiceControlManager uddiServiceControlManager,
                                 final UDDICoordinator uddiCoordinator,
                                 final ServiceCache serviceCache,
                                 final UDDIProxiedServiceInfoManager uddiProxiedServiceInfoManager,
                                 final ServiceManager serviceManager) {
        this.uddiRegistryManager = uddiRegistryManager;
        this.uddiServiceControlManager = uddiServiceControlManager;
        this.uddiCoordinator = uddiCoordinator;
        this.serviceCache = serviceCache;
        this.uddiProxiedServiceInfoManager = uddiProxiedServiceInfoManager;
        this.serviceManager = serviceManager;
    }

    @Override
    public long saveUDDIRegistry(final UDDIRegistry uddiRegistry) throws SaveException, UpdateException {
        if(uddiRegistry.getOid() == PersistentEntity.DEFAULT_OID){
            logger.info("Saving UDDI Registry '" + uddiRegistry.getName()+"'");
            uddiRegistryManager.save(uddiRegistry);
        }else{
            logger.info("Updating UDDI Registry '" + uddiRegistry.getName()+"' oid = " + uddiRegistry.getOid());
            uddiRegistryManager.update(uddiRegistry);
        }

        return uddiRegistry.getOid();
    }

    @Override
    public void deleteUDDIRegistry(final long oid) throws DeleteException, FindException, UDDIException, UDDIRegistryNotEnabledException {

        UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(oid);
        if(uddiRegistry == null) throw new FindException("Could not find UDDI Registry to delete");

        //delete all UDDIProxiedService's which belong to this registry
        Collection<UDDIProxiedServiceInfo> allProxiedServices = uddiRegistryManager.findAllByRegistryOid(uddiRegistry.getOid());
        for(UDDIProxiedServiceInfo proxiedService: allProxiedServices){
            deleteGatewayWsdlFromUDDI(proxiedService);
        }

        logger.log(Level.INFO, "Deleting UDDI Registry oid = " + oid);
        uddiRegistryManager.delete(oid);
    }

    @Override
    public Collection<UDDIRegistry> findAllUDDIRegistries() throws FindException {
        return uddiRegistryManager.findAll();
    }

    @Override
    public UDDIRegistry findByPrimaryKey(long registryOid) throws FindException {
        return uddiRegistryManager.findByPrimaryKey(registryOid);
    }

    @Override
    public boolean subscriptionNotificationsAvailable( final long registryOid ) {
        return uddiCoordinator.isNotificationServiceAvailable( registryOid );
    }

    @Override
    public void testUDDIRegistryAuthentication(final UDDIRegistry uddiRegistry) throws UDDIException {
        final UDDIClient uddiClient = getUDDIClient(uddiRegistry);

        try {
            uddiClient.authenticate();
        } catch (UDDIException e) {
            //the original exception may not be serializable
            throw new UDDIException(ExceptionUtils.getMessage(e));
        }
    }

    private UDDIClient getUDDIClient(final UDDIRegistry uddiRegistry) {
        return UDDIHelper.newUDDIClient( uddiRegistry );
    }

    @Override
    public UDDIProxiedServiceInfo getUDDIProxiedServiceInfo(long serviceOid) throws FindException {
        return uddiProxiedServiceInfoManager.findByPublishedServiceOid(serviceOid);
    }

    @Override
    public String deleteGatewayWsdlFromUDDI(UDDIProxiedServiceInfo proxiedServiceInfo)
            throws FindException, DeleteException, UDDIRegistryNotEnabledException {
        final UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(proxiedServiceInfo.getUddiRegistryOid());
        if(!uddiRegistry.isEnabled()) throw new UDDIRegistryNotEnabledException("UDDIRegistry is not enabled. Cannot use");

        final Set<UDDIProxiedService> proxiedServices = proxiedServiceInfo.getProxiedServices();
        final Set<String> keysToDelete = new HashSet<String>();
        for(UDDIProxiedService proxiedService: proxiedServices){
            keysToDelete.add(proxiedService.getUddiServiceKey());
        }
        final UDDIClient uddiClient = getUDDIClient(uddiRegistry);

        String errorMsg = null;
        try {
            uddiClient.deleteBusinessServicesByKey(keysToDelete);
            logger.log(Level.INFO, "Successfully deleted published Gateway WSDL from UDDI Registry");
        } catch (UDDIException e) {
            errorMsg = "Errors deleting published Business Services from UDDI: " + ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, errorMsg, ExceptionUtils.getDebugException(e));
        }

        uddiProxiedServiceInfoManager.delete(proxiedServiceInfo.getOid());//cascade delete
        logger.log(Level.INFO, "Deleted UDDIProxiedServiceInfo");
        return errorMsg;
    }

    @Override
    public void updateProxiedServiceOnly(UDDIProxiedServiceInfo proxiedServiceInfo) throws UpdateException, FindException {
        final UDDIProxiedServiceInfo original = uddiProxiedServiceInfoManager.findByPrimaryKey(proxiedServiceInfo.getOid());
        if(original == null) throw new FindException("Could not find UDDIProxiedServiceInfo with id: " + proxiedServiceInfo);

        proxiedServiceInfo.throwIfFinalPropertyModified(original);

        if(original.isUpdateProxyOnLocalChange() == proxiedServiceInfo.isUpdateProxyOnLocalChange()) return;

        uddiProxiedServiceInfoManager.update(proxiedServiceInfo);
    }

    @Override
    public Collection<UDDIProxiedServiceInfo> getAllProxiedServicesForRegistry(long registryOid) throws FindException {
        return uddiRegistryManager.findAllByRegistryOid(registryOid);
    }

    @Override
    public long saveUDDIServiceControlOnly( final UDDIServiceControl uddiServiceControl )
            throws UpdateException, SaveException, FindException {
        if ( uddiServiceControl.getOid() == UDDIServiceControl.DEFAULT_OID ){
            final PublishedService service = serviceCache.getCachedService(uddiServiceControl.getPublishedServiceOid());
            final Wsdl wsdl;
            try {
                wsdl = service.parsedWsdl();
            } catch (WSDLException e) {
                final String msg = "Could not obtain WSDL for associated published service: " + ExceptionUtils.getMessage(e);
                logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
                throw new SaveException(msg);
            }

            //validate that an end point can be found
            final String endPoint = uddiServiceControl.getAccessPointUrl();
            //todo take appropriate action as WSDL definition may have just changed - UDDICoordinator
//            try {
//                endPoint = UDDIUtilities.extractEndPointFromWsdl(wsdl, uddiServiceControl.getWsdlServiceName(),
//                        uddiServiceControl.getWsdlPortName(), uddiServiceControl.getWsdlPortBinding());
//            } catch (UDDIUtilities.WsdlEndPointNotFoundException e) {
//                final String msg = "Cannot save UDDIServiceControl as no valid endpoint can be found from the Published Service's WSDL: "
//                        + ExceptionUtils.getMessage(e);
//                logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
//                throw new SaveException(msg);
//            }

            //make sure that we are not monitoring an endpoint of this SSG
            if(UDDIUtilities.isGatewayUrl(endPoint, ServerConfig.getInstance().getHostname())){
                final String msg = "Cannot save UDDIServiceControl as the WSDL endpoint routes back to the SecureSpan Gateway";
                logger.log(Level.WARNING, msg);
                throw new SaveException(msg);
            }

            final UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(uddiServiceControl.getUddiRegistryOid());
            if(uddiRegistry == null) throw new FindException("Cannot find UDDIRegistry");

            final UDDIClient uddiClient = getUDDIClient(uddiRegistry);
            try {
                final String businessName = uddiClient.getBusinessEntityName(uddiServiceControl.getUddiBusinessKey());
                uddiServiceControl.setUddiBusinessName(businessName);
            } catch (UDDIException e) {
                final String msg = "Cannot find BusinessEntity with businessKey: " + uddiServiceControl.getUddiBusinessKey();
                logger.log(Level.WARNING, msg, e);
                throw new SaveException(msg);
            }
            return uddiServiceControlManager.save(uddiServiceControl);
        }else{
            UDDIServiceControl original = uddiServiceControlManager.findByPrimaryKey(uddiServiceControl.getOid());
            if(original == null) throw new FindException("Cannot find UDDIServiceControl with oid: " + uddiServiceControl.getOid());
            uddiServiceControl.throwIfFinalPropertyModified(original);
            uddiServiceControlManager.update( uddiServiceControl );
            return uddiServiceControl.getOid();
        }
    }

    @Override
    public void deleteUDDIServiceControl(long uddiServiceControlOid) throws FindException, DeleteException {
        uddiServiceControlManager.delete(uddiServiceControlOid);
    }

    @Override
    public UDDIServiceControl getUDDIServiceControl( final long serviceOid ) throws FindException {
        return uddiServiceControlManager.findByPublishedServiceOid( serviceOid );
    }

    @Override
    public Collection<UDDIServiceControl> getAllServiceControlsForRegistry( final long registryOid ) throws FindException {
        return uddiServiceControlManager.findByUDDIRegistryOid( registryOid );
    }

    @Override
    public void publishGatewayWsdl(final long publishedServiceOid,
                                   final long uddiRegistryOid,
                                   final String uddiBusinessKey,
                                   final String uddiBusinessName,
                                   final boolean updateWhenGatewayWsdlChanges)
            throws FindException, PublishProxiedServiceException, VersionException, UpdateException, SaveException, UDDIRegistryNotEnabledException {

        if(publishedServiceOid < 1) throw new IllegalArgumentException("publishedServiceOid must be > 1");
        if(uddiRegistryOid < 1) throw new IllegalArgumentException("uddiRegistryOid must be > 1");
        if(uddiBusinessKey == null || uddiBusinessKey.trim().isEmpty()) throw new IllegalArgumentException("uddiBusinessKey cannot be null or empty");
        if(uddiBusinessName == null || uddiBusinessName.trim().isEmpty()) throw new IllegalArgumentException("uddiBusinessName cannot be null or empty");

        final UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(uddiRegistryOid);
        if(uddiRegistry == null) throw new IllegalArgumentException("Cannot find UDDIRegistry with oid: " + uddiRegistryOid);
        if(!uddiRegistry.isEnabled()) throw new UDDIRegistryNotEnabledException("UDDIRegistry is not enabled. Cannot use");

//        final PublishedService service = serviceCache.getCachedService(publishedServiceOid);
        //do not want a cached service, what the most up to date and need its most up to date WSDL
        final PublishedService service = serviceManager.findByPrimaryKey(publishedServiceOid);
        if(service == null) throw new SaveException("Cannot find published service with oid: " + publishedServiceOid);

        final Wsdl wsdl = getWsdl(service);

        final UDDIProxiedServiceInfo serviceInfo = new UDDIProxiedServiceInfo(service.getOid(), uddiRegistry.getOid(),
                uddiBusinessKey, uddiBusinessName, updateWhenGatewayWsdlChanges);

        final UDDIClient uddiClient = getUDDIClient(uddiRegistry);
        try {
            uddiProxiedServiceInfoManager.saveUDDIProxiedServiceInfo(serviceInfo, uddiClient, wsdl);
        } catch (UDDIException e) {
            final String msg = "Could not publish Gateway WSDL to UDDI: " + ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
            throw new PublishProxiedServiceException(msg);
        } catch (WsdlToUDDIModelConverter.MissingWsdlReferenceException e) {
            final String msg = "Could not publish Gateway WSDL to UDDI: " + ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
            throw new PublishProxiedServiceException(msg);
        }
    }

    private Wsdl getWsdl(PublishedService service) throws PublishProxiedServiceException {
        final Wsdl wsdl;
        try {
            wsdl = service.parsedWsdl();
        } catch (WSDLException e) {
            final String msg = "Could not obtain Published Service's WSDL: " + ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
            throw new PublishProxiedServiceException(msg);
        }
        return wsdl;
    }

    @Override
    public void updatePublishedGatewayWsdl(long uddiProxiedServiceInfoOid)
            throws FindException, UDDIRegistryNotEnabledException, PublishProxiedServiceException, UpdateException,
            VersionException {
        final UDDIProxiedServiceInfo serviceInfo = uddiProxiedServiceInfoManager.findByPrimaryKey(uddiProxiedServiceInfoOid);
        if(serviceInfo == null) throw new UpdateException("Cannot find UDDIProxiedServiceInfo with id: " + uddiProxiedServiceInfoOid);

        final UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(serviceInfo.getUddiRegistryOid());
        if(!uddiRegistry.isEnabled()) throw new UDDIRegistryNotEnabledException("UDDIRegistry is not enabled. Cannot use");

        //final PublishedService service = serviceCache.getCachedService(serviceInfo.getPublishedServiceOid());
        //do not want a cached service, what the most up to date and need its most up to date WSDL
        final PublishedService service = serviceManager.findByPrimaryKey(serviceInfo.getPublishedServiceOid());

        final Wsdl wsdl = getWsdl(service);

        final UDDIClient uddiClient = getUDDIClient(uddiRegistry);
        try {
            //the management of whether any existing tModels need to be deleted is left to the uddi proxied service manager
            //as it can only tell what needs to be deleted after it has published the representation of the gateway wsdl to UDDI
            uddiProxiedServiceInfoManager.updateUDDIProxiedService(serviceInfo, uddiClient, wsdl);
        } catch (UDDIException e) {
            final String msg = "Could not publish Gateway WSDL to UDDI: " + ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
            throw new PublishProxiedServiceException(msg);
        } catch (VersionException e) {
            logger.log(Level.WARNING, "Could not update UDDIProxiedService: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            throw e;
        } catch (WsdlToUDDIModelConverter.MissingWsdlReferenceException e) {
            e.printStackTrace();
        }
    }
}

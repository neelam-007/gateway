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
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.uddi.*;
import com.l7tech.objectmodel.*;
import com.l7tech.util.Pair;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.ServerConfig;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.common.uddi.guddiv3.BusinessService;
import com.l7tech.common.uddi.guddiv3.TModel;

import javax.wsdl.WSDLException;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

public class UDDIRegistryAdminImpl implements UDDIRegistryAdmin{
    protected static final Logger logger = Logger.getLogger(UDDIRegistryAdminImpl.class.getName());

    final private UDDIRegistryManager uddiRegistryManager;
    final private UDDIProxiedServiceManager uddiProxiedServiceManager;
    final private UDDIServiceControlManager uddiServiceControlManager;
    final private ServiceManager serviceManager;
    final private ExternalGatewayURLManager externalGatewayURLManager;

    public UDDIRegistryAdminImpl(final UDDIRegistryManager uddiRegistryManager,
                                 final UDDIProxiedServiceManager uddiProxiedServiceManager,
                                 final UDDIServiceControlManager uddiServiceControlManager,
                                 final ServiceManager serviceManager,
                                 final ExternalGatewayURLManager externalGatewayURLManager) {
        this.uddiRegistryManager = uddiRegistryManager;
        this.uddiProxiedServiceManager = uddiProxiedServiceManager;
        this.uddiServiceControlManager = uddiServiceControlManager;
        this.serviceManager = serviceManager;
        this.externalGatewayURLManager = externalGatewayURLManager;
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
        Collection<UDDIProxiedService> allProxiedServices = uddiRegistryManager.findAllByRegistryOid(uddiRegistry.getOid());
        for(UDDIProxiedService proxiedService: allProxiedServices){
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
    public void testUDDIRegistryAuthentication(final UDDIRegistry uddiRegistry) throws UDDIException {
        final UDDIClient uddiClient = getUDDIClient(uddiRegistry);

        try {
            uddiClient.authenticate();
        } catch (UDDIException e) {
            //the original exception may not be serializable
            throw new UDDIException(e.getMessage());
        }
    }

    private UDDIClient getUDDIClient(final UDDIRegistry uddiRegistry) {
        return UDDIHelper.newUDDIClient( uddiRegistry );
    }

    @Override
    public UDDIProxiedService getUDDIProxiedService(long serviceOid) throws FindException {
        return uddiProxiedServiceManager.findByUniqueKey("serviceOid", serviceOid);
    }

    @Override
    public String deleteGatewayWsdlFromUDDI(UDDIProxiedService uddiProxiedService)
            throws FindException, DeleteException, UDDIRegistryNotEnabledException {
        final UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(uddiProxiedService.getUddiRegistryOid());
        if(!uddiRegistry.isEnabled()) throw new UDDIRegistryNotEnabledException("UDDIRegistry is not enabled. Cannot use");

        final UDDIProxiedService proxiedService = uddiProxiedServiceManager.findByPrimaryKey(uddiProxiedService.getOid());
        final UDDIClient uddiClient = getUDDIClient(uddiRegistry);

        String errorMsg = null;
        try {
            uddiClient.deleteAllBusinessServicesForGatewayWsdl(proxiedService.getGeneralKeywordServiceIdentifier());
            logger.log(Level.INFO, "Successfully deleted published Gateway WSDL from UDDI Registry");
        } catch (UDDIException e) {
            errorMsg = "Errors deleting published Business Services from UDDI: " + e.getMessage();
            logger.log(Level.WARNING, errorMsg, e);
        }

        uddiProxiedServiceManager.delete(uddiProxiedService.getOid());
        logger.log(Level.INFO, "Deleted UDDIProxiedService");
        return errorMsg;
    }

    @Override
    public void updateProxiedServiceOnly(UDDIProxiedService uddiProxiedService) throws UpdateException, FindException {
        final UDDIProxiedService original = uddiProxiedServiceManager.findByPrimaryKey(uddiProxiedService.getOid());
        if(original == null) throw new FindException("Could not find UDDIProxiedService with id: " + uddiProxiedService);

        uddiProxiedService.throwIfFinalPropertyModified(original);

        if(original.isUpdateProxyOnLocalChange() == uddiProxiedService.isUpdateProxyOnLocalChange()) return;

        uddiProxiedServiceManager.update(uddiProxiedService);
    }

    @Override
    public Collection<UDDIProxiedService> getAllProxiedServicesForRegistry(long registryOid) throws FindException {
        return uddiRegistryManager.findAllByRegistryOid(registryOid);
    }

    @Override
    public long saveUDDIServiceControlOnly( final UDDIServiceControl uddiServiceControl )
            throws UpdateException, SaveException, FindException {
        if ( uddiServiceControl.getOid() == UDDIServiceControl.DEFAULT_OID ){
            final PublishedService service = serviceManager.findByPrimaryKey(uddiServiceControl.getPublishedServiceOid());
            final Wsdl wsdl;
            try {
                wsdl = service.parsedWsdl();
            } catch (WSDLException e) {
                final String msg = "Could not obtain WSDL for associated published service: " + ExceptionUtils.getMessage(e);
                logger.log(Level.WARNING, msg, e);
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
    public long publishGatewayWsdl(final UDDIProxiedService uddiProxiedService)
            throws FindException, PublishProxiedServiceException, VersionException, UpdateException, SaveException, UDDIRegistryNotEnabledException {
        final UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(uddiProxiedService.getUddiRegistryOid());
        if(!uddiRegistry.isEnabled()) throw new UDDIRegistryNotEnabledException("UDDIRegistry is not enabled. Cannot use");

        final PublishedService service = serviceManager.findByPrimaryKey(uddiProxiedService.getServiceOid());

        final boolean update = uddiProxiedService.getOid() != PersistentEntity.DEFAULT_OID;
        final String generalKeyword;
        if(update){
            final UDDIProxiedService original = uddiProxiedServiceManager.findByPrimaryKey(uddiProxiedService.getOid());
            uddiProxiedService.throwIfFinalPropertyModified(original);
            generalKeyword = original.getGeneralKeywordServiceIdentifier();
        }else{
            //generate a new identifier
            generalKeyword = service.getOidAsLong().toString();
            uddiProxiedService.setGeneralKeywordServiceIdentifier(generalKeyword);
        }

        final Wsdl wsdl;
        try {
            wsdl = service.parsedWsdl();
        } catch (WSDLException e) {
            final String msg = "Could not obtain Published Service's WSDL: " + e.getMessage();
            logger.log(Level.WARNING, msg);
            throw new PublishProxiedServiceException(msg);
        }

        //protected service external url
        final String protectedServiceExternalURL = externalGatewayURLManager.getExternalSsgURLForService(service.getOidAsLong().toString());
        //protected service gateway external wsdl url
        final String protectedServiceWsdlURL = externalGatewayURLManager.getExternalWsdlUrlForService(service.getOidAsLong().toString());

        WsdlToUDDIModelConverter modelConverter = new WsdlToUDDIModelConverter(wsdl, protectedServiceWsdlURL,
                protectedServiceExternalURL, uddiProxiedService.getUddiBusinessKey(), service.getOid(), generalKeyword);
        final Pair<List<BusinessService>, Map<String, TModel>> servicesAndModels;
        try {
            servicesAndModels = modelConverter.convertWsdlToUDDIModel();
        } catch (WsdlToUDDIModelConverter.MissingWsdlReferenceException e) {
            final String msg = e.getMessage();
            logger.log(Level.WARNING, msg);
            throw new PublishProxiedServiceException(msg);
        }

        final UDDIClient uddiClient = getUDDIClient(uddiRegistry);
        try {
            if(!update){
                uddiProxiedServiceManager.saveUDDIProxiedService(uddiProxiedService, uddiClient, servicesAndModels.left, servicesAndModels.right, generalKeyword);
            }else{
                //Get the info on all published business services from UDDI
                UDDIProxiedServiceDownloader serviceDownloader = new UDDIProxiedServiceDownloader(uddiClient);
                Pair<List<BusinessService>, Map<String, TModel>> modelFromUddi = serviceDownloader.downloadAllBusinessServicesForService(generalKeyword);

                //the management of whether any existing tModels need to be deleted is left to the uddi proxied service manager
                //as it can only tell what needs to be deleted after it has published the representation of the gateway wsdl to UDDI
                uddiProxiedServiceManager.updateUDDIProxiedService(uddiProxiedService,
                        uddiClient,
                        servicesAndModels.left, servicesAndModels.right,
                        modelFromUddi.left, modelFromUddi.right, generalKeyword);
            }
        } catch(SaveException e){
            logger.log(Level.WARNING, "Could not save UDDIProxiedService: " + e.getMessage());
            try {
                logger.log(Level.WARNING, "Attempting to rollback UDDI updates");
                //Attempt to roll back UDDI updates
                uddiClient.deleteBusinessServices(servicesAndModels.left);
                logger.log(Level.WARNING, "UDDI updates rolled back successfully");
            } catch (UDDIException e1) {
                logger.log(Level.WARNING, "Could not rollback UDDI updates: " + e1.getMessage());
            }
            throw e;
        } catch (UpdateException e){
            logger.log(Level.WARNING, "Could not update UDDIProxiedService: " + e.getMessage());
            //Attempt to roll back UDDI updates? - no, as the information in the entity is still valid.
            //it contains a keyword which has not changed. If we successfully published to UDDI, then leave it like
            //that. We don't hold any references to anything UDDI specific in the entity.
            throw e;
        } catch (UDDIException e) {
            final String msg = "Could not publish Gateway WSDL to UDDI: " + e.getMessage();
            logger.log(Level.WARNING, msg);
            throw new PublishProxiedServiceException(msg);
        }

        return uddiProxiedService.getOid();
    }
}

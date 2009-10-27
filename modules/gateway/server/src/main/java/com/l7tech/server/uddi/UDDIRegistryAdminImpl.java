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
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.uddi.*;
import com.l7tech.objectmodel.*;
import com.l7tech.util.SyspropUtil;
import com.l7tech.util.Pair;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.ServerConfig;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.common.uddi.guddiv3.BusinessService;
import com.l7tech.common.uddi.guddiv3.TModel;
import com.l7tech.common.protocol.SecureSpanConstants;

import javax.wsdl.WSDLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class UDDIRegistryAdminImpl implements UDDIRegistryAdmin{
    protected static final Logger logger = Logger.getLogger(UDDIRegistryAdminImpl.class.getName());

    final private UDDIRegistryManager uddiRegistryManager;
    final private UDDIProxiedServiceManager uddiProxiedServiceManager;
    final private ServiceManager serviceManager;
    final private ServerConfig serverConfig;

    public UDDIRegistryAdminImpl(final UDDIRegistryManager uddiRegistryManager,
                                 final UDDIProxiedServiceManager uddiProxiedServiceManager,
                                 final ServiceManager serviceManager,
                                 final ServerConfig serverConfig) {
        this.uddiRegistryManager = uddiRegistryManager;
        this.uddiProxiedServiceManager = uddiProxiedServiceManager;
        this.serviceManager = serviceManager;
        this.serverConfig = serverConfig;
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
    public void deleteUDDIRegistry(final long oid) throws DeleteException, FindException, UDDIException {

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
    public void testUDDIRegistryAuthentication(final UDDIRegistry uddiRegistry) throws FindException, UDDIException {
        final UDDIClient uddiClient = getUDDIClient(uddiRegistry);

        try {
            uddiClient.authenticate();
        } catch (UDDIException e) {
            //the original exception may not be serializable
            throw new UDDIException(e.getMessage());
        }
    }

    private UDDIClient getUDDIClient(final UDDIRegistry uddiRegistry) {
        final UDDIClient uddiClient = UDDIClientFactory.getInstance().newUDDIClient(uddiRegistry.getInquiryUrl(),
                uddiRegistry.getPublishUrl(), uddiRegistry.getSecurityUrl(), uddiRegistry.getRegistryAccountUserName(),
                uddiRegistry.getRegistryAccountPassword(), getDefaultPolicyAttachmentVersion());
        return uddiClient;
    }

    @Override
    public UDDIProxiedService getUDDIProxiedService(long serviceOid) throws FindException {
        return uddiProxiedServiceManager.findByUniqueKey("serviceOid", serviceOid);
    }

    @Override
    public void deleteGatewayWsdlFromUDDI(UDDIProxiedService uddiProxiedService)
            throws FindException, UDDIException, DeleteException {
        final UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(uddiProxiedService.getUddiRegistryOid());

        final UDDIProxiedService proxiedService = uddiProxiedServiceManager.findByPrimaryKey(uddiProxiedService.getOid());
        final UDDIClient uddiClient = getUDDIClient(uddiRegistry);
        uddiClient.deleteAllBusinessServicesForGatewayWsdl(proxiedService.getGeneralKeywordServiceIdentifier());
        logger.log(Level.INFO, "Successfully deleted published Gateway WSDL from UDDI Registry");

        uddiProxiedServiceManager.delete(uddiProxiedService.getOid());
        logger.log(Level.INFO, "Deleted UDDIProxiedService");
    }

    @Override
    public Collection<UDDIProxiedService> getAllProxiedServicesForRegistry(long registryOid) throws FindException {
        return uddiRegistryManager.findAllByRegistryOid(registryOid);
    }

    @Override
    public long publishGatewayWsdl(final UDDIProxiedService uddiProxiedService)
            throws FindException, PublishProxiedServiceException, VersionException, UpdateException, SaveException {
        final UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(uddiProxiedService.getUddiRegistryOid());
        final PublishedService service = serviceManager.findByPrimaryKey(uddiProxiedService.getServiceOid());

        final boolean update = uddiProxiedService.getOid() != PersistentEntity.DEFAULT_OID;
        final String generalKeyword;
        if(update){
            //Get the proxied service
            final UDDIProxiedService original = uddiProxiedServiceManager.findByPrimaryKey(uddiProxiedService.getOid());
            if(uddiProxiedService.getGeneralKeywordServiceIdentifier() == null)
                throw new IllegalStateException("General keyword service identifier property must be set on existing UDDIProxiedServices");
            //the service identifier is not allowed to be modified by client code once saved
            if(!original.getGeneralKeywordServiceIdentifier().equals(uddiProxiedService.getGeneralKeywordServiceIdentifier())){
                throw new IllegalStateException("It is not possible to modify the general keyword service identifier once the UDDIProxiedService has been saved");
            }
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

        final String hostName;
        final String clusterHost = serverConfig.getPropertyCached("clusterHost");
        if(clusterHost == null || clusterHost.trim().isEmpty()){
            logger.log(Level.INFO, "Property clusterHost is not set. Defauting to local host name for Gateway URL");
            try {
                hostName = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                logger.log(Level.WARNING, "Could not find host name for SSG: " + e.getMessage());
                //todo - update if necessary, decided not to throw runtime, is this ok? this should not happen 
                throw new PublishProxiedServiceException("Cannot determine the hostname of the SecureSpan Gateway");
            }
        }else{
            hostName = clusterHost;
        }

        //protected service external url
        String port = serverConfig.getPropertyCached("clusterhttpport");
        String uri = SecureSpanConstants.SERVICE_FILE + service.getOid();
        final String protectedServiceExternalURL =  "http://" + clusterHost + ":" + port + uri;

        //protected service gateway external wsdl url
        String wsdlURI = SecureSpanConstants.WSDL_PROXY_FILE;
        String query = SecureSpanConstants.HttpQueryParameters.PARAM_SERVICEOID + "=" + service.getOid();
        final String protectedServiceWsdlURL = "http://" + hostName + ":" + port + wsdlURI + "?" + query;

        WsdlToUDDIModelConverter modelConverter = new WsdlToUDDIModelConverter(wsdl, protectedServiceWsdlURL,
                protectedServiceExternalURL, uddiProxiedService.getUddiBusinessKey(), service.getOid(), generalKeyword);
        Pair<List<BusinessService>, Map<String, TModel>> servicesAndModels = modelConverter.convertWsdlToUDDIModel();

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
                for(BusinessService businessService: servicesAndModels.left){
                    uddiClient.deleteBusinessService(businessService.getServiceKey());
                }
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

    private static final String SYSPROP_DEFAULT_VERSION = "com.l7tech.uddi.defaultVersion";

    private PolicyAttachmentVersion getDefaultPolicyAttachmentVersion() {
        String id = SyspropUtil.getString(
                SYSPROP_DEFAULT_VERSION,
                PolicyAttachmentVersion.v1_2.toString());
        return PolicyAttachmentVersion.valueOf(id);
    }
}

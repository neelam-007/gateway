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
import com.l7tech.wsdl.Wsdl;
import com.l7tech.common.uddi.guddiv3.BusinessService;
import com.l7tech.common.uddi.guddiv3.TModel;

import javax.wsdl.WSDLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

public class UDDIRegistryAdminImpl implements UDDIRegistryAdmin{
    protected static final Logger logger = Logger.getLogger(UDDIRegistryAdminImpl.class.getName());

    final private UDDIRegistryManager uddiRegistryManager;
    final private UDDIProxiedServiceManager uddiProxiedServiceManager;
    final private ServiceManager serviceManager;

    public UDDIRegistryAdminImpl(final UDDIRegistryManager uddiRegistryManager,
                                 final UDDIProxiedServiceManager uddiProxiedServiceManager,
                                 final ServiceManager serviceManager) {
        this.uddiRegistryManager = uddiRegistryManager;
        this.uddiProxiedServiceManager = uddiProxiedServiceManager;
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
    public void deleteUDDIRegistry(final long oid) throws DeleteException, FindException {
        logger.info("Updating UDDI Registry oid = " + oid);
        uddiRegistryManager.delete(oid);
    }

    @Override
    public Collection<UDDIRegistry> findAllUDDIRegistries() throws FindException {
        return uddiRegistryManager.findAll();
    }

    @Override
    public void testUDDIRegistryAuthentication(final long uddiRegistryOid) throws FindException, UDDIException {
        final UDDIRegistry uddiRegistry= uddiRegistryManager.findByPrimaryKey(uddiRegistryOid);

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
    public long publishGatewayWsdl(final UDDIProxiedService uddiProxiedService)
            throws FindException, PublishProxiedServiceException, VersionException, UpdateException, SaveException {
        final UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(uddiProxiedService.getUddiRegistryOid());
        final PublishedService service = serviceManager.findByPrimaryKey(uddiProxiedService.getServiceOid());

        final boolean update = uddiProxiedService.getServiceOid() != PersistentEntity.DEFAULT_OID;

        final Wsdl wsdl;
        try {
            wsdl = service.parsedWsdl();
        } catch (WSDLException e) {
            final String msg = "Could not obtain Published Service's WSDL: " + e.getMessage();
            logger.log(Level.WARNING, msg);
            throw new PublishProxiedServiceException(msg);
        }

        final String relativeURL = service.getRoutingUri();
        //todo get the cluster hostname, error if not set
        final String protectedServiceUrl = relativeURL;
        //todo work out the SSG WSDL address for the protected service
        final String protectedServiceWsdlURL = relativeURL;

        WsdlToUDDIModelConverter modelConverter = new WsdlToUDDIModelConverter(wsdl, protectedServiceWsdlURL,
                protectedServiceUrl, uddiProxiedService.getUddiBusinessKey(), service.getOid());
        Pair<List<BusinessService>, Map<String, TModel>> servicesAndModels = modelConverter.convertWsdlToUDDIModel();

        try {
            if(!update){
                uddiProxiedServiceManager.saveUDDIProxiedService(uddiProxiedService, getUDDIClient(uddiRegistry), servicesAndModels.left, servicesAndModels.right);
            }else{
                uddiProxiedServiceManager.updateUDDIProxiedService(uddiProxiedService, getUDDIClient(uddiRegistry), servicesAndModels.left, servicesAndModels.right);
            }
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

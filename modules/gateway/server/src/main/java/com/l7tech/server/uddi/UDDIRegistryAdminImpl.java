/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Oct 14, 2009
 * Time: 12:42:53 PM
 */
package com.l7tech.server.uddi;

import com.l7tech.gateway.common.admin.UDDIRegistryAdmin;
import com.l7tech.gateway.common.uddi.UDDIRegistry;
import com.l7tech.uddi.*;
import com.l7tech.objectmodel.*;
import com.l7tech.util.SyspropUtil;

import java.util.Collection;
import java.util.logging.Logger;

public class UDDIRegistryAdminImpl implements UDDIRegistryAdmin{
    protected static final Logger logger = Logger.getLogger(UDDIRegistryAdminImpl.class.getName());

    final private UDDIRegistryManager uddiRegistryManager;

    public UDDIRegistryAdminImpl(UDDIRegistryManager uddiRegistryManager) {
        this.uddiRegistryManager = uddiRegistryManager;
    }

    @Override
    public long saveUDDIRegistry(UDDIRegistry uddiRegistry) throws SaveException, UpdateException {
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
    public void deleteUDDIRegistry(long oid) throws DeleteException, FindException {
        logger.info("Updating UDDI Registry oid = " + oid);
        uddiRegistryManager.delete(oid);
    }

    @Override
    public Collection<UDDIRegistry> findAllUDDIRegistries() throws FindException {
        return uddiRegistryManager.findAll();
    }

    @Override
    public void testUDDIRegistryAuthentication(long registryOid) throws FindException, UDDIException {
        UDDIRegistry uddiRegistry= uddiRegistryManager.findByPrimaryKey(registryOid);

        UDDIClient uddiClient = UDDIClientFactory.getInstance().newUDDIClient(uddiRegistry.getInquiryUrl(),
                uddiRegistry.getPublishUrl(), uddiRegistry.getSecurityUrl(), uddiRegistry.getRegistryAccountUserName(),
                uddiRegistry.getRegistryAccountPassword(), getDefaultPolicyAttachmentVersion());

        try {
            uddiClient.authenticate();
        } catch (UDDIException e) {
            //the original exception may not be serializable
            throw new UDDIException(e.getMessage());
        }
    }

    private static final String SYSPROP_DEFAULT_VERSION = "com.l7tech.uddi.defaultVersion";

    private PolicyAttachmentVersion getDefaultPolicyAttachmentVersion() {
        String id = SyspropUtil.getString(
                SYSPROP_DEFAULT_VERSION,
                PolicyAttachmentVersion.v1_2.toString());
        return PolicyAttachmentVersion.valueOf(id);
    }
}

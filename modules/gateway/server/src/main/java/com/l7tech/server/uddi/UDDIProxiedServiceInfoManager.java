/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.server.uddi;

import com.l7tech.gateway.common.uddi.UDDIProxiedServiceInfo;
import com.l7tech.gateway.common.admin.UDDIRegistryAdmin;
import com.l7tech.objectmodel.*;
import com.l7tech.uddi.UDDIClient;
import com.l7tech.uddi.UDDIException;
import com.l7tech.uddi.WsdlToUDDIModelConverter;
import com.l7tech.wsdl.Wsdl;

import java.util.Collection;

public interface UDDIProxiedServiceInfoManager extends EntityManager<UDDIProxiedServiceInfo, EntityHeader> {

    /**
     * Save a UDDIProxiedService. This manages the publish of information to a UDDI Registry
     * <p/>
     * See http://sarek.l7tech.com/mediawiki/index.php?title=CentraSite_ActiveSOA_Design#Strategy_for_publishing_and_updating_BusinessServices_to_UDDI
     * for implementation strategy
     *
     * @param uddiProxiedServiceInfo      the UDDIProxiedService to save
     * @throws com.l7tech.objectmodel.SaveException
     */
    void saveUDDIProxiedServiceInfo( UDDIProxiedServiceInfo uddiProxiedServiceInfo )
            throws SaveException;

    /**
     * Update a UDDIProxiedService. This manages the update of information previously published to a UDDI Registry
     * <p/>
     * See http://sarek.l7tech.com/mediawiki/index.php?title=CentraSite_ActiveSOA_Design#Strategy_for_publishing_and_updating_BusinessServices_to_UDDI
     * for implementation strategy
     *
     * @param uddiProxiedServiceInfo   the UDDIProxiedServiceInfo to save or update
     * @param uddiClient           the UDDIClient configured for the UDDI registry to publish the proxied service to
     * @param wsdl Wsdl of the service
     * @throws com.l7tech.objectmodel.UpdateException
     *          any problems updating
     * @throws com.l7tech.objectmodel.VersionException
     *          any problems with versions
     * @throws com.l7tech.objectmodel.DeleteException
     * @throws com.l7tech.objectmodel.FindException
     * @throws com.l7tech.uddi.UDDIException
     */
    void updateUDDIProxiedService( UDDIProxiedServiceInfo uddiProxiedServiceInfo,
                                   UDDIClient uddiClient,
                                   Wsdl wsdl)
            throws UpdateException, VersionException, UDDIException, FindException, UDDIRegistryAdmin.PublishProxiedServiceException, WsdlToUDDIModelConverter.MissingWsdlReferenceException;

    /**
     * Find a UDDIProxiedService by published service identifier (OID)
     *
     * @param publishedServiceOid The identifier for the service
     * @return The UDDIProxiedService or null
     * @throws FindException if an error occurs
     */
    UDDIProxiedServiceInfo findByPublishedServiceOid( long publishedServiceOid ) throws FindException;

    /**
     * Find UDDIProxiedServiceInfos with metrics enabled for the given registry.
     *
     * @param registryOid The registry OID.
     * @param metricsEnabled The metrics enabled state to match
     * @return The collection of UDDIProxiedServiceInfos (may be emptry but never null)
     * @throws FindException If an error occurs
     */
    Collection<UDDIProxiedServiceInfo> findByUDDIRegistryAndMetricsState( long registryOid,
                                                                          boolean metricsEnabled ) throws FindException;
}

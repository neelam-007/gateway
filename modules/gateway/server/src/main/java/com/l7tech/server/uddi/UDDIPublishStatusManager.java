/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.server.uddi;

import com.l7tech.gateway.common.uddi.UDDIPublishStatus;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;

public interface UDDIPublishStatusManager extends EntityManager<UDDIPublishStatus, EntityHeader> {

    /**
     * Find a UDDIProxiedService by published service identifier (OID)
     *
     * @param proxiedServiceOid The identifier for the UDDIProxiedServiceInfo
     * @return The UDDIPublishStatus or null
     * @throws com.l7tech.objectmodel.FindException if an error occurs
     */
    UDDIPublishStatus findByProxiedSerivceInfoOid( long proxiedServiceOid ) throws FindException;

}

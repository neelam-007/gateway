/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.server.uddi;

import com.l7tech.gateway.common.uddi.UDDIPublishStatus;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.GoidEntityManager;

public interface UDDIPublishStatusManager extends GoidEntityManager<UDDIPublishStatus, EntityHeader> {

    /**
     * Find a UDDIProxiedService by published service identifier (GOID)
     *
     * @param proxiedServiceGoid The identifier for the UDDIProxiedServiceInfo
     * @return The UDDIPublishStatus or null
     * @throws com.l7tech.objectmodel.FindException if an error occurs
     */
    UDDIPublishStatus findByProxiedSerivceInfoGoid( Goid proxiedServiceGoid ) throws FindException;

}

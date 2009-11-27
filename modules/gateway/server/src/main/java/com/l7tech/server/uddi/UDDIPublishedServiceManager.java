package com.l7tech.server.uddi;

import com.l7tech.gateway.common.uddi.UDDIPublishedService;
import com.l7tech.objectmodel.*;

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
public interface UDDIPublishedServiceManager extends EntityManager<UDDIPublishedService, EntityHeader> {

    void deleteByServiceKey(String serviceKey) throws FindException, DeleteException;

}

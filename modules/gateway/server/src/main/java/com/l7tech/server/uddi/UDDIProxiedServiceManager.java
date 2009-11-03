package com.l7tech.server.uddi;

import com.l7tech.gateway.common.uddi.UDDIProxiedService;
import com.l7tech.objectmodel.*;

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
public interface UDDIProxiedServiceManager extends EntityManager<UDDIProxiedService, EntityHeader> {

    void deleteByServiceKey(String serviceKey) throws FindException, DeleteException;

}

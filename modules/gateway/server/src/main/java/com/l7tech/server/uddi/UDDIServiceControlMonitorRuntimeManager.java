/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.server.uddi;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;

public interface UDDIServiceControlMonitorRuntimeManager extends EntityManager<UDDIServiceControlMonitorRuntime,EntityHeader> {

    /**
     * Get the UDDIServiceControlMonitorRuntime which maps 1:1 to the supplied UDDIServiceControl's oid supplied
     * @param uddiServiceControlOid long oid of the UDDIServiceControl
     * @return UDDIServiceControlMonitorRuntime runtime information for the UDDIServiceControl
     * @throws FindException problem reading from the db
     */
    public UDDIServiceControlMonitorRuntime findByServiceControlOid( final long uddiServiceControlOid) throws FindException;
}

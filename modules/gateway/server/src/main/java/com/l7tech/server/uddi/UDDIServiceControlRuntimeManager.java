/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.server.uddi;

import com.l7tech.gateway.common.uddi.UDDIServiceControlRuntime;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.GoidEntityManager;

public interface UDDIServiceControlRuntimeManager extends GoidEntityManager<UDDIServiceControlRuntime,EntityHeader> {

    /**
     * Get the UDDIServiceControlRuntime which maps 1:1 to the supplied UDDIServiceControl's goid supplied
     * @param uddiServiceControlGoid Goid goid of the UDDIServiceControl
     * @return UDDIServiceControlMonitorRuntime runtime information for the UDDIServiceControl
     * @throws FindException problem reading from the db
     */
    public UDDIServiceControlRuntime findByServiceControlGoid( final Goid uddiServiceControlGoid) throws FindException;
}

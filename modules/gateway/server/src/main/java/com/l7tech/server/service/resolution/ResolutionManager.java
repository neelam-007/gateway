/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.service.resolution;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.DeleteException;

/**
 * @author alex
 */
public interface ResolutionManager {
    void recordResolutionParameters(PublishedService service) throws DuplicateObjectException, UpdateException;

    void deleteResolutionParameters(long serviceOid) throws DeleteException;

    void checkDuplicateResolution(PublishedService service) throws DuplicateObjectException, ServiceResolutionException;
}

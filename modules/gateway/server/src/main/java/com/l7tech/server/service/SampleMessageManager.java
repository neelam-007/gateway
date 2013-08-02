package com.l7tech.server.service;

import com.l7tech.objectmodel.*;
import com.l7tech.gateway.common.service.SampleMessage;

/**
 * Manages persistent instances of {@link com.l7tech.gateway.common.service.SampleMessage}.
 */
public interface SampleMessageManager extends GoidEntityManager<SampleMessage, EntityHeader> {
    EntityHeader[] findHeaders(Goid serviceId, String operationName) throws FindException;
}

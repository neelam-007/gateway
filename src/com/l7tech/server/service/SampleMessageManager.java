package com.l7tech.server.service;

import com.l7tech.objectmodel.*;
import com.l7tech.service.SampleMessage;

/**
 * Manages persistent instances of {@link com.l7tech.service.SampleMessage}.
 */
public interface SampleMessageManager extends EntityManager<SampleMessage, EntityHeader> {
    EntityHeader[] findHeaders(long serviceOid, String operationName) throws FindException;
    void update(SampleMessage sm) throws UpdateException;
    void delete(SampleMessage sm) throws DeleteException;
}

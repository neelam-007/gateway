package com.l7tech.server.service;

import com.l7tech.objectmodel.*;
import com.l7tech.service.SampleMessage;

/**
 * Manages persistent instances of {@link com.l7tech.service.SampleMessage}.
 */
public interface SampleMessageManager extends EntityManager {
    SampleMessage findByPrimaryKey(long oid) throws FindException;
    EntityHeader[] findHeaders(long serviceOid, String operationName) throws FindException;
    long save(SampleMessage sm) throws SaveException;
    void update(SampleMessage sm) throws UpdateException;
    void delete(SampleMessage sm) throws DeleteException;
}

/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.transport.jms;

import com.l7tech.objectmodel.*;
import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsProvider;

import java.util.Collection;

/**
 * @author alex
 */
public interface JmsConnectionManager extends EntityManager<JmsConnection, EntityHeader> {
    Collection<JmsProvider> findAllProviders() throws FindException;

    void update(JmsConnection conn) throws VersionException, UpdateException;
}

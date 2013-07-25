/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.transport.jms;

import com.l7tech.gateway.common.transport.jms.JmsProviderType;
import com.l7tech.objectmodel.*;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsProvider;

import java.util.Collection;
import java.util.EnumSet;

/**
 * @author alex
 */
public interface JmsConnectionManager extends GoidEntityManager<JmsConnection, EntityHeader> {
    EnumSet<JmsProviderType> findAllProviders() throws FindException;
}

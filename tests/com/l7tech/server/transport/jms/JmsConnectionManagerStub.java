/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.transport.jms;

import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsProvider;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManagerStub;
import com.l7tech.objectmodel.FindException;

import java.util.Collection;
import java.util.Collections;

/** @author alex */
public class JmsConnectionManagerStub extends EntityManagerStub<JmsConnection, EntityHeader> implements JmsConnectionManager {
    public Collection<JmsProvider> findAllProviders() throws FindException {
        return Collections.emptyList();
    }
}
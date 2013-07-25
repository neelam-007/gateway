/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.transport.jms;

import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsProviderType;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.GoidEntityManagerStub;

import java.util.EnumSet;

/** @author alex */
public class JmsConnectionManagerStub extends GoidEntityManagerStub<JmsConnection, EntityHeader> implements JmsConnectionManager {

    public JmsConnectionManagerStub() {
        super();
    }

    public JmsConnectionManagerStub( final JmsConnection... entitiesIn ) {
        super( entitiesIn );
    }

    @Override
    public EnumSet<JmsProviderType> findAllProviders() throws FindException {
        return EnumSet.noneOf(JmsProviderType.class);
    }
}
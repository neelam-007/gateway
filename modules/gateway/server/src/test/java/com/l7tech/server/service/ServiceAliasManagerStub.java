/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.service;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.objectmodel.Entity;

/** @author alex */
public class ServiceAliasManagerStub extends AliasManagerStub<PublishedServiceAlias, PublishedService, ServiceHeader> implements ServiceAliasManager {
    public ServiceAliasManagerStub(PublishedServiceAlias... entitiesIn) {
        super(entitiesIn);
    }

    @Override
    public ServiceHeader getNewEntityHeader(ServiceHeader serviceHeader) {
        return new ServiceHeader(serviceHeader);
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return PublishedServiceAlias.class;
    }
}

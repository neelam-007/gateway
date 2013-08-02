package com.l7tech.server.service;

import com.l7tech.objectmodel.Goid;
import com.l7tech.server.EntityManagerStub;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;

import java.util.Collection;
import java.util.Collections;

/**
 *
 */
public class ServiceDocumentManagerStub extends EntityManagerStub<ServiceDocument, EntityHeader> implements ServiceDocumentManager {

    @Override
    public Collection<ServiceDocument> findByServiceId(Goid serviceId) throws FindException {
        return Collections.emptyList();
    }

    @Override
    public Collection<ServiceDocument> findByServiceIdAndType(Goid serviceId, String type) throws FindException {
        return Collections.emptyList();
    }
}

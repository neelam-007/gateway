package com.l7tech.server.service;

import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.EntityManager;

import java.util.Collection;

/**
 * Manager for ServiceDocuments.
 *
 * @author Steve Jones
 */
public interface ServiceDocumentManager extends EntityManager<ServiceDocument, EntityHeader> {

    /**
     * Find ServiceDocuments by service identifier.
     *
     * @param serviceId The identifier for the owner service
     * @return The (possibly empty) collection of ServiceDocuments
     * @throws FindException if an error occurs
     */
    Collection<ServiceDocument> findByServiceId(Goid serviceId) throws FindException;

    /**
     * Find ServiceDocuments by service identifier and type.
     *
     * @param serviceId The identifier for the owner service
     * @return The (possibly empty) collection of ServiceDocuments
     * @throws FindException if an error occurs
     */
    Collection<ServiceDocument> findByServiceIdAndType(Goid serviceId, String type) throws FindException;
}

package com.l7tech.server.service;

import java.util.Collection;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.gateway.common.service.ServiceDocument;

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
    Collection<ServiceDocument> findByServiceId(long serviceId) throws FindException;

    /**
     * Find ServiceDocuments by service identifier and type.
     *
     * @param serviceId The identifier for the owner service
     * @return The (possibly empty) collection of ServiceDocuments
     * @throws FindException if an error occurs
     */
    Collection<ServiceDocument> findByServiceIdAndType(long serviceId, String type) throws FindException;
}

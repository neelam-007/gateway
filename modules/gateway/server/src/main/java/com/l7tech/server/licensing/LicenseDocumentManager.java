package com.l7tech.server.licensing;

import com.l7tech.gateway.common.licensing.LicenseDocument;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SaveException;

/**
 * Management of the LicenseDocument Entity.
 *
 * @author Jamie Williams - wilja33 - jamie.williams2@ca.com
 */
public interface LicenseDocumentManager extends EntityManager<LicenseDocument, EntityHeader> {

    // TODO jwilliams: find a better way to handle the problem with the read only findAll() implementation

    /**
     * Save the specified LicenseDocument and manually flush. Required for situations when findAll() is called
     * immediately subsequent, as the persistence of the new entity has been found to be unreliable.
     *
     * @param entity the LicenseDocument to persist
     * @return the key of the persisted entity
     * @throws SaveException
     */
    public Goid saveWithImmediateFlush(LicenseDocument entity) throws SaveException;
}

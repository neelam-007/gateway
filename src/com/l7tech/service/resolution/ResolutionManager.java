package com.l7tech.service.resolution;

import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.service.PublishedService;

/**
 * LAYER 7 TECHNOLOGIES, INC
 *
 * User: flascell
 * Date: Nov 25, 2003
 * Time: 10:46:59 AM
 * $Id$
 *
 * The ResolutionManager (actually its corresponding table) enforces the uniqueness of resolution
 * parameters across all services.
 *
 * This is used by the ServiceManager when updating and saving services to ensure that resolution
 * parameters do not conflict.
 *
 */
public class ResolutionManager {

    /**
     * Records resolution parameters for the passed service.
     *
     * If those resolution parameters conflict with resolution parameters of another service, this
     * will throw a DuplicateObjectException exception.
     *
     * This sould be called by service manager when saving and updating services.
     *
     * @param service the service whose resolution parameters should be recorded
     * @throws DuplicateObjectException this is thrown when there is a conflict between the resolution parameters of
     * the passed service and the ones of another service.
     */
    public void recordResolutionParameters(PublishedService service) throws DuplicateObjectException {
        // todo
    }
}

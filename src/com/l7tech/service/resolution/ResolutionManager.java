package com.l7tech.service.resolution;

import com.l7tech.service.PublishedService;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.message.Request;

/**
 * LAYER 7 TECHNOLOGIES, INC
 *
 * User: flascell
 * Date: Nov 25, 2003
 * Time: 10:46:59 AM
 * $Id$
 *
 * Manages the resolvers and the resolution parameters table.
 *
 * This is used by the ServiceManager when updating and saving services to ensure that resolution
 * parameters do not conflict.
 *
 * It is also used at run time by the message processor when a service cannot be resolved from
 * the service cache.
 *
 */
public class ResolutionManager {

    /**
     * This will verify that the resolution parameters for the service passed does not conflict
     * with resolution parameters of another exising services.
     *
     * Called by the ServiceManager when updating or saving a new service.
     *
     * @param service service from which we check the resolution parameters of
     * @return false is no conflict exists, true if there is a conflict
     */
    public boolean isConflictingResolutionParameters(PublishedService service) {
        // todo
        return false;
    }

    /**
     * Replaces the recorded resolution parameters of an existing service by new ones.
     *
     * This is called by the service manager when updating a service.
     *
     * @param service the service from which the new resolution parameters will be based on.
     * the oid of this service is used to determine which parameters to delete.
     * @throws UpdateException
     */
    public void replaceResolutionParameters(PublishedService service) throws UpdateException {
        // todo
    }

    /**
     * Records resolution parameters for this service.
     *
     * Called by the ServiceManager when a new service is saved
     *
     * @param service object from which the oid and the resolution parameters are extracted
     * @throws SaveException
     */
    public void saveNewResolutionParameters(PublishedService service) throws SaveException {
        // todo
    }

    /**
     * Resolves a service for the request passed using the resolution parameters table.
     *
     * Called at runtime by the message processor.
     *
     * @param request the request from which to resolve a service
     * @return the service oid that matches, if no match is found this throws
     * @throws FindException if no match is found
     */
    public long resolveService(Request request) throws FindException {
        // todo
        return -1;
    }

    /**
     * the resolver types
     */
    //public static final Class[] RESOLVER_TYPES = {SoapActionResolver.class, UrnResolver.class};
}

package com.l7tech.proxy.datamodel;

import java.util.List;

/**
 * Interface implemented by providers of SSG config information.
 * User: mike
 * Date: Jun 5, 2003
 * Time: 2:02:44 PM
 */
public interface SsgFinder {
    /**
     * Get the list of Ssgs known to this client proxy.
     * @return A List of the canonical Ssg objects.
     *         The List is read-only but the Ssg objects in it are the real deal.
     */
    public List getSsgList();

    /**
     * Find the Ssg with the specified name.  If multiple Ssgs have the same name only the
     * first one is returned.
     *
     * @param name the name to look for (ie, "R&D Gateway")
     * @return The requested Ssg.  Never null.
     * @throws com.l7tech.proxy.datamodel.SsgNotFoundException If the specified name was not found.
     */
    public Ssg getSsgByName(String name) throws SsgNotFoundException;

    /**
     * Find the Ssg with the specified local endpoint.  If multiple Ssgs have the same endpoint
     * only the first one is returned.
     *
     * @param endpoint The endpoint to look for (ie, "SSG0")
     * @return The requested Ssg.  Never null.
     * @throws com.l7tech.proxy.datamodel.SsgNotFoundException If the specified endpoint was not found.
     */
    public Ssg getSsgByEndpoint(String endpoint) throws SsgNotFoundException;
}

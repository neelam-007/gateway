package com.l7tech.proxy.datamodel;

import com.l7tech.proxy.datamodel.exceptions.SsgNotFoundException;

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
    List getSsgList();

    /**
     * Find the Ssg with the specified local endpoint.  If multiple Ssgs have the same endpoint
     * only the first one is returned.
     *
     * @param endpoint The endpoint to look for (ie, "SSG0")
     * @return The requested Ssg.  Never null.
     * @throws com.l7tech.proxy.datamodel.exceptions.SsgNotFoundException If the specified endpoint was not found.
     */
    Ssg getSsgByEndpoint(String endpoint) throws SsgNotFoundException;

    /**
     * Find the Ssg with the specified hostname.  If multiple Ssgs have the same hostname only one of them
     * will be returned.
     * @param hostname The hostname to look for.
     * @return A registered Ssg with that hostname.
     * @throws com.l7tech.proxy.datamodel.exceptions.SsgNotFoundException if no Ssg was registered with the specified hostname.
     */
    Ssg getSsgByHostname(String hostname) throws SsgNotFoundException;

    /**
     * Get the default SSG.
     * Returns the first SSG that has its Default flag set.  Usually there is only one such SSG.
     * @return the Default SSG
     * @throws com.l7tech.proxy.datamodel.exceptions.SsgNotFoundException if no Default SSG was found
     */
    Ssg getDefaultSsg() throws SsgNotFoundException;

    /**
     * Notify that one of an Ssg's fields might have changed, possibly requiring a rebuild of one or
     * more lookup caches.
     * @param ssg The SSG that was modified.  If null, will assume all Ssgs may have been modified.
     */
    void onSsgUpdated(Ssg ssg);
}

package com.l7tech.proxy.datamodel;

import java.util.Collection;

/**
 * Manages our SSG state.
 * User: mike
 * Date: Jun 2, 2003
 * Time: 12:12:38 PM
 * To change this template use Options | File Templates.
 */
public interface SsgManager {

    public static class SsgNotFoundException extends Exception {
        public SsgNotFoundException() {
        }

        public SsgNotFoundException(String message) {
            super(message);
        }

        public SsgNotFoundException(String message, Throwable cause) {
            super(message, cause);
        }

        public SsgNotFoundException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Get the list of Ssgs known to this client proxy.
     * @return A Collection of the canonical Ssg objects.
     *         The Collection is read-only but the Ssg objects in it are the real deal.
     */
    public Collection getSsgList();

    /**
     * Find the Ssg with the specified name.  If multiple Ssgs have the same name only the
     * first one is returned.
     *
     * @param name the name to look for (ie, "R&D Gateway")
     * @return The requested Ssg.  Never null.
     * @throws SsgNotFoundException If the specified name was not found.
     */
    public Ssg getSsgByName(String name) throws SsgNotFoundException;

    /**
     * Find the Ssg with the specified local endpoint.  If multiple Ssgs have the same endpoint
     * only the first one is returned.
     *
     * @param endpoint The endpoint to look for (ie, "SSG0")
     * @return The requested Ssg.  Never null.
     * @throws SsgNotFoundException If the specified endpoint was not found.
     */
    public Ssg getSsgByEndpoint(String endpoint) throws SsgNotFoundException;

    /**
     * Register a new Ssg with this client proxy.  Takes no action if an Ssg which equals() the specified ssg
     * is already registered.
     * @param ssg The new Ssg.
     * @return true iff. the new ssg was not already registered.
     */
    public boolean add(Ssg ssg);

    /**
     * Forget all about a registered Ssg.
     *
     * @param ssg The Ssg to forget about.
     * @throws SsgNotFoundException If the specified Ssg was not found.
     */
    public void remove(Ssg ssg) throws SsgNotFoundException;
}

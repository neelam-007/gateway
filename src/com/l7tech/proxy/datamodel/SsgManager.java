package com.l7tech.proxy.datamodel;

import java.io.IOException;

/**
 * Interface implemented by objects that keep track of our SSG configuration.
 * User: mike
 * Date: Jun 2, 2003
 * Time: 12:12:38 PM
 */
public interface SsgManager extends SsgFinder {
    /**
     * Create a new Ssg instance, but do not yet register it.
     */
    Ssg createSsg();

    /**
     * Register a new Ssg with this client proxy.  Takes no action if an Ssg which equals() the specified ssg
     * is already registered.
     * @param ssg The new Ssg.
     * @return true iff. the new ssg was not already registered.
     * @throws IllegalArgumentException might be thrown if the Ssg was not obtained by calling createSsg()
     */
    boolean add(Ssg ssg);

    /**
     * Forget all about a registered Ssg.
     *
     * @param ssg The Ssg to forget about.
     * @throws SsgNotFoundException If the specified Ssg was not found.
     */
    void remove(Ssg ssg) throws SsgNotFoundException;

    /**
     * Get the default SSG.
     * Returns the first SSG that has its Default flag set.  Usually there is only one such SSG.
     * @return the Default SSG
     * @throws SsgNotFoundException if no Default SSG was found
     */
    Ssg getDefaultSsg() throws SsgNotFoundException;

    /**
     * Set the default SSG.
     * If this method returns, it's guaranteed that the specified Ssg
     * is in the Ssg list and is the only one with its Default flag set to true.
     * @param ssg the SSG that should be made the new default ssg
     * @throws SsgNotFoundException if the specified SSG is not registered
     */
    void setDefaultSsg(Ssg ssg) throws SsgNotFoundException;

    /**
     * Save changes back to persistent store.
     * Changes to the SSG configuration are not guaranteed to be persisted unless users call
     * this method after each cluster of changes to Ssgs and calls to add() and remove().
     */
    void save() throws IOException;
}

package com.l7tech.proxy.datamodel;

import java.io.IOException;

/**
 * Interface implemented by objects that keep track of our SSG configuration.
 * User: mike
 * Date: Jun 2, 2003
 * Time: 12:12:38 PM
 * To change this template use Options | File Templates.
 */
public interface SsgManager extends SsgFinder {
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
     * @throws com.l7tech.proxy.datamodel.SsgNotFoundException If the specified Ssg was not found.
     */
    public void remove(Ssg ssg) throws SsgNotFoundException;

    /**
     * Save changes back to persistent store.
     * Changes to the SSG configuration are not guaranteed to be persisted unless users call
     * this method after each cluster of changes to Ssgs and calls to add() and remove().
     */
    public void save() throws IOException;
}

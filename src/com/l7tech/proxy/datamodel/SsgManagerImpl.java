package com.l7tech.proxy.datamodel;

import com.l7tech.common.util.FileUtils;
import com.l7tech.proxy.datamodel.exceptions.SsgNotFoundException;

import java.beans.XMLEncoder;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * Extends SsgFinderImpl to support saving state back to the ssgs.xml file.
 * User: mike
 * Date: Jun 2, 2003
 * Time: 1:51:56 PM
 */
public class SsgManagerImpl extends SsgFinderImpl implements SsgManager {
    private static class SsgManagerHolder {
        private static final SsgManagerImpl ssgManager = new SsgManagerImpl();
    }

    /** Get the singleton SsgManagerImpl. */
    public static SsgManagerImpl getSsgManagerImpl() {
        return SsgManagerHolder.ssgManager;
    }

    /**
     * Erase our SSG list.
     */
    public synchronized void clear() {
        ssgs.clear();
        hostCache.clear();
    }

    /**
     * Save our SSG state to disk.  Caller is responsible for ensuring that only one process will be
     * calling this method at any given time.
     */
    public synchronized void save() throws IOException {
        // TODO: add lockfile so multiple Client Proxy instances can safely share a data store

        FileUtils.saveFileSafely(STORE_FILE, new FileUtils.Saver() {
            public void doSave(FileOutputStream fos) throws IOException {
                XMLEncoder encoder = null;
                try {
                    encoder = new XMLEncoder(fos);
                    encoder.writeObject(ssgs); // TODO worry about locking SSGs while saving
                    encoder.close();
                    encoder = null;
                } finally {
                    if (encoder != null)
                        encoder.close();
                }
            }
        });

        rebuildHostCache();
    }

    /**
     * Create a new Ssg instance, but do not yet register it.
     */
    public synchronized Ssg createSsg() {
        if (!init)
            initialize();
        return new Ssg(nextId());
    }

    /**
     * Register a new Ssg with this client proxy.  Takes no action if an Ssg that equals() the new object is
     * already registered.
     * @param ssg The new Ssg.
     * @return true iff. the given ssg was not already registered
     * @throws IllegalArgumentException if the given Ssg was not obtained by calling createSsg()
     */
    public synchronized boolean add(final Ssg ssg) {
        if (!init)
            initialize();
        if (ssg.getId() == 0)
            throw new IllegalArgumentException("Unable to register Gateway: it has not been assigned an ID");
        boolean result = ssgs.add(ssg);
        rebuildHostCache();
        return result;
    }

    /**
     * Forget all about a registered Ssg.  This does not delete the Ssg's associated keystore file, if any;
     * to do this, use SsgKeyStoreManager.deleteStores().
     *
     * @see SsgKeyStoreManager#deleteStores
     * @param ssg The Ssg to forget about.
     * @throws com.l7tech.proxy.datamodel.exceptions.SsgNotFoundException If the specified Ssg was not found.
     */
    public synchronized void remove(final Ssg ssg) throws SsgNotFoundException {
        if (!init)
            initialize();
        if (!ssgs.remove(ssg))
            throw new SsgNotFoundException("The specified Gateway was not found");
        rebuildHostCache();
    }

    /**
     * Set the default SSG.
     * If this method returns, it's guaranteed that the specified Ssg
     * is in the Ssg list and is the only one with its Default flag set to true.
     * @param ssg
     * @throws com.l7tech.proxy.datamodel.exceptions.SsgNotFoundException
     */
    public synchronized void setDefaultSsg(Ssg ssg) throws SsgNotFoundException {
        boolean found = false;
        for (Iterator i = ssgs.iterator(); i.hasNext();) {
            Ssg s = (Ssg) i.next();
            if (s.equals(ssg)) {
                s.setDefaultSsg(true);
                found = true;
            } else {
                s.setDefaultSsg(false);
            }
        }
        if (!found)
            throw new SsgNotFoundException("The requested default Gateway is not currently registered.");
        rebuildHostCache();
    }

    /**
     * Find the lowest unused ID number.
     * This doesn't really belong in the FinderImpl -- it belongs in the subclasses -- but our load()
     * method needs to update nextId while loading so we're stuck with it up here.
     */
    protected synchronized long nextId() {
        if (!init)
            initialize();
        int i = 1;
        Ssg prototype = new Ssg(i);
        while (ssgs.contains(prototype)) {
            prototype.setId(++i);
        }
        return i;
    }
}

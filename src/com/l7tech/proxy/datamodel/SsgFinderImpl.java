/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import java.beans.XMLDecoder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.HashMap;

import com.l7tech.common.util.FileUtils;
import com.l7tech.proxy.datamodel.exceptions.SsgNotFoundException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Provides read-only access to the ssgs.xml file.
 *
 * User: mike
 * Date: Jul 21, 2003
 * Time: 9:32:31 AM
 */
public class SsgFinderImpl implements SsgFinder {
    private static final Logger log = Logger.getLogger(SsgFinderImpl.class.getName());

    protected static final String STORE_DIR = System.getProperty("user.home") + File.separator + ".l7tech";
    protected static final String STORE_FILE = STORE_DIR + File.separator + "ssgs.xml";

    protected SortedSet ssgs = new TreeSet();
    protected HashMap hostCache = new HashMap();
    protected boolean init = false; // should be private; relaxed for performace

    private static class SsgFinderHolder {
        private static final SsgFinderImpl ssgFinder = new SsgFinderImpl();
    }

    /** Get a singleton SsgFinderImpl. */
    public static SsgFinderImpl getSsgFinderImpl() {
        return SsgFinderHolder.ssgFinder;
    }

    protected SsgFinderImpl() {
    }

    /**
     * Ensure that this instance is initialized.
     * Load our SSG state from disk if it hasn't been done yet.
     */
    protected synchronized void initialize() {
        if (!init) {
            load();
            init = true;
        }
    }

    /**
     * Rebuild our SSG-to-hostname cache.
     */
    protected synchronized void rebuildHostCache() {
        hostCache.clear();
        for (Iterator i = ssgs.iterator(); i.hasNext();) {
            Ssg ssg = (Ssg) i.next();
            hostCache.put(ssg.getSsgAddress(), ssg);
        }
    }

    /**
     * Unconditionally load our SSG state from disk.
     */
    public synchronized void load() {
        FileInputStream in = null;
        XMLDecoder decoder = null;
        try {
            in = FileUtils.loadFileSafely(STORE_FILE);
            decoder = new XMLDecoder(in);
            final Collection newssgs = (Collection)decoder.readObject();
            if (newssgs != null) {
                ssgs.clear();
                ssgs.addAll(newssgs);
            }
        } catch (FileNotFoundException e) {
            log.info("No Gateway store found -- will create a new one");
        } catch (IOException e) {
            log.log(Level.SEVERE, "Unable to load Gateway store", e);
        } catch (ClassCastException e) {
            log.log(Level.SEVERE, "Badly formatted Gateway store " + STORE_FILE, e);
        } finally {
            if (decoder != null)
                decoder.close();
            if (in != null)
                try { in.close(); } catch (IOException e) {}
        }
        rebuildHostCache();
        init = true;
    }

    /**
     * Get the list of Ssgs known to this client proxy.
     * @return A List of the canonical Ssg objects.
     *         The List is read-only but the Ssg objects it contains are the real deal.
     */
    public synchronized List getSsgList() {
        if (!init)
            initialize();
        return Collections.unmodifiableList(new ArrayList(ssgs));
    }

    /**
     * Find the Ssg with the specified ID.
     * @param id the ID to look for (ie, 3)
     * @return The requested Ssg.  Never null.
     * @throws SsgNotFoundException If the specified ID was not found.
     */
    public synchronized Ssg getSsgById(final long id) throws SsgNotFoundException {
        Ssg found = getSsgByIdFast(id);
        if (found == null)
            throw new SsgNotFoundException("No Gateway is registered with the id " + id);
        return found;
    }

    /**
     * Find the Ssg with the specified ID.
     * @param id the ID to look for (ie, 3)
     * @return the Ssg with the specified id, or NULL if it wasn't found.
     */
    private synchronized Ssg getSsgByIdFast(final long id) {
        if (!init)
            initialize();
        if (id == 0)
            throw new IllegalArgumentException("Must provide a valid ID");
        Ssg prototype = new Ssg(id);
        SortedSet foundSet = ssgs.tailSet(prototype);
        if (foundSet.isEmpty() || !prototype.equals(foundSet.first()))
            return null;
        return (Ssg) foundSet.first();
    }

    /**
     * Find the Ssg with the specified local endpoint.  If multiple Ssgs have the same endpoint
     * only the first one is returned.
     *
     * @param endpoint The endpoint to look for (ie, "SSG0")
     * @return The requested Ssg.  Never null.
     * @throws com.l7tech.proxy.datamodel.exceptions.SsgNotFoundException If the specified endpoint was not found.
     */
    public synchronized Ssg getSsgByEndpoint(final String endpoint) throws SsgNotFoundException {
        if (!init)
            initialize();
        for (Iterator i = ssgs.iterator(); i.hasNext();) {
            final Ssg ssg = (Ssg)i.next();
            if (endpoint.equals(ssg.getLocalEndpoint()))
                return ssg;
        }
        throw new SsgNotFoundException("No Gateway is registered with the local endpoint " + endpoint);
    }

    /**
     * Find the Ssg with the specified hostname.  If multiple Ssgs have the same hostname only one of them
     * will be returned.
     * @param hostname The hostname to look for.
     * @return A registered Ssg with that hostname.
     * @throws com.l7tech.proxy.datamodel.exceptions.SsgNotFoundException if no Ssg was registered with the specified hostname.
     */
    public Ssg getSsgByHostname(String hostname) throws SsgNotFoundException {
        if (!init)
            initialize();
        Ssg ssg = (Ssg) hostCache.get(hostname);
        if (ssg == null) {
            // on cache miss, do complete search before giving up
            for (Iterator i = ssgs.iterator(); i.hasNext();) {
                ssg = (Ssg) i.next();
                if (hostname == ssg.getSsgAddress() || (hostname != null && hostname.equals(ssg.getSsgAddress()))) {
                    hostCache.put(ssg.getSsgAddress(), ssg);
                    return ssg;
                }
            }
            throw new SsgNotFoundException("No Gateway was found with the specified hostname.");
        }
        return ssg;
    }

    /**
     * Get the default SSG.
     * Returns the first SSG that has its Default flag set.  Usually there is only one such SSG.
     * @return the Default SSG
     * @throws com.l7tech.proxy.datamodel.exceptions.SsgNotFoundException if no Default SSG was found
     */
    public Ssg getDefaultSsg() throws SsgNotFoundException {
        if (!init)
            initialize();
        for (Iterator i = ssgs.iterator(); i.hasNext();) {
            Ssg ssg = (Ssg) i.next();
            if (ssg.isDefaultSsg())
                return ssg;
        }
        throw new SsgNotFoundException("No default Gateway is currently registered.");
    }

    /**
     * Notify that one of an Ssg's fields might have changed, possibly requiring a rebuild of one or
     * more lookup caches.
     * @param ssg The SSG that was modified.  If null, will assume that all SSGs might have been modified.
     */
    public void onSsgUpdated(Ssg ssg) {
        if (!init)
            initialize();
        rebuildHostCache();
    }
}

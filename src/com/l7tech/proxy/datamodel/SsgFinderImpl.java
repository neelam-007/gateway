/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import org.apache.log4j.Category;

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

/**
 * Provides read-only access to the ssgs.xml file.
 *
 * User: mike
 * Date: Jul 21, 2003
 * Time: 9:32:31 AM
 */
public class SsgFinderImpl implements SsgFinder {
    private static final Category log = Category.getInstance(SsgFinderImpl.class);

    protected static final String STORE_DIR = System.getProperty("user.home") + File.separator + ".l7tech";
    protected static final String STORE_FILE = STORE_DIR + File.separator + "ssgs.xml";
    protected static final String STORE_FILE_NEW = STORE_DIR + File.separator + "ssgs_xml.OLD";
    protected static final String STORE_FILE_OLD = STORE_DIR + File.separator + "ssgs_xml.NEW";

    protected SortedSet ssgs = new TreeSet();
    protected boolean init = false; // should be private; relaxed for performace
    private long nextId = 1;

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
     * Unconditionally load our SSG state from disk.
     */
    public synchronized void load() {
        FileInputStream in = null;
        XMLDecoder decoder = null;
        try {
            try {
                in = new FileInputStream(STORE_FILE);
            } catch (FileNotFoundException e) {
                // Check for an interrupted update operation
                in = new FileInputStream(STORE_FILE_OLD);
            }
            decoder = new XMLDecoder(in);
            final Collection newssgs = (Collection)decoder.readObject();
            if (newssgs != null) {
                ssgs.clear();
                ssgs.addAll(newssgs);
                for (Iterator i = ssgs.iterator(); i.hasNext();) {
                    long id  = ((Ssg)i.next()).getId();
                    if (nextId <= id)
                        nextId = id + 1;
                }
            }
        } catch (FileNotFoundException e) {
            log.info("No SSG store found -- will create a new one");
        } catch (IOException e) {
            log.error(e);
        } catch (ClassCastException e) {
            log.error("Badly formatted SSG store " + STORE_FILE, e);
        } finally {
            if (decoder != null)
                decoder.close();
            if (in != null)
                try { in.close(); } catch (IOException e) {}
        }
        init = true;
    }

    /**
     * Get the list of Ssgs known to this client proxy.
     * @return A List of the canonical Ssg objects.
     *         The List is read-only but the Ssg objects it contains are the real deal.
     */
    public synchronized List getSsgList() {
        initialize();
        return Collections.unmodifiableList(new ArrayList(ssgs));
    }

    /**
     * Find the Ssg with the specified ID.
     * @param id the ID to look for (ie, 3)
     * @return The requested Ssg.  Never null.
     * @throws SsgNotFoundException If the specified name was not found.
     */
    public synchronized Ssg getSsgById(final long id) throws SsgNotFoundException {
        if (!init)
            initialize();
        for (Iterator i = ssgs.iterator(); i.hasNext();) {
            final Ssg ssg = (Ssg)i.next();
            if (id == ssg.getId())
                return ssg;
        }
        throw new SsgNotFoundException("No SSG is registered with the id " + id);
    }

    /**
     * Find the Ssg with the specified name.  If multiple Ssgs have the same name only the
     * first one is returned.
     *
     * @param name the name to look for (ie, "R&D Gateway")
     * @return The requested Ssg.  Never null.
     * @throws SsgNotFoundException If the specified name was not found.
     */
    public synchronized Ssg getSsgByName(final String name) throws SsgNotFoundException {
        if (!init)
            initialize();
        for (Iterator i = ssgs.iterator(); i.hasNext();) {
            final Ssg ssg = (Ssg)i.next();
            if (name.equals(ssg.getName()))
                return ssg;
        }
        throw new SsgNotFoundException("No SSG is registered with the name " + name);
    }

    /**
     * Find the Ssg with the specified local endpoint.  If multiple Ssgs have the same endpoint
     * only the first one is returned.
     *
     * @param endpoint The endpoint to look for (ie, "SSG0")
     * @return The requested Ssg.  Never null.
     * @throws SsgNotFoundException If the specified endpoint was not found.
     */
    public synchronized Ssg getSsgByEndpoint(final String endpoint) throws SsgNotFoundException {
        if (!init)
            initialize();
        for (Iterator i = ssgs.iterator(); i.hasNext();) {
            final Ssg ssg = (Ssg)i.next();
            if (endpoint.equals(ssg.getLocalEndpoint()))
                return ssg;
        }
        throw new SsgNotFoundException("No SSG is registered with the local endpoint " + endpoint);
    }

    /**
     * Get the default SSG.
     * Returns the first SSG that has its Default flag set.  Usually there is only one such SSG.
     * @return the Default SSG
     * @throws SsgNotFoundException if no Default SSG was found
     */
    public Ssg getDefaultSsg() throws SsgNotFoundException {
        for (Iterator i = ssgs.iterator(); i.hasNext();) {
            Ssg ssg = (Ssg) i.next();
            if (ssg.isDefaultSsg())
                return ssg;
        }
        throw new SsgNotFoundException("No default SSG is currently registered.");
    }

    /**
     * Increment our ID counter, and return the next one.
     * This doesn't really belong in the FinderImpl -- it belongs in the subclasses -- but our load()
     * method needs to update nextId while loading so we're stuck with it up here.
     */
    protected synchronized long nextId() {
        if (!init)
            initialize();
        return nextId++;
    }
}

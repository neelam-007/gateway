package com.l7tech.proxy.datamodel;

import org.apache.log4j.Category;

import java.util.*;
import java.beans.XMLEncoder;
import java.beans.XMLDecoder;
import java.io.*;

/**
 * Package private implementation of an SsgManager.
 * User: mike
 * Date: Jun 2, 2003
 * Time: 1:51:56 PM
 */
class SsgManagerImpl implements SsgManager {
    private static final Category log = Category.getInstance(SsgManagerImpl.class);
    private static final SsgManagerImpl INSTANCE = new SsgManagerImpl();

    private static final String STORE_DIR = System.getProperty("user.home") + File.separator + ".l7proxy";
    private static final String STORE_FILE = STORE_DIR + File.separator + "ssgs.xml";

    private SortedSet ssgs = new TreeSet();
    private boolean init = false;

    private SsgManagerImpl() {
    }

    /** Get the singleton SsgManagerImpl. */
    public static SsgManagerImpl getInstance() {
        return INSTANCE;
    }

    /**
     * Erase our SSG list.
     */
    public synchronized void clear() {
        ssgs.clear();
    }

    /**
     * Load our SSG state from disk if it hasn't been done yet.
     */
    private synchronized void initialize() {
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
            in = new FileInputStream(STORE_FILE);
            decoder = new XMLDecoder(in);
            final Collection newssgs = (Collection)decoder.readObject();
            if (newssgs != null) {
                clear();
                ssgs.addAll(newssgs);
            }
        } catch (FileNotFoundException e) {
            log.info(e);
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
     * Save our SSG state to disk.
     */
    public synchronized void save() throws IOException {
        FileOutputStream out = null;
        XMLEncoder encoder = null;

        // If mkdir fails, new FileOutputStream will throw FileNotFoundException
        new File(STORE_DIR).mkdir();

        try {
            out = new FileOutputStream(STORE_FILE);
            encoder = new XMLEncoder(out);
            encoder.writeObject(ssgs);
        } finally {
            if (encoder != null)
                encoder.close();
            if (out != null)
                out.close();
        }
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
     * Find the Ssg with the specified name.  If multiple Ssgs have the same name only the
     * first one is returned.
     *
     * @param name the name to look for (ie, "R&D Gateway")
     * @return The requested Ssg.  Never null.
     * @throws com.l7tech.proxy.datamodel.SsgNotFoundException If the specified name was not found.
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
     * @throws com.l7tech.proxy.datamodel.SsgNotFoundException If the specified endpoint was not found.
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
     * Register a new Ssg with this client proxy.  Takes no action if an Ssg that equals() the new object is
     * already registered.
     * @param ssg The new Ssg.
     * @return true iff. the given ssg was not already registered
     */
    public synchronized boolean add(final Ssg ssg) {
        if (!init)
            initialize();
        return ssgs.add(ssg);
    }

    /**
     * Forget all about a registered Ssg.
     *
     * @param ssg The Ssg to forget about.
     * @throws com.l7tech.proxy.datamodel.SsgNotFoundException If the specified Ssg was not found.
     */
    public synchronized void remove(final Ssg ssg) throws SsgNotFoundException {
        if (!init)
            initialize();
        if (!ssgs.remove(ssg))
            throw new SsgNotFoundException("The specified SSG was not found");
    }
}

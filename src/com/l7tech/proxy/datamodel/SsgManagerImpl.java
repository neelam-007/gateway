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

    private static final String STORE_DIR = System.getProperty("user.home") + File.separator + ".l7proxy";
    private static final String STORE_FILE = STORE_DIR + File.separator + "ssgs.xml";
    private static final String STORE_FILE_NEW = STORE_DIR + File.separator + "ssgs_xml.OLD";
    private static final String STORE_FILE_OLD = STORE_DIR + File.separator + "ssgs_xml.NEW";

    private SortedSet ssgs = new TreeSet();
    private boolean init = false;
    private long nextId = 1;

    private SsgManagerImpl() {
    }

    private static class SsgManagerHolder {
        private static final SsgManagerImpl ssgManager = new SsgManagerImpl();
    }

    /** Get the singleton SsgManagerImpl. */
    public static SsgManagerImpl getInstance() {
        return SsgManagerHolder.ssgManager;
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
            try {
                in = new FileInputStream(STORE_FILE);
            } catch (FileNotFoundException e) {
                // Check for an interrupted update operation
                in = new FileInputStream(STORE_FILE_OLD);
            }
            decoder = new XMLDecoder(in);
            final Collection newssgs = (Collection)decoder.readObject();
            if (newssgs != null) {
                clear();
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
     * Save our SSG state to disk.  Caller is responsible for ensuring that only one process will be
     * calling this method at any given time.
     */
    /*
     *    oldFile   curFile  newFile  Description                    Action to take
     *    --------  -------  -------  -----------------------------  --------------------------------
     *  1    -         -        -     Newly created store file       (>newFile) => curFile
     *  2    -         -        +     Create was interrupted         -newFile; (do #1)
     *  3    -         +        -     Normal operation               curFile => oldFile; (do #1); -oldFile
     *  4    -         +        +     Update was interrupted         -newFile; (do #3)
     *  5    +         -        -     Update was interrupted         oldFile => curFile; (do #3)
     *  6    +         -        +     Update was interrupted         -newFile; (do #5)
     *  7    +         +        -     Update was interrupted         -oldFile; (do #3)
     *  8    +         +        +     Invalid; can't happen          -newFile; -oldFile; (do #3)
     *
     *  We guarantee to end up in state #3 if we complete successfully.
     */
    public synchronized void save() throws IOException {
        // TODO: add lockfile so multiple Client Proxy instances can safely share a data store

        FileOutputStream out = null;
        XMLEncoder encoder = null;

        // If mkdir fails, new FileOutputStream will throw FileNotFoundException
        new File(STORE_DIR).mkdir();

        try {
            File oldFile = new File(STORE_FILE_OLD);
            File curFile = new File(STORE_FILE);
            File newFile = new File(STORE_FILE_NEW);

            // At start: any state is possible

            if (oldFile.exists() && !curFile.exists())
                oldFile.renameTo(curFile);
            // States 5 and 6 now ruled out

            if (newFile.exists())
                newFile.delete();
            // States 2, 4, 6 and 8 now ruled out

            if (oldFile.exists())
                oldFile.delete();
            // States 5, 6, 7, and 8 now ruled out

            // We are now in either State 1 or State 3

            out = new FileOutputStream(newFile);
            encoder = new XMLEncoder(out);
            encoder.writeObject(ssgs);
            encoder.close();
            encoder = null;
            out = null;

            // If interrupted here, we end up in State 4 (or State 2 if no existing file)

            if (curFile.exists())
                if (!curFile.renameTo(oldFile))
                    // If we need to do this, it has to succeed
                    throw new IOException("Unable to rename " + curFile + " to " + oldFile);

            // If interrupted here, we end up in State 6 (or State 2 if was no existing file)

            if (!newFile.renameTo(curFile))
                // This must succeed in order for the update to complete
                throw new IOException("Unable to rename " + newFile + " to " + curFile);

            // If interrupted here, we end up in State 7 (or State 3 if was no existing file)

            oldFile.delete();

            // We are now in State 3 (invariant)

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
     * Get the next unused Ssg Id.
     */
    public synchronized long nextId() {
        if (!init)
            initialize();
        return nextId++;
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
            throw new IllegalArgumentException("Unable to register ssg: it has not been assigned an ID");
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

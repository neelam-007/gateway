package com.l7tech.proxy.datamodel;

import org.apache.log4j.Category;

import java.beans.XMLEncoder;
import java.io.File;
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
    private static final Category log = Category.getInstance(SsgManagerImpl.class);

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
    }

    /**
     * Save our SSG state to disk.  Caller is responsible for ensuring that only one process will be
     * calling this method at any given time.
     *
     * <pre>
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
     *</pre>
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

    /**
     * Set the default SSG.
     * If this method returns, it's guaranteed that the specified Ssg
     * is in the Ssg list and is the only one with its Default flag set to true.
     * @param ssg
     * @throws SsgNotFoundException
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
            throw new SsgNotFoundException("The requested default SSG is not currently registered.");
    }
}

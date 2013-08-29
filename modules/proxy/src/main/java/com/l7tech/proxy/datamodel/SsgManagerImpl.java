package com.l7tech.proxy.datamodel;

import com.l7tech.util.FileUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.proxy.datamodel.exceptions.SsgNotFoundException;

import java.beans.XMLEncoder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.Iterator;

/**
 * Extends SsgFinderImpl to support saving state back to the ssgs.xml file.
 * User: mike
 * Date: Jun 2, 2003
 * Time: 1:51:56 PM
 */
public class SsgManagerImpl extends SsgFinderImpl implements SsgManager {
    protected File LOCK_FILE = new File(getStoreDir() + File.separator + "confdir.lck");

    private Thread shutdownHook = null;
    private RandomAccessFile lockRaf = null;
    private FileLock lock = null;

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
        endpointCache.clear();
    }

    /**
     * Save our SSG state to disk.  Caller is responsible for ensuring that only one process will be
     * calling this method at any given time; see {@link #lockConfiguration} for a way to do this.
     */
    public synchronized void save() throws IOException {
        FileUtils.saveFileSafely(getStorePath(), new FileUtils.Saver() {
            public void doSave(FileOutputStream fos) {
                XMLEncoder encoder = null;
                try {
                    encoder = new XMLEncoder(fos);
                    encoder.writeObject(ssgs);
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

    /** Exception throws if the configuration directory is already locked by some other SsgManagerImpl instance. */
    public static class ConfigurationAlreadyLockedException extends Exception {
        public ConfigurationAlreadyLockedException(String message) {
            super(message);
        }
    }

    /**
     * Attempt to obtain write ownership of the Bridge ssgs.xml file.
     * If the lock was successful this method returns.  It fails with an exception if another process
     * has locked the configuration or if there is an IOException while trying to obtain a lock.
     * <p/>
     * If the configuration is already owned by this SsgManagerImpl instance this method succeeds immediately.
     * <p/>
     * This lock is advisory, but is respected by other users of SsgManagerImpl that call lockConfiguration().
     * <p/>
     * The configuration can still be read by SsgFinder instances, including other SsgManagerImpl instances
     * that don't try to lock the configuration.
     * <p/>
     * A shutdown hook will be registered that calls {@link #releaseConfiguration()} during JVM shutdown
     * if it hasn't already been called by then.
     *
     * @throws ConfigurationAlreadyLockedException  if write ownership of the ssgs.xml file is already claimed by some
     *                                              other SsgManagerImpl instance
     * @throws IOException if there is some other problem creating or locking the lock file
     */
    public synchronized void lockConfiguration() throws ConfigurationAlreadyLockedException, IOException {
        if (lock != null) return; // already locked by us

        boolean gotLock = false;
        try {
            //noinspection IOResourceOpenedButNotSafelyClosed
            lockRaf = new RandomAccessFile(LOCK_FILE, "rw");
            //noinspection ChannelOpenedButNotSafelyClosed
            lock = lockRaf.getChannel().tryLock();
            if (lock == null) throw new ConfigurationAlreadyLockedException("configuration directory is already in use");  

            shutdownHook = new Thread() {
                public void run() {
                    shutdownHook = null; // avoid illegal call to removeShutdownHook() during shutdown process
                    releaseConfiguration();
                }
            };
            Runtime.getRuntime().addShutdownHook(shutdownHook);

            gotLock = true;
        } finally {
            if (!gotLock) releaseConfiguration();
        }
    }

    /**
     * Relinquish write ownership of the Bridge ssgs.xml file.
     * This method always succeeds.
     */
    public synchronized void releaseConfiguration() {
        if (shutdownHook != null)
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        shutdownHook = null;
        if (lock != null)
            LOCK_FILE.delete();  // delete before releasing lock to avoid race condition.  hopefully this'll work on windows too
        ResourceUtils.closeQuietly(lock);
        lock = null;
        ResourceUtils.closeQuietly(lockRaf);
        lockRaf = null;
    }


    /**
     * Find the lowest unused ID number.
     * @return the lowest unused Ssg ID.
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

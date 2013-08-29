package com.l7tech.proxy.datamodel;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.proxy.datamodel.exceptions.SsgNotFoundException;
import com.l7tech.util.*;
import org.jetbrains.annotations.NotNull;

import java.beans.ExceptionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides read-only access to the ssgs.xml file.
 *
 * User: mike
 * Date: Jul 21, 2003
 * Time: 9:32:31 AM
 */
public class SsgFinderImpl implements SsgFinder {
    private static final Logger log = Logger.getLogger(SsgFinderImpl.class.getName());

    protected long lastLoaded = Long.MIN_VALUE; // time that config file was last loaded
    protected SortedSet ssgs = new TreeSet();
    protected HashMap hostCache = new HashMap();
    protected HashMap endpointCache = new HashMap();
    protected boolean init = false;
    String storePath = null;
    static ExceptionListener exceptionListener = null;

    private static class SsgFinderHolder {
        private static final SsgFinderImpl ssgFinder = new SsgFinderImpl();
    }

    /** Get a singleton SsgFinderImpl. */
    public static SsgFinderImpl getSsgFinderImpl() {
        return SsgFinderHolder.ssgFinder;
    }

    protected SsgFinderImpl() {
    }

    /** @return the path the config file we are saving and loading from. */
    public String getStorePath() {
        return storePath != null ? storePath : getStoreDir() + File.separator + "ssgs.xml";
    }

    protected File getStoreFile() {
        return new File(getStorePath());
    }

    public String getStoreDir() {
        return Ssg.PROXY_CONFIG;
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
        endpointCache.clear();
        for (Iterator i = ssgs.iterator(); i.hasNext();) {
            Ssg ssg = (Ssg) i.next();
            hostCache.put(ssg.getSsgAddress(), ssg);
            Ssg existingWithThisEndpoint = (Ssg)endpointCache.put(ssg.getLocalEndpoint(), ssg);
            if (existingWithThisEndpoint != null)
                log.warning("Duplicate Gateway Account label \"" + ssg.getLocalEndpoint() + "\": " + ssg + " and " + existingWithThisEndpoint);
        }
    }

    /**
     * Unconditionally load our SSG state from disk.
     */
    public synchronized void load() {
        FileUtils.LastModifiedFileInputStream in = null;
        SafeXMLDecoder decoder = null;
        try {
            in = FileUtils.loadFileSafely(getStorePath());
            decoder = new SafeXMLDecoder(getClassFilter(), in, null, exceptionListener, null);
            final Collection newssgs = (Collection)decoder.readObject();
            if (newssgs != null) {
                ssgs.clear();
                ssgs.addAll(newssgs);
            }
            lastLoaded = in.getLastModified();
        } catch (FileNotFoundException e) {
            log.info("No Gateway store found -- will create a new one");
        } catch (IOException e) {
            log.log(Level.SEVERE, "Unable to load Gateway store", e);
        } catch (ClassCastException e) {
            log.log(Level.SEVERE, "Badly formatted Gateway store " + getStorePath(), e);
        } finally {
            if (decoder != null)
                decoder.close();
            if (in != null)
                try { in.close(); } catch (IOException e) {}
        }
        rebuildHostCache();
        init = true;
    }

    ClassFilter getClassFilter() {
        Set<String> classes = new HashSet<>(Arrays.asList(
            "java.lang.String",
            "java.util.TreeSet",
            "java.util.HashMap",
            "java.util.LinkedList",
            "java.util.LinkedHashMap"
        ));
        Set<String> constructors = new HashSet<>(Arrays.asList(
            "java.util.TreeSet()",
            "java.util.HashMap()",
            "java.util.LinkedList()",
            "java.util.LinkedHashMap()"
        ));
        Set<String> methods = new HashSet<>(Arrays.asList(
            "java.lang.reflect.Array.set(java.lang.Object,int,java.lang.Object)",
            "java.util.LinkedList.add(java.lang.Object)",
            "java.util.TreeSet.add(java.lang.Object)",
            "java.util.HashMap.remove(java.lang.Object)", // TODO do we really need to allow remove?
            "java.util.HashMap.put(java.lang.Object,java.lang.Object)"
        ));

        ClassFilter staticFilter = new StringClassFilter(classes, constructors, methods);
        ClassFilter annotationFilter = new AnnotationClassFilter(null, Arrays.asList("com.l7tech.")) {
            @Override
            protected boolean permitClass(@NotNull Class<?> clazz) {
                return super.permitClass(clazz) || Assertion.class.isAssignableFrom(clazz);
            }

            @Override
            public boolean permitConstructor(@NotNull Constructor<?> constructor) {
                if (super.permitConstructor(constructor))
                    return true;

                if (Assertion.class.isAssignableFrom(constructor.getDeclaringClass())) {
                    if (constructor.getParameterTypes().length < 1)
                        return true;
                }

                return false;
            }

            @Override
            public boolean permitMethod(@NotNull Method method) {
                if (super.permitMethod(method))
                    return true;

                if (Assertion.class.isAssignableFrom(method.getDeclaringClass())) {
                    if (method.getName().startsWith("set") && !"set".equals(method.getName()))
                        return true;

                    // TODO this may not be safe if any assertion returns an unsafe class from a getter
                    if (method.getName().startsWith("get") && !"get".equals(method.getName()))
                        return true;
                }

                return false;
            }
        };
        return new CompositeClassFilter(staticFilter, annotationFilter);
    }

    /**
     * Reload config file from disk if it has changed since last time we loaded it.
     */
    public void loadIfChanged() {
        if (getStoreFile().lastModified() > lastLoaded) {
            log.info("Reloading SSG configuration from " + getStorePath());
            load();
        }
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
        Ssg ssg = (Ssg)endpointCache.get(endpoint);
        if (ssg != null)
            return ssg;
        // on cache miss, do complete search before giving up
        for (Iterator i = ssgs.iterator(); i.hasNext();) {
            ssg = (Ssg)i.next();
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

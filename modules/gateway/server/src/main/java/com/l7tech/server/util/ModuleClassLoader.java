package com.l7tech.server.util;

import com.l7tech.util.HexUtils;
import com.l7tech.util.ResourceUtils;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A ClassLoader for modules.
 *
 * <p>Modules are somewhat similar to WAR files, except that any classes are in
 * the root instead of a "classes" directory and there is no meta information.</p>
 *
 * <p>So perhaps its more of a regular JAR but with a lib directory ...</p>
 *
 * <p>Do NOT create multiple instances with the same working directory and id.</p>
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class ModuleClassLoader extends ClassLoader {

    //- PUBLIC

    /**
     * Create a module class loader with the given options.
     *
     * <p>Note that the loader will delete any directories under the working
     * directory that look like "owned" directories.</p>
     *
     * @param parent          The parent classloader for all module ClassLoaders
     * @param id              The identifier for this module loader
     * @param resource        Only load modules which contain this resource (may be null)
     * @param moduleDirectory The readable directory with modules to load
     * @param workDirectory   The working directory for expanded modules
     */
    public ModuleClassLoader(ClassLoader parent, String id, String resource, File moduleDirectory, File workDirectory) {
        this(parent, id, resource, moduleDirectory, workDirectory, null);
    }

    /**
     * Create a module class loader with the given options.
     *
     * <p>Note that the loader will delete any directories under the working
     * directory that look like "owned" directories.</p>
     *
     * @param parent          The parent classloader for all module ClassLoaders
     * @param id              The identifier for this module loader
     * @param resource        Only load modules which contain this resource (may be null)
     * @param moduleDirectory The readable directory with modules to load
     * @param workDirectory   The working directory for expanded modules
     * @param finalLoader     This loader will be treated as the "final" module in the chain (may be null)
     */
    public ModuleClassLoader(ClassLoader parent, String id, String resource, File moduleDirectory, File workDirectory, ClassLoader finalLoader) {
        super(parent);

        if (moduleDirectory == null) throw new IllegalArgumentException("moduleDirectory must not be null");
        if (workDirectory == null) throw new IllegalArgumentException("workDirectory must not be null");

        cleanExisting(workDirectory, id);

        delegateLoaders = buildModuleLoaders(parent, finalLoader, moduleDirectory, workDirectory, id, resource);
    }

    //- PROTECTED

    /**
     * Find the class in one of the modules.
     *
     * @param name The class name
     * @return The class
     * @throws ClassNotFoundException if not in any module
     */
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> clazz = null;

        for (ClassLoader delegateClassLoader : delegateLoaders) {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                if (contextClassLoader != null) Thread.currentThread().setContextClassLoader(null);
                clazz = delegateClassLoader.loadClass(name);
            }
            catch(ClassNotFoundException cnfe) {
                // ignore
            }
            finally {
                if (contextClassLoader != null) Thread.currentThread().setContextClassLoader(contextClassLoader);
            }
            if (clazz != null) break;
        }

        if (clazz == null) throw new ClassNotFoundException(name);

        return clazz;
    }

    /**
     * Find the resource in a module (in listed order)
     *
     * @param name The resource name
     * @return the URL or null
     */
    protected URL findResource(String name) {
        URL url = null;

        for (ClassLoader delegateClassLoader : delegateLoaders) {
            url = delegateClassLoader.getResource(name);
            if (url != null) break;
        }

        return url;
    }

    /**
     * Find all the resource URLs (in listed order)
     *
     * @param name The resource name
     * @return the Enumeration (never null)
     * @throws IOException if an underlying loader throws IOException
     */
    protected Enumeration<URL> findResources(String name) throws IOException {
        Set<URL> urlList = new LinkedHashSet<URL>();

        for (ClassLoader delegateClassLoader : delegateLoaders) {
            urlList.addAll(Collections.list(delegateClassLoader.getResources(name)));
        }

        // since all the delegate loaders share a parent with this loader we must remove the parents
        // items from this list else there will be duplicates
        if (!delegateLoaders.isEmpty()) {
            ClassLoader parentLoader = delegateLoaders.get(0).getParent();
            if (parentLoader != null) urlList.removeAll(Collections.list(parentLoader.getResources(name)));
        }

        return Collections.enumeration(urlList);
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ModuleClassLoader.class.getName());
    private static final String EXPANDED_MODULE_DIR_PREFIX = "x-module-";
    private static final String JAR_EXTENSION = ".jar";
    private static final String ZIP_EXTENSION = ".zip";
    private static final String LIB_PREFIX = "lib/";

    private final List<ClassLoader> delegateLoaders;

    /**
     * Ensure any old expanded module directories are cleaned.
     *
     * @param workDirectory The working directory to clean
     * @param id            The id for this loader
     */
    private void cleanExisting(File workDirectory, String id) {
        File[] files = workDirectory.listFiles();
        if (files != null)  for (File file : files) {
            if (file.isDirectory()) {
                if(file.getName().startsWith(EXPANDED_MODULE_DIR_PREFIX + id)) {
                    File[] libs = file.listFiles();
                    if (libs != null) for (File lib : libs) {
                        lib.delete();
                    }
                    file.delete();
                }
            }
        }
    }

    /**
     * Create ClassLoaders for all modules.
     *
     * <p>This will:</p>
     *
     * <ul>
     *   <li>List the jars in the moduleDirectory</li>
     *   <li>Extract libs for each module to the working directory</li>
     *   <li>Create a URLClassLoader with the original jar and all the lib jars</li>
     * </ul>
     *
     * @return The list of (URL)ClassLoaders
     */
    private List<ClassLoader> buildModuleLoaders(ClassLoader parent,
                                                 ClassLoader finalLoader,
                                                 File moduleDirectory,
                                                 File workDirectory,
                                                 String id,
                                                 String resource) {
        List<ClassLoader> loaders = new ArrayList<ClassLoader>();
        File[] possibleModules = moduleDirectory.listFiles();

        if (possibleModules != null) {
            for (File module : possibleModules) {
                if (module.getName().endsWith(JAR_EXTENSION)) {
                    ClassLoader mloader = buildModuleLoader(module, parent, workDirectory, id, resource);
                    if (mloader != null)
                        loaders.add(mloader);
                }
            }
        }

        if (finalLoader != null)
            loaders.add(finalLoader);

        return Collections.unmodifiableList(loaders);
    }

    /**
     *
     */
    private ClassLoader buildModuleLoader(File moduleJar, ClassLoader parent, File workDirectory, String id, String resource) {
        ClassLoader loader = null;

        File moduleLibDirectory = new File(workDirectory, EXPANDED_MODULE_DIR_PREFIX + id + "-" + moduleJar.getName());
        moduleLibDirectory.deleteOnExit();
        boolean error = false;
        boolean resourceFound = resource==null;

        List<File> moduleJarFiles = new ArrayList<File>();
        moduleJarFiles.add(moduleJar);

        String resourceNS = resource;
        String resourceS = resource;
        if (resource != null) {
            if (resource.startsWith("/")) {
                resourceNS = resource.substring(1);
            }
            else {
                resourceS = "/" + resource;
            }
        }

        JarFile jarFile = null;
        try {
            jarFile = new JarFile(moduleJar);
            if (!resourceFound) {
                // check if we should use this module
                for(JarEntry entry : Collections.list(jarFile.entries())) {
                    if (entry.getName().startsWith(resourceNS) ||
                        entry.getName().startsWith(resourceS)) {
                        resourceFound = true;
                        break;
                    }
                }
            }

            if (resourceFound) {
                for(JarEntry entry : Collections.list(jarFile.entries())) {
                    if (!entry.isDirectory() &&
                        entry.getName().startsWith(LIB_PREFIX) &&
                        (entry.getName().endsWith(JAR_EXTENSION) || entry.getName().endsWith(ZIP_EXTENSION))) {
                        if (!moduleLibDirectory.exists()) {
                            if(!moduleLibDirectory.mkdirs()) {
                                error = true;
                                logger.warning("Could not create module temp directory '"+moduleLibDirectory.getAbsolutePath()+"'.");
                                break;
                            }
                        }

                        File moduleLib = new File(moduleLibDirectory, entry.getName().substring(LIB_PREFIX.length()));
                        if (!moduleLib.getParentFile().equals(moduleLibDirectory)) {
                            logger.info("Skipping lib in subdirectory '"+entry.getName()+"'.");
                            continue;
                        }

                        moduleLib.deleteOnExit();
                        moduleJarFiles.add(moduleLib);

                        InputStream entryIn = null;
                        OutputStream entryOut = null;
                        try {
                            entryIn = jarFile.getInputStream(entry);
                            entryOut = new BufferedOutputStream(new FileOutputStream(moduleLib));
                            HexUtils.copyStream(entryIn, entryOut);
                            entryOut.flush();
                        }
                        catch(IOException ioe) {
                            error = true;
                            logger.log(Level.WARNING, "Could not expand module lib '"+entry.getName()+
                                    "' to temp directory '"+moduleLibDirectory.getAbsolutePath()+"'.", ioe);
                            break;
                        }
                        finally {
                            ResourceUtils.closeQuietly(entryIn);
                            ResourceUtils.closeQuietly(entryOut);
                        }
                    }
                }
            }
        }
        catch (IOException ioe) {
            logger.log(Level.WARNING, "Could not load module jar '"+moduleJar.getAbsolutePath()+"'.", ioe);
        }
        finally {
            if (jarFile != null) try{ jarFile.close(); }catch(IOException ioe){
                logger.log(Level.FINE, "Error closing JarFile '"+moduleJar.getAbsolutePath()+"'.", ioe); };
        }

        return error || !resourceFound ? null : createLoader(parent, moduleJarFiles);
    }

    /**
     *
     */
    private ClassLoader createLoader(ClassLoader parent, List<File> jarFiles) {
        ClassLoader loader = null;
        try {
            List<URL> urlList = new ArrayList<URL>();
            for (File jarFile : jarFiles) {
                urlList.add(jarFile.toURI().toURL());
            }
            URL[] urls = urlList.toArray(new URL[urlList.size()]);
            if (parent == null) {
                loader = new URLClassLoader(urls);
            }
            else {
                loader = new URLClassLoader(urls, parent);
            }
        }
        catch(MalformedURLException murle) {
            logger.log(Level.WARNING, "Could not create module class loader", murle);
        }
        return loader;
    }
}

package com.l7tech.assertion.base.util.classloaders;

import com.l7tech.objectmodel.SaveException;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;

import java.io.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Upload JAR class loader. This classloader is meant to be used when we have libraries that we
 * want to use that need to be uploaded to the SSG manually by the user before we can start to use
 * them.
 */
public abstract class UploadJarClassLoader extends CustomClassLoader {

    private static final Logger logger = Logger.getLogger(UploadJarClassLoader.class.getName());

    private boolean initialized = false;
    private Map<String, Boolean> libraries = new HashMap<String, Boolean>();

    protected String ssgHome;
    protected HashSet<InitializationListener> initializationListeners = new HashSet<InitializationListener>();

    /**
     * Define the class for the listener, this listener will be invoked when ALL the required libraries
     * have been uploaded by the user.
     */
    public static interface InitializationListener {
        void initialized();
    }

    /**
     * The default constructor.
     *
     * @param parent the parent classloader
     * @param ssgHome the SSG home location
     */
    public UploadJarClassLoader(ClassLoader parent, String ssgHome) {
        super(parent);
        this.ssgHome = ssgHome;

        File libDir = new File(ssgHome + File.separator + "var" + File.separator + "lib" + File.separator);
        if(!libDir.exists()) libDir.mkdir();

        //Go through the libraries that need to be uploaded and store them before
        //we try to initialize the class loader
        Map<String, String> uploadLibrariesList = getDefinedLibrariesToUpload();
        for(String libraryToUpload : uploadLibrariesList.keySet()) {
            libraries.put(libraryToUpload, false);
        }

        initialize();
    }

    /**
     * Retrieves whether the class loader has been initialized.
     *
     * @return true if initialized, false otherwise
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Method which will notify the class loader, that a library has been added and that we should
     * be trying to initialize the library if we have not already done so.
     */
    public void notifyLibraryAdded() {
        initialized = false;
        initialize();
    }

    /**
     * Adds to the listeners that are interested in knowing if the class loader has been initialized. If it has
     * already been initialized then we automatically fire the initialized method.
     *
     * @param listener the listener to add
     */
    public void addInitializationListener(InitializationListener listener) {
        if(!initialized) {
            initializationListeners.add(listener);
        } else {
            listener.initialized();
        }
    }

    @Override
    public void initialize() {
        //If all libraries are uploaded, then we can start the initialization process
        if(checkLibrariesLoaded()) {
            try {
                //Do any post steps that are required after the required libraries are uploaded
                postLibraryLoad();
                initialized = true;

                //Fire the initialization listeners because initialization is complete
                for(InitializationListener listener : initializationListeners) {
                    try {
                        listener.initialized();
                    } catch(Throwable th) {
                        logger.log(Level.WARNING, "Error occured on listener fire when class loader initialized", th);
                    }
                }
                initializationListeners.clear();
            } catch (Exception e) {
                logger.log(Level.WARNING, "There was an exception when class loader initialization", e);
            }
        }
    }

    /**
     * Check if the libraries that are required have been loaded and are present. If they are present,
     * but have not been loaded then this method will also load them and mark them as such.

     * @return true if loaded, false otherwise
     */
    private boolean checkLibrariesLoaded() {
        boolean allFound = true;

        //Check if the libraries are uploaded, and the content is loaded, if
        //they are not loaded but exist then we need to load the contents
        for(String jar : libraries.keySet()) {
            File jarFile = new File(getJarPath() + jar);
            if(!jarFile.exists() || !jarFile.canRead()) {
                libraries.put(jar, false);
                allFound = false;
                break;
            } else {
                loadLibraryContents(jarFile);
                libraries.put(jar, true);
            }
        }

        return allFound;
    }

    /**
     * Method which will load the library contents of a file. This method will iterate through a JAR that is provided
     * and load all of the needed information from it using your custom class loader.
     *
     * @param libraryFile the library file to load
     */
    private void loadLibraryContents(File libraryFile) {
        try {

            //Create the needed streams in order to load the library contents
            ByteArrayOutputStream baos = null;
            JarInputStream jarIS = new JarInputStream(new FileInputStream(libraryFile));
            JarEntry entry = jarIS.getNextJarEntry();

            try {
                while(entry != null) {

                    if(!entry.isDirectory()) {
                            baos = new ByteArrayOutputStream();
                            IOUtils.copyStream(jarIS, baos);
                            boolean customLoadingRuleMatched = customLoadingRules(entry, baos);

                            if(!customLoadingRuleMatched) {
                                if(entry.getName().endsWith(".class")) {
                                    String className = entry.getName().substring(0, entry.getName().length() - 6).replace('/', '.');
                                    availableClassBytes.put(className, baos.toByteArray());

                                    // Define the package
                                    if(className.lastIndexOf('.') > -1) {
                                        String packageName = className.substring(0, className.lastIndexOf('.'));
                                        if(!loadedPackages.containsKey(packageName) && getPackage(packageName) == null) {
                                            Package p = definePackage(packageName, null, null, null, null, null, null, null);
                                            loadedPackages.put(packageName, p);
                                        }
                                    }
                                } else {
                                    availableResourceBytes.put(entry.getName(), baos.toByteArray());
                                }
                            }
                            baos.close();
                    }

                    entry = jarIS.getNextJarEntry();
                }
            } catch(IOException e) {
                logger.warning("Unable to load JAR entry (" + entry.getName() + ").");
            } finally {
                ResourceUtils.closeQuietly(jarIS);
                ResourceUtils.closeQuietly(baos);
            }
        } catch(FileNotFoundException e) {
            logger.warning("Unable to load the JAR file.");
        } catch(IOException e) {
            logger.warning("Unable to load the JAR file.");
        }
    }

    /**
     * Adds a library to the class loader.
     *
     * @param filename the file name of the library
     * @param bytes the actual bytes of the file
     *
     * @throws SaveException exception if save was not successful
     */
    public void addLibrary(String filename, byte[] bytes) throws SaveException {
        File libDir = new File(getJarPath());

        if(libDir.exists() && (!libDir.isDirectory() || !libDir.canRead())) {
            throw new SaveException("Unable to create library directory.");
        }

        if(!libDir.exists() && !libDir.mkdir()) {
            throw new SaveException("Unable to create library directory.");
        }

        File libraryFile = new File(libDir, filename);
        try {
            FileOutputStream fos = new FileOutputStream(libraryFile);
            fos.write(bytes);
            fos.close();
            notifyLibraryAdded();
        } catch(IOException e) {
            throw new SaveException("Unable to save library.");
        }
    }

    /**
     * Retrieves the installed libraries.
     *
     * @return the installed libraries.
     */
    public List<String> getInstalledLibraries() {
        File libDir = new File(getJarPath());
        ArrayList<String> results = new ArrayList<String>();

        if(!libDir.exists() || !libDir.isDirectory() || !libDir.canRead()) {
            return results;
        }

        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if(name.endsWith(".jar") || name.endsWith(".license")) {
                    return true;
                }

                return false;
            }
        };
        for(String name : libDir.list(filter)) {
            results.add(name);
        }

        return results;
    }

    /**
     * @return the string path to where you would like to upload the JAR libraries to
     */
    public abstract String getJarPath();

    /**
     * A map containing the file names and their md5 checksums. For example, to load abcd-1.2.3.jar, return
     * a map containing "abcd-1.2.3.jar", "cba484e032a9bd7d5165bddff51b22b7". If the md5 checksum is left blank,
     * no validation of the uploaded jar file will be made.
     * @return a map of libraries file names that need to be uploaded, mapped to their md5sum for checking
     * (left blank if no check needed)
     */
    public abstract Map<String, String> getDefinedLibrariesToUpload();

    /**
     * This method will be invoked when loading entries from a JAR file and you would like to do something custom with
     * the file
     */
    protected abstract boolean customLoadingRules(JarEntry entry, ByteArrayOutputStream baos);

    /**
     * This method will be invoked once all required libraries have been uploaded and there needs to be some special
     * initialization step that needs to take place
     * @throws Exception
     */
    protected abstract void postLibraryLoad() throws Exception;
}

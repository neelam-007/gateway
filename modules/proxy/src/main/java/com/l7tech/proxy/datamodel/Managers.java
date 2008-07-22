/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.proxy.datamodel;

import com.l7tech.common.mime.HybridStashManager;
import com.l7tech.common.mime.StashManager;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.wsp.WspConstants;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.Manifest;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;

/**
 * Used to obtain datamodel classes.
 */
public class Managers {
    private static final Logger logger = Logger.getLogger(Managers.class.getName());

    private static CredentialManager credentialManager = null;
    private static BridgeStashManagerFactory stashManagerFactory = new DefaultBridgeStashManagerFactory();
    private static int stashFileUnique = 1; // used to generate unique filenames for stashing large attachments
    private static AssertionRegistry assertionRegistry = null;
    private static final String PROP_ASSERTION_CLASSNAMES = "com.l7tech.proxy.policy.modularAssertionClassnames";

    public static final String[] BUNDLED_MODULAR_ASSERTIONS = new String[] {
    };

    /**
     * Get the CredentialManager.
     * @return the current CredentialManager instance.
     */
    public static CredentialManager getCredentialManager() {
        if (credentialManager == null) {
            credentialManager = CredentialManagerImpl.getInstance();
        }
        return credentialManager;
    }

    /**
     * Change the CredentialManager.
     * @param credentialManager the new CredentialManager to use.
     */
    public static void setCredentialManager(CredentialManager credentialManager) {
        Managers.credentialManager = credentialManager;
    }

    private static synchronized int getStashFileUnique() {
        return stashFileUnique++;
    }

    public static interface BridgeStashManagerFactory {
        StashManager createStashManager();
    }
    
    public static class DefaultBridgeStashManagerFactory implements BridgeStashManagerFactory {
        public StashManager createStashManager() {
            return new HybridStashManager(Ssg.ATTACHMENT_DISK_THRESHOLD, Ssg.ATTACHMENT_DIR,
                                          "att" + getStashFileUnique());
        }
    }

    /** Change the stash manager factory that createStashManager will use from now on. */
    public static void setBridgeStashManagerFactory(BridgeStashManagerFactory factory) {
        if (factory == null) throw new NullPointerException();
        stashManagerFactory = factory;
    }

    /**
     * Obtain a stash manager.  Currently always creates a new {@link HybridStashManager}.
     *
     * @return a new StashManager reader to stash input stream to RAM or disk according to their size.
     */
    public static StashManager createStashManager() {
        return stashManagerFactory.createStashManager();
    }

    public synchronized static AssertionRegistry getAssertionRegistry() {
        if (assertionRegistry == null) {
            assertionRegistry = new AssertionRegistry();
            if (WspConstants.getTypeMappingFinder() == null) {
                // Must be standalone SSB.   Attach default WspReader to assertion registry now
                WspConstants.setTypeMappingFinder(assertionRegistry);
            }

            for (String assname : BUNDLED_MODULAR_ASSERTIONS) {
                loadModularAssertion(assname, Managers.class.getClassLoader());
            }

            loadDynamicModularAssertions();
            loadModularAssertionsFromJars();
            loadModularAssertionsFromSystemProperty();
        }
        return assertionRegistry;
    }

    private static void loadModularAssertionsFromSystemProperty() {
        String assnames = System.getProperty(PROP_ASSERTION_CLASSNAMES);
        if (assnames == null) return;
        String[] names = assnames.split(",\\s*");
        if (names == null || names.length == 0) return;
        for (String assname : names) {
            loadModularAssertion(assname, Managers.class.getClassLoader());
        }
    }

    private static void loadModularAssertion(String name, ClassLoader loader) {
        try {
            Class assclass = loader.loadClass(name);
            if (assclass == null) return;
            assertionRegistry.registerAssertion(assclass);
            logger.info("Registered modular assertion " + assclass.getName());
        } catch (ClassNotFoundException e) {
            logger.warning("Unable to load " + name + "; modular assertion will not be available");
        }
    }

    private static void loadModularAssertionsFromJars() {
        try {
            Enumeration<URL> mfurls = AssertionRegistry.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
            while (mfurls.hasMoreElements()) {
                URL url = mfurls.nextElement();
                InputStream is = null;
                try {
                    is = url.openStream();
                    Manifest mf = new Manifest(is);
                    // TODO check if the rest of the manifest looks sane
                    // TODO check signature someday hopefully
                    Attributes asses = mf.getAttributes("ModularAssertion-List");
                    if (asses == null) continue;
                    for (Object o : asses.values()) {
                        String assname = (String) o;
                        loadModularAssertion(assname, Managers.class.getClassLoader());
                    }
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Unable to load manifest " + url.toString() + "; modular assertions in that jar will not be available");
                } finally {
                    if (is != null) is.close();
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Unable to load JAR manifests; modular assertions will not be available");
        }
    }

    private static void loadDynamicModularAssertions() {
        File modularAssertionDirectory = new File("lib/assertions");
        
        if ( modularAssertionDirectory.isDirectory() ) {
            for ( File assertionJarFile : modularAssertionDirectory.listFiles() ) {
                String name = assertionJarFile.getName();
                logger.log( Level.CONFIG, "Processing modular assertion file '"+name+"'." );

                if ( name.endsWith( ".aar" ) || name.endsWith( ".jar" ) ) {
                    JarFile jar = null;
                    try {
                        jar = new JarFile(assertionJarFile, false);

                        Manifest manifest = jar.getManifest();
                        Attributes attr = manifest.getMainAttributes();
                        String assertionNamesStr = attr.getValue("ModularAssertion-List");
                        String[] assertionClassnames = assertionNamesStr == null ? new String[0] : assertionNamesStr.split("\\s+");
                        if ( assertionClassnames.length < 1 ) {
                            logger.log(Level.INFO, "Modular assertion Jar file '"+name+"' contains no modular assertions (ignoring it)");
                        } else {
                            ClassLoader assertionClassLoader =
                                    new URLClassLoader(new URL[]{ assertionJarFile.toURI().toURL() }, Managers.class.getClassLoader());

                            for ( String assname : assertionClassnames ) {
                                loadModularAssertion(assname, assertionClassLoader);
                            }
                        }
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "Unable to load JAR manifests; modular assertions will not be available");
                    } finally {
                        if ( jar != null ) {
                            try {
                                jar.close();
                            } catch (IOException ioe) {
                                logger.log( Level.WARNING, "Error closing modular assertion Jar.", ioe );
                            }
                        }
                    }
                } else {
                    logger.log( Level.INFO, "Ignoring modular assertion file '"+name+"' (only aar/jar files processed)." );
                }
            }
        }
    }

    public synchronized static void setAssertionRegistry(AssertionRegistry reg) {
        assertionRegistry = reg;
    }
}

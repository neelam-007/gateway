package com.l7tech.gateway.common.api.solutionkit;

import com.l7tech.util.FileUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Specialized class loader for hooking customization code into the Solution Kit Manager.
 */
public class SolutionKitCustomizationClassLoader extends URLClassLoader implements Closeable {
    private static final Logger logger = Logger.getLogger(SolutionKitCustomizationClassLoader.class.getName());

    private final File tmpJarFile;

    public SolutionKitCustomizationClassLoader(final URL[] urls, final ClassLoader parent, final File tmpJarFile) {
        super(urls, parent);
        this.tmpJarFile = tmpJarFile;
    }

    @Override
    public void close() throws IOException {
        super.close();

        try {
            FileUtils.delete(tmpJarFile);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to delete customization jar file: " + tmpJarFile, e);
        }
    }
}

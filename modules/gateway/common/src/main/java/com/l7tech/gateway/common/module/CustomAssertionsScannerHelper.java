package com.l7tech.gateway.common.module;

import com.l7tech.util.ResourceUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A helper class having shared functionality (for instance determining whether a {@link JarFile} is a custom assertion
 * or listing all assertions within a module) between the {@code CustomAssertionsScanner) and the UI components.
 */
public class CustomAssertionsScannerHelper {

    // for convenience there are two versions of the property file name
    // one without the leading path separator, the other without
    private String caPropFileNS;
    private String caPropFileS;

    /**
     * Default constructor.
     *
     * @param customAssertionPropertyFileName    Configured Custom Assertions .properties file (default "custom_assertions.properties").
     */
    public CustomAssertionsScannerHelper(final String customAssertionPropertyFileName) {
        // for convenience create two versions of the property file name
        // one without the leading path separator, the other without
        caPropFileNS = customAssertionPropertyFileName;
        caPropFileS = customAssertionPropertyFileName;
        if (customAssertionPropertyFileName != null) {
            if (customAssertionPropertyFileName.startsWith("/")) {
                caPropFileNS = customAssertionPropertyFileName.substring(1);
            } else {
                caPropFileS = "/" + customAssertionPropertyFileName;
            }
        }
    }

    /**
     * @return {@code true} if the Custom Assertions .properties file configuration is not {@code null}.
     */
    public boolean hasCustomAssertionPropertyFileName() {
        return (caPropFileNS != null && caPropFileS != null);
    }

    /**
     * Determine whether the specified {@link JarFile} is a Custom Assertion.
     *
     * @param jarFile    the module to process.  Required.
     * @return {@code true} if the specified {@code jarFile} is a modular assertion module, {@code false} otherwise
     */
    public boolean isCustomAssertion(@NotNull final JarFile jarFile) {
        // check if we should use this module
        final Enumeration<JarEntry> jarEntries = jarFile.entries();
        // loop through all files inside the jar, and look for the custom assertion properties file
        while (jarEntries.hasMoreElements()) {
            final JarEntry entry = jarEntries.nextElement();
            if (entry.getName().startsWith(caPropFileNS) || entry.getName().startsWith(caPropFileS)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extract all Custom Assertions contained within the specified {@code JarFile}.
     * <p/>
     * TODO: {@code CustomAssertionsScanner} is loading the .properties file using {@code CustomAssertionClassLoader}, which cannot be used outside the server code-base. Perhaps refactor the server not to use {@code CustomAssertionClassLoader} for loading the .properties file.
     *
     * @param jarFile    the {@code JarFile} to scan for Custom Assertions.  Required.
     * @return A read-only {@code Collection} of Custom Assertion Class Names within the specified {@code JarFile}.
     * @throws IOException if an error happens while reading the {@code JarFile}
     */
    public Collection<String> getAssertions(@NotNull final JarFile jarFile) throws IOException {
        final List<String> assertions = new ArrayList<>();

        final Enumeration<JarEntry> jarEntries = jarFile.entries();

        // loop through all files inside the jar, and look for the custom assertion properties file
        while (jarEntries.hasMoreElements()) {
            final JarEntry entry = jarEntries.nextElement();
            // skip if null or directory
            if (entry == null || entry.isDirectory()) {
                continue;
            }
            if (entry.getName().startsWith(caPropFileNS) || entry.getName().startsWith(caPropFileS)) {
                InputStream entryIn = null;
                try {
                    entryIn = jarFile.getInputStream(entry);

                    // load all properties
                    final Properties props = new Properties();
                    props.load(entryIn);

                    // loop through all properties
                    for (final Object prop : props.keySet()) {
                        final String key = prop.toString();
                        if (key.endsWith(".class")) {
                            final String baseKey = key.substring(0, key.indexOf(".class"));
                            final String assertionClass = (String)props.get(baseKey + ".class");
                            assertions.add(assertionClass);
                        }
                    }

                } finally {
                    ResourceUtils.closeQuietly(entryIn);
                }
            }
        }

        return Collections.unmodifiableCollection(assertions);
    }
}

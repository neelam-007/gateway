package com.l7tech.gateway.common.module;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

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
     * @param jarFile    the module to process.  Required and cannot be {@code null}.
     * @return {@code true} if the specified {@code jarFile} is a modular assertion module, {@code false} otherwise
     */
    public boolean isCustomAssertion(@NotNull final JarFile jarFile) {
        // check if we should use this module
        final Enumeration<JarEntry> jarEntries = jarFile.entries();
        // loop through all files inside the jar, and look for the custom assertion properties file
        while (jarEntries.hasMoreElements()) {
            if (isCustomAssertionPropertiesFile(jarEntries.nextElement())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Same as {@link #isCustomAssertion(java.util.jar.JarFile)} but with {@code JarInputStream} instead of {@code JarFile}.
     *
     * @param jis    the module to process.  Required and cannot be {@code null}.
     * @return {@code true} if the specified {@code JarInputStream} is a modular assertion module, {@code false} otherwise
     * @throws java.io.IOException if an error occurs while reading the specified {@code JarInputStream}
     * @see #isCustomAssertion(java.util.jar.JarFile)
     */
    public boolean isCustomAssertion(@NotNull final JarInputStream jis) throws IOException {
        // check if we should use this module
        // loop through all files inside the jar, and look for the custom assertion properties file
        JarEntry entry;
        while ((entry = jis.getNextJarEntry()) != null) {
            if (isCustomAssertionPropertiesFile(entry)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether the specified {@code JarEntry} is the configured Custom Assertions properties file.
     *
     * @param entry    the {@code JarEntry} to test.
     * @return {@code true} is the specified {@code JarEntry} is the configured Custom Assertions properties file,
     * or {@code false} otherwise.
     */
    private boolean isCustomAssertionPropertiesFile(final JarEntry entry) {
        return ( entry != null && !entry.isDirectory() && (entry.getName().startsWith(caPropFileNS) || entry.getName().startsWith(caPropFileS)) );
    }

    /**
     * Extract all Custom Assertions contained within the specified {@code JarFile}.
     * <p/>
     * TODO: {@code CustomAssertionsScanner} is loading the .properties file using {@code CustomAssertionClassLoader}, which cannot be used outside the server code-base. Perhaps refactor the server not to use {@code CustomAssertionClassLoader} for loading the .properties file.
     *
     * @param jarFile    the {@code JarFile} to scan for Custom Assertions.  Required and cannot be {@code null}.
     * @return A read-only {@code Collection} of Custom Assertion Class Names within the specified {@code JarFile}.
     * @throws IOException if an error happens while reading the {@code JarFile}
     */
    public Collection<String> getAssertions(@NotNull final JarFile jarFile) throws IOException {
        final Collection<String> assertions = new ArrayList<>();

        // loop through all files inside the jar, and look for the custom assertion properties file
        final Enumeration<JarEntry> jarEntries = jarFile.entries();
        while (jarEntries.hasMoreElements()) {
            final JarEntry entry = jarEntries.nextElement();
            try (final InputStream is = jarFile.getInputStream(entry)) {
                if (isCustomAssertionPropertiesFile(entry)) {
                    getAssertions(is, assertions);
                }
            }
        }

        return Collections.unmodifiableCollection(assertions);
    }

    /**
     * Same as {@link #getAssertions(java.util.jar.JarFile)} but with {@code JarInputStream} instead of {@code JarFile}.
     *
     * @param jis    the {@code JarInputStream} to scan for Custom Assertions.  Required and cannot be {@code null}.
     * @return A read-only {@code Collection} of Custom Assertion Class Names within the specified {@code JarInputStream}.
     * @throws IOException if an error happens while reading the {@code JarInputStream}
     * @see #getAssertions(java.util.jar.JarFile)
     */
    public Collection<String> getAssertions(@NotNull final JarInputStream jis) throws IOException {
        final Collection<String> assertions = new ArrayList<>();

        // loop through all files inside the jar, and look for the custom assertion properties file
        JarEntry entry;
        while ((entry = jis.getNextJarEntry()) != null) {
            if (isCustomAssertionPropertiesFile(entry)) {
                getAssertions(jis, assertions);
            }
        }

        return Collections.unmodifiableCollection(assertions);
    }

    /**
     * Extract all Modular Assertions contained within the specified {@code JarEntry} {@code InputStream}.
     *
     * @param entryStream    The {@code JarEntry} {@code InputStream} to process.  Required and cannot be {@code null}.
     * @param assertions     The {@code Collection} holding the assertions found so far.  Required and cannot be {@code null}.
     * @throws IOException if an error happens while reading the {@code entryStream}
     */
    private void getAssertions(
            @NotNull final InputStream entryStream,
            @NotNull final Collection<String> assertions
    ) throws IOException {
        // load all properties
        final Properties props = new Properties();
        props.load(entryStream);
        // loop through all properties
        for (final Object prop : props.keySet()) {
            final String key = prop.toString();
            if (key.endsWith(".class")) {
                final String baseKey = key.substring(0, key.indexOf(".class"));
                final String assertionClass = (String) props.get(baseKey + ".class");
                assertions.add(assertionClass);
            }
        }
    }
}

package com.l7tech.gateway.common.module;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * A helper class having shared functionality (for instance determining whether a {@link JarFile} is a modular assertion
 * or listing all assertions within a module) between the {@code ModularAssertionsScanner) and the UI components.
 * <p/>
 * TODO: Refactor {@code ModularAssertionsScanner} to use this logic.
 */
public class ModularAssertionsScannerHelper {

    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final String ASSERTION_NAMES_SPLITTER_REGEX = "\\s+";

    private final String manifestHdrAssertionList;

    /**
     * Default constructor.
     *
     * @param manifestHdrAssertionList    The jar manifest file header containing the Modular Assertion Names list.  Required.
     */
    public ModularAssertionsScannerHelper(@NotNull final String manifestHdrAssertionList) {
        this.manifestHdrAssertionList = manifestHdrAssertionList;
    }

    /**
     * Determine whether the specified {@link JarFile} is a Custom Assertion.
     *
     * @param jarFile    the module to process.  Required.
     * @return {@code true} if the specified {@code jarFile} is a modular assertion module, {@code false} otherwise
     * @throws IOException if an error occurs while reading the specified {@code jarFile}
     */
    public boolean isModularAssertion(@NotNull final JarFile jarFile) throws IOException {
        return !getAssertions(jarFile).isEmpty();
    }

    /**
     * Extract all Modular Assertions contained within the specified {@code JarFile}.
     * <p/>
     * TODO: Modify {@code ModularAssertionsScanner} to use this logic.
     *
     * @param jarFile    the {@code JarFile} to scan for Modular Assertions.  Required.
     * @return A read-only {@code Collection} of Modular Assertion Class Names within the specified {@code JarFile}.
     * @throws IOException if an error happens while reading the {@code JarFile}
     */
    public Collection<String> getAssertions(@NotNull final JarFile jarFile) throws IOException {
        final Manifest manifest = jarFile.getManifest();
        final Attributes attr = manifest != null ? manifest.getMainAttributes() : null;
        final String assertionNamesStr = attr != null ? attr.getValue(manifestHdrAssertionList) : null;
        return Collections.unmodifiableCollection(
                Arrays.asList(
                        assertionNamesStr == null
                                ? EMPTY_STRING_ARRAY
                                : assertionNamesStr.split(ASSERTION_NAMES_SPLITTER_REGEX)
                )
        );
    }
}

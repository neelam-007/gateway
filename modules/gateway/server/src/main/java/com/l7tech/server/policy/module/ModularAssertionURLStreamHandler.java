package com.l7tech.server.policy.module;

import com.l7tech.server.policy.ServerAssertionRegistry;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * Implementation of URLStreamHandler to handle URLs with <b>assnmod</b> protocol.
 *
 * This is needed in order to proper load resources from modules. The {@link ModularAssertionClassLoader} already links the URL to the correct
 * path using an inner implementation of {@link URLStreamHandler} but in some cases, some libraries pick that URL, transform it to a String and
 * then try to rebuild the URL again. This handler is needed for this kind of case.
 * When URLs with <b>assnmod</b> protocol are built from String we need to proper handle resource loading.
 *
 * @see URL
 */
public class ModularAssertionURLStreamHandler extends URLStreamHandler {

    /** The type of protocol used for modass: assnmod */
    public static final String PROTOCOL = ModularAssertionClassLoader.NR_PROTO;

    private ServerAssertionRegistry registry;

    /**
     * @param registry the registry to get modular assertion information
     */
    public ModularAssertionURLStreamHandler(@NotNull ServerAssertionRegistry registry) {
        this.registry = registry;
    }

    @Override
    protected URLConnection openConnection(final URL url) {
        return new URLConnection(url) {
            @Override
            public void connect() {
                // No implementation is necessary
            }

            @Override
            public InputStream getInputStream() throws IOException {
                // Loads the file full path, split into assertion name (first part before !) and file path
                String fullPath = url.getPath();

                int index = fullPath.indexOf('!');
                String assName = fullPath.substring(0, index);
                String file = fullPath.substring(index + 1);
                // if file is contained into a nested jar or whatever compressed file into the AAR-INF directory,
                // find only the path to the file after the last ! sign before trying to load it
                if (file.contains("!")) {
                    file = file.substring(file.lastIndexOf('!') + 1);
                }
                // from the assertion module we load the file and returns to whoever asked for it
                ModularAssertionModule module = registry.getModuleByName(assName);
                if (module == null) {
                    throw new IOException("Requested url (" + fullPath + ") is unreachable due to possible module unloaded operation");
                }
                InputStream fileStream = module.getResourceStream(file, false);
                if (fileStream == null) {
                    throw new IOException("File " + file + " not found in module " + assName);
                }
                return fileStream;
            }
        };
    }
}

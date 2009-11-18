package com.l7tech.server.processcontroller.patching.builder;

import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;

import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.io.IOException;
import java.io.InputStream;

/**
 * Patch entry specification for an input stream.
 *
 * @author jbufu
 */
public class PatchSpecStreamEntry implements PatchSpecEntry {

    public PatchSpecStreamEntry(String entryName, InputStream input) {
        this.entryName = entryName;
        this.input = input;
    }

    @Override
    public String getEntryName() {
        return entryName;
    }

    @Override
    public void setEntryName(String entryName) {
        this.entryName = entryName;
    }

    @Override
    public void toJar(JarOutputStream jos) throws IOException {
        try {
            jos.putNextEntry(new ZipEntry(entryName));
            IOUtils.copyStream(input, jos);
            jos.closeEntry();
        } finally {
            ResourceUtils.closeQuietly(input);
        }
    }

    // - PRIVATE

    private String entryName;
    private final InputStream input;

}

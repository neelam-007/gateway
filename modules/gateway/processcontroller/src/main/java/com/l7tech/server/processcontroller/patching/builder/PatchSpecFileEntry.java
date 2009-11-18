package com.l7tech.server.processcontroller.patching.builder;

import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.jar.JarOutputStream;

/**
 * Patch entry specification based on a file.
 *
 * @author jbufu
 */
public class PatchSpecFileEntry implements PatchSpecEntry {

    // - PUBLIC

    public PatchSpecFileEntry(String entryName, String fileName) {
        this.entryName = entryName;
        this.fileName = fileName;
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
        InputStream in = null;
        try {
            jos.putNextEntry(new ZipEntry(entryName));
            in = new FileInputStream(fileName);
            IOUtils.copyStream(in, jos);
            jos.closeEntry();
        } finally {
            ResourceUtils.closeQuietly(in);
        }
    }

    // - PRIVATE

    private String entryName;
    private final String fileName;
}

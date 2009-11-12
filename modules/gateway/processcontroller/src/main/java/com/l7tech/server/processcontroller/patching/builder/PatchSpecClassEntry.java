package com.l7tech.server.processcontroller.patching.builder;

import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.server.processcontroller.patching.PatchUtils;

import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.io.IOException;
import java.io.InputStream;

/**
 * Patch entry specification for a .class file
 *
 * @author jbufu
 */
public class PatchSpecClassEntry implements PatchSpecEntry {

    // - PUBLIC

    public PatchSpecClassEntry(Class<?> clazz) {
        this.clazz = clazz;
    }

    @Override
    public String getEntryName() {
        return PatchUtils.classToEntryName(clazz.getName());
    }

    @Override
    public void toJar(JarOutputStream jos) throws IOException {
        InputStream is = clazz.getResourceAsStream(clazz.getSimpleName() + ".class");
        try {
            jos.putNextEntry(new ZipEntry(getEntryName()));
            IOUtils.copyStream(is, jos);
            jos.closeEntry();
        } finally {
            ResourceUtils.closeQuietly(is);
        }
    }

    // - PRIVATE

    private final Class<?> clazz;


}

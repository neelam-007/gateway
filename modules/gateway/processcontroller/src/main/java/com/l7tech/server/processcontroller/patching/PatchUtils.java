package com.l7tech.server.processcontroller.patching;

import com.l7tech.util.*;
import com.l7tech.common.io.JarSignerParams;
import com.l7tech.common.io.JarUtils;
import com.l7tech.server.processcontroller.patching.builder.PatchSpec;
import com.l7tech.server.processcontroller.patching.builder.PatchSpecEntry;

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.security.SecureRandom;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.jar.Attributes;
import java.util.zip.ZipEntry;
import java.util.Random;

/**
 * @author jbufu
 */
public class PatchUtils {

    // - PUBLIC

    public static File buildPatch(PatchSpec patchSpec, JarSignerParams signerParams) throws IOException, PatchException {
        // build and sign
        return JarUtils.sign(buildUnsignedPatch(patchSpec), signerParams);
    }

    public static File buildUnsignedPatch(PatchSpec patchSpec) throws IOException {
        // main class
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, PATCH_MANIFEST_VERSION);
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, patchSpec.getMainClass());
        if (! patchSpec.getEntries().containsKey(patchSpec.getMainClassEntryName()))
            throw new IllegalArgumentException("Main-Class file not provided in the patch specification.");

        // jar
        String patchFileName = patchSpec.getOutputFilename();
        File patchFile = patchFileName != null ? new File(patchFileName) : File.createTempFile("patch", PatchPackageManager.PATCH_EXTENSION);
        JarOutputStream jos = new JarOutputStream(new FileOutputStream(patchFile), manifest);
        String comment = patchSpec.getProperties().getProperty(PatchPackage.Property.ID.name() ) + " : " +
                         patchSpec.getProperties().getProperty(PatchPackage.Property.DESCRIPTION.name() );
        jos.setComment(comment);

        // patch properties
        jos.putNextEntry(new ZipEntry(PatchPackage.PATCH_PROPERTIES_ENTRY));
        patchSpec.getProperties().store(jos, comment);
        jos.closeEntry();

        // file entries
        for (PatchSpecEntry entry : patchSpec.getEntries().values()) {
            entry.toJar(jos);
        }

        jos.close();
        return patchFile;
    }

    public static String generatePatchId() {
        byte[] bytes = new byte[PATCH_ID_LENGTH];
        random.nextBytes(bytes);
        return HexUtils.hexDump(bytes);
    }

    public static String classToEntryName(String className) {
        return className.replace('.', File.separatorChar) + ".class";
    }

    // - PRIVATE

    private static final String PATCH_MANIFEST_VERSION = "1.0";
    private static final int PATCH_ID_LENGTH = 16;

    private static Random random = new SecureRandom();

    private PatchUtils() { }

}

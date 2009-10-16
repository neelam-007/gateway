package com.l7tech.server.processcontroller.patching;

import com.l7tech.util.IOUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.OSDetector;
import com.l7tech.common.io.ProcUtils;
import com.l7tech.common.io.ProcResult;

import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.security.SecureRandom;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.jar.Attributes;
import java.util.zip.ZipEntry;
import java.util.Map;
import java.util.Random;

/**
 * @author jbufu
 */
public class PatchUtils {

    // - PUBLIC

    public static File buildPatch(PatchSpec patchSpec, JarSignerParams signerParams) throws IOException, PatchException {

        // main class
        Manifest manifest = new Manifest();
        String mainClass = patchSpec.getMainClass();
        String mainClassFile = mainClass.replace('.', File.separatorChar) + ".class";
        if (! patchSpec.getEntries().containsKey(mainClassFile))
            throw new IllegalArgumentException("Main-Class file not provieded in the patch specification.");
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, PATCH_MANIFEST_VERSION);
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, mainClass);

        // jar
        File patchFile = File.createTempFile("patch", ".zip");
        JarOutputStream jos = new JarOutputStream(new FileOutputStream(patchFile), manifest);
        String comment = patchSpec.getProperties().getProperty(PatchPackage.Property.ID.name() ) + " : " +
                         patchSpec.getProperties().getProperty(PatchPackage.Property.DESCRIPTION.name() );
        jos.setComment(comment);

        // patch properties
        jos.putNextEntry(new ZipEntry(PatchPackage.PATCH_PROPERTIES_ENTRY));
        patchSpec.getProperties().store(jos, comment);
        jos.closeEntry();

        // file entries
        Map<String,String> entries = patchSpec.getEntries();
        for(String zipEntryPath : entries.keySet()) {
            jos.putNextEntry(new ZipEntry(zipEntryPath));
            IOUtils.copyStream(new FileInputStream(entries.get(zipEntryPath)), jos);
            jos.closeEntry();
        }

        jos.close();

        // sign
        return sign(patchFile, signerParams);
    }

    public static File sign(File patch, JarSignerParams signParams) throws IOException {

        ProcResult signResult = ProcUtils.exec(getJavaBinary(), ProcUtils.args("-classpath", getToolsJarPath(), JARSIGNER_CLASS_NAME, signParams.getOptions(), patch.getAbsolutePath(), signParams.getAlias()));

        int signStatus = signResult.getExitStatus();
        if (signStatus != 0) {
            throw new IOException("JarSigner returned exit status " + signStatus + "\n" + new String(signResult.getOutput()));
        }

        return patch;
    }

    public static String generatePatchId() {
        byte[] bytes = new byte[PATCH_ID_LENGTH];
        random.nextBytes(bytes);
        return HexUtils.hexDump(bytes);
    }

    // - PRIVATE

    private static final String PATCH_MANIFEST_VERSION = "1.0";
    private static final int PATCH_ID_LENGTH = 16;
    private static final String JARSIGNER_CLASS_NAME = "sun.security.tools.JarSigner";

    private static Random random = new SecureRandom();

    private PatchUtils() { }

    private static String getToolsJarPath() {
        try {
            Class jarsignerClass = ToolProvider.getSystemToolClassLoader().loadClass(JARSIGNER_CLASS_NAME);
            return jarsignerClass.getProtectionDomain().getCodeSource().getLocation().getPath();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot find tools.jar.", e);
        }
    }

    private static File getJavaBinary() {
        File java = null;
        IllegalStateException thrown = null;
        try {
            java = new File(System.getProperty("java.home"), "bin" + File.separator + (OSDetector.isWindows() ? "java.exe" : "java"));
            if (! java.canExecute())
                thrown = new IllegalStateException("Cannot execute java binary: " + java.getCanonicalPath());
        } catch (Exception e) {
            thrown = new IllegalStateException("Cannot get java binary.", e);
        }

        if (thrown == null)
            return java;
        else
            throw thrown;
    }
}

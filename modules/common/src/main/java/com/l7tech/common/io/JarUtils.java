package com.l7tech.common.io;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.OSDetector;
import com.l7tech.util.SyspropUtil;

import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

/**
 * Jar file manipulation utilities, delegating operations to the external jarsigner tool.
 *
 * @author jbufu
 */
public class JarUtils {

    // - PUBLIC

    public static File sign(File jar, JarSignerParams signParams) throws IOException {

        ProcResult signResult = ProcUtils.exec(getJavaBinary(), ProcUtils.args("-classpath", getToolsJarPath(), JARSIGNER_CLASS_NAME, signParams.getOptions(), jar.getAbsolutePath(), signParams.getAlias()));

        int signStatus = signResult.getExitStatus();
        if (signStatus != 0) {
            throw new IOException("JarSigner returned exit status " + signStatus + "\n" + new String(signResult.getOutput()));
        }

        return jar;
    }


    // - PRIVATE

    private static final String JARSIGNER_CLASS_NAME = "sun.security.tools.jarsigner.Main";

    private JarUtils() { }

    private static String getToolsJarPath() {
        try {
            Class jarsignerClass = ToolProvider.getSystemToolClassLoader().loadClass(JARSIGNER_CLASS_NAME);
            final URL protPathUrl = jarsignerClass.getProtectionDomain().getCodeSource().getLocation();
            File protPath = new File(protPathUrl.toURI());
            if (!protPath.exists())
                throw new FileNotFoundException("File not found: " + protPathUrl);
            return protPath.getCanonicalPath();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot find tools.jar: " + ExceptionUtils.getMessage(e), e);
        }
    }

    public static File getJavaBinary() {
        File java = null;
        IllegalStateException thrown = null;
        try {
            java = new File( SyspropUtil.getProperty( "java.home" ), "bin" + File.separator + (OSDetector.isWindows() ? "java.exe" : "java") );
            if (! java.canExecute())
                thrown = new IllegalStateException("Cannot execute java binary: " + java.getCanonicalPath());
        } catch (Exception e) {
            thrown = new IllegalStateException("Cannot get java binary: " + ExceptionUtils.getMessage(e), e);
        }

        if (thrown == null)
            return java;
        else
            throw thrown;
    }

}

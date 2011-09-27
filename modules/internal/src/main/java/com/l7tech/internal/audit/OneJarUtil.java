package com.l7tech.internal.audit;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.Signature;

/**
 * This hack can be used to force a OneJar'ed utility to register RSA Crypto-J 4.1 FIPS as its security provider,
 * using the embedded version of the jarfile.
 */
public class OneJarUtil {
    private static final String EMBEDDED_PROVIDER_JAR = "lib/cryptojFIPS-5.0.jar";
    private static final String EMBEDDED_PROVIDER_CLASNAME = "com.rsa.jsafe.provider.JsafeJCE";

    private static boolean isEcdsaSupportAvailable() {
        try {
            Signature.getInstance("SHA512withECDSA");
            return true;
        } catch (NoSuchAlgorithmException e) {
            return false;
        }
    }

    public static void configureEmbeddedSecurityProvider() {
        // If we don't already have an implementation of SHA512withECDSA, we'll need to write copy of
        // the provider jarfile to disk, and add it as a security provider.  This doesn't work
        // with One-JAR's embedded pseudo-URL since the JCE security mechanism needs a real provider
        // URL in order to verify the jarfile signature.

        if (isEcdsaSupportAvailable()) {
            // JDK is already configured for ECDSA.  No further action required.
            return;
        }

        // See if we are running under One-JAR with bcprov embedded as lib/bcprov-jdk6-145.jar
        InputStream jarstream = AuditSignatureChecker.class.getClassLoader().getResourceAsStream(EMBEDDED_PROVIDER_JAR);
        if (jarstream == null) {
            System.err.println("Unable to register ECDSA provider -- embedded provider " + EMBEDDED_PROVIDER_JAR + " not found.  No elliptic curve signatures can be verified.");
            return;
        }

        File tmpjar;
        FileOutputStream fos = null;
        try {
            tmpjar = File.createTempFile("ecprov", ".jar");
            tmpjar.deleteOnExit();

            fos = new FileOutputStream(tmpjar);
            IOUtils.copyStream(jarstream, fos);

        } catch (IOException e) {
            System.err.println("Unable to register ECDSA provider -- no elliptic curve signatures can be verified.  Error: " + ExceptionUtils.getMessage(e));
            return;
        } finally {
            ResourceUtils.closeQuietly(fos);
        }

        try {
            ClassLoader provLoader = new URLClassLoader(new URL[] { tmpjar.toURI().toURL() }, ClassLoader.getSystemClassLoader());
            Class provClass = provLoader.loadClass(EMBEDDED_PROVIDER_CLASNAME);
            Security.addProvider((Provider)provClass.newInstance());
        } catch (Exception e) {
            System.err.println("Unable to register ECDSA provider -- no elliptic curve signatures can be verified.  Error: " + ExceptionUtils.getMessage(e));
        }
    }
}

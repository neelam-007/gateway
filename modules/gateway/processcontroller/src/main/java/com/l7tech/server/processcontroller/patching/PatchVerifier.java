package com.l7tech.server.processcontroller.patching;

import com.l7tech.util.*;
import com.l7tech.common.io.NullOutputStream;
import com.l7tech.common.io.CertUtils;
import com.l7tech.server.processcontroller.ConfigServiceImpl;

import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Attributes;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.ConsoleHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.security.cert.X509Certificate;
import java.security.cert.Certificate;
import java.security.CodeSource;
import java.security.CodeSigner;

/**
 * Utility class for verifying patch package signature integrity and if the signer is trusted.
 *
 * @author jbufu
 */
public class PatchVerifier {

    // - PUBLIC

    public static void main(String[] args) throws Exception {
        initLogging();

        if (args == null || args.length == 0) {
            System.err.println();
            System.exit(PATCH_VERIFIER_ERROR_EXIT);
        }

        String java = args[0];
        String jar = getJarParam(args);

        Set<X509Certificate> userTrustedCerts = null;
        try {
            // user certs from (root owned), patch-controlled, keystore -- initialized how?
            // keystore from host.properties:host.controller.patch.truststore.file
            // must be owned by root and not writeable by anyone else
            userTrustedCerts = new ConfigServiceImpl().getTrustedPatchCerts();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to load user trusted certificates for patch verification: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }

        try {
            PatchPackage patch = PatchVerifier.getVerifiedPackage(new File(jar), userTrustedCerts);
            if (java == null || (!java.equals(APPLIANCE_JAVA) && ! java.equals(patch.getProperty(PatchPackage.Property.JAVA_BINARY)) )) {
                System.err.println("Invalid java binary for installing patches: " + java);
                System.exit(PATCH_VERIFIER_ERROR_EXIT);
            }
        } catch (Exception e) {
            System.err.println("Patch verification failed: " + ExceptionUtils.getMessage(e));
            System.exit(PATCH_VERIFIER_ERROR_EXIT);
        }

        System.exit(0);
    }

    public static PatchPackage getVerifiedPackage(final File patchFile, Set<X509Certificate> trustedUserCerts) throws IOException, PatchException {
        // cache entries
        // read all entry input streams to verify signature
        // cache certificates
        JarFile jar = new JarFile(patchFile, true);
        Enumeration<JarEntry> entries = jar.entries();
        Set<List<X509Certificate>> certPaths = new HashSet<List<X509Certificate>>();
        while(entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            checkSignature(jar, entry);
            if (entry.getName().startsWith("META-INF/")) {
                continue;
            }

            Certificate[] certificates = entry.getCertificates();
            if(certificates == null || certificates.length == 0)
                throw new PatchException("Invalid patch: entry is not signed: " + entry.getName());

            final String[] nonX509type = new String[1];
            List<X509Certificate> certPath = Functions.map(Arrays.asList(certificates),
                new Functions.Unary<X509Certificate, Certificate>() {
                    @Override
                    public X509Certificate call(Certificate certificate) {
                        if (certificate instanceof X509Certificate) {
                            return (X509Certificate) certificate;
                        } else {
                            nonX509type[0] = certificate.getType();
                            return null;
                        }
                    }
                });

            if (nonX509type[0] != null)
                throw new PatchException("Invalid patch: signed with non-X509 certificate: " + nonX509type[0]);

            certPaths.add(certPath);
        }

        // main class
        Object mainClass = jar.getManifest().getMainAttributes().get(Attributes.Name.MAIN_CLASS);
        if (mainClass == null || ! (mainClass instanceof String) || ((String)mainClass).isEmpty())
            throw new PatchException("Invalid patch: main class not specified.");
        checkEntryExists(jar, PatchUtils.classToEntryName((String) mainClass));

        for(String manifestEntry : jar.getManifest().getEntries().keySet())
            checkEntryExists(jar, manifestEntry);

        final Properties properties = extractPatchProperties(jar);
        checkTrustedCertificates(certPaths, trustedUserCerts);
        logger.log(Level.FINE, "Package read from file " + patchFile + "(ID: " + properties.get(PatchPackage.Property.ID) + ") is a valid and trusted patch.");
        return new PatchPackage() {
            @Override
            public File getFile() {
                return patchFile;
            }
            @Override
            public String getProperty(Property prop) {
                return properties.getProperty(prop.name());
            }
        };
    }

    // - PRIVATE

    private PatchVerifier() { }

    private static final Logger logger = Logger.getLogger(PatchVerifier.class.getName());

    private static final String APPLIANCE_JAVA="/opt/SecureSpan/JDK/jre/bin/java";
    private static final int PATCH_VERIFIER_ERROR_EXIT = 255;


    private static void initLogging() {
        // configure logging if the logs directory is found, else leave console output
        final File logsDir = new File("/opt/SecureSpan/Controller/var/logs");
        if ( logsDir.exists() && logsDir.canWrite() ) {
            JdkLoggerConfigurator.configure("com.l7tech.server.processcontroller.patching.verifier", "com/l7tech/server/processcontroller/patching/resources/logging.properties", "etc/conf/patchinglogging.properties", false, true);
        }
        if ( SyspropUtil.getBoolean( "com.l7tech.server.log.console", false ) ) {
            Logger.getLogger( "" ).addHandler( new ConsoleHandler() );
        }
    }

    private static void checkSignature(JarFile jar, JarEntry entry) throws IOException, PatchException {
        InputStream zis = null;
        try {
            zis = jar.getInputStream(entry);
            IOUtils.copyStream(zis, new NullOutputStream());
        } catch (SecurityException e) {
            throw new PatchException("Invalid patch: signature error while processing entry: " + entry.getName() + " : " + ExceptionUtils.getMessage(e), e);
        } finally {
            ResourceUtils.closeQuietly(zis);
        }
    }

    private static Properties extractPatchProperties(JarFile jar) throws PatchException {
        Properties properties = new Properties();
        InputStream zis = null;
        try {
            zis = jar.getInputStream(new ZipEntry(PatchPackage.PATCH_PROPERTIES_ENTRY));
            properties.load(zis);
            for(PatchPackage.Property prop : PatchPackage.Property.values()) {
                if (prop.isRequired() && (! properties.containsKey(prop.name()) || properties.getProperty(prop.name()) == null))
                    throw new IllegalArgumentException("Invalid patch: required patch property missing: " + prop.name());
            }
        } catch (Exception e) {
            throw new PatchException("Invalid patch: error extracting patch properties: " + ExceptionUtils.getMessage(e), e);
        } finally {
            ResourceUtils.closeQuietly(zis);
        }
        return properties;
    }

    private static void checkEntryExists(JarFile jar, String entryName) throws PatchException {
        if (jar.getJarEntry(entryName) == null)
            throw new PatchException("Invalid patch: entry not present in patch jar: " + entryName);
    }

    private static void checkTrustedCertificates(Set<List<X509Certificate>> certPaths, Set<X509Certificate> trustedCerts) throws PatchException {


        Set<X509Certificate> allTrusted = new HashSet<X509Certificate>();
        allTrusted.addAll(getJarCodeSignerCerts());

        if (trustedCerts == null || trustedCerts.isEmpty())
            logger.log(Level.INFO, "No user supplied trusted certificates for patch verification.");
        else
            allTrusted.addAll(trustedCerts);

        for (List<X509Certificate> certPath : certPaths) {
            X509Certificate signer = certPath.get(0); // only verify individual certs, not certificate paths to trusted CAs
            boolean isTrusted = false;
            for (X509Certificate trusted : allTrusted) {
                if (CertUtils.certsAreEqual(trusted, signer)) {
                    isTrusted = true;
                    break;
                }
            }
            if (!isTrusted)
                throw new PatchException("Certificate is not trusted for signing patches: " + signer);
        }
    }

    private static Set<X509Certificate> getJarCodeSignerCerts() {
        Set<X509Certificate> result = new HashSet<X509Certificate>(); 
        CodeSource codeSource = PatchVerifier.class.getProtectionDomain().getCodeSource();
        if (codeSource != null) {
            CodeSigner[] signers = codeSource.getCodeSigners();
            if (signers == null) {
                logger.log(Level.WARNING, "Code signer certificate for patch verification not found.");
            } else {
                for (CodeSigner signer : signers) {
                    Certificate signerCert = signer.getSignerCertPath().getCertificates().get(0);
                    if (! (signerCert instanceof X509Certificate)) {
                        logger.log(Level.WARNING, "Ignoring non-X509 code signer certificate: " + signer);
                    } else {
                        result.add((X509Certificate) signerCert);
                    }
                }
            }
        }
        return result;
    }

    private static String getJarParam(String[] args) {
        List<String> argList = Arrays.asList(args);
        String jar = null;
        for(int i=0; i < argList.size(); i++) {
            if ("-jar".equals(argList.get(i))) {
                if (jar != null) {
                    System.err.println("More than one -jar option specified in the patch installation command line.");
                    System.exit(PATCH_VERIFIER_ERROR_EXIT);
                }
                if (i+1 >= argList.size()) {
                    System.err.println("No -jar option specified in the patch installation command line.");
                    System.exit(PATCH_VERIFIER_ERROR_EXIT);
                }
                jar = argList.get(i+1);
            }
        }
        logger.log(Level.INFO, "Installing patch: " + jar);
        return jar;
    }
}

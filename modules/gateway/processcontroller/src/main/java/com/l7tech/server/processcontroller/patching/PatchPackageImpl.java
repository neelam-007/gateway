package com.l7tech.server.processcontroller.patching;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.Functions;
import com.l7tech.common.io.NullOutputStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

/**
 * @author jbufu
 */
public class PatchPackageImpl implements PatchPackage {

    public PatchPackageImpl(File patchFile) throws IOException, PatchException {
        this.patchFile = patchFile;
        this.jar = new JarFile(patchFile, true);

        // cache entries
        // read all entry input streams to verify signature
        // cache certificates
        Enumeration<JarEntry> entries = jar.entries();
        while(entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            this.entries.put(entry.getName(), entry);
            if (entry.getName().startsWith("META-INF/"))
                continue;
            checkSignature(entry);

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

        extractPatchProperties();
    }

    @Override
    public File getFile() {
        return patchFile;
    }

    @Override
    public Set<List<X509Certificate>> getCertificatePaths() {
        return certPaths;
    }

    @Override
    public String getProperty(Property prop) {
        return properties.getProperty(prop.name());
    }

    // - PRIVATE

    private final File patchFile;
    private final JarFile jar;
    private final Map<String, JarEntry> entries = new HashMap<String, JarEntry>();
    private final Set<List<X509Certificate>> certPaths = new HashSet<List<X509Certificate>>();
    private final Properties properties = new Properties();

    private void checkSignature(JarEntry entry) throws IOException, PatchException {
        InputStream zis = null;
        try {
            zis = jar.getInputStream(entry);
            IOUtils.copyStream(zis, new NullOutputStream());
        } catch (SecurityException e) {
            throw new PatchException("Inalid patch: entry incorrectly signed: " + entry.getName());
        } finally {
            ResourceUtils.closeQuietly(zis);
        }
    }

    private void extractPatchProperties() throws PatchException {
        InputStream zis = null;
        try {
            zis = jar.getInputStream(entries.get(PATCH_PROPERTIES_ENTRY));
            properties.load(zis);
            for(Property prop : Property.values()) {
                if (prop.isRequired() && (! properties.containsKey(prop.name()) || properties.getProperty(prop.name()) == null))
                    throw new IllegalArgumentException("Invalid patch: required patch property missing: " + prop.name());
            }
        } catch (Exception e) {
            throw new PatchException("Invalid patch: error extracting patch properties: " + ExceptionUtils.getMessage(e), e);
        } finally {
            ResourceUtils.closeQuietly(zis);
        }
    }
}

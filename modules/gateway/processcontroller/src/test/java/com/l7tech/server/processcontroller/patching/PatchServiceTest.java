package com.l7tech.server.processcontroller.patching;

import org.junit.Test;
import org.junit.Ignore;
import org.junit.Before;
import org.junit.After;
import org.springframework.test.util.ReflectionTestUtils;
import com.l7tech.server.processcontroller.CxfUtils;
import com.l7tech.server.processcontroller.ConfigService;
import com.l7tech.server.processcontroller.ConfigServiceStub;
import com.l7tech.server.processcontroller.PCUtils;
import com.l7tech.server.processcontroller.patching.builder.PatchSpec;
import com.l7tech.server.processcontroller.patching.builder.PatchSpecClassEntry;
import com.l7tech.util.IOUtils;
import com.l7tech.util.FileUtils;
import com.l7tech.util.Functions;
import com.l7tech.common.io.JarSignerParams;
import com.l7tech.common.io.JarUtils;
import junit.framework.Assert;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.util.ByteArrayDataSource;
import java.io.*;
import java.security.cert.X509Certificate;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.jar.*;

/**
 * @author jbufu
 */
public class PatchServiceTest {

    @Before
    public void setUp() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
        // init signing config
        KeyStore keyStore = KeyStore.getInstance(TEST_SIGNER_KEYSTORE_TYPE);
        untrustedKeystorePath = PatchServiceTest.class.getResource(TEST_UNTRUSTED_SIGNER_KEYSTORE_FILE).getPath();
        keystorePath = PatchServiceTest.class.getResource(TEST_SIGNER_KEYSTORE_FILE).getPath();
        keyStore.load(new FileInputStream(keystorePath), TEST_SIGNER_PASSWORD.toCharArray());
        Enumeration<String> aliases = keyStore.aliases();
        X509Certificate patchSignerCert = null;
        PrivateKey patchSignerKey = null;
        while(aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (keyStore.isCertificateEntry(alias)) {
                if (patchSignerCert != null)
                    throw new IllegalStateException("Patch test keystore contains more than one certificate.");
                Certificate cert = keyStore.getCertificate(alias);
                if (! (cert instanceof X509Certificate))
                    throw new IllegalStateException("Patch test keystore contains non-X509 certificates.");
                patchSignerCert = (X509Certificate) cert;
            }
            if (keyStore.isKeyEntry(alias)) {
                if (patchSignerKey != null)
                    throw new IllegalStateException("Patch test keystore contains more than one key.");
                Key key = keyStore.getKey(alias, TEST_SIGNER_PASSWORD.toCharArray());
                if (! (key instanceof PrivateKey))
                    throw new IllegalStateException("Patch test keystore contains non-private key.");
                patchSignerKey = (PrivateKey) key;
            }
        }
        this.patchSignerCert = patchSignerCert;

        // init services / mocks
        config = new ConfigServiceStub();
        ReflectionTestUtils.setField(config, "trustedPatchCerts", new HashSet<X509Certificate>() {{ add(PatchServiceTest.this.patchSignerCert); }});
        PatchPackageManager packageManager = new PatchPackageManagerImpl();
        ReflectionTestUtils.setField(packageManager, "repositoryDir", config.getPatchesDirectory());
        ReflectionTestUtils.setField(packageManager, "trustedSigners", new PatchTrustStore() {
            @Override
            public Set<X509Certificate> getTrustedCerts() {
                return config.getTrustedPatchCerts();
            }
        });
        ReflectionTestUtils.setField(patchService, "config", config);
        ReflectionTestUtils.setField(patchService, "packageManager", packageManager);
        ReflectionTestUtils.setField(patchService, "recordManager", new PatchRecordManager() { @Override public void save(PatchRecord record) { /*do nothing*/ } });
    }

    @After
    public void tearDown() {
        File patchesDir = config.getPatchesDirectory();
        if (patchesDir != null && patchesDir.exists() && ! FileUtils.deleteDir(patchesDir))
            throw new RuntimeException("Failed to delete temporary package repository dir: " + patchesDir.getAbsolutePath());
    }

    @Test
    public void isTestMachineAnAppliance() throws Exception {
        Assert.assertFalse("The test machine appears seems to be a ssg appliance; this will break other tests.", PCUtils.isAppliance());
    }

    @Test
    @Ignore("Until the full PC / patch service API are deployed for testing")
    public void testDeployedPatchUpload() throws Exception {
        PatchServiceApi api = new CxfUtils.ApiBuilder("https://localhost:8765/services/patchServiceApi").build(PatchServiceApi.class);
        File patch = PatchUtils.buildPatch(getTestPatchSpec("success_touch_tmp_helloworld_txt"), getTestPatchSigningParams());
        PatchStatus status = api.uploadPatch(new DataHandler(new FileDataSource(patch)));
        Assert.assertEquals(PatchStatus.State.UPLOADED.name(), status.getField(PatchStatus.Field.STATE));
    }

    @Test
    @Ignore("Until the full PC / patch service API are deployed for testing")
    public void testLargePatchUpload() throws Exception {
    }

    @Test
    public void testPatchUpload() throws Exception {
        File patch = null;
        boolean tempDeleteOk;
        try {
            patch = PatchUtils.buildPatch(getTestPatchSpec("success_touch_tmp_helloworld_txt"), getTestPatchSigningParams());
            PatchStatus status = patchService.uploadPatch(new DataHandler(new FileDataSource(patch)));
            Assert.assertEquals(PatchStatus.State.UPLOADED.name(), status.getField(PatchStatus.Field.STATE));
        } finally {
            tempDeleteOk = patch == null || ! patch.exists() || patch.delete();
        }

        if (! tempDeleteOk)
            throw new IOException("Error deleting patch file.");
    }

    @Test
    public void testOverwriteInstalledPatch() throws Exception {
        String patchId = "upload_overwrite_id";
        File patch = null;
        boolean tempDeleteOk;
        try {
            patch = PatchUtils.buildPatch(getTestPatchSpec(patchId), getTestPatchSigningParams());
            // first upload
            PatchStatus status = patchService.uploadPatch(new DataHandler(new FileDataSource(patch)));
            Assert.assertEquals(PatchStatus.State.UPLOADED.name(), status.getField(PatchStatus.Field.STATE));
            Assert.assertEquals("Uploaded patch id is not the expected one.", patchId, status.getField(PatchStatus.Field.ID));
            // install
            status = patchService.installPatch(patchId, new ArrayList<String>());
            Assert.assertEquals(status.getField(PatchStatus.Field.STATUS_MSG), PatchStatus.State.INSTALLED.name(), status.getField(PatchStatus.Field.STATE));
            // second upload
            try {
                patchService.uploadPatch(new DataHandler(new FileDataSource(patch)));
                Assert.fail("Packages of installed paches should not be overwriteable.");
            } catch (PatchException expected) {
                // do nothing
            }
        } finally {
            tempDeleteOk = patch == null || ! patch.exists() || patch.delete();
        }

        if (! tempDeleteOk)
            throw new IOException("Error deleting patch file.");
    }
    @Test
    public void testInstallTwice() throws Exception {
        String patchId = "install_twice";
        File patch = null;
        boolean tempDeleteOk;
        try {
            patch = PatchUtils.buildPatch(getTestPatchSpec(patchId), getTestPatchSigningParams());
            // upload
            PatchStatus status = patchService.uploadPatch(new DataHandler(new FileDataSource(patch)));
            Assert.assertEquals(PatchStatus.State.UPLOADED.name(), status.getField(PatchStatus.Field.STATE));
            Assert.assertEquals("Uploaded patch id is not the expected one.", patchId, status.getField(PatchStatus.Field.ID));
            // install
            status = patchService.installPatch(patchId, new ArrayList<String>());
            Assert.assertEquals(status.getField(PatchStatus.Field.STATUS_MSG), PatchStatus.State.INSTALLED.name(), status.getField(PatchStatus.Field.STATE));
            // second install
            try {
                patchService.installPatch(patchId, new ArrayList<String>());
                Assert.fail("Installing a patch twice should have failed.");
            } catch (PatchException expected) {
                // do nothing
            }
        } finally {
            tempDeleteOk = patch == null || ! patch.exists() || patch.delete();
        }

        if (! tempDeleteOk)
            throw new IOException("Error deleting patch file.");
    }

    @Test
    public void testPatchRollback() throws Exception {
        testPatchRollback(true, true); // rollback allowed
    }

    @Test
    public void testPatchRollbackNotAllowed() throws Exception {
        try {
            testPatchRollback(true, false); // rollback not allowed
            Assert.fail("Patch configured to not allow rollback; rollback attempt should have failed.");
        } catch (PatchException expected) {
            // do nothing
        }
    }

    @Test
    public void testPatchRollbackNotInstalled() throws Exception {
        try {
            testPatchRollback(false, true); // rollback not allowed
            Assert.fail("Rollback attempt should have failed for a package that is not installed.");
        } catch (PatchException expected) {
            // do nothing
        }
    }

    private void testPatchRollback(boolean install, boolean allowRollback) throws Exception {
        File patch = null;
        File rollback = null;
        boolean temp1DeleteOk, temp2DeleteOk;
        try {
            String patchId = "success_touch_tmp_helloworld_txt_allow_rollback";
            patch = PatchUtils.buildPatch(getTestPatchSpec(patchId).property(PatchPackage.Property.ROLLBACK_ALLOWED, Boolean.toString(allowRollback)), getTestPatchSigningParams());
            // upload
            PatchStatus status = patchService.uploadPatch(new DataHandler(new FileDataSource(patch)));
            Assert.assertEquals(PatchStatus.State.UPLOADED.name(), status.getField(PatchStatus.Field.STATE));
            Assert.assertEquals("Uploaded patch id is not the expected one.", patchId, status.getField(PatchStatus.Field.ID));
            // install
            if (install) {
                status = patchService.installPatch(patchId, new ArrayList<String>());
                Assert.assertEquals(status.getField(PatchStatus.Field.STATUS_MSG), PatchStatus.State.INSTALLED.name(), status.getField(PatchStatus.Field.STATE));
            }
            // upload rollback
            String rollbackId = "rollback_touch_tmp_helloworld_txt_allow_rollback";
            rollback = PatchUtils.buildPatch(getTestPatchSpec(rollbackId).property(PatchPackage.Property.ROLLBACK_FOR_ID, patchId), getTestPatchSigningParams());
            status = patchService.uploadPatch(new DataHandler(new FileDataSource(rollback)));
            Assert.assertEquals(PatchStatus.State.UPLOADED.name(), status.getField(PatchStatus.Field.STATE));
            Assert.assertEquals("Uploaded patch id is not the expected one.", rollbackId, status.getField(PatchStatus.Field.ID));
            // rollback
            status = patchService.installPatch(rollbackId, new ArrayList<String>());
            Assert.assertEquals(status.getField(PatchStatus.Field.STATUS_MSG), PatchStatus.State.INSTALLED.name(), status.getField(PatchStatus.Field.STATE));
            status = patchService.getStatus(patchId);
            Assert.assertEquals(status.getField(PatchStatus.Field.STATUS_MSG), PatchStatus.State.ROLLED_BACK.name(), status.getField(PatchStatus.Field.STATE));
        } finally {
            temp1DeleteOk = patch == null || ! patch.exists() || patch.delete();
            temp2DeleteOk = rollback == null || ! rollback.exists() || rollback.delete();
        }

        if (! temp1DeleteOk || ! temp2DeleteOk)
            throw new IOException("Error deleting patch file.");
    }
    @Test
    public void testNoManifestEntry() throws Exception {
        File patch = null;
        boolean tempDeleteOk;
        try {
            patch = PatchUtils.buildPatch(getTestPatchSpec(null), getTestPatchSigningParams());

            final Exception[] thrown = new Exception[]{null};
            final boolean[] modified = new boolean[]{false};
            File modifiedPatch = modifyPatch(patch, new HashMap<String, Functions.TernaryVoid<JarEntry, InputStream, JarOutputStream>>() {{
                put(JarFile.MANIFEST_NAME, new Functions.TernaryVoid<JarEntry, InputStream, JarOutputStream>() {
                    @Override
                    public void call(JarEntry jarEntry, InputStream inputStream, JarOutputStream jos) {
                        try {
                            Manifest manifest = new Manifest(inputStream);
                            modified[0] = manifest.getEntries().remove(PatchPackage.PATCH_PROPERTIES_ENTRY) != null;
                            jos.putNextEntry(new JarEntry(jarEntry.getName()));
                            manifest.write(jos);
                            jos.closeEntry();
                        } catch (Exception e) {
                            thrown[0] = e;
                        }
                    }
                });
            }});

            if (thrown[0] != null) throw thrown[0];
            Assert.assertTrue("Failed to modify jar manifest", modified[0]);

            try {
                patchService.uploadPatch(new DataHandler(new FileDataSource(modifiedPatch)));
                Assert.fail("Patch upload should have failed if a file did not have a manifest entry.");
            } catch (PatchException expected) {
                // do nothing
            }
        } finally {
            tempDeleteOk = patch == null || ! patch.exists() || patch.delete();
        }

        if (! tempDeleteOk)
            throw new IOException("Error deleting patch file.");
    }

    @Test
    @Ignore("Disable test FOR NOW which fails under JDK 7.")
    public void testEntryNotHashed() throws Exception {
        File patch = null;
        boolean tempDeleteOk;
        try {
            patch = PatchUtils.buildPatch(getTestPatchSpec(null), getTestPatchSigningParams());

            final Exception[] thrown = new Exception[]{null};
            final boolean[] modified = new boolean[]{false};
            File modifiedPatch = modifyPatch(patch, new HashMap<String, Functions.TernaryVoid<JarEntry, InputStream, JarOutputStream>>() {{
                put(JarFile.MANIFEST_NAME, new Functions.TernaryVoid<JarEntry, InputStream, JarOutputStream>() {
                    @Override
                    public void call(JarEntry jarEntry, InputStream inputStream, JarOutputStream jos) {
                        try {
                            Manifest manifest = new Manifest(inputStream);
                            modified[0] = manifest.getEntries().get(PatchPackage.PATCH_PROPERTIES_ENTRY).remove(new Attributes.Name("SHA1-Digest")) != null;
                            jos.putNextEntry(new JarEntry(jarEntry.getName()));
                            manifest.write(jos);
                            jos.closeEntry();
                        } catch (Exception e) {
                            thrown[0] = e;
                        }
                    }
                });
            }});

            if (thrown[0] != null) throw thrown[0];
            Assert.assertTrue("Failed to modify jar manifest", modified[0]);

            try {
                patchService.uploadPatch(new DataHandler(new FileDataSource(modifiedPatch)));
                Assert.fail("Patch upload should have failed if a file did not have a digest in the manifest.");
            } catch (PatchException expected) {
                // do nothing
            }
        } finally {
            tempDeleteOk = patch == null || ! patch.exists() || patch.delete();
        }

        if (! tempDeleteOk)
            throw new IOException("Error deleting patch file.");
    }


    @Test
    public void testNoSignatureEntry() throws Exception {
        File patch = null;
        boolean tempDeleteOk;
        try {
            patch = PatchUtils.buildPatch(getTestPatchSpec(null), getTestPatchSigningParams());

            final Exception[] thrown = new Exception[]{null};
            final boolean[] modified = new boolean[]{false};
            File modifiedPatch = modifyPatch(patch, new HashMap<String, Functions.TernaryVoid<JarEntry, InputStream, JarOutputStream>>() {{
                put("META-INF/MYKEY.SF", new Functions.TernaryVoid<JarEntry, InputStream, JarOutputStream>() {
                    @Override
                    public void call(JarEntry jarEntry, InputStream inputStream, JarOutputStream jos) {
                        try {
                            Manifest manifest = new Manifest(inputStream);
                            modified[0] = manifest.getEntries().remove(PatchPackage.PATCH_PROPERTIES_ENTRY) != null;
                            jos.putNextEntry(new JarEntry(jarEntry.getName()));
                            manifest.write(jos);
                            jos.closeEntry();
                        } catch (Exception e) {
                            thrown[0] = e;
                        }
                    }
                });
            }});

            if (thrown[0] != null) throw thrown[0];
            Assert.assertTrue("Failed to modify jar signature file", modified[0]);

            try {
                patchService.uploadPatch(new DataHandler(new FileDataSource(modifiedPatch)));
                Assert.fail("Patch upload should have failed if a file did not have a digest in the signature file.");
            } catch (PatchException expected) {
                // do nothing
            }
        } finally {
            tempDeleteOk = patch == null || ! patch.exists() || patch.delete();
        }

        if (! tempDeleteOk)
            throw new IOException("Error deleting patch file.");
    }

    @Test
    @Ignore("Disable test FOR NOW which fails under JDK 7.")
    public void testEntryNotSigned() throws Exception {
        File patch = null;
        boolean tempDeleteOk;
        try {
            patch = PatchUtils.buildPatch(getTestPatchSpec(null), getTestPatchSigningParams());

            final Exception[] thrown = new Exception[]{null};
            final boolean[] modified = new boolean[]{false};
            File modifiedPatch = modifyPatch(patch, new HashMap<String, Functions.TernaryVoid<JarEntry, InputStream, JarOutputStream>>() {{
                put("META-INF/MYKEY.SF", new Functions.TernaryVoid<JarEntry, InputStream, JarOutputStream>() {
                    @Override
                    public void call(JarEntry jarEntry, InputStream inputStream, JarOutputStream jos) {
                        try {
                            Manifest manifest = new Manifest(inputStream);
                            modified[0] = manifest.getEntries().get(PatchPackage.PATCH_PROPERTIES_ENTRY).remove(new Attributes.Name("SHA1-Digest")) != null;
                            jos.putNextEntry(new JarEntry(jarEntry.getName()));
                            manifest.write(jos);
                            jos.closeEntry();
                        } catch (Exception e) {
                            thrown[0] = e;
                        }
                    }
                });
            }});

            if (thrown[0] != null) throw thrown[0];
            Assert.assertTrue("Failed to modify jar signature file", modified[0]);

            try {
                patchService.uploadPatch(new DataHandler(new FileDataSource(modifiedPatch)));
                Assert.fail("Patch upload should have failed if a file did not have a digest in the signature file.");
            } catch (PatchException expected) {
                // do nothing
            }
        } finally {
            tempDeleteOk = patch == null || ! patch.exists() || patch.delete();
        }

        if (! tempDeleteOk)
            throw new IOException("Error deleting patch file.");
    }

    @Test
    public void testNoManifestAndNoSignatureEntry() throws Exception {
        File patch = null;
        boolean tempDeleteOk;
        try {
            patch = PatchUtils.buildPatch(getTestPatchSpec(null), getTestPatchSigningParams());

            final Exception[] thrown = new Exception[]{null};
            final boolean[] modified = new boolean[]{false};
            File modifiedPatch = modifyPatch(patch, new HashMap<String, Functions.TernaryVoid<JarEntry, InputStream, JarOutputStream>>() {{
                put(JarFile.MANIFEST_NAME, new Functions.TernaryVoid<JarEntry, InputStream, JarOutputStream>() {
                    @Override
                    public void call(JarEntry jarEntry, InputStream inputStream, JarOutputStream jos) {
                        try {
                            Manifest manifest = new Manifest(inputStream);
                            modified[0] = manifest.getEntries().remove(PatchPackage.PATCH_PROPERTIES_ENTRY) != null;
                            jos.putNextEntry(new JarEntry(jarEntry.getName()));
                            manifest.write(jos);
                            jos.closeEntry();
                        } catch (Exception e) {
                            thrown[0] = e;
                        }
                    }
                });
                put("META-INF/MYKEY.SF", new Functions.TernaryVoid<JarEntry, InputStream, JarOutputStream>() {
                    @Override
                    public void call(JarEntry jarEntry, InputStream inputStream, JarOutputStream jos) {
                        try {
                            Manifest manifest = new Manifest(inputStream);
                            modified[0] = manifest.getEntries().remove(PatchPackage.PATCH_PROPERTIES_ENTRY) != null;
                            jos.putNextEntry(new JarEntry(jarEntry.getName()));
                            manifest.write(jos);
                            jos.closeEntry();
                        } catch (Exception e) {
                            thrown[0] = e;
                        }
                    }
                });
            }});

            if (thrown[0] != null) throw thrown[0];
            Assert.assertTrue("Failed to modify jar", modified[0]);

            try {
                patchService.uploadPatch(new DataHandler(new FileDataSource(modifiedPatch)));
                Assert.fail("Patch upload should have failed if a file did not have a manifest or signature file entry.");
            } catch (PatchException expected) {
                // do nothing
            }
        } finally {
            tempDeleteOk = patch == null || ! patch.exists() || patch.delete();
        }

        if (! tempDeleteOk)
            throw new IOException("Error deleting patch file.");
    }

    @Test
    @Ignore("Disable test FOR NOW which fails under JDK 7.")
    public void testManifestNotSigned() throws Exception {
        File patch = null;
        boolean tempDeleteOk;
        try {
            patch = PatchUtils.buildPatch(getTestPatchSpec(null), getTestPatchSigningParams());

            final Exception[] thrown = new Exception[]{null};
            final boolean[] modified = new boolean[]{false};
            File modifiedPatch = modifyPatch(patch, new HashMap<String, Functions.TernaryVoid<JarEntry, InputStream, JarOutputStream>>() {{
                put("META-INF/MYKEY.SF", new Functions.TernaryVoid<JarEntry, InputStream, JarOutputStream>() {
                    @Override
                    public void call(JarEntry jarEntry, InputStream inputStream, JarOutputStream jos) {
                        try {
                            Manifest manifest = new Manifest(inputStream);
                            modified[0] = manifest.getMainAttributes().remove(new Attributes.Name("SHA1-Digest-Manifest")) != null;
                            jos.putNextEntry(new JarEntry(jarEntry.getName()));
                            manifest.write(jos);
                            jos.closeEntry();
                        } catch (Exception e) {
                            thrown[0] = e;
                        }
                    }
                });
            }});

            if (thrown[0] != null) throw thrown[0];
            Assert.assertTrue("Failed to modify jar signature file", modified[0]);

            try {
                patchService.uploadPatch(new DataHandler(new FileDataSource(modifiedPatch)));
                Assert.fail("Patch upload should have failed if the manifest was not signed.");
            } catch (PatchException expected) {
                // do nothing
            }
        } finally {
            tempDeleteOk = patch == null || ! patch.exists() || patch.delete();
        }

        if (! tempDeleteOk)
            throw new IOException("Error deleting patch file.");
    }

    @Test
    @Ignore("Disable test FOR NOW which fails under JDK 7.")
    public void testManifestMainAttributesNotSigned() throws Exception {
        File patch = null;
        boolean tempDeleteOk;
        try {
            patch = PatchUtils.buildPatch(getTestPatchSpec(null), getTestPatchSigningParams());

            final Exception[] thrown = new Exception[]{null};
            final boolean[] modified = new boolean[]{false};
            File modifiedPatch = modifyPatch(patch, new HashMap<String, Functions.TernaryVoid<JarEntry, InputStream, JarOutputStream>>() {{
                put("META-INF/MYKEY.SF", new Functions.TernaryVoid<JarEntry, InputStream, JarOutputStream>() {
                    @Override
                    public void call(JarEntry jarEntry, InputStream inputStream, JarOutputStream jos) {
                        try {
                            Manifest manifest = new Manifest(inputStream);
                            modified[0] = manifest.getMainAttributes().remove(new Attributes.Name("SHA1-Digest-Manifest-Main-Attributes")) != null;
                            jos.putNextEntry(new JarEntry(jarEntry.getName()));
                            manifest.write(jos);
                            jos.closeEntry();
                        } catch (Exception e) {
                            thrown[0] = e;
                        }
                    }
                });
            }});

            if (thrown[0] != null) throw thrown[0];
            Assert.assertTrue("Failed to modify jar signature file", modified[0]);

            try {
                patchService.uploadPatch(new DataHandler(new FileDataSource(modifiedPatch)));
                Assert.fail("Patch upload should have failed if the manifest main attributes were not signed.");
            } catch (PatchException expected) {
                // do nothing
            }
        } finally {
            tempDeleteOk = patch == null || ! patch.exists() || patch.delete();
        }

        if (! tempDeleteOk)
            throw new IOException("Error deleting patch file.");
    }

    @Test
    public void testExtraEntry() throws Exception {
        File patch = null;
        boolean tempDeleteOk;
        try {
            patch = PatchUtils.buildPatch(getTestPatchSpec(null), getTestPatchSigningParams());

            final Exception[] thrown = new Exception[]{null};
            final boolean[] modified = new boolean[]{false};
            File modifiedPatch = modifyPatch(patch, new HashMap<String, Functions.TernaryVoid<JarEntry, InputStream, JarOutputStream>>() {{
                put(JarFile.MANIFEST_NAME, new Functions.TernaryVoid<JarEntry, InputStream, JarOutputStream>() {
                    @Override
                    public void call(JarEntry jarEntry, InputStream inputStream, JarOutputStream jos) {
                        try {
                            jos.putNextEntry(new JarEntry(jarEntry.getName()));
                            Manifest manifest = new Manifest(inputStream);
                            manifest.write(jos);
                            jos.closeEntry();
                            jos.putNextEntry(new ZipEntry("extra"));
                            jos.write(0);
                            jos.closeEntry();
                            modified[0] = true;
                        } catch (Exception e) {
                            thrown[0] = e;
                        }
                    }
                });
            }});

            if (thrown[0] != null) throw thrown[0];
            Assert.assertTrue("Failed to modify jar.", modified[0]);

            try {
                patchService.uploadPatch(new DataHandler(new FileDataSource(modifiedPatch)));
                Assert.fail("Patch upload should have failed if there was an extra entry in the jar file.");
            } catch (PatchException expected) {
                // do nothing
            }
        } finally {
            tempDeleteOk = patch == null || ! patch.exists() || patch.delete();
        }

        if (! tempDeleteOk)
            throw new IOException("Error deleting patch file.");
    }

    @Test
    public void testExtraManifestEntry() throws Exception {
        File patch = null;
        boolean tempDeleteOk;
        try {
            patch = PatchUtils.buildPatch(getTestPatchSpec(null), getTestPatchSigningParams());

            final Exception[] thrown = new Exception[]{null};
            final boolean[] modified = new boolean[]{false};
            File modifiedPatch = modifyPatch(patch, new HashMap<String, Functions.TernaryVoid<JarEntry, InputStream, JarOutputStream>>() {{
                put(JarFile.MANIFEST_NAME, new Functions.TernaryVoid<JarEntry, InputStream, JarOutputStream>() {
                    @Override
                    public void call(JarEntry jarEntry, InputStream inputStream, JarOutputStream jos) {
                        try {
                            jos.putNextEntry(new JarEntry(jarEntry.getName()));
                            Manifest manifest = new Manifest(inputStream);
                            modified[0] = manifest.getEntries().put("extra", new Attributes()) == null;
                            manifest.write(jos);
                            jos.closeEntry();
                        } catch (Exception e) {
                            thrown[0] = e;
                        }
                    }
                });
            }});

            if (thrown[0] != null) throw thrown[0];
            Assert.assertTrue("Failed to modify jar.", modified[0]);

            try {
                patchService.uploadPatch(new DataHandler(new FileDataSource(modifiedPatch)));
                Assert.fail("Patch upload should have failed if there was an extra entry in the manifest.");
                // should pass per jar signing api; make sure our own check fails
                // http://java.sun.com/javase/6/docs/technotes/guides/jar/jar.html#Signature%20File
                // Paths or URLs appearing in the manifest file but not in the signature file are not used in the calculation.
            } catch (PatchException expected) {
                System.out.println("");
                // do nothing
            }
        } finally {
            tempDeleteOk = patch == null || ! patch.exists() || patch.delete();
        }

        if (! tempDeleteOk)
            throw new IOException("Error deleting patch file.");
    }

    @Test
    public void testExtraManifestAttribute() throws Exception {
        File patch = null;
        boolean tempDeleteOk;
        try {
            patch = PatchUtils.buildPatch(getTestPatchSpec(null), getTestPatchSigningParams());

            final Exception[] thrown = new Exception[]{null};
            final boolean[] modified = new boolean[]{false};
            File modifiedPatch = modifyPatch(patch, new HashMap<String, Functions.TernaryVoid<JarEntry, InputStream, JarOutputStream>>() {{
                put(JarFile.MANIFEST_NAME, new Functions.TernaryVoid<JarEntry, InputStream, JarOutputStream>() {
                    @Override
                    public void call(JarEntry jarEntry, InputStream inputStream, JarOutputStream jos) {
                        try {
                            jos.putNextEntry(new JarEntry(jarEntry.getName()));
                            Manifest manifest = new Manifest(inputStream);
                            modified[0] = ! "bla".equals(manifest.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_TITLE, "bla"));
                            manifest.write(jos);
                            jos.closeEntry();
                        } catch (Exception e) {
                            thrown[0] = e;
                        }
                    }
                });
            }});

            if (thrown[0] != null) throw thrown[0];
            Assert.assertTrue("Failed to modify jar.", modified[0]);

            try {
                patchService.uploadPatch(new DataHandler(new FileDataSource(modifiedPatch)));
                Assert.fail("Patch upload should have failed if the manifest file was modified.");
            } catch (PatchException expected) {
                // do nothing
            }
        } finally {
            tempDeleteOk = patch == null || ! patch.exists() || patch.delete();
        }

        if (! tempDeleteOk)
            throw new IOException("Error deleting patch file.");
    }

    @Test
    public void testSignatureFileModified() throws Exception {
        File patch = null;
        boolean tempDeleteOk;
        try {
            patch = PatchUtils.buildPatch(getTestPatchSpec(null), getTestPatchSigningParams());

            final Exception[] thrown = new Exception[]{null};
            final boolean[] modified = new boolean[]{false};
            File modifiedPatch = modifyPatch(patch, new HashMap<String, Functions.TernaryVoid<JarEntry, InputStream, JarOutputStream>>() {{
                put("META-INF/MYKEY.SF", new Functions.TernaryVoid<JarEntry, InputStream, JarOutputStream>() {
                    @Override
                    public void call(JarEntry jarEntry, InputStream inputStream, JarOutputStream jos) {
                        try {
                            jos.putNextEntry(new JarEntry(jarEntry.getName()));
                            Manifest manifest = new Manifest(inputStream);
                            manifest.write(jos);
                            jos.write(0x0a);
                            jos.closeEntry();
                            modified[0] = true;
                        } catch (Exception e) {
                            thrown[0] = e;
                        }
                    }
                });
            }});

            if (thrown[0] != null) throw thrown[0];
            Assert.assertTrue("Failed to modify jar.", modified[0]);

            try {
                patchService.uploadPatch(new DataHandler(new FileDataSource(modifiedPatch)));
                Assert.fail("Patch upload should have failed if the signature file was modified.");
            } catch (PatchException expected) {
                // do nothing
            }
        } finally {
            tempDeleteOk = patch == null || ! patch.exists() || patch.delete();
        }

        if (! tempDeleteOk)
            throw new IOException("Error deleting patch file.");
    }

    @Test
    public void testSignatureBlockFileModified() throws Exception {
        File patch = null;
        boolean tempDeleteOk;
        try {
            patch = PatchUtils.buildPatch(getTestPatchSpec(null), getTestPatchSigningParams());

            final Exception[] thrown = new Exception[]{null};
            final boolean[] modified = new boolean[]{false};
            File modifiedPatch = modifyPatch(patch, new HashMap<String, Functions.TernaryVoid<JarEntry, InputStream, JarOutputStream>>() {{
                put("META-INF/MYKEY.DSA", new Functions.TernaryVoid<JarEntry, InputStream, JarOutputStream>() {
                    @Override
                    public void call(JarEntry jarEntry, InputStream inputStream, JarOutputStream jos) {
                        try {
                            jos.putNextEntry(new JarEntry(jarEntry.getName()));
                            jos.write(0x0a);
                            IOUtils.copyStream(inputStream, jos);
                            jos.closeEntry();
                            modified[0] = true;
                        } catch (Exception e) {
                            thrown[0] = e;
                        }
                    }
                });
            }});

            if (thrown[0] != null) throw thrown[0];
            Assert.assertTrue("Failed to modify jar.", modified[0]);

            try {
                patchService.uploadPatch(new DataHandler(new FileDataSource(modifiedPatch)));
                Assert.fail("Patch upload should have failed if the signature block file was modified.");
            } catch (PatchException expected) {
                // do nothing
            }
        } finally {
            tempDeleteOk = patch == null || ! patch.exists() || patch.delete();
        }

        if (! tempDeleteOk)
            throw new IOException("Error deleting patch file.");
    }

    @Test
    public void testFileModified() throws Exception {
        File patch = null;
        boolean tempDeleteOk;
        try {
            patch = PatchUtils.buildPatch(getTestPatchSpec(null), getTestPatchSigningParams());

            final Exception[] thrown = new Exception[]{null};
            final boolean[] modified = new boolean[]{false};
            File modifiedPatch = modifyPatch(patch, new HashMap<String, Functions.TernaryVoid<JarEntry, InputStream, JarOutputStream>>() {{
                put(PatchPackage.PATCH_PROPERTIES_ENTRY, new Functions.TernaryVoid<JarEntry, InputStream, JarOutputStream>() {
                    @Override
                    public void call(JarEntry jarEntry, InputStream inputStream, JarOutputStream jos) {
                        try {
                            jos.putNextEntry(new JarEntry(jarEntry.getName()));
                            IOUtils.copyStream(inputStream, jos);
                            jos.write(0x0a);
                            jos.closeEntry();
                            modified[0] = true;
                        } catch (Exception e) {
                            thrown[0] = e;
                        }
                    }
                });
            }});

            if (thrown[0] != null) throw thrown[0];
            Assert.assertTrue("Failed to modify jar.", modified[0]);

            try {
                patchService.uploadPatch(new DataHandler(new FileDataSource(modifiedPatch)));
                Assert.fail("Patch upload should have failed if a file was modified.");
            } catch (PatchException expected) {
                // do nothing
            }
        } finally {
            tempDeleteOk = patch == null || ! patch.exists() || patch.delete();
        }

        if (! tempDeleteOk)
            throw new IOException("Error deleting patch file.");
    }

    @Test
    @Ignore("Disable test FOR NOW which fails under JDK 7.")
    public void testCorruptedArchive() throws Exception {
        File patch = null;
        boolean tempDeleteOk;
        try {
            patch = PatchUtils.buildPatch(getTestPatchSpec(null), getTestPatchSigningParams());

            try {
                if (patch.length() > Integer.MAX_VALUE)
                    throw new IllegalArgumentException("Patch is too big."); // shouldn't happen
                int patchSize = (int) patch.length();
                byte[] modified = new byte[patchSize + 1];
                IOUtils.slurpStream(new FileInputStream(patch), modified);
                modified[patchSize] = 0x0a;
                patchService.uploadPatch(new DataHandler(new ByteArrayDataSource(modified, "")));
                Assert.fail("Patch upload should have failed if a file was modified.");
            } catch (PatchException expected) {
                // do nothing
            }
        } finally {
            tempDeleteOk = patch == null || ! patch.exists() || patch.delete();
        }

        if (! tempDeleteOk)
            throw new IOException("Error deleting patch file.");
    }

    @Test
    public void testSignerNotTrusted() throws Exception {
        File patch = null;
        boolean tempDeleteOk;
        try {
            patch = PatchUtils.buildPatch(getTestPatchSpec(null), getUntrustedPatchSigningParams());
            //ReflectionTestUtils.setField(config, "trustedPatchCerts", new HashSet<X509Certificate>());
            patchService.uploadPatch(new DataHandler(new FileDataSource(patch)));
            Assert.fail("Patch upload should have failed if the signer of the patch is not trusted.");
        } catch (PatchException expected) {
        } finally {
            tempDeleteOk = patch == null || ! patch.exists() || patch.delete();
        }

        if (! tempDeleteOk)
            throw new IOException("Error deleting patch file.");
    }

    @Test
    public void testMainClassNotSpecified() throws Exception {
        File patch = null;
        boolean tempDeleteOk;
        try {
            patch = PatchUtils.buildUnsignedPatch(getTestPatchSpec(null));

            final Exception[] thrown = new Exception[]{null};
            final boolean[] modified = new boolean[]{false};
            File modifiedPatch = modifyPatch(patch, new HashMap<String, Functions.TernaryVoid<JarEntry, InputStream, JarOutputStream>>() {{
                put(JarFile.MANIFEST_NAME, new Functions.TernaryVoid<JarEntry, InputStream, JarOutputStream>() {
                    @Override
                    public void call(JarEntry jarEntry, InputStream inputStream, JarOutputStream jos) {
                        try {
                            jos.putNextEntry(new JarEntry(jarEntry.getName()));
                            Manifest manifest = new Manifest(inputStream);
                            modified[0] = manifest.getMainAttributes().remove(Attributes.Name.MAIN_CLASS) != null;
                            manifest.write(jos);
                            jos.closeEntry();
                        } catch (Exception e) {
                            thrown[0] = e;
                        }
                    }
                });
            }});

            if (thrown[0] != null) throw thrown[0];
            Assert.assertTrue("Failed to modify jar.", modified[0]);

            File modifiedSignedPatch = JarUtils.sign(modifiedPatch, getTestPatchSigningParams());
            try {
                patchService.uploadPatch(new DataHandler(new FileDataSource(modifiedSignedPatch)));
                Assert.fail("Patch upload should have failed if the jar main class is not specified.");
            } catch (PatchException expected) {
                // do nothing
            }
        } finally {
            tempDeleteOk = patch == null || ! patch.exists() || patch.delete();
        }

        if (! tempDeleteOk)
            throw new IOException("Error deleting patch file.");
    }

    @Test
    public void testMainClassModified() throws Exception {
        File patch = null;
        boolean tempDeleteOk;
        try {
            patch = PatchUtils.buildPatch(getTestPatchSpec(null), getTestPatchSigningParams());

            final Exception[] thrown = new Exception[]{null};
            final boolean[] modified = new boolean[]{false};
            File modifiedPatch = modifyPatch(patch, new HashMap<String, Functions.TernaryVoid<JarEntry, InputStream, JarOutputStream>>() {{
                put(JarFile.MANIFEST_NAME, new Functions.TernaryVoid<JarEntry, InputStream, JarOutputStream>() {
                    @Override
                    public void call(JarEntry jarEntry, InputStream inputStream, JarOutputStream jos) {
                        try {
                            jos.putNextEntry(new JarEntry(jarEntry.getName()));
                            Manifest manifest = new Manifest(inputStream);
                            modified[0] = ! "bla".equals(manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "bla"));
                            manifest.write(jos);
                            jos.closeEntry();
                        } catch (Exception e) {
                            thrown[0] = e;
                        }
                    }
                });
            }});

            if (thrown[0] != null) throw thrown[0];
            Assert.assertTrue("Failed to modify jar.", modified[0]);

            try {
                patchService.uploadPatch(new DataHandler(new FileDataSource(modifiedPatch)));
                Assert.fail("Patch upload should have failed if the jar main class was modified.");
            } catch (PatchException expected) {
                // do nothing
            }
        } finally {
            tempDeleteOk = patch == null || ! patch.exists() || patch.delete();
        }

        if (! tempDeleteOk)
            throw new IOException("Error deleting patch file.");
    }

    @Test
    public void testMainClassFileNotProvided() throws Exception {
        File patch = null;
        boolean tempDeleteOk;
        try {
            patch = PatchUtils.buildUnsignedPatch(getTestPatchSpec(null));

            final Exception[] thrown = new Exception[]{null};
            final boolean[] modified = new boolean[]{false};
            File modifiedPatch = modifyPatch(patch, new HashMap<String, Functions.TernaryVoid<JarEntry, InputStream, JarOutputStream>>() {{
                put(PatchUtils.classToEntryName("com.l7tech.server.processcontroller.patching.HelloFileWorldPatch"),
                    new Functions.TernaryVoid<JarEntry, InputStream, JarOutputStream>() {
                    @Override
                    public void call(JarEntry jarEntry, InputStream inputStream, JarOutputStream jos) {
                        // skip it
                        modified[0] = true;
                    }
                });
            }});

            if (thrown[0] != null) throw thrown[0];
            Assert.assertTrue("Failed to modify jar.", modified[0]);

            try {
                patchService.uploadPatch(new DataHandler(new FileDataSource(JarUtils.sign(modifiedPatch, getTestPatchSigningParams()))));
                Assert.fail("Patch upload should have failed if the jar main class was modified.");
            } catch (PatchException expected) {
                // do nothing
            }
        } finally {
            tempDeleteOk = patch == null || ! patch.exists() || patch.delete();
        }

        if (! tempDeleteOk)
            throw new IOException("Error deleting patch file.");
    }

    @Test
    public void testMissingPatchProperties() throws Exception {
        File patch = null;
        boolean tempDeleteOk;
        try {
            patch = PatchUtils.buildPatch(getTestPatchSpec(null), getTestPatchSigningParams());

            final Exception[] thrown = new Exception[]{null};
            final boolean[] modified = new boolean[]{false};
            File modifiedPatch = modifyPatch(patch, new HashMap<String, Functions.TernaryVoid<JarEntry, InputStream, JarOutputStream>>() {{
                put(PatchPackage.PATCH_PROPERTIES_ENTRY, new Functions.TernaryVoid<JarEntry, InputStream, JarOutputStream>() {
                    @Override
                    public void call(JarEntry jarEntry, InputStream inputStream, JarOutputStream jos) {
                        // skip it
                        modified[0] = true;
                    }
                });
            }});

            if (thrown[0] != null) throw thrown[0];
            Assert.assertTrue("Failed to modify jar.", modified[0]);

            try {
                patchService.uploadPatch(new DataHandler(new FileDataSource(modifiedPatch)));
                Assert.fail("Patch upload should have failed if there was no patch.properties file.");
            } catch (PatchException expected) {
                // do nothing
            }
        } finally {
            tempDeleteOk = patch == null || ! patch.exists() || patch.delete();
        }

        if (! tempDeleteOk)
            throw new IOException("Error deleting patch file.");
    }

    @Test
    @Ignore("Apparently allowed by the Jar signing spec; would be desirable for the signature verification to fail if extra files are added under META-INF/")
    public void testExtraMetainfEntry() throws Exception {
        File patch = null;
        boolean tempDeleteOk;
        try {
            patch = PatchUtils.buildPatch(getTestPatchSpec(null), getTestPatchSigningParams());

            final Exception[] thrown = new Exception[]{null};
            final boolean[] modified = new boolean[]{false};
            File modifiedPatch = modifyPatch(patch, new HashMap<String, Functions.TernaryVoid<JarEntry, InputStream, JarOutputStream>>() {{
                put(JarFile.MANIFEST_NAME, new Functions.TernaryVoid<JarEntry, InputStream, JarOutputStream>() {
                    @Override
                    public void call(JarEntry jarEntry, InputStream inputStream, JarOutputStream jos) {
                        try {
                            jos.putNextEntry(new JarEntry(jarEntry.getName()));
                            Manifest manifest = new Manifest(inputStream);
                            manifest.write(jos);
                            jos.closeEntry();

                            jos.putNextEntry(new ZipEntry("META-INF/extra"));
                            jos.write(0);
                            jos.closeEntry();
                            modified[0] = true;

                        } catch (Exception e) {
                            thrown[0] = e;
                        }
                    }
                });
            }});

            if (thrown[0] != null) throw thrown[0];
            Assert.assertTrue("Failed to modify jar.", modified[0]);

            try {
                patchService.uploadPatch(new DataHandler(new FileDataSource(modifiedPatch)));
                Assert.fail("Patch upload should have failed if there was an extra entry under META-INF/.");
            } catch (PatchException expected) {
                System.out.println("");
                // do nothing
            }
        } finally {
            tempDeleteOk = patch == null || ! patch.exists() || patch.delete();
        }

        if (! tempDeleteOk)
            throw new IOException("Error deleting patch file.");
    }

    // - PRIVATE

    private static final String TEST_SIGNER_KEYSTORE_FILE = "testPatchSigner.jks";
    private static final String TEST_UNTRUSTED_SIGNER_KEYSTORE_FILE = "testUntrustedPatchSigner.jks";
    private static final String TEST_SIGNER_KEYSTORE_TYPE = "JKS";
    private static final String TEST_SIGNER_PASSWORD = "password"; // for both key and keystore

    private final PatchServiceApi patchService = new PatchServiceApiImpl();
    private ConfigService config;
    private X509Certificate patchSignerCert;
    private String keystorePath;
    private String untrustedKeystorePath;

    private PatchSpec getTestPatchSpec(String patchId) {
        Class mainClass = HelloFileWorldPatch.class; 
        PatchSpec spec = new PatchSpec(patchId == null || patchId.isEmpty() ? PatchUtils.generatePatchId() : patchId, "test patch", false, mainClass.getName());
        return spec.entry(new PatchSpecClassEntry(mainClass));
    }

    private JarSignerParams getTestPatchSigningParams() {
        return new JarSignerParams(keystorePath, TEST_SIGNER_PASSWORD, "mykey", TEST_SIGNER_PASSWORD);
    }

    private JarSignerParams getUntrustedPatchSigningParams() {
        return new JarSignerParams(untrustedKeystorePath, TEST_SIGNER_PASSWORD, "untrustedSigner", TEST_SIGNER_PASSWORD);
    }

    /** modifiers must .putNextEntry() and .closeEntry() on the output stream */
    private File modifyPatch(File patch, Map<String,Functions.TernaryVoid<JarEntry, InputStream,JarOutputStream>> modifiers) throws IOException {
        File modifiedJar;
        try (JarFile jar = new JarFile(patch)) {
            modifiedJar = File.createTempFile(patch.getName(), "_modified.zip", null);
            modifiedJar.deleteOnExit();
            try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(modifiedJar))) {
                Enumeration<JarEntry> entries = jar.entries();
                while(entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (modifiers.containsKey(entry.getName())) {
                        modifiers.get(entry.getName()).call(entry, jar.getInputStream(entry),jos);
                    } else {
                        jos.putNextEntry(new JarEntry(entry));
                        IOUtils.copyStream(jar.getInputStream(entry), jos);
                        jos.closeEntry();

                    }
                }
            }
        }

        return modifiedJar;
    }
}

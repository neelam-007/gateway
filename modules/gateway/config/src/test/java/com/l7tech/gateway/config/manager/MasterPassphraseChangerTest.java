package com.l7tech.gateway.config.manager;

import com.l7tech.util.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 *
 */
public class MasterPassphraseChangerTest {

    final File configDir = new File("kmptest");
    final File ompFile = new File(configDir, "omp.dat");
    final File kmpFile = new File(configDir, "kmp.properties");
    final File propFile = new File(configDir, "blah.properties");

    @Before
    public void createTestDirectory() throws Exception {
        //noinspection ResultOfMethodCallIgnored
        configDir.mkdirs();
        if (!configDir.isDirectory()) fail("Unable to create directory: " + configDir);
        if (!configDir.canRead()) fail("Not readable: " + configDir);
        if (!configDir.canWrite()) fail("Not writable: " + configDir);
    }

    @After
    public void cleanupTestDirectory() throws Exception {
        File[] files = configDir.listFiles();
        final String kmpName = kmpFile.getName();
        final String ompName = ompFile.getName();
        final String propName = propFile.getName();
        for (File file : files) {
            String name = file.getName();
            if (name.startsWith(kmpName) || name.startsWith(ompName) || name.startsWith(propName))
                //noinspection ResultOfMethodCallIgnored
                file.delete();
        }
        //noinspection ResultOfMethodCallIgnored
        configDir.delete();

        assertFalse("Test directory shall have been successfully deleted after each test", configDir.exists());
    }

    @Test
    public void testGenerateNewKmp_withPreviousOmp() throws Exception {
        // Set up config dir contents
        final byte[] ompPlaintextBytes = "masterpass".getBytes(Charsets.UTF8);
        setFileContents(ompFile, ObfuscatedFileMasterPasswordFinder.obfuscate(ompPlaintextBytes));
        setFileContents(propFile, "node.cluster.pass=" + new MasterPasswordManager(ompPlaintextBytes).encryptPassword("SuperSeeekrit".toCharArray()));
        setFileContents(kmpFile,
                "keystore.type=PKCS12\n" +
                "keystore.password=keypass123\n" +
                "keystore.entry.password=keypass123\n");



        // Run kmp populator
        new MasterPassphraseChanger(configDir.getAbsolutePath(), MasterPassphraseChanger.PASSWORD_PROPERTIES, "kmp", "generateAll").run();

        assertTrue("A kmp file should exist", kmpFile.exists());
        assertEquals("Omp file that already existed shall have been truncated", 0L, ompFile.length());

        // Check that kmp file got fleshed out
        Properties kmp = props(kmpFile);
        assertTrue("kmp keystore shall have been created", kmp.getProperty(KeyStorePrivateKeyMasterPasswordFinder.PROP_KEYSTORE_CONTENTS_BASE64).trim().length() > 0);
        assertTrue("kmp passphrase shall have been created", kmp.getProperty(KeyStorePrivateKeyMasterPasswordFinder.PROP_MASTER_PASSPHRASE_CIPHERTEXT_BASE64).trim().length() > 0);

        // Check that properties got re-encrypted successfully
        Properties props = props(propFile);
        String passval = props.getProperty("node.cluster.pass");
        assertTrue("Password property shall have been reencrypted", passval.startsWith("$L7C$"));

        // Check that encrypted password can be decrypted
        final MasterPasswordManager mpm = new MasterPasswordManager(new KeyStorePrivateKeyMasterPasswordFinder(ompFile));
        assertEquals("Re-encrypted password shall be decrypted successfully using keystore-protected master passphrase", "SuperSeeekrit", new String(mpm.decryptPassword(passval)));
    }

    @Test
    public void testGenerateNewKmp_noPreviousOmp() throws Exception {
        // Set up config dir contents
        setFileContents(propFile, "node.cluster.pass=SuperSeeekrit");
        setFileContents(kmpFile,
                "keystore.type=PKCS12\n" +
                "keystore.password=keypass123\n" +
                "keystore.entry.password=keypass123\n");

        // Run kmp populator
        new MasterPassphraseChanger(configDir.getAbsolutePath(), MasterPassphraseChanger.PASSWORD_PROPERTIES, "kmp", "generateAll").run();

        assertTrue("A kmp file should exist", kmpFile.exists());
        assertTrue("No omp file should exist", !ompFile.exists());

        // Check that kmp file got fleshed out
        Properties kmp = props(kmpFile);
        assertTrue("kmp keystore shall have been created", kmp.getProperty(KeyStorePrivateKeyMasterPasswordFinder.PROP_KEYSTORE_CONTENTS_BASE64).trim().length() > 0);
        assertTrue("kmp passphrase shall have been created", kmp.getProperty(KeyStorePrivateKeyMasterPasswordFinder.PROP_MASTER_PASSPHRASE_CIPHERTEXT_BASE64).trim().length() > 0);

        // Check that properties got re-encrypted
        Properties props = props(propFile);
        String passval = props.getProperty("node.cluster.pass");
        assertTrue("Password property shall have been encrypted", passval.startsWith("$L7C$"));

        // Check that encrypted password can be decrypted
        final MasterPasswordManager mpm = new MasterPasswordManager(new KeyStorePrivateKeyMasterPasswordFinder(ompFile));
        assertEquals("Re-encrypted password shall be decrypted successfully using keystore-protected master passphrase", "SuperSeeekrit", new String(mpm.decryptPassword(passval)));
    }

    @Test(expected = FileNotFoundException.class)
    public void testGenerateNewKmp_noKmpFile() throws Exception {
        new MasterPassphraseChanger(configDir.getAbsolutePath(), MasterPassphraseChanger.PASSWORD_PROPERTIES, "kmp", "generateAll").run();
    }

    @Test
    public void testMigrateFromKmpToOmp() throws Exception {
        // Generate new KMP
        testGenerateNewKmp_withPreviousOmp();

        // Now test migrating back to OMP
        new MasterPassphraseChanger(configDir.getAbsolutePath(), MasterPassphraseChanger.PASSWORD_PROPERTIES, "kmp", "migrateFromKmpToOmp").run();

        assertTrue("No kmp file should exist", !kmpFile.exists());
        assertTrue("An omp file should exist", ompFile.exists());
        assertTrue("The OMP file should be non-empty", ompFile.length() > 0);

        // Check that properties got re-encrypted
        Properties props = props(propFile);
        String passval = props.getProperty("node.cluster.pass");
        assertTrue("Password property shall have been reencrypted", passval.startsWith("$L7C$"));

        // Check that encrypted password can still be decrypted with obfuscated master passphrase
        final MasterPasswordManager mpm = new MasterPasswordManager(new ObfuscatedFileMasterPasswordFinder(ompFile));
        assertEquals("Encrypted password can still be decrypted successfully using obfuscated master passphrase", "SuperSeeekrit", new String(mpm.decryptPassword(passval)));
    }

    private static void setFileContents(File file, String contents) throws IOException {
        FileUtils.save(contents.getBytes(Charsets.UTF8), file);
    }

    private Properties props(File file) throws IOException {
        InputStream is = null;
        try {
            Properties props = new Properties();
            props.load(is = new FileInputStream(file));
            return props;
        } finally {
            ResourceUtils.closeQuietly(is);
        }
    }
}

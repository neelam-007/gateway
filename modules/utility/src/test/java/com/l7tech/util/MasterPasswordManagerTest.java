package com.l7tech.util;

import com.l7tech.test.BenchmarkRunner;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

/**
 * Test for MasterPasswordManager class.
 */
public class MasterPasswordManagerTest {
    private static final String LEGACY_CIPHERTEXT = "$L7C$XeKVziKOzNnDHXebZNXMkg==$AqL4EnD+Rz9V8J/ez6hilPFM6e9ZDvLwqwvgPMrzPuk=";

    private static final String CIPHERTEXT_TRALALA =
            "$L7C2$1bead,WLU3fZIeV6+DqlKGvfmSR1aOfShLwJuRX53dlGtEoZA=$dIw8/xDAUaRiJ9Hh1BUSzMQm5XFsv143GosAhsnWLj3QqhafXB4Uv5ysMOyaFiF1ff+oo73MNURaD6Flgo/4vQ==";
    private static final String CIPHERTEXT_7LAYER =
            "$L7C2$1bead,dZ+ujSreFSo0ItT4Mi0A6ENY3gQYlArDVq7TDJx9v9Q=$H0SSu1q/DS0cgV6JxHsqz3R78lpzdna08W696s4D+hxvsSf8FvHoDd22ZFH8GUF0pU8sIjPX9hSaQQlMAosB7Q==";
    private static final String CIPHERTEXT_PASSWORD =
            "$L7C2$1bead,eSdd8IGrHlK12rHpSaBQ/y9zbLP6bRi0YZUmUFSAIRk=$JRTH2ocD9trjuQgQLn9K6zSLTVxXWTuNU+qxhaHfhIH3V4Fn6REQKM66PVO1ZvhJ65AZaGH7c7nAVsH/rD4UbQ==";
    private static final String CIPHERTEXT_LONG =
            "$L7C2$1bead,GbqMGWaG8tTmqX9ajSKfKeernvkGbJtlZ3DGOQnZmHA=$/vGwGKUeF77DCY2EUr4tOUD7TV0hI2Uz4MT4mvNluoI6MfCgvnIvnp1wcoSdzzIhz54FBlOMzojGvt5ixRd01SXa+VyJDGFKxwjOLqqzvw/+GDtDEPBGduqAsM9MJpBdb5ZElEybzh5oklVf+E+1LsKcWKcLhnVMK4fVpwga7kigIwb2DK1Wh0C8c5NFVJ3iG2h/TMUyPfODm9gSscaeMQ==";
    private static final String CIPHERTEXT_EMPTY =
            "$L7C2$1bead,Ak3+DKF9uAxiu9H7PzguFhZou4Oqx/R5Uy72BESF1rs=$AWzML/nqK2HiKd6+/d459oQBI8zvF5vd8FM7QMmmZo2910PsxERLO4MlrdKecZ4YUQN3gWK+JxSz6evAuCy+zQ==";

    private static final String CIPHERTEXT_LEGACY_TRALALA =
            "$L7C$eYr/xG/Ssax3iq/IRDweEQ==$h4717RLYSe6llHScVnGPOA==";
    private static final String CIPHERTEXT_LEGACY_7LAYER =
            "$L7C$R5+x4i1UeifGQ5i+FsWgYQ==$dVOB1N3q+Bwj51g1OzLBog==";
    private static final String CIPHERTEXT_LEGACY_PASSWORD =
            "$L7C$34ezo1MKwnDXzBV1lUzYMg==$hBMdLrIrJJXYJu+xdVfawA==";
    private static final String CIPHERTEXT_LEGACY_LONG =
            "$L7C$P+/FD73LFh+6ISrDdVxzug==$xVSBiG9iyKWzLf9g3TU3V9yYBL8QY/hhfMIfgny0gmY8LFUE3WWI++lheyU4+ajZONgxnqwGWmdCdDqaWPtzMn4JSagMfrIyXtKB9zBPOGwuFDXo8VEJJI8uAfaLwSJvpNeSlpLrHtCJ/hyr2DRCCA==";
    private static final String CIPHERTEXT_LEGACY_EMPTY =
            "$L7C$Olc7Q+YufXZyqyTMdk5MZg==$Vd44AboooDXgc/yDzR6iXw==";

    private static Collection<Pair<String, String>> TEST_VECTORS = new ArrayList<>( Arrays.asList(
            new Pair<>( "tralala", CIPHERTEXT_TRALALA ),
            new Pair<>( "7layer", CIPHERTEXT_7LAYER ),
            new Pair<>( "password", CIPHERTEXT_PASSWORD ),
            new Pair<>( "A very long password indeed!!!  asdkjfhasdjfhasdlkfjhasdfkjhadskfjhadsfkljahdsflkjhdslkjsadhljkaf", CIPHERTEXT_LONG ),
            new Pair<>( "", CIPHERTEXT_EMPTY ),
            new Pair<>( "tralala", CIPHERTEXT_LEGACY_TRALALA ),
            new Pair<>( "7layer", CIPHERTEXT_LEGACY_7LAYER ),
            new Pair<>( "password", CIPHERTEXT_LEGACY_PASSWORD ),
            new Pair<>( "A very long password indeed!!!  asdkjfhasdjfhasdlkfjhasdfkjhadskfjhadsfkljahdsflkjhdslkjsadhljkaf", CIPHERTEXT_LEGACY_LONG ),
            new Pair<>( "", CIPHERTEXT_LEGACY_EMPTY )
            ) );

    private static final String CIPHERTEXT_PASSWORD_NO_CIPHERTEXT =
            "$L7C2$2710,0rZxxPrW0pnF/KCVahM90enkT77Z70Q/vXpilySIYWk=$" +
                    HexUtils.encodeBase64( ArrayUtils.concatMulti( HexUtils.decodeBase64( "ekpxZfE8AxB/pvKvNzWuSQ==" ), HexUtils.decodeBase64( "SeMXdh21YulDmJmTh9y0yFtGzYewzrTnRscnYzVUwR4=" ) ), true );
    private static final String CIPHERTEXT_PASSWORD_NOT_ENOUGH_CIPHERTEXT =
            "$L7C2$2710,0rZxxPrW0pnF/KCVahM90enkT77Z70Q/vXpilySIYWk=$" +
                    HexUtils.encodeBase64( ArrayUtils.concat( HexUtils.decodeBase64( "ekpxZfE8AxB/pvKvNzWuSQ==" ), HexUtils.decodeBase64( "SeMXdh21YulDmJmTh9y0yFtGzYewzrTnRscnYzVU" ) ), true );

    private static final String CIPHERTEXT_LONG_BAD_ITERATION_COUNT =
            "$L7C2$2711,MlJa9oq1dd9SmRrI9rYc03segImJvW48VuC9E71Xn8Y=$MLfUyj31TTTqTE97QP39VmBerYV+5YV5Cr0pdBSNiILJzXvlH3l31hz/mp4EL6FRDHpTMQlxgns9psFxdh6UsYaw4m9WOeZlfciCvngLdVfKC92smSJNBRShsvy1/qr5zAD6vS9iycb4ZBWbOCEy1OJsXu7tbD/rbPsU0novKFo8pBXWZppsdwfJL8WSsw/JFsT01+HoIrIjD2gnvruDQQ==";
    private static final String CIPHERTEXT_LONG_BAD_SALT =
            "$L7C2$2710,MlJa9oq1dd9SmRrI9rYd03segImJvW48VuC9E71Xn8Y=$MLfUyj31TTTqTE97QP39VmBerYV+5YV5Cr0pdBSNiILJzXvlH3l31hz/mp4EL6FRDHpTMQlxgns9psFxdh6UsYaw4m9WOeZlfciCvngLdVfKC92smSJNBRShsvy1/qr5zAD6vS9iycb4ZBWbOCEy1OJsXu7tbD/rbPsU0novKFo8pBXWZppsdwfJL8WSsw/JFsT01+HoIrIjD2gnvruDQQ==";
    private static final String CIPHERTEXT_LONG_BAD_IV =
            "$L7C2$2710,MlJa9oq1dd9SmRrI9rYc03segImJvW48VuC9E71Xn8Y=$MLfUxj31TTTqTE97QP39VmBerYV+5YV5Cr0pdBSNiILJzXvlH3l31hz/mp4EL6FRDHpTMQlxgns9psFxdh6UsYaw4m9WOeZlfciCvngLdVfKC92smSJNBRShsvy1/qr5zAD6vS9iycb4ZBWbOCEy1OJsXu7tbD/rbPsU0novKFo8pBXWZppsdwfJL8WSsw/JFsT01+HoIrIjD2gnvruDQQ==";
    private static final String CIPHERTEXT_LONG_BAD_MAC =
            "$L7C2$2710,MlJa9oq1dd9SmRrI9rYc03segImJvW48VuC9E71Xn8Y=$MLfUyj31TTTqTE97QP39VmBerYV+5YV5Cr0pdBSNiILJzXvlH3l31hz/mp4EL6FRDHpTMQlxgns9psFxdh6UsYaw4m9WOeZlfciCvngLdVfKC92smSJNBRShsvy1/qr5zAD6vS9iycb4ZBWbOCEy1OJsXu7tbD/rbPsU0novKFo8pBXWZppsdwfJL8WSsw/JFsT01+HoIrIjD2gnvsuDQQ==";
    private static final String CIPHERTEXT_LONG_BAD_CIPHERTEXT =
            "$L7C2$2710,MlJa9oq1dd9SmRrI9rYc03segImJvW48VuC9E71Xn8Y=$MLfUyj31TTTqTE97QP39VmBerYV+5YV5Cr0pdBSNiILJzXvlH3l31hz/mp4EL6FRDHpTMQlxgns9psGxdh6UsYaw4m9WOeZlfciCvngLdVfKC92smSJNBRShsvy1/qr5zAD6vS9iycb4ZBWbOCEy1OJsXu7tbD/rbPsU0novKFo8pBXWZppsdwfJL8WSsw/JFsT01+HoIrIjD2gnvruDQQ==";
    private static final String CIPHERTEXT_LONG_SHORT_SALT =
            "$L7C2$2710,MlJa9oq1dd9SmRrI9rYc03segImJvW48VuC9E71X$MLfUyj31TTTqTE97QP39VmBerYV+5YV5Cr0pdBSNiILJzXvlH3l31hz/mp4EL6FRDHpTMQlxgns9psFxdh6UsYaw4m9WOeZlfciCvngLdVfKC92smSJNBRShsvy1/qr5zAD6vS9iycb4ZBWbOCEy1OJsXu7tbD/rbPsU0novKFo8pBXWZppsdwfJL8WSsw/JFsT01+HoIrIjD2gnvruDQQ==";
    private static final String CIPHERTEXT_LONG_SHORT_MAC =
            "$L7C2$2710,MlJa9oq1dd9SmRrI9rYc03segImJvW48VuC9E71Xn8Y=$MLfUyj31TTTqTE97QP39VmBerYV+5YV5Cr0pdBSNiILJzXvlH3l31hz/mp4EL6FRDHpTMQlxgns9psFxdh6UsYaw4m9WOeZlfciCvngLdVfKC92smSJNBRShsvy1/qr5zAD6vS9iycb4ZBWbOCEy1OJsXu7tbD/rbPsU0novKFo8pBXWZppsdwfJL8WSsw/JFsT01+HoIrIjD2gnvruD";
    private static final String CIPHERTEXT_7LAYER_WRONG_PASSPHRASE =
            "$L7C2$1bead,8oSahx2F2myXH06W9piLm99hcUwqpXH+lQLrCEqo5Jo=$0PjTGAk8qwW1ZXdmlxeum9LhsCfMfnEp+qUpf761kbyI8vc7qphN8CvJqr1ZA+cZMzlItnIMH+aKGlxtsMbYTg==";

    private static final String[] TEST_VECTORS_INVALID_L7C2_BAD_MAC_VALUE = {
            CIPHERTEXT_LONG_BAD_ITERATION_COUNT,
            CIPHERTEXT_LONG_BAD_SALT,
            CIPHERTEXT_LONG_BAD_IV,
            CIPHERTEXT_LONG_BAD_MAC,
            CIPHERTEXT_LONG_BAD_CIPHERTEXT,
            CIPHERTEXT_LONG_SHORT_SALT,
            CIPHERTEXT_LONG_SHORT_MAC,
            CIPHERTEXT_7LAYER_WRONG_PASSPHRASE
    };

    @After
    public void after() {
        cleanupSysprops();
    }

    @AfterClass
    public static void afterClass() {
        cleanupSysprops();
    }

    private static void cleanupSysprops() {
        SyspropUtil.clearProperties( "com.l7tech.util.MasterPasswordManager.emitLegacyEncryption" );
    }

    private void legacyMode() {
        SyspropUtil.setProperty( "com.l7tech.util.MasterPasswordManager.emitLegacyEncryption", "true" );
    }


    @Test
    public void testObfuscate() throws Exception {
        String cleartext = "secret password";
        String obfuscated = ObfuscatedFileMasterPasswordFinder.obfuscate(cleartext.getBytes(Charsets.UTF8));
        System.out.println(cleartext + " -> " + obfuscated);
    }

    @Test
    public void testUnobfuscate() throws Exception {
        String obfuscated = "$L7O$LTc0MzIyOTY4NjYwODQ1MTk4ODU=$xU3WNeqb/t+tl+BtH4Be";
        byte[] cleartext = ObfuscatedFileMasterPasswordFinder.unobfuscate(obfuscated);
        System.out.println(obfuscated + " -> " + new String(cleartext, Charsets.UTF8));
    }

    @Test
    public void testObfuscationRoundTrip() throws Exception {
        String cleartext = "mumbleahasdfoasdghuigh";
        String obfuscated = ObfuscatedFileMasterPasswordFinder.obfuscate(cleartext.getBytes(Charsets.UTF8));
        assertFalse(cleartext.equalsIgnoreCase(obfuscated));
        byte[] unobfuscated = ObfuscatedFileMasterPasswordFinder.unobfuscate(obfuscated);
        assertTrue(Arrays.equals(cleartext.getBytes(Charsets.UTF8), unobfuscated));
    }

    @Test
    public void testObfuscationRoundTrip_binaryData() throws Exception {
        byte[] cleartext = new byte[] { -2, 4, 6, 22, -128, 127, 4, 55 };
        String obfuscated = ObfuscatedFileMasterPasswordFinder.obfuscate(cleartext);
        byte[] unobfuscated = ObfuscatedFileMasterPasswordFinder.unobfuscate(obfuscated);
        assertTrue(Arrays.equals(cleartext, unobfuscated));
    }

    @Test
    public void testBackwardCompatibility() throws Exception {
        // !!! this string must never change, since it's historical data to guarantee backward compatibility with existing client data
        String obfuscated = "$L7O$LTQ5ODIzNTQ4ODUzOTIyNTc1MzM=$AH9iXRGlLs5hbsJ12LPT";
        byte[] cleartext = ObfuscatedFileMasterPasswordFinder.unobfuscate(obfuscated);
        assertEquals("secret password", new String(cleartext, Charsets.UTF8));
    }

    @Test
    public void testCompatibilityWithTestVectors() throws Exception {
        // In normal mode, both L7C and L7C2 strings can be recognized.
        for ( Pair<String, String> entry : TEST_VECTORS ) {
            String expectedPlaintext = entry.left;
            String ciphertext = entry.right;
            String gotPlaintext = new String( new MasterPasswordManager( staticFinder( "7layer" ) ).decryptPassword( ciphertext ) );
            assertEquals( expectedPlaintext, gotPlaintext );
        }
    }

    @Test
    public void testCompatibilityWithTestVectorsInLegacyMode() throws Exception {
        legacyMode();

        // In legacy mode, both L7C and L7C2 strings can be recognized.
        testCompatibilityWithTestVectors();
    }

    private SecretEncryptorKeyFinder staticFinder(final String masterPassword) {
        return staticFinder( masterPassword.getBytes( Charsets.UTF8 ) );
    }

    private SecretEncryptorKeyFinder staticFinder( final byte[] keyBytes ) {
        return new SecretEncryptorKeyFinder() {
            @Override
            public byte[] findMasterPasswordBytes() {
                return Arrays.copyOf( keyBytes, keyBytes.length );
            }
        };
    }

    @Test
    public void testEncrypt() throws Exception {
        String cleartext = "big secret password";
        MasterPasswordManager mpm = new MasterPasswordManager(staticFinder("my master password"));

        String ciphertext = mpm.encryptPassword(cleartext.toCharArray());
        System.out.println(cleartext + " -> " + ciphertext);
        assertTrue( ciphertext.startsWith( "$L7C2$" ) );
    }

    @Test
    public void testLegacyEncrypt() throws Exception {
        legacyMode();
        String cleartext = "big secret password";
        MasterPasswordManager mpm = new MasterPasswordManager(staticFinder("my master password"));

        String ciphertext = mpm.encryptPassword(cleartext.toCharArray());
        System.out.println(cleartext + " -> " + ciphertext);
        assertTrue( ciphertext.startsWith( "$L7C$" ) );
    }

    @Test
    public void testLegacyDecrypt() throws Exception {
        // this string must never change, since it's historical data to guarantee backward compatibility with existing test data
        MasterPasswordManager mpm = new MasterPasswordManager(staticFinder("my master password"));

        char[] cleartextChars = mpm.decryptPassword( LEGACY_CIPHERTEXT );
        System.out.println( LEGACY_CIPHERTEXT + " -> " + new String(cleartextChars) );
    }

    @Test
    public void testEncryptionRoundTrip() throws Exception {
        String cleartext = "round trip password 23423432";
        MasterPasswordManager mpm = new MasterPasswordManager(staticFinder("rt master password grewge"));
        String ciphertext = mpm.encryptPassword(cleartext.toCharArray());
        assertTrue( ciphertext.startsWith( "$L7C2$" ) );
        String dic = Integer.toHexString( L7C2SecretEncryptor.DEFAULT_PBKDF_ITERATION_COUNT );
        assertTrue( "should have used default iteration count", ciphertext.startsWith( "$L7C2$" + dic + "," ) );
        mpm = new MasterPasswordManager(staticFinder("rt master password grewge"));
        String decrypted = new String(mpm.decryptPassword(ciphertext));
        assertEquals(cleartext, decrypted);
    }

    @Test
    public void testEncryptionRoundTrip_highEntropyMode() throws Exception {
        byte[] keyBytes = HexUtils.decodeBase64( "knRIb7vS1O+vRzy5qn3xlv9wDh9Eb87K4+Lu/OuYLas=" );

        String cleartext = "round trip password 23524622";
        MasterPasswordManager mpm = new MasterPasswordManager( keyBytes, true );
        String ciphertext = mpm.encryptPassword(cleartext.toCharArray());
        assertTrue( ciphertext.startsWith( "$L7C2$" ) );
        assertTrue( "should have iteration count of 1 in high-entropy mode", ciphertext.startsWith( "$L7C2$1," ) );
        show( mpm, "HE_mode" );

        mpm = new MasterPasswordManager( keyBytes ); // Doesn't matter what mode the receiving MPM is in
        String decrypted = new String(mpm.decryptPassword(ciphertext));
        assertEquals(cleartext, decrypted);
    }

    @Test
    public void testLegacyEncryptionRoundTrip() throws Exception {
        legacyMode();
        String cleartext = "round trip password 23423432";
        MasterPasswordManager mpm = new MasterPasswordManager(staticFinder("rt master password grewge"));
        String ciphertext = mpm.encryptPassword(cleartext.toCharArray());
        assertTrue( ciphertext.startsWith( "$L7C$" ) );
        mpm = new MasterPasswordManager(staticFinder("rt master password grewge"));
        String decrypted = new String(mpm.decryptPassword(ciphertext));
        assertEquals(cleartext, decrypted);
    }

    @Test
    public void testMasterKeyMissing() throws Exception {
        MasterPasswordManager mpm = new MasterPasswordManager(new SecretEncryptorKeyFinder() {
            @Override
            public byte[] findMasterPasswordBytes() {
                throw new IllegalStateException("UNIT TEST EXCEPTION: No master key available");
            }
        });

        try {
            mpm.encryptPassword( "blah".toCharArray() );
            fail( "expected RuntimeException for lack of master key" );
        } catch ( RuntimeException e ) {
            System.out.println( e.getMessage() );
            assertTrue( "Inner RuntimeException should be propagated", e.getMessage().equals( "UNIT TEST EXCEPTION: No master key available" ) );
        }

        mpm = new MasterPasswordManager(new SecretEncryptorKeyFinder() {
            @Override
            public byte[] findMasterPasswordBytes() {
                return null;
            }
        });

        try {
            mpm.encryptPassword( "blah".toCharArray() );
            fail( "expected RuntimeException for lack of master key" );
        } catch ( RuntimeException e ) {
            assertTrue( e.getMessage().contains( "Unable to obtain key for encryption" ) );
        }
    }
    
    private void show(String password, String obf) {
        System.out.println(password + "\t OBF-->\t " + obf);
    }

    private void show(MasterPasswordManager mpm, String plaintextPassword) {
        String enc = mpm.encryptPassword(plaintextPassword.toCharArray());
        System.out.println(plaintextPassword + "\t ENC-->\t " + enc);
        assertTrue( "Encrypted password must not line wrap", !enc.contains( "\015" ) );
        assertTrue( "Encrypted password must not line wrap", !enc.contains( "\012" ) );
    }

    @Test
    public void testGenerateTestPasswords() throws Exception {
        String master = "7layer";
        String masterObf = ObfuscatedFileMasterPasswordFinder.obfuscate(master.getBytes(Charsets.UTF8));
        show(master, masterObf);
        System.out.println();

        MasterPasswordManager mpm = new MasterPasswordManager(staticFinder(master));
        show(mpm, "tralala");
        show(mpm, "7layer");
        show(mpm, "");
        show(mpm, "password");
        show( mpm, "A very long password indeed!!!  asdkjfhasdjfhasdlkfjhasdfkjhadskfjhadsfkljahdsflkjhdslkjsadhljkaf" );
    }

    @Test
    public void testLegacyGenerateTestPasswords() throws Exception {
        legacyMode();
        String master = "7layer";
        String masterObf = ObfuscatedFileMasterPasswordFinder.obfuscate(master.getBytes(Charsets.UTF8));
        show(master, masterObf);
        System.out.println();

        MasterPasswordManager mpm = new MasterPasswordManager(staticFinder(master));
        show(mpm, "tralala");
        show(mpm, "7layer");
        show(mpm, "");
        show(mpm, "password");
        show( mpm, "A very long password indeed!!!  asdkjfhasdjfhasdlkfjhasdfkjhadskfjhadsfkljahdsflkjhdslkjsadhljkaf" );
    }

    @Test
    public void testLooksLikeEncryptedPassword() {
        MasterPasswordManager mpm = new MasterPasswordManager(new byte[] { 2, 3, 4});
        assertTrue(mpm.looksLikeEncryptedPassword("$L7C$asdf"));
        assertTrue(mpm.looksLikeEncryptedPassword("$L7C$"));
        assertTrue(mpm.looksLikeEncryptedPassword("$L7C$asdfg$asdfg5asdfg"));
        assertTrue(mpm.looksLikeEncryptedPassword("$L7C$asdf askjghaeugh a3957 "));
        assertTrue(mpm.looksLikeEncryptedPassword("$L7C$a sdfaksdjfhalkjrhg arepgiuhaeg"));
        assertTrue(mpm.looksLikeEncryptedPassword("$L7C$$  as4%%$df"));
        assertTrue(mpm.looksLikeEncryptedPassword("$L7C$$$"));

        assertFalse(mpm.looksLikeEncryptedPassword(" $L7C$"));
        assertFalse(mpm.looksLikeEncryptedPassword("$ L7C$"));
        assertFalse(mpm.looksLikeEncryptedPassword("$L7D$"));
        assertFalse(mpm.looksLikeEncryptedPassword("L7D"));
        assertFalse(mpm.looksLikeEncryptedPassword("7layer"));
        assertFalse(mpm.looksLikeEncryptedPassword("password"));
    }

    @Test( expected = ParseException.class )
    public void testDecryptPasswordThatIsntAnEncryptedPassword() throws Exception {
        MasterPasswordManager mpm = new MasterPasswordManager(new byte[] { 2, 3, 4});
        mpm.decryptPassword( "garbage" );
    }

    @Test
    public void testDecryptPasswordIfEncrypted() throws Exception {
        String cleartext = "round trip password 23423432";
        MasterPasswordManager mpm = new MasterPasswordManager(staticFinder("rt master password grewge"));
        String ciphertext = mpm.encryptPassword(cleartext.toCharArray());
        assertTrue( ciphertext.startsWith( "$L7C2$" ) );
        mpm = new MasterPasswordManager(staticFinder("rt master password grewge"));
        String decrypted = new String(mpm.decryptPasswordIfEncrypted(ciphertext));
        assertEquals(cleartext, decrypted);

        String notDecrypted = new String(mpm.decryptPasswordIfEncrypted("x" + ciphertext));
        assertEquals("x" + ciphertext, notDecrypted);
    }

    @Test
    public void testLegacyDecryptPasswordIfEncrypted() throws Exception {
        legacyMode();
        String cleartext = "round trip password 23423432";
        MasterPasswordManager mpm = new MasterPasswordManager(staticFinder("rt master password grewge"));
        String ciphertext = mpm.encryptPassword(cleartext.toCharArray());
        assertTrue( ciphertext.startsWith( "$L7C$" ) );
        mpm = new MasterPasswordManager(staticFinder("rt master password grewge"));
        String decrypted = new String(mpm.decryptPasswordIfEncrypted(ciphertext));
        assertEquals(cleartext, decrypted);

        String notDecrypted = new String(mpm.decryptPasswordIfEncrypted("x" + ciphertext));
        assertEquals("x" + ciphertext, notDecrypted);
    }

    private String destroyCiphertext( String s ) {
        // Perturb an encrypted password by scambling the ciphertext after the salt.
        String source = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        String dest   = "nopqrstuvwxyzabcdefghijklmNOPQRSTUVWXYZABCDEFGHIJKLM6789012345";
        StringBuilder sb = new StringBuilder( s );
        int numdollars = 0;
        for ( int i = 0; i < sb.length(); ++i ) {
            char c = sb.charAt( i );
            if ( '$' == c ) {
                numdollars ++;
            }
            if ( numdollars < 3 )
                continue;

            int idx = source.indexOf( c );
            if ( idx >= 0 ) {
                sb.replace( i, i + 1, dest.substring( idx, idx + 1 ) );
            }
        }
        return sb.toString();
    }

    @Test
    public void testDecryptPasswordIfEncrypted_invalid() throws Exception {
        String cleartext = "round trip password 23423432";
        MasterPasswordManager mpm = new MasterPasswordManager(staticFinder("rt master password grewge"));
        String ciphertext = mpm.encryptPassword(cleartext.toCharArray());
        ciphertext = destroyCiphertext( ciphertext );
        assertTrue( ciphertext.startsWith( "$L7C2$" ) );

        mpm = new MasterPasswordManager(staticFinder("rt master password grewge"));
        String decrypted = new String(mpm.decryptPasswordIfEncrypted(ciphertext));

        assertEquals( "must return original string unmodified if decryption fails", ciphertext, decrypted );

        String notDecrypted = new String(mpm.decryptPasswordIfEncrypted("x" + ciphertext));
        assertEquals("x" + ciphertext, notDecrypted);
    }

    @Test
    public void testLegacyDecryptPasswordIfEncrypted_invalid() throws Exception {
        legacyMode();

        boolean worked = false;
        for ( int tries = 0; tries < 8; ++tries ) {
            String cleartext = "round trip password 23423432";
            MasterPasswordManager mpm = new MasterPasswordManager(staticFinder("rt master password grewge"));
            String ciphertext = mpm.encryptPassword(cleartext.toCharArray());
            ciphertext = destroyCiphertext( ciphertext );
            assertTrue( ciphertext.startsWith( "$L7C$" ) );

            mpm = new MasterPasswordManager(staticFinder("rt master password grewge"));
            String decrypted = new String(mpm.decryptPasswordIfEncrypted(ciphertext));

            String notDecrypted = new String(mpm.decryptPasswordIfEncrypted("x" + ciphertext));
            assertEquals("x" + ciphertext, notDecrypted);

            // With legacy L7C format, there is a small chance that the decryption will be lucky and succeed
            // causing the result to be gibberish instead of the original unmodified string
            // If this happens, we will allow the test to retry
            if ( ciphertext.equals( decrypted ) ) {
                // Expected to return the original string unmodified, if decryption fails
                worked = true;
                break;
            }
        }

        assertTrue( "At least one attempt must have failed and retured the original string", worked );

    }

    @Test
    public void testL7C2NotEnoughEncyptedData() throws Exception {
        MasterPasswordManager mpm = new MasterPasswordManager( staticFinder( "7layer" ) );
        try {
            mpm.decryptPassword( CIPHERTEXT_PASSWORD_NOT_ENOUGH_CIPHERTEXT );
            fail( "expected exception not thrown " );
        } catch ( ParseException e ) {
            // This error means there wasn't enough data to even try running the MAC
            assertEquals( "Invalid encrypted password -- not enough encrypted data", e.getMessage() );
        }

        try {
            mpm.decryptPassword( CIPHERTEXT_PASSWORD_NO_CIPHERTEXT );
            fail( "expected exception not thrown " );
        } catch ( ParseException e ) {
            // This error means there wasn't enough data to even try running the MAC
            assertEquals( "Invalid encrypted password -- not enough encrypted data", e.getMessage() );
        }
    }

    @Test
    public void testL7C2MacProtection() throws Exception {
        MasterPasswordManager mpm = new MasterPasswordManager( staticFinder( "7layer" ) );
        for ( String ciphertext : TEST_VECTORS_INVALID_L7C2_BAD_MAC_VALUE ) {
            try {
                mpm.decryptPassword( ciphertext );
                fail( "expected exception not thrown -- decryption succeeded for " + ciphertext );
            } catch ( ParseException e ) {
                // No matter what is wrong with the encyrpted password, the only error we should ever get is "bad mac value"
                assertEquals( "Unable to decrypt password: bad mac value", e.getMessage() );
            }
        }
    }

    @Test
    public void testCreateMasterPasswordManager_legacy_withkdf() throws Exception {
        MasterPasswordManager mpm = MasterPasswordManager.createMasterPasswordManager( "7layer".getBytes(), false, true );
        assertEquals( "password", new String( mpm.decryptPassword( CIPHERTEXT_PASSWORD ) ) );
        assertEquals( "password", new String( mpm.decryptPassword( CIPHERTEXT_LEGACY_PASSWORD ) ) );
    }

    @Test
    public void testCreateMasterPasswordManager_nolegacy_withkdf() throws Exception {
        MasterPasswordManager mpm = MasterPasswordManager.createMasterPasswordManager( "7layer".getBytes(), false, false );
        assertEquals( "password", new String( mpm.decryptPassword( CIPHERTEXT_PASSWORD ) ) );
        try {
            assertEquals( "password", new String( mpm.decryptPassword( CIPHERTEXT_LEGACY_PASSWORD ) ) );
            fail( "expected ParseException when legacy acceptance disabled" );
        } catch ( ParseException e ) {
            // Ok
        }
    }

    @Test( expected = IllegalArgumentException.class )
    public void testCreateMasterPasswordManager_nokdf_insufficientKeyMaterial() throws Exception {
        MasterPasswordManager.createMasterPasswordManager( "7layer".getBytes(), true, false );
    }

    @Test
    public void testCreateMasterPasswordManager_legacy_nokdf() throws Exception {
        byte[] keyBytes = HexUtils.decodeBase64( "knRIb7vS1O+vRzy5qn3xlv9wDh9Eb87K4+Lu/OuYLas=" );

        MasterPasswordManager mpm = MasterPasswordManager.createMasterPasswordManager( keyBytes, true, false );
        assertEquals( "HE_mode", new String( mpm.decryptPassword( "$L7C2$1,Lm3DjCPoNgFAnMOpLraW6YM7+WlmDEIuM5H30q5HtnU=$NLbAbsh0j+SYm573NjKAk2kEOTfTpS74B3OVfHrcGhEXgsNuBROyS1y7awsAWWli+6e1P4RLSNssrSOoRGZIlA==" ) ) );
        assertTrue( "New password should have been encrypted with iteration count of 1", mpm.encryptPassword( "blah".toCharArray() ).startsWith( "$L7C2$1," ) );
    }

    @Test
    public void testPerformaceOfDefaultIterationCount() throws Exception {
        // See if it is time to increase the default iteration count
        final MasterPasswordManager mpm = new MasterPasswordManager( staticFinder( "7layer" ) );

        // Do a burn-in run
        final String got = mpm.encryptPassword( "blah blah blah".toCharArray() );
        assertArrayEquals( "blah blah blah".toCharArray(), mpm.decryptPassword( got ) );

        long before = System.nanoTime();
        assertArrayEquals( "blah blah blah".toCharArray(), mpm.decryptPassword( got ) );
        long after = System.nanoTime();
        long nanos = after - before;
        double seconds = nanos / 1000000000d;
        double minimum = 1d / 10d; // If time to decrypt goes below a tenth of a second on a general purpose CPU, we will raise an error
        System.out.println( "seconds to do one decrypt with current default iteration count: " + seconds );

        if ( seconds < minimum ) {
            // Figure out what would be a better value
            System.out.println( "Current default iteration count appears to be too low -- taking more detailed measurement" );
            long totalNanos = new BenchmarkRunner( new Runnable() {
                @Override
                public void run() {
                    try {
                        assertArrayEquals( "blah blah blah".toCharArray(), mpm.decryptPassword( got ) );
                    } catch ( Exception e ) {
                        throw new RuntimeException( e );
                    }
                }
            }, 200, 1, "PBKDF2 iteration count measurement" ).run();
            double nanosPerDecryption = totalNanos / 200d;
            double secondsPerDecryption = nanosPerDecryption / 1000000000d;
            if ( secondsPerDecryption < minimum ) {
                int defaultCount = L7C2SecretEncryptor.DEFAULT_PBKDF_ITERATION_COUNT;
                double secondsPerIteration = secondsPerDecryption / defaultCount;
                double targetIterations = ( 1d / secondsPerIteration ) * 0.4;
                if ( targetIterations > Integer.MAX_VALUE ) {
                    System.out.println( "Whoa, CPUs have gotten fast!  Or the hash function used by our KDF is now hardware accelerated!  Or I suck at math!");
                    targetIterations = Integer.MAX_VALUE;
                }
                int target = (int) targetIterations;
                String hex = Integer.toHexString( target );
                fail( "Default L7C2 PBKDF2 iteration count of 0x" + Integer.toHexString( defaultCount ) + " (" + defaultCount + ") is too low for current CPU.\n" +
                        "Must take at least 1/10th a sec per decryption on a general purpose CPU in order to have any hope of slowing down a GPU farm\n" +
                        "(or ASIC aray!), but each decryption is now taking only " + secondsPerDecryption + " sec. " +
                      "Please increase\nL7C2SecretEncryptor.DEFAULT_PBKDF_ITERATION_COUNT; a possible better value for this CPU might be 0x" + hex + " (" + target + ")");
            } else {
                System.out.println( "Never mind, looks good after all (" + secondsPerDecryption + " sec per decryption on this CPU)");
            }
        }
    }
}

package com.l7tech.server.security.signer;

import com.l7tech.gateway.common.module.ModuleDigest;
import com.l7tech.gateway.common.security.signer.SignedZipVisitor;
import com.l7tech.gateway.common.security.signer.SignerUtils;
import com.l7tech.util.*;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.*;

import java.io.*;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Testing modules signature verifier as well as signer.
 */
public class SignatureVerifierTest extends SignatureTestUtils {

    protected static final Random rnd = new Random();

    // we are going to treat this verifier (and holder of the signer certs) as trustworthy
    private static SignatureVerifier TRUSTED_VERIFIER;
    private static final String[] TRUSTED_SIGNER_DNS = {
            "cn=signer.team1.apim.ca.com",
            "cn=signer.team2.apim.ca.com",
            "cn=signer.team3.apim.ca.com",
            "cn=signer.team4.apim.ca.com"
    };

    // we are going to treat this verifier (and holder of the signer certs) as untrustworthy
    private static SignatureVerifier UNTRUSTED_VERIFIER;
    // the first 4 are the same DN's as trusted ones
    private static final String[] UNTRUSTED_SIGNER_DNS =
            ArrayUtils.concat(
                    TRUSTED_SIGNER_DNS,
                    new String[]{
                            "cn=untrusted.signer1.apim.ca.com",
                            "cn=untrusted.signer2.apim.ca.com"
                    }
            );

    @BeforeClass
    public static void setUpOnce() throws Exception {
        SignatureTestUtils.beforeClass();

        // create two, trusted and untrusted, signature verifiers and signer cert holders
        TRUSTED_VERIFIER = createSignatureVerifier(TRUSTED_SIGNER_DNS);
        UNTRUSTED_VERIFIER = createSignatureVerifier(UNTRUSTED_SIGNER_DNS);
    }

    @AfterClass
    public static void cleanUpOnce() throws Exception {
        SignatureTestUtils.afterClass();
    }

    @Before
    public void setUp() throws Exception {
        Assert.assertThat("UNTRUSTED_VERIFIER is created", TRUSTED_VERIFIER, Matchers.notNullValue());
        Assert.assertThat("UNTRUSTED_VERIFIER is created", UNTRUSTED_VERIFIER, Matchers.notNullValue());
    }

    /**
     * Parse specified signed zip file ({@code signedZip}), extract data and signature bytes and pass them to  the
     * specified {@code modifyCallback} for modification.
     * Returned {@code Pair} of data and signature bytes are then written into the same zip.
     */
    private static void doTestModifyZipAndVerify(
            final InputStream signedZip,
            final Functions.BinaryThrows<Pair<byte[], byte[]>, byte[], byte[], Exception> modifyCallback
    ) throws Exception {
        Assert.assertNotNull(signedZip);
        Assert.assertNotNull(modifyCallback);

        // signed data and signature properties bytes
        // process input sip file
        final Pair<
                byte[], // data bytes
                byte[]  // signature properties bytes
        > signedDataAndSignatureBytes = SignerUtils.walkSignedZip(
                signedZip,
                new SignedZipVisitor<byte[], byte[]>() {
                    @Override
                    public byte[] visitData(@NotNull final ZipInputStream zis) throws IOException {
                        final byte[] dataBytes = IOUtils.slurpStream(zis);
                        Assert.assertNotNull(dataBytes);
                        Assert.assertThat(dataBytes.length, Matchers.greaterThan(0));
                        return dataBytes;
                    }

                    @Override
                    public byte[] visitSignature(@NotNull final ZipInputStream zis) throws IOException {
                        final byte[] signatureBytes = IOUtils.slurpStream(zis);
                        Assert.assertNotNull(signatureBytes);
                        Assert.assertThat(signatureBytes.length, Matchers.greaterThan(0));
                        return signatureBytes;
                    }
                },
                true
        );

        // get signed data and signature properties bytes
        final byte[] dataBytes = signedDataAndSignatureBytes.left;
        final byte[] signatureBytes = signedDataAndSignatureBytes.right;
        // make sure both are read correctly
        if (dataBytes == null || signatureBytes == null) {
            throw new IOException("Invalid signed Zip file. Either 'Signed Data' or 'Signature Properties' or both are missing from signed Zip");
        }

        // do the modification
        final Pair<byte[], byte[]> dataAndSignatureBytes = modifyCallback.call(dataBytes, signatureBytes);
        Assert.assertNotNull(dataAndSignatureBytes);
        Assert.assertNotNull(dataAndSignatureBytes.left);
        Assert.assertThat(dataAndSignatureBytes.left.length, Matchers.greaterThan(0));
        Assert.assertNotNull(dataAndSignatureBytes.right);
        Assert.assertThat(dataAndSignatureBytes.right.length, Matchers.greaterThan(0));

        // now repack the zip with modified bytes
        final ByteArrayOutputStream outputZip = new ByteArrayOutputStream(1024);
        try (final ZipOutputStream zos = new ZipOutputStream(outputZip)) {
            // first zip entry should be the signed data bytes
            zos.putNextEntry(new ZipEntry(SIGNED_DATA_ZIP_ENTRY));
            // write the modified bytes into the first zip entry
            IOUtils.copyStream(new ByteArrayInputStream(dataAndSignatureBytes.left), zos);

            // next zip entry is the signature information
            zos.putNextEntry(new ZipEntry(SIGNATURE_PROPS_ZIP_ENTRY));
            // write the modified bytes into the first zip entry
            IOUtils.copyStream(new ByteArrayInputStream(dataAndSignatureBytes.right), zos);
        }

        // get the modified zip bytes
        final byte[] modifiedSignedZipBytes = outputZip.toByteArray();

        // finally verify the tampered zip
        try {
            TRUSTED_VERIFIER.verify(new ByteArrayInputStream(modifiedSignedZipBytes));
            Assert.fail("verify should have failed with tampered file!!!");
        } catch (SignatureException ignore) {
            // this is expected
        }
    }

    /**
     * As the name says it'll flip a random byte of the specified byte array.
     * @return the same (as not a copy of) but modified byte array.
     */
    private static byte[] flipRandomByte(final byte[] bytes) {
        Assert.assertNotNull(bytes);
        Assert.assertThat(bytes.length, Matchers.greaterThan(0));
        // flip random byte
        for (int i = 0; i < 100; ++i) {  // 100 attempts should be more then enough
            final int byteToFlip = rnd.nextInt(bytes.length - 1);
            if (bytes[byteToFlip] != (byte) (~(bytes[byteToFlip]) & 0xff)) {
                bytes[byteToFlip] = (byte) (~(bytes[byteToFlip]) & 0xff);
                break;
            }
        }
        return bytes;
    }

    /**
     * Utility function for extracting specified property value.
     */
    private static String readB64Property(final String signatureProps, final String key) throws Exception {
        Assert.assertThat(signatureProps, Matchers.not(Matchers.isEmptyOrNullString()));
        Assert.assertThat(key, Matchers.not(Matchers.isEmptyOrNullString()));
        Assert.assertThat(key, Matchers.anyOf(Matchers.is("cert"), Matchers.is("signature")));
        // load props
        final Properties sigProps = new Properties();
        try (final StringReader reader = new StringReader(signatureProps)) {
            sigProps.load(reader);
        }
        // extract property value
        final String propertyB64 = (String)sigProps.get(key);
        Assert.assertThat(propertyB64, Matchers.not(Matchers.isEmptyOrNullString()));
        return propertyB64;
    }

    /**
     * Utility function for flipping specified property value.
     */
    private static String readAndFlipProperty(final Properties sigProps, final String key) throws Exception {
        Assert.assertThat(sigProps, Matchers.notNullValue());
        Assert.assertThat(key, Matchers.not(Matchers.isEmptyOrNullString()));
        Assert.assertThat(key, Matchers.anyOf(Matchers.is("cert"), Matchers.is("signature")));
        // extract property value
        final String propertyB64 = (String)sigProps.get(key);
        Assert.assertThat(propertyB64, Matchers.not(Matchers.isEmptyOrNullString()));
        // flip random byte
        final byte[] modPropBytes = flipRandomByte(HexUtils.decodeBase64(propertyB64));
        // store modified signature
        sigProps.setProperty(key, HexUtils.encodeBase64(modPropBytes));
        Assert.assertThat(propertyB64, Matchers.not(Matchers.equalTo((String)sigProps.get(key))));
        // save the props
        try (final StringWriter writer = new StringWriter()) {
            sigProps.store(writer, "Signature");
            writer.flush();
            return writer.toString();
        }
    }

    /**
     * As the name says it'll flip a random byte for the specified signature or signing cert.
     * @return a {@code String} holding the flipped value of the signature or signing cert.
     */
    private static String flipRandomSignatureOrSignerCertByte(final Either<String, String> signatureOrSignCert) throws Exception {
        Assert.assertNotNull(signatureOrSignCert);
        Assert.assertTrue(signatureOrSignCert.isLeft() || signatureOrSignCert.isRight());
        final Properties sigProps = new Properties();
        if (signatureOrSignCert.isLeft()) {
            // signature
            try (final StringReader reader = new StringReader(signatureOrSignCert.left())) {
                sigProps.load(reader);
            }
            return readAndFlipProperty(sigProps, "signature");
        } else {
            // signing cert
            try (final StringReader reader = new StringReader(signatureOrSignCert.right())) {
                sigProps.load(reader);
            }
            return readAndFlipProperty(sigProps, "cert");
        }
    }

    /**
     * Test for:
     * <ol>
     *     <li>tamper with bytes after signing (flipping random byte)</li>
     *     <li>tamper with signature after signing (flipping random byte)</li>
     *     <li>tamper with signer cert after signing (flipping random byte)</li>
     * </ol>
     */
    private static void doTamperWithZip(final byte[] bytes) throws Exception {
        // tamper with bytes after signing
        doTestModifyZipAndVerify(
                new ByteArrayInputStream(bytes),
                new Functions.BinaryThrows<Pair<byte[], byte[]>, byte[], byte[], Exception>() {
                    @Override
                    public Pair<byte[], byte[]> call(final byte[] dataBytes, final byte[] sigBytes) throws Exception {
                        Assert.assertNotNull(dataBytes);
                        Assert.assertThat(dataBytes.length, Matchers.greaterThan(0));
                        return Pair.pair(flipRandomByte(dataBytes), sigBytes);
                    }
                }
        );

        // tamper with signature after signing (flipping random byte)
        doTestModifyZipAndVerify(
                new ByteArrayInputStream(bytes),
                new Functions.BinaryThrows<Pair<byte[], byte[]>, byte[], byte[], Exception>() {
                    @Override
                    public Pair<byte[], byte[]> call(final byte[] dataBytes, final byte[] sigBytes) throws Exception {
                        // read the signature props
                        final Properties sigProps = new Properties();
                        sigProps.load(new ByteArrayInputStream(sigBytes));
                        final String signatureB64 = (String)sigProps.get("signature");
                        // flip random byte
                        final byte[] modSignBytes = flipRandomByte(HexUtils.decodeBase64(signatureB64));
                        // store modified signature
                        sigProps.setProperty("signature", HexUtils.encodeBase64(modSignBytes));
                        Assert.assertThat(signatureB64, Matchers.not(Matchers.equalTo((String)sigProps.get("signature"))));

                        // save the props
                        try (final ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                            sigProps.store(bos, "Signature");
                            return Pair.pair(dataBytes, bos.toByteArray());
                        }
                    }
                }
        );

        // tamper with signer cert after signing (flipping random byte)
        doTestModifyZipAndVerify(
                new ByteArrayInputStream(bytes),
                new Functions.BinaryThrows<Pair<byte[], byte[]>, byte[], byte[], Exception>() {
                    @Override
                    public Pair<byte[], byte[]> call(final byte[] dataBytes, final byte[] sigBytes) throws Exception {
                        // read the signature props
                        final Properties sigProps = new Properties();
                        sigProps.load(new ByteArrayInputStream(sigBytes));
                        final String signerCertB64 = (String)sigProps.get("cert");
                        // flip random byte
                        final byte[] modSignerCertBytes = flipRandomByte(HexUtils.decodeBase64(signerCertB64));
                        // store modified signature
                        sigProps.setProperty("cert", HexUtils.encodeBase64(modSignerCertBytes));
                        Assert.assertThat(signerCertB64, Matchers.not(Matchers.equalTo((String)sigProps.get("cert"))));

                        // save the props
                        try (final ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                            sigProps.store(bos, "Signature");
                            return Pair.pair(dataBytes, bos.toByteArray());
                        }
                    }
                }
        );
    }

    @Test
    public void test_verify_zip() throws Exception {
        // these are our sample bytes
        final byte[] bytes = "this is a test file".getBytes(Charsets.UTF8);
        // sign the bytes using TRUSTED_VERIFIER signer certs i.e. all DN's inside TRUSTED_SIGNER_DNS array.
        for (final String dn : TRUSTED_SIGNER_DNS) {
            // do the actual signing
            try (final InputStream signedZip = new ByteArrayInputStream(SignatureTestUtils.sign(TRUSTED_VERIFIER, new ByteArrayInputStream(bytes), dn))) {
                // verify signature
                TRUSTED_VERIFIER.verify(signedZip);
            }
        }

        // sign using untrusted certs
        // sign the bytes using UNTRUSTED_VERIFIER signer certs i.e. all DN's inside UNTRUSTED_SIGNER_DNS array.
        for (final String dn : UNTRUSTED_SIGNER_DNS) {
            // do the actual signing
            try (final InputStream signedZip = new ByteArrayInputStream(SignatureTestUtils.sign(UNTRUSTED_VERIFIER, new ByteArrayInputStream(bytes), dn))) {
                // verify signature
                try {
                    TRUSTED_VERIFIER.verify(signedZip);
                    Assert.fail("verify should have failed with untrusted signer!!!");
                } catch (SignatureException ignore) {
                    // this is expected
                }
            }
        }

        // sign zip with TRUSTED_SIGNER_DNS[0]
        final byte[] trustedSignedZipBytes = SignatureTestUtils.sign(TRUSTED_VERIFIER, new ByteArrayInputStream(bytes), TRUSTED_SIGNER_DNS[0]);
        // verify signature can be verified
        try (final InputStream signedZip = new ByteArrayInputStream(trustedSignedZipBytes)) {
            TRUSTED_VERIFIER.verify(signedZip);
        }
        doTamperWithZip(trustedSignedZipBytes);

        // change untrusted signer cert to trusted
        final byte[] untrustedSignedZipBytes = SignatureTestUtils.sign(UNTRUSTED_VERIFIER, new ByteArrayInputStream(bytes), UNTRUSTED_SIGNER_DNS[3]);
        // verify signature cannot be verified
        try (final InputStream signedZip = new ByteArrayInputStream(untrustedSignedZipBytes)) {
            TRUSTED_VERIFIER.verify(signedZip);
            Assert.fail("verify should have failed with untrusted signer!!!");
        } catch (SignatureException ignore) {
            // this is expected
        }
        doTamperWithZip(untrustedSignedZipBytes);

        // tamper with signer cert after signing (swap untrusted with trusted cert)
        doTestModifyZipAndVerify(
                new ByteArrayInputStream(untrustedSignedZipBytes),
                new Functions.BinaryThrows<Pair<byte[], byte[]>, byte[], byte[], Exception>() {
                    @Override
                    public Pair<byte[], byte[]> call(final byte[] dataBytes, final byte[] sigBytes) throws Exception {
                        // read the signature props
                        final Properties sigProps = new Properties();
                        sigProps.load(new ByteArrayInputStream(sigBytes));
                        final String signerCertB64 = (String)sigProps.get("cert");
                        final byte[] signerCertBytes = HexUtils.decodeBase64(signerCertB64);
                        // extract trusted signer cert

                        final Pair<Void, byte[]> signatureBytes = SignerUtils.walkSignedZip(
                                new ByteArrayInputStream(trustedSignedZipBytes),
                                new SignedZipVisitor<Void, byte[]>() {
                                    @Override
                                    public Void visitData(@NotNull final ZipInputStream zis) throws IOException {
                                        // don't care
                                        return null;
                                    }

                                    @Override
                                    public byte[] visitSignature(@NotNull final ZipInputStream zis) throws IOException {
                                        return IOUtils.slurpStream(zis);
                                    }
                                },
                                true
                        );

                        // get the actual bytes
                        final byte[] trustedSignatureBytes = signatureBytes.right;
                        if (trustedSignatureBytes == null) {
                            throw new IOException("Invalid signed Zip file. 'Signature Properties' missing from signed Zip");
                        }

                        Assert.assertFalse(Arrays.equals(signerCertBytes, trustedSignatureBytes));
                        // store modified signature
                        sigProps.setProperty("cert", HexUtils.encodeBase64(trustedSignatureBytes));
                        Assert.assertThat(signerCertB64, Matchers.not(Matchers.equalTo((String)sigProps.get("cert"))));
                        // save the props
                        try (final ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                            sigProps.store(bos, "Signature");
                            return Pair.pair(dataBytes, bos.toByteArray());
                        }
                    }
                }
        );
    }

    @Test
    public void test_verify() throws Exception {
        // these are our sample bytes
        final byte[] bytes = "this is a test file".getBytes(Charsets.UTF8);
        // sign the bytes using TRUSTED_VERIFIER signer certs i.e. all DN's inside TRUSTED_SIGNER_DNS array.
        for (final String dn : TRUSTED_SIGNER_DNS) {
            // do the actual signing
            final String trustedSigProps = SignatureTestUtils.signAndGetSignature(TRUSTED_VERIFIER, new ByteArrayInputStream(bytes), dn);
            // verify signature using both digest and input stream
            TRUSTED_VERIFIER.verify(ModuleDigest.digest(bytes), trustedSigProps);
            TRUSTED_VERIFIER.verify(new ByteArrayInputStream(bytes), trustedSigProps);
            // randomly flip a data byte
            final byte[] modBytes = flipRandomByte(Arrays.copyOf(bytes, bytes.length));
            // verify signature using both digest and input stream
            try {
                TRUSTED_VERIFIER.verify(ModuleDigest.digest(modBytes), trustedSigProps);
                Assert.fail("verify should have failed with untrusted signer!!!");
            } catch (SignatureException ignore) {
                // this is expected
            }
            try {
                TRUSTED_VERIFIER.verify(new ByteArrayInputStream(modBytes), trustedSigProps);
                Assert.fail("verify should have failed with untrusted signer!!!");
            } catch (SignatureException ignore) {
                // this is expected
            }
            // randomly flip a signature byte
            final String modSignature = flipRandomSignatureOrSignerCertByte(Either.<String, String>left(trustedSigProps));
            // verify signature using both digest and input stream
            try {
                TRUSTED_VERIFIER.verify(ModuleDigest.digest(bytes), modSignature);
                Assert.fail("verify should have failed with untrusted signer!!!");
            } catch (SignatureException ignore) {
                // this is expected
            }
            try {
                TRUSTED_VERIFIER.verify(new ByteArrayInputStream(bytes), modSignature);
                Assert.fail("verify should have failed with untrusted signer!!!");
            } catch (SignatureException ignore) {
                // this is expected
            }
            // randomly flip a signer cert byte
            final String modSignerCert = flipRandomSignatureOrSignerCertByte(Either.<String, String>left(trustedSigProps));
            // verify signature using both digest and input stream
            try {
                TRUSTED_VERIFIER.verify(ModuleDigest.digest(bytes), modSignerCert);
                Assert.fail("verify should have failed with untrusted signer!!!");
            } catch (SignatureException ignore) {
                // this is expected
            }
            try {
                TRUSTED_VERIFIER.verify(new ByteArrayInputStream(bytes), modSignerCert);
                Assert.fail("verify should have failed with untrusted signer!!!");
            } catch (SignatureException ignore) {
                // this is expected
            }
        }

        // sign using untrusted certs
        // sign the bytes using UNTRUSTED_VERIFIER signer certs i.e. all DN's inside UNTRUSTED_SIGNER_DNS array.
        for (final String dn : UNTRUSTED_SIGNER_DNS) {
            // do the actual signing
            final String untrustedSigProps = SignatureTestUtils.signAndGetSignature(UNTRUSTED_VERIFIER, new ByteArrayInputStream(bytes), dn);
            // verify signature using both digest and input stream
            try {
                TRUSTED_VERIFIER.verify(ModuleDigest.digest(bytes), untrustedSigProps);
                Assert.fail("verify should have failed with untrusted signer!!!");
            } catch (SignatureException ignore) {
                // this is expected
            }
            try {
                TRUSTED_VERIFIER.verify(new ByteArrayInputStream(bytes), untrustedSigProps);
                Assert.fail("verify should have failed with untrusted signer!!!");
            } catch (SignatureException ignore) {
                // this is expected
            }
            // randomly flip a byte
            final byte[] modBytes = flipRandomByte(Arrays.copyOf(bytes, bytes.length));
            // verify signature using both digest and input stream
            try {
                TRUSTED_VERIFIER.verify(ModuleDigest.digest(modBytes), untrustedSigProps);
                Assert.fail("verify should have failed with untrusted signer!!!");
            } catch (SignatureException ignore) {
                // this is expected
            }
            try {
                TRUSTED_VERIFIER.verify(new ByteArrayInputStream(modBytes), untrustedSigProps);
                Assert.fail("verify should have failed with untrusted signer!!!");
            } catch (SignatureException ignore) {
                // this is expected
            }
            // randomly flip a signature byte
            final String modSignature = flipRandomSignatureOrSignerCertByte(Either.<String, String>left(untrustedSigProps));
            // verify signature using both digest and input stream
            try {
                TRUSTED_VERIFIER.verify(ModuleDigest.digest(bytes), modSignature);
                Assert.fail("verify should have failed with untrusted signer!!!");
            } catch (SignatureException ignore) {
                // this is expected
            }
            try {
                TRUSTED_VERIFIER.verify(new ByteArrayInputStream(bytes), modSignature);
                Assert.fail("verify should have failed with untrusted signer!!!");
            } catch (SignatureException ignore) {
                // this is expected
            }
            // randomly flip a signer cert byte
            final String modSignerCert = flipRandomSignatureOrSignerCertByte(Either.<String, String>left(untrustedSigProps));
            // verify signature using both digest and input stream
            try {
                TRUSTED_VERIFIER.verify(ModuleDigest.digest(bytes), modSignerCert);
                Assert.fail("verify should have failed with untrusted signer!!!");
            } catch (SignatureException ignore) {
                // this is expected
            }
            try {
                TRUSTED_VERIFIER.verify(new ByteArrayInputStream(bytes), modSignerCert);
                Assert.fail("verify should have failed with untrusted signer!!!");
            } catch (SignatureException ignore) {
                // this is expected
            }
        }

        // tamper with signer cert after signing (swap untrusted with trusted cert)
        final String trustedSigProps = SignatureTestUtils.signAndGetSignature(TRUSTED_VERIFIER, new ByteArrayInputStream(bytes), TRUSTED_SIGNER_DNS[1]);
        final String untrustedSigProps = SignatureTestUtils.signAndGetSignature(UNTRUSTED_VERIFIER, new ByteArrayInputStream(bytes), UNTRUSTED_SIGNER_DNS[3]);
        // verify signature will fail for both
        try {
            TRUSTED_VERIFIER.verify(ModuleDigest.digest(bytes), untrustedSigProps);
            Assert.fail("verify should have failed with untrusted signer!!!");
        } catch (SignatureException ignore) {
            // this is expected
        }
        try {
            TRUSTED_VERIFIER.verify(new ByteArrayInputStream(bytes), untrustedSigProps);
            Assert.fail("verify should have failed with untrusted signer!!!");
        } catch (SignatureException ignore) {
            // this is expected
        }
        // now swap certs
        // load untrusted signature properties
        final Properties sigProps = new Properties();
        try (final StringReader reader = new StringReader(untrustedSigProps)) {
            sigProps.load(reader);
        }
        final String untrustedSignerB64 = (String)sigProps.get("cert");
        // store trusted signer cert
        final String trustedSignerB64 = readB64Property(trustedSigProps, "cert");
        Assert.assertThat(untrustedSignerB64, Matchers.not(Matchers.equalTo(trustedSignerB64)));
        sigProps.setProperty("cert", trustedSignerB64);
        Assert.assertThat(untrustedSignerB64, Matchers.not(Matchers.equalTo((String)sigProps.get("cert"))));
        // save the props
        try (final StringWriter writer = new StringWriter()) {
            sigProps.store(writer, "Signature");
            writer.flush();
            final String modUntrustedSigProps = writer.toString();
            Assert.assertThat(modUntrustedSigProps, Matchers.not(Matchers.equalTo(untrustedSigProps)));
            // verify signature will fail for both
            try {
                TRUSTED_VERIFIER.verify(ModuleDigest.digest(bytes), modUntrustedSigProps);
                Assert.fail("verify should have failed with untrusted signer!!!");
            } catch (SignatureException ignore) {
                // this is expected
            }
            try {
                TRUSTED_VERIFIER.verify(new ByteArrayInputStream(bytes), modUntrustedSigProps);
                Assert.fail("verify should have failed with untrusted signer!!!");
            } catch (SignatureException ignore) {
                // this is expected
            }
        }
    }

    /**
     * Use this test to generate self-signed code-signing certs for our dev teams.
     * Trust store holding all public keys is also generated.
     * Root CA keystore, holding the root CA key-pair is also generated.
     */
    @Ignore
    @Test
    public void testGenerateTrustStoreAndTeamCerts() throws Exception {
        final Triple<File, File, Map<String, File>> trustedStoreTriple =
                generateTrustedKeyStore(
                        "CN=signer.mag.apim.ca.com",
                        "CN=signer.matterhorn.apim.ca.com",
                        "CN=signer.portal.apim.ca.com",
                        "CN=signer.gateway.apim.ca.com"
                );

        System.out.println("trusted store: " + trustedStoreTriple.left.getCanonicalPath());
        System.out.println("Root CA store: " + trustedStoreTriple.middle.getCanonicalPath());
        System.out.println("Printing team key-stores:");
        for (final Map.Entry<String, File> entry : trustedStoreTriple.right.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue().getCanonicalPath());
        }
    }
}

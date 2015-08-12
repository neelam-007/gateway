package com.l7tech.gateway.common.security.signer;

import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.util.*;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class SignerUtilsTest {

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    private Pair<X509Certificate, PrivateKey> generateSelfSignedKeyPair() throws Exception {
        final TestCertificateGenerator gen = new TestCertificateGenerator();
        return gen.basicConstraintsCa(1).subject("cn=test1.apim.ca.com").keySize(1024).generateWithKey();
    }

    /**
     * Extracts signed.dat and signature.properties bytes from the given signed Zip {@code InputStream}.
     *
     * @param signedZipStream    signed Zip {@code InputStream}.
     * @return a {@code Pair} of signed.dat and signature.properties bytes, respectively.
     */
    private Pair<byte[], byte[]> extractSignedZipDataAndSignature(final InputStream signedZipStream) throws Exception {
        Assert.assertNotNull("Signed Zip InputStream is null", signedZipStream);

        // verify signed zip file
        final Pair<
                byte[],  // signed.dat bytes
                byte[]   // signature.properties bytes
                > dataAndSignatureBytes = SignerUtils.walkSignedZip(
                signedZipStream,
                new SignedZipVisitor<byte[], byte[]>() {
                    @Override
                    public byte[] visitData(@NotNull final ZipInputStream zis) throws IOException {
                        return IOUtils.slurpStream(zis);
                    }

                    @Override
                    public byte[] visitSignature(@NotNull final ZipInputStream zis) throws IOException {
                        return IOUtils.slurpStream(zis);
                    }
                },
                true
        );

        // make sure both signed.dat and signature.properties bytes are read.
        Assert.assertNotNull("signed.dat bytes are not null", dataAndSignatureBytes.left);
        Assert.assertThat("signed.dat bytes are not empty", dataAndSignatureBytes.left.length, Matchers.greaterThan(0));
        Assert.assertNotNull("signature.properties bytes are not null", dataAndSignatureBytes.right);
        Assert.assertThat("signature.properties bytes are not empty", dataAndSignatureBytes.right.length, Matchers.greaterThan(0));

        return dataAndSignatureBytes;
    }

    @Test
    public void testSignAndVerifyZip() throws Exception {
        final byte[] testData = "test data".getBytes(Charsets.UTF8);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final Pair<X509Certificate, PrivateKey> keyPair = generateSelfSignedKeyPair();
        // sign content
        SignerUtils.signZip(
                keyPair.left,
                keyPair.right,
                new ByteArrayInputStream(testData),
                baos
        );

        // verify signed zip file
        final Pair<
                byte[],  // signed.dat bytes
                byte[]   // signature.properties bytes
        > dataAndSignatureBytes = extractSignedZipDataAndSignature(new ByteArrayInputStream(baos.toByteArray()));

        // make sure both signed.dat and signature.properties bytes are read.
        Assert.assertNotNull("dataAndSignatureBytes is not null", dataAndSignatureBytes);
        Assert.assertNotNull("signed.dat bytes are not null", dataAndSignatureBytes.getKey());
        Assert.assertThat("signed.dat bytes are not empty", dataAndSignatureBytes.getKey().length, Matchers.greaterThan(0));
        Assert.assertThat("signed.dat bytes are what expected", dataAndSignatureBytes.getKey(), Matchers.equalTo(testData));
        Assert.assertNotNull("signature.properties bytes are not null", dataAndSignatureBytes.getValue());
        Assert.assertThat("signature.properties bytes are not empty", dataAndSignatureBytes.getValue().length, Matchers.greaterThan(0));
        final Properties sigProps = new Properties();
        sigProps.load(new ByteArrayInputStream(dataAndSignatureBytes.getValue()));
        final String signerCertB64 = (String)sigProps.get("cert");
        Assert.assertThat(HexUtils.decodeBase64(signerCertB64), Matchers.equalTo(keyPair.left.getEncoded()));

        // finally verify content signature

        // first calculate digest
        final DigestInputStream dis = new DigestInputStream(new ByteArrayInputStream(testData), MessageDigest.getInstance("SHA-256"));
        IOUtils.copyStream(dis, new com.l7tech.common.io.NullOutputStream());
        final X509Certificate sawCert = SignerUtils.verifySignatureWithDigest(dis.getMessageDigest().digest(), sigProps);
        Assert.assertNotNull("sawCert cannot be null", sawCert);
        Assert.assertNotNull("sawCert.getEncoded() cannot be null", sawCert.getEncoded());
        Assert.assertThat(sawCert.getEncoded(), Matchers.equalTo(keyPair.left.getEncoded()));
    }

    /**
     * Signs (using a newly generated self-signed key pair) and extracts signed.dat and signature.properties bytes all in one step.
     *
     * @param testData    test data bytes to sign.
     * @return a {@code Pair} of signed.dat and signature.properties bytes, respectively.
     */
    private Pair<byte[], byte[]> signAndExtractDataAndSignatureBytes(final byte[] testData) throws Exception {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final Pair<X509Certificate, PrivateKey> keyPair = generateSelfSignedKeyPair();
        // sign content
        SignerUtils.signZip(
                keyPair.left,
                keyPair.right,
                new ByteArrayInputStream(testData),
                baos
        );

        return extractSignedZipDataAndSignature(new ByteArrayInputStream(baos.toByteArray()));
    }

    @Test
    public void testVerifyZip() throws Exception {
        final byte[] testData = "test date 1".getBytes(Charsets.UTF8);
        // extract
        final Pair<
                byte[],  // signed.dat bytes
                byte[]   // signature.properties bytes
        > dataAndSignatureBytes = signAndExtractDataAndSignatureBytes(testData);
        // verify dataAndSignatureBytes
        Assert.assertNotNull("dataAndSignatureBytes is not null", dataAndSignatureBytes);
        Assert.assertNotNull("signed.dat bytes are not null", dataAndSignatureBytes.getKey());
        Assert.assertThat("signed.dat bytes are not empty", dataAndSignatureBytes.getKey().length, Matchers.greaterThan(0));
        Assert.assertThat("signed.dat bytes are what expected", dataAndSignatureBytes.getKey(), Matchers.equalTo(testData));
        Assert.assertNotNull("signature.properties bytes are not null", dataAndSignatureBytes.getValue());
        Assert.assertThat("signature.properties bytes are not empty", dataAndSignatureBytes.getValue().length, Matchers.greaterThan(0));

        // test with valid signed zip having signed.dat as first entry and signature.properties as second
        doTestVerifyZip(
                new Functions.UnaryVoidThrows<ZipOutputStream, Exception>() {
                    @Override
                    public void call(final ZipOutputStream zos) throws Exception {
                        Assert.assertNotNull(zos);
                        // first zip entry should be the signed data bytes
                        zos.putNextEntry(new ZipEntry(SignerUtils.SIGNED_DATA_ZIP_ENTRY));
                        IOUtils.copyStream(new ByteArrayInputStream(dataAndSignatureBytes.getKey()), zos);
                        // next zip entry is the signature information
                        zos.putNextEntry(new ZipEntry(SignerUtils.SIGNATURE_PROPS_ZIP_ENTRY));
                        IOUtils.copyStream(new ByteArrayInputStream(dataAndSignatureBytes.getValue()), zos);
                    }
                },
                true
        );

        // test with valid signed zip having signature.properties as first entry and signed.dat as second
        doTestVerifyZip(
                new Functions.UnaryVoidThrows<ZipOutputStream, Exception>() {
                    @Override
                    public void call(final ZipOutputStream zos) throws Exception {
                        Assert.assertNotNull(zos);
                        // first zip entry is the signature information
                        zos.putNextEntry(new ZipEntry(SignerUtils.SIGNATURE_PROPS_ZIP_ENTRY));
                        IOUtils.copyStream(new ByteArrayInputStream(dataAndSignatureBytes.getValue()), zos);
                        // next zip entry should be the signed data bytes
                        zos.putNextEntry(new ZipEntry(SignerUtils.SIGNED_DATA_ZIP_ENTRY));
                        IOUtils.copyStream(new ByteArrayInputStream(dataAndSignatureBytes.getKey()), zos);
                    }
                },
                true
        );

        // test with invalid signed zip missing signed.dat
        doTestVerifyZip(
                new Functions.UnaryVoidThrows<ZipOutputStream, Exception>() {
                    @Override
                    public void call(final ZipOutputStream zos) throws Exception {
                        Assert.assertNotNull(zos);
                        // only zip entry is the signature information
                        zos.putNextEntry(new ZipEntry(SignerUtils.SIGNATURE_PROPS_ZIP_ENTRY));
                        IOUtils.copyStream(new ByteArrayInputStream(dataAndSignatureBytes.getValue()), zos);
                    }
                },
                false
        );

        // test with invalid signed zip missing signature.properties
        doTestVerifyZip(
                new Functions.UnaryVoidThrows<ZipOutputStream, Exception>() {
                    @Override
                    public void call(final ZipOutputStream zos) throws Exception {
                        Assert.assertNotNull(zos);
                        // only zip entry is the signed.dat
                        zos.putNextEntry(new ZipEntry(SignerUtils.SIGNED_DATA_ZIP_ENTRY));
                        IOUtils.copyStream(new ByteArrayInputStream(dataAndSignatureBytes.getKey()), zos);
                    }
                },
                false
        );

        // test with invalid signed zip missing both signed.dat and signature.properties
        doTestVerifyZip(
                new Functions.UnaryVoidThrows<ZipOutputStream, Exception>() {
                    @Override
                    public void call(final ZipOutputStream zos) throws Exception {
                        Assert.assertNotNull(zos);
                    }
                },
                false
        );

        // test with invalid signed zip missing both but the zip has other files
        doTestVerifyZip(
                new Functions.UnaryVoidThrows<ZipOutputStream, Exception>() {
                    @Override
                    public void call(final ZipOutputStream zos) throws Exception {
                        Assert.assertNotNull(zos);
                        // add some test file
                        zos.putNextEntry(new ZipEntry("test1.dat"));
                        IOUtils.copyStream(new ByteArrayInputStream("test1.dat data".getBytes(Charsets.UTF8)), zos);
                        // add another test file
                        zos.putNextEntry(new ZipEntry("test2.dat"));
                        IOUtils.copyStream(new ByteArrayInputStream("test2.dat data".getBytes(Charsets.UTF8)), zos);
                    }
                },
                false
        );

        // test with valid signed zip having signed.dat as first entry and signature.properties as second and additional files
        doTestVerifyZip(
                new Functions.UnaryVoidThrows<ZipOutputStream, Exception>() {
                    @Override
                    public void call(final ZipOutputStream zos) throws Exception {
                        Assert.assertNotNull(zos);
                        // first zip entry should be the signed data bytes
                        zos.putNextEntry(new ZipEntry(SignerUtils.SIGNED_DATA_ZIP_ENTRY));
                        IOUtils.copyStream(new ByteArrayInputStream(dataAndSignatureBytes.getKey()), zos);
                        // next zip entry is the signature information
                        zos.putNextEntry(new ZipEntry(SignerUtils.SIGNATURE_PROPS_ZIP_ENTRY));
                        IOUtils.copyStream(new ByteArrayInputStream(dataAndSignatureBytes.getValue()), zos);
                        // add some test file
                        zos.putNextEntry(new ZipEntry("test1.dat"));
                        IOUtils.copyStream(new ByteArrayInputStream("test1.dat data".getBytes(Charsets.UTF8)), zos);
                        // add another test file
                        zos.putNextEntry(new ZipEntry("test2.dat"));
                        IOUtils.copyStream(new ByteArrayInputStream("test2.dat data".getBytes(Charsets.UTF8)), zos);
                    }
                },
                true // should succeed as both required files are first
        );

        // test with valid signed zip having signed.dat as first entry then some additional files and finally the signature.properties as second
        doTestVerifyZip(
                new Functions.UnaryVoidThrows<ZipOutputStream, Exception>() {
                    @Override
                    public void call(final ZipOutputStream zos) throws Exception {
                        Assert.assertNotNull(zos);
                        // first zip entry should be the signed data bytes
                        zos.putNextEntry(new ZipEntry(SignerUtils.SIGNED_DATA_ZIP_ENTRY));
                        IOUtils.copyStream(new ByteArrayInputStream(dataAndSignatureBytes.getKey()), zos);
                        // add some test file
                        zos.putNextEntry(new ZipEntry("test1.dat"));
                        IOUtils.copyStream(new ByteArrayInputStream("test1.dat data".getBytes(Charsets.UTF8)), zos);
                        // add another test file
                        zos.putNextEntry(new ZipEntry("test2.dat"));
                        IOUtils.copyStream(new ByteArrayInputStream("test2.dat data".getBytes(Charsets.UTF8)), zos);
                        // next zip entry is the signature information
                        zos.putNextEntry(new ZipEntry(SignerUtils.SIGNATURE_PROPS_ZIP_ENTRY));
                        IOUtils.copyStream(new ByteArrayInputStream(dataAndSignatureBytes.getValue()), zos);
                    }
                },
                false
        );

        // test with valid signed zip having signed.dat as first entry and signature.properties as second and an empty dir
        doTestVerifyZip(
                new Functions.UnaryVoidThrows<ZipOutputStream, Exception>() {
                    @Override
                    public void call(final ZipOutputStream zos) throws Exception {
                        Assert.assertNotNull(zos);
                        // first zip entry should be the signed data bytes
                        zos.putNextEntry(new ZipEntry(SignerUtils.SIGNED_DATA_ZIP_ENTRY));
                        IOUtils.copyStream(new ByteArrayInputStream(dataAndSignatureBytes.getKey()), zos);
                        // next zip entry is the signature information
                        zos.putNextEntry(new ZipEntry(SignerUtils.SIGNATURE_PROPS_ZIP_ENTRY));
                        IOUtils.copyStream(new ByteArrayInputStream(dataAndSignatureBytes.getValue()), zos);
                        // add some dir
                        zos.putNextEntry(new ZipEntry("folder1/"));
                    }
                },
                true // should succeed as both required files are first
        );

        // test with valid signed zip having signed.dat as first entry and then an empty folder and finally signature.properties
        doTestVerifyZip(
                new Functions.UnaryVoidThrows<ZipOutputStream, Exception>() {
                    @Override
                    public void call(final ZipOutputStream zos) throws Exception {
                        Assert.assertNotNull(zos);
                        // first zip entry should be the signed data bytes
                        zos.putNextEntry(new ZipEntry(SignerUtils.SIGNED_DATA_ZIP_ENTRY));
                        IOUtils.copyStream(new ByteArrayInputStream(dataAndSignatureBytes.getKey()), zos);
                        // add some dir
                        zos.putNextEntry(new ZipEntry("folder1/"));
                        // next zip entry is the signature information
                        zos.putNextEntry(new ZipEntry(SignerUtils.SIGNATURE_PROPS_ZIP_ENTRY));
                        IOUtils.copyStream(new ByteArrayInputStream(dataAndSignatureBytes.getValue()), zos);
                    }
                },
                false
        );

        // test with valid signed zip having signed.dat as first entry and signature.properties as second and additional files inside dir
        doTestVerifyZip(
                new Functions.UnaryVoidThrows<ZipOutputStream, Exception>() {
                    @Override
                    public void call(final ZipOutputStream zos) throws Exception {
                        Assert.assertNotNull(zos);
                        // first zip entry should be the signed data bytes
                        zos.putNextEntry(new ZipEntry(SignerUtils.SIGNED_DATA_ZIP_ENTRY));
                        IOUtils.copyStream(new ByteArrayInputStream(dataAndSignatureBytes.getKey()), zos);
                        // next zip entry is the signature information
                        zos.putNextEntry(new ZipEntry(SignerUtils.SIGNATURE_PROPS_ZIP_ENTRY));
                        IOUtils.copyStream(new ByteArrayInputStream(dataAndSignatureBytes.getValue()), zos);
                        // add some dir
                        zos.putNextEntry(new ZipEntry("folder1/"));
                        // add some test file
                        zos.putNextEntry(new ZipEntry("folder1/test1.dat"));
                        IOUtils.copyStream(new ByteArrayInputStream("test1.dat data".getBytes(Charsets.UTF8)), zos);
                        // add another test file
                        zos.putNextEntry(new ZipEntry("folder1/test2.dat"));
                        IOUtils.copyStream(new ByteArrayInputStream("test2.dat data".getBytes(Charsets.UTF8)), zos);
                    }
                },
                true // should succeed as both required files are first
        );

        // test with valid signed zip having signed.dat as first entry and then additional files inside a dir and finally signature.properties
        doTestVerifyZip(
                new Functions.UnaryVoidThrows<ZipOutputStream, Exception>() {
                    @Override
                    public void call(final ZipOutputStream zos) throws Exception {
                        Assert.assertNotNull(zos);
                        // first zip entry should be the signed data bytes
                        zos.putNextEntry(new ZipEntry(SignerUtils.SIGNED_DATA_ZIP_ENTRY));
                        IOUtils.copyStream(new ByteArrayInputStream(dataAndSignatureBytes.getKey()), zos);
                        // add some dir
                        zos.putNextEntry(new ZipEntry("folder1/"));
                        // add some test file
                        zos.putNextEntry(new ZipEntry("folder1/test1.dat"));
                        IOUtils.copyStream(new ByteArrayInputStream("test1.dat data".getBytes(Charsets.UTF8)), zos);
                        // add another test file
                        zos.putNextEntry(new ZipEntry("folder1/test2.dat"));
                        IOUtils.copyStream(new ByteArrayInputStream("test2.dat data".getBytes(Charsets.UTF8)), zos);
                        // next zip entry is the signature information
                        zos.putNextEntry(new ZipEntry(SignerUtils.SIGNATURE_PROPS_ZIP_ENTRY));
                        IOUtils.copyStream(new ByteArrayInputStream(dataAndSignatureBytes.getValue()), zos);
                    }
                },
                false
        );

        // test with valid signed zip having signed.dat as first entry and signature.properties as second and entry with blank name
        doTestVerifyZip(
                new Functions.UnaryVoidThrows<ZipOutputStream, Exception>() {
                    @Override
                    public void call(final ZipOutputStream zos) throws Exception {
                        Assert.assertNotNull(zos);
                        // first zip entry should be the signed data bytes
                        zos.putNextEntry(new ZipEntry(SignerUtils.SIGNED_DATA_ZIP_ENTRY));
                        IOUtils.copyStream(new ByteArrayInputStream(dataAndSignatureBytes.getKey()), zos);
                        // next zip entry is the signature information
                        zos.putNextEntry(new ZipEntry(SignerUtils.SIGNATURE_PROPS_ZIP_ENTRY));
                        IOUtils.copyStream(new ByteArrayInputStream(dataAndSignatureBytes.getValue()), zos);
                        // add some dir
                        zos.putNextEntry(new ZipEntry(""));
                    }
                },
                true // should succeed as both required files are first
        );

        // test with valid signed zip having signed.dat as first entry and then a entry with blank name and finally signature.properties
        doTestVerifyZip(
                new Functions.UnaryVoidThrows<ZipOutputStream, Exception>() {
                    @Override
                    public void call(final ZipOutputStream zos) throws Exception {
                        Assert.assertNotNull(zos);
                        // first zip entry should be the signed data bytes
                        zos.putNextEntry(new ZipEntry(SignerUtils.SIGNED_DATA_ZIP_ENTRY));
                        IOUtils.copyStream(new ByteArrayInputStream(dataAndSignatureBytes.getKey()), zos);
                        // add some dir
                        zos.putNextEntry(new ZipEntry(""));
                        // next zip entry is the signature information
                        zos.putNextEntry(new ZipEntry(SignerUtils.SIGNATURE_PROPS_ZIP_ENTRY));
                        IOUtils.copyStream(new ByteArrayInputStream(dataAndSignatureBytes.getValue()), zos);
                    }
                },
                false // should succeed as both required files are first
        );

        // test with invalid signed zip having signed.dat as first entry and signature.properties as second in a folder
        doTestVerifyZip(
                new Functions.UnaryVoidThrows<ZipOutputStream, Exception>() {
                    @Override
                    public void call(final ZipOutputStream zos) throws Exception {
                        Assert.assertNotNull(zos);
                        // first zip entry should be the signed data bytes
                        zos.putNextEntry(new ZipEntry("dir1/" + SignerUtils.SIGNED_DATA_ZIP_ENTRY));
                        IOUtils.copyStream(new ByteArrayInputStream(dataAndSignatureBytes.getKey()), zos);
                        // next zip entry is the signature information
                        zos.putNextEntry(new ZipEntry("dir1/" + SignerUtils.SIGNATURE_PROPS_ZIP_ENTRY));
                        IOUtils.copyStream(new ByteArrayInputStream(dataAndSignatureBytes.getValue()), zos);
                    }
                },
                false
        );

        // finally test with a non zip file
        try {
            SignerUtils.verifyZip(new ByteArrayInputStream(testData));
            Assert.fail("verifyZip should have failed for non-valid zip");
        } catch (final IOException ignore) {
            System.out.println(ignore.getMessage());
            // expected IOException
        }
    }

    /**
     * Creates a new zip with content provided by zipContentCallback in that particular order and validates its signature.
     *
     * @param zipContentCallback    a callback for writing into the zip stream
     */
    private void doTestVerifyZip(
            final Functions.UnaryVoidThrows<ZipOutputStream, Exception> zipContentCallback,
            final boolean verifyResult
    ) throws Exception {
        // create a new zip file
        final ByteArrayOutputStream outputZip = new ByteArrayOutputStream();
        try (final ZipOutputStream zos = new ZipOutputStream(outputZip)) {
            zipContentCallback.call(zos);
        }

        // verify newly created zip file input stream signature
        if (verifyResult) {
            SignerUtils.verifyZip(new ByteArrayInputStream(outputZip.toByteArray()));
        } else {
            try {
                SignerUtils.verifyZip(new ByteArrayInputStream(outputZip.toByteArray()));
                Assert.fail("verifyZip should have failed");
            } catch (final IOException ignore) {
                System.out.println(ignore.getMessage());
                // expected IOException
            }
        }
    }
}

package com.l7tech.gateway.common.security.signer;

import com.l7tech.common.io.NonCloseableInputStream;
import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.util.*;
import org.apache.commons.lang.StringUtils;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.Nullable;
import org.junit.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class SignerUtilsTest {
    /**
     * IMPORTANT: Revisit this code if renaming fields {@link com.l7tech.gateway.common.security.signer.SignerUtils#SIGNED_DATA_ZIP_ENTRY}
     * and {@link com.l7tech.gateway.common.security.signer.SignerUtils#SIGNATURE_PROPS_ZIP_ENTRY}.
     */
    public static final String SIGNED_DATA_ZIP_ENTRY = extractProperty(SignerUtils.class, "SIGNED_DATA_ZIP_ENTRY");
    public static final String SIGNATURE_PROPS_ZIP_ENTRY = extractProperty(SignerUtils.class, "SIGNATURE_PROPS_ZIP_ENTRY");

    @Before
    public void setUp() throws Exception {
        xPath = XPathFactory.newInstance().newXPath();
        final HashMap<String, String> map = new HashMap<>();
        map.put("l7", "http://ns.l7tech.com/2010/04/gateway-management");
        xPath.setNamespaceContext(new NamespaceContextImpl(map));
    }

    @After
    public void tearDown() throws Exception {
    }

    private Pair<X509Certificate, PrivateKey> generateSelfSignedKeyPair() throws Exception {
        final TestCertificateGenerator gen = new TestCertificateGenerator();
        return gen.basicConstraintsCa(1).subject("cn=test1.apim.ca.com").keySize(1024).generateWithKey();
    }

    /**
     * Execute private method {@link SignerUtils#readSignedZip(java.io.InputStream, InnerPayloadFactory)}
     * with default {@link com.l7tech.gateway.common.security.signer.SignerUtils.SignedZip.InnerPayload} factory.
     *
     * @param is    signed zip input stream
     * @return {@link com.l7tech.gateway.common.security.signer.SignerUtils.SignedZip.InnerPayload}
     * @throws Exception if an error happens while executing the method or the method throws an exception.
     */
    public static SignerUtils.SignedZip.InnerPayload execReadSignedZip(final InputStream is) throws Exception {
        Assert.assertNotNull(is);
        final Method method = SignerUtils.class.getDeclaredMethod("readSignedZip", InputStream.class, InnerPayloadFactory.class);
        if (method == null) {
            throw new RuntimeException("method readSignedZip either missing from class SignerUtils or its deceleration has been modified");
        }
        method.setAccessible(true);
        final Object ret = method.invoke(null, is, SignerUtils.SignedZip.InnerPayload.FACTORY);
        Assert.assertThat(ret, Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(SignerUtils.SignedZip.InnerPayload.class)));
        return (SignerUtils.SignedZip.InnerPayload)ret;
    }

    public static String extractProperty(final Class aClass, final String propName) {
        Assert.assertNotNull(aClass);
        Assert.assertThat(propName, Matchers.not(Matchers.isEmptyOrNullString()));
        try {
            final Field field = aClass.getDeclaredField(propName);
            if (field == null) {
                throw new RuntimeException("field '" + propName + "' is missing from class '" + aClass + "'");
            }
            field.setAccessible(true);
            final Object ret = field.get(null);
            Assert.assertThat(ret, Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(String.class)));
            return (String)ret;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extracts signed.dat and signature.properties bytes from the given signed Zip {@code InputStream}.
     *
     * @param signedZipStream    signed Zip {@code InputStream}.
     * @return a {@code Pair} of signed.dat and signature.properties bytes, respectively.
     */
    private Pair<byte[], byte[]> extractSignedZipDataAndSignature(final InputStream signedZipStream) throws Exception {
        Assert.assertNotNull("Signed Zip InputStream is null", signedZipStream);

        // read signed zip file
        final SignerUtils.SignedZip.InnerPayload payload = execReadSignedZip(signedZipStream);
        final Pair<
                byte[],  // signed.dat bytes
                byte[]   // signature.properties bytes
                > dataAndSignatureBytes = Pair.pair(payload.getDataBytes(), payload.getSignaturePropertiesBytes());

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
        try (final SignerUtils.SignedZip.InnerPayload payload = execReadSignedZip(new ByteArrayInputStream(baos.toByteArray()))) {
            // make sure both signed.dat and signature.properties bytes are read.
            Assert.assertNotNull("zipContent is not null", payload);
            Assert.assertNotNull("signed.dat bytes are not null", payload.getDataBytes());
            Assert.assertThat("signed.dat bytes are not empty", payload.getDataBytes().length, Matchers.greaterThan(0));
            Assert.assertThat("signed.dat bytes are what expected", payload.getDataBytes(), Matchers.equalTo(testData));
            Assert.assertNotNull("signature.properties bytes are not null", payload.getSignaturePropertiesBytes());
            Assert.assertThat("signature.properties bytes are not empty", payload.getSignaturePropertiesBytes().length, Matchers.greaterThan(0));
            Assert.assertNotNull("signature.properties are not null", payload.getSignatureProperties());
            final String signerCertB64 = (String) payload.getSignatureProperties().get("cert");
            Assert.assertThat(HexUtils.decodeBase64(signerCertB64), Matchers.equalTo(keyPair.left.getEncoded()));

            // finally verify content signature
            Assert.assertThat("signature.properties are not null or empty", payload.getSignaturePropertiesString(), Matchers.not(Matchers.isEmptyOrNullString()));
            final X509Certificate sawCert = SignerUtils.verifySignature(new ByteArrayInputStream(testData), payload.getSignaturePropertiesString());
            Assert.assertNotNull("sawCert cannot be null", sawCert);
            Assert.assertNotNull("sawCert.getEncoded() cannot be null", sawCert.getEncoded());
            Assert.assertThat(sawCert.getEncoded(), Matchers.equalTo(keyPair.left.getEncoded()));
        }
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
                        zos.putNextEntry(new ZipEntry(SIGNED_DATA_ZIP_ENTRY));
                        IOUtils.copyStream(new ByteArrayInputStream(dataAndSignatureBytes.getKey()), zos);
                        // next zip entry is the signature information
                        zos.putNextEntry(new ZipEntry(SIGNATURE_PROPS_ZIP_ENTRY));
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
                        zos.putNextEntry(new ZipEntry(SIGNATURE_PROPS_ZIP_ENTRY));
                        IOUtils.copyStream(new ByteArrayInputStream(dataAndSignatureBytes.getValue()), zos);
                        // next zip entry should be the signed data bytes
                        zos.putNextEntry(new ZipEntry(SIGNED_DATA_ZIP_ENTRY));
                        IOUtils.copyStream(new ByteArrayInputStream(dataAndSignatureBytes.getKey()), zos);
                    }
                },
                false // order does matter after refactoring
        );

        // test with invalid signed zip missing signed.dat
        doTestVerifyZip(
                new Functions.UnaryVoidThrows<ZipOutputStream, Exception>() {
                    @Override
                    public void call(final ZipOutputStream zos) throws Exception {
                        Assert.assertNotNull(zos);
                        // only zip entry is the signature information
                        zos.putNextEntry(new ZipEntry(SIGNATURE_PROPS_ZIP_ENTRY));
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
                        zos.putNextEntry(new ZipEntry(SIGNED_DATA_ZIP_ENTRY));
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
                        zos.putNextEntry(new ZipEntry(SIGNED_DATA_ZIP_ENTRY));
                        IOUtils.copyStream(new ByteArrayInputStream(dataAndSignatureBytes.getKey()), zos);
                        // next zip entry is the signature information
                        zos.putNextEntry(new ZipEntry(SIGNATURE_PROPS_ZIP_ENTRY));
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
                        zos.putNextEntry(new ZipEntry(SIGNED_DATA_ZIP_ENTRY));
                        IOUtils.copyStream(new ByteArrayInputStream(dataAndSignatureBytes.getKey()), zos);
                        // add some test file
                        zos.putNextEntry(new ZipEntry("test1.dat"));
                        IOUtils.copyStream(new ByteArrayInputStream("test1.dat data".getBytes(Charsets.UTF8)), zos);
                        // add another test file
                        zos.putNextEntry(new ZipEntry("test2.dat"));
                        IOUtils.copyStream(new ByteArrayInputStream("test2.dat data".getBytes(Charsets.UTF8)), zos);
                        // next zip entry is the signature information
                        zos.putNextEntry(new ZipEntry(SIGNATURE_PROPS_ZIP_ENTRY));
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
                        zos.putNextEntry(new ZipEntry(SIGNED_DATA_ZIP_ENTRY));
                        IOUtils.copyStream(new ByteArrayInputStream(dataAndSignatureBytes.getKey()), zos);
                        // next zip entry is the signature information
                        zos.putNextEntry(new ZipEntry(SIGNATURE_PROPS_ZIP_ENTRY));
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
                        zos.putNextEntry(new ZipEntry(SIGNED_DATA_ZIP_ENTRY));
                        IOUtils.copyStream(new ByteArrayInputStream(dataAndSignatureBytes.getKey()), zos);
                        // add some dir
                        zos.putNextEntry(new ZipEntry("folder1/"));
                        // next zip entry is the signature information
                        zos.putNextEntry(new ZipEntry(SIGNATURE_PROPS_ZIP_ENTRY));
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
                        zos.putNextEntry(new ZipEntry(SIGNED_DATA_ZIP_ENTRY));
                        IOUtils.copyStream(new ByteArrayInputStream(dataAndSignatureBytes.getKey()), zos);
                        // next zip entry is the signature information
                        zos.putNextEntry(new ZipEntry(SIGNATURE_PROPS_ZIP_ENTRY));
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
                        zos.putNextEntry(new ZipEntry(SIGNED_DATA_ZIP_ENTRY));
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
                        zos.putNextEntry(new ZipEntry(SIGNATURE_PROPS_ZIP_ENTRY));
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
                        zos.putNextEntry(new ZipEntry(SIGNED_DATA_ZIP_ENTRY));
                        IOUtils.copyStream(new ByteArrayInputStream(dataAndSignatureBytes.getKey()), zos);
                        // next zip entry is the signature information
                        zos.putNextEntry(new ZipEntry(SIGNATURE_PROPS_ZIP_ENTRY));
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
                        zos.putNextEntry(new ZipEntry(SIGNED_DATA_ZIP_ENTRY));
                        IOUtils.copyStream(new ByteArrayInputStream(dataAndSignatureBytes.getKey()), zos);
                        // add some dir
                        zos.putNextEntry(new ZipEntry(""));
                        // next zip entry is the signature information
                        zos.putNextEntry(new ZipEntry(SIGNATURE_PROPS_ZIP_ENTRY));
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
                        zos.putNextEntry(new ZipEntry("dir1/" + SIGNED_DATA_ZIP_ENTRY));
                        IOUtils.copyStream(new ByteArrayInputStream(dataAndSignatureBytes.getKey()), zos);
                        // next zip entry is the signature information
                        zos.putNextEntry(new ZipEntry("dir1/" + SIGNATURE_PROPS_ZIP_ENTRY));
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

    /**
     * Convenient  unit test to sign {@code ServerModuleFile)'s within a SKAR file.<br/>
     * The output skar file will be in the same folder as the skarToSign suffixed with "-signed"<br/>
     * Should work with skar of skars.<br/>
     * Note: any existing output file will be overwritten.<br/>
     * Note: by default the method signs using gatewayKeyStore.p12, change the path before running in your environment.
     * <p/>
     * Run in your IDE environment, add skarToSign files to your choosing and modify the signer keystore file to point to your signer keystore.
     */
    @Ignore
    @Test
    public void testSignServerModuleFilesInsideSkar() throws Exception {
        signServerModuleFilesInsideSkar(new File("\\\\filer2.l7tech.com\\departments\\Dev\\tluong\\SolutionKitManager\\SimpleSolutionKit-1.0-20150716-signed.skar"));
        signServerModuleFilesInsideSkar(new File("\\\\filer2.l7tech.com\\departments\\Dev\\tluong\\SolutionKitManager\\SimpleSolutionKit-1.0-20150716.skar"));
        signServerModuleFilesInsideSkar(new File("\\\\filer2.l7tech.com\\departments\\Dev\\tluong\\SolutionKitManager\\SimpleSolutionKit-1.1-20150803.skar"));
        signServerModuleFilesInsideSkar(new File("\\\\filer2.l7tech.com\\departments\\Dev\\tluong\\SolutionKitManager\\demo sprint 02.1\\SimpleSolutionKit-1.0-20150716.skar"));
        signServerModuleFilesInsideSkar(new File("\\\\filer2.l7tech.com\\departments\\Dev\\tluong\\SolutionKitManager\\demo sprint 02.1\\SimpleSolutionKit-1.1-20150716.skar"));
    }

    private void signServerModuleFilesInsideSkar(final File skarFileToSign) throws Exception {
        Assert.assertNotNull(skarFileToSign);
        Assert.assertTrue(skarFileToSign.exists() && skarFileToSign.isFile());

        final File signedSkarFile = new File(skarFileToSign.getParent(), generateSignedName(skarFileToSign.getName()));
        try (
                final BufferedInputStream bis = new BufferedInputStream(new FileInputStream(skarFileToSign));
                final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(signedSkarFile))
        ) {
            signServerModuleFilesInsideSkar(
                    bis,
                    new File("D:\\work\\Signer\\gatewayKeyStore.p12"),
                    "PKCS12",
                    "7layer",
                    null,
                    "7layer",
                    bos
            );
        }
    }

    private static String generateSignedName(final String fileName) {
        Assert.assertTrue(StringUtils.isNotBlank(fileName));
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            return fileName.substring(0, i) + "-signed" + fileName.substring(i);
        }
        return fileName + "-signed";
    }

    private XPath xPath;
    private static final String SK_INSTALL_BUNDLE_FILENAME = "InstallBundle.xml";
    private void signServerModuleFilesInsideSkar(
            final InputStream skarFileToSignStream,
            final File signerKeyStore,
            final String storeType,
            @Nullable final String storePass,
            @Nullable final String alias,
            @Nullable final String entryPass,
            final OutputStream outputStream
    ) throws Exception {
        Assert.assertNotNull(skarFileToSignStream);
        Assert.assertNotNull(signerKeyStore);
        Assert.assertTrue(signerKeyStore.exists() && signerKeyStore.isFile());
        Assert.assertTrue(StringUtils.isNotBlank(storeType));

        // reed the
        try (
                final ZipInputStream zis = new ZipInputStream(skarFileToSignStream);
                final ZipOutputStream zos = new ZipOutputStream(outputStream)
        ) {
            ZipEntry entry = zis.getNextEntry();
            // loop while there are more entries
            while (entry != null) {
                // check if entry is directory
                try {
                    if (!entry.isDirectory()) {
                        final String fileName = entry.getName();
                        if (SK_INSTALL_BUNDLE_FILENAME.equals(fileName)) {
                            final Document doc = XmlUtil.parse(new NonCloseableInputStream(zis));
                            // get all SERVER_MODULE_FILE nodes
                            final NodeList nodes = (NodeList)xPath.compile("/l7:Bundle/l7:References/l7:Item[l7:Type='SERVER_MODULE_FILE']/l7:Resource/l7:ServerModuleFile").evaluate(doc.getDocumentElement(), XPathConstants.NODESET);
                            for (int i = 0; i < nodes.getLength(); ++i) {
                                final Node node = nodes.item(i);
                                if (node!= null) {
                                    try {
                                        DomUtils.findExactlyOneChildElementByName((Element) node, "http://ns.l7tech.com/2010/04/gateway-management", "Signature");
                                    } catch (final MissingRequiredElementException ignore) {
                                        final Element dataElem = DomUtils.findExactlyOneChildElementByName((Element) node, "http://ns.l7tech.com/2010/04/gateway-management", "ModuleData");
                                        try (
                                                final ByteArrayInputStream byesStream = new ByteArrayInputStream(HexUtils.decodeBase64(DomUtils.getTextValue(dataElem)));
                                                final ByteArrayOutputStream bos = new ByteArrayOutputStream()
                                        ) {
                                            SignerUtils.signWithKeyStore(
                                                    signerKeyStore,
                                                    storeType,
                                                    storePass != null ? storePass.toCharArray() : null,
                                                    alias,
                                                    entryPass != null ? entryPass.toCharArray() : null,
                                                    byesStream,
                                                    bos
                                            );
                                            final Map<String, String> sigPropsMap = gatSignaturePropertiesMap(
                                                    getSignatureProperties(bos.toByteArray()),
                                                    SignerUtils.ALL_SIGNING_PROPERTIES
                                            );
                                            // generate sig xml elem
                                            //final Element sigElem = DomUtils.createAndAppendElement((Element) node, "Signature");
                                            final Element sigElem = createAndInsertAfterElement((Element) node, "Signature", dataElem);
                                            for (final Map.Entry<String, String> propEntry : sigPropsMap.entrySet()) {
                                                final Element propElem = DomUtils.createAndAppendElement(sigElem, "Property");
                                                propElem.setAttribute("key", propEntry.getKey());
                                                final Element propValueElem = DomUtils.createAndAppendElement(propElem, "StringValue");
                                                propValueElem.setTextContent(propEntry.getValue());
                                            }
                                        }
                                    }
                                }
                            }
                            // save the zip entry in the zip output stream
                            zos.putNextEntry(new ZipEntry(fileName));
                            // save the modified doc content here
                            XmlUtil.nodeToFormattedOutputStream(doc, zos);
                        } else if (StringUtils.isNotBlank(fileName) && fileName.toLowerCase().endsWith(".skar")) {
                            // create the sip entry
                            zos.putNextEntry(new ZipEntry(fileName));
                            // Get the input bytes for a child SKAR, and  recursively call the this method.
                            signServerModuleFilesInsideSkar(
                                    new NonCloseableInputStream(zis),
                                    signerKeyStore,
                                    storeType,
                                    storePass,
                                    alias,
                                    entryPass,
                                    new NonCloseableOutputStream(zos)
                            );
                        } else {
                            // anything else simply add to the zip
                            zos.putNextEntry(new ZipEntry(fileName));
                            IOUtils.copyStream(zis, zos);
                        }
                    } else {
                        // anything else simply add to the zip
                        zos.putNextEntry(new ZipEntry(entry.getName()));
                    }
                } finally {
                    // close the entry
                    zis.closeEntry();
                }

                // finally move to the next entry
                entry = zis.getNextEntry();
            }
        }
    }

    private static Element createAndInsertAfterElement(final Element parent, String localName, final Element afterElem) {
        Assert.assertNotNull(parent);
        Assert.assertTrue(StringUtils.isNotBlank(localName));
        Assert.assertNotNull(afterElem);
        final Element element = parent.getOwnerDocument().createElementNS(parent.getNamespaceURI(), localName);
        element.setPrefix(parent.getPrefix());
        Node afterElemSibling = afterElem.getNextSibling();
        while (!(afterElemSibling instanceof Element) && afterElemSibling != null) {
            afterElemSibling = afterElemSibling.getNextSibling();
        }
        if (afterElemSibling == null) {
            parent.appendChild(element);
        } else {
            parent.insertBefore(element, afterElemSibling);
        }
        return element;
    }

    private static Properties getSignatureProperties(final byte[] signedContent) throws Exception {
        Assert.assertNotNull(signedContent);
        try (final InputStream is = new ByteArrayInputStream(signedContent)) {
            // read the signed zip file
            final SignerUtils.SignedZip.InnerPayload payload = execReadSignedZip(is);
            Assert.assertNotNull(payload);
            return payload.getSignatureProperties();
        }
    }

    private static Map<String, String> gatSignaturePropertiesMap(
            final Properties sigProps,
            final String[] keys
    ) throws IOException {
        Assert.assertNotNull(sigProps);
        Assert.assertNotNull(keys);
        Assert.assertThat(keys, Matchers.not(Matchers.emptyArray()));

        final Map<String, String> ret = new TreeMap<>();
        for (final String key : keys) {
            final String value = (String) sigProps.get(key);
            if (value != null) {
                ret.put(key, value);
            }
        }
        return ret.isEmpty() ? null : Collections.unmodifiableMap(ret);
    }
}

package com.l7tech.gateway.common.security.signer;

import com.l7tech.common.io.NonCloseableInputStream;
import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.module.ModuleDigest;
import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.test.util.TestUtils;
import com.l7tech.util.*;
import org.apache.commons.lang.StringUtils;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.Nullable;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class SignerUtilsTest {
    /**
     * IMPORTANT: Revisit this code if renaming fields:
     * <ul>
     *     <li>{@link com.l7tech.gateway.common.security.signer.SignerUtils#SIGNED_DATA_ZIP_ENTRY}</li>
     *     <li>{@link com.l7tech.gateway.common.security.signer.SignerUtils#SIGNATURE_PROPS_ZIP_ENTRY}</li>
     *     <li>{@link com.l7tech.gateway.common.security.signer.SignerUtils#SIGNING_CERT_PROPS}</li>
     *     <li>{@link com.l7tech.gateway.common.security.signer.SignerUtils#SIGNATURE_PROP}</li>
     *     <li>{@link com.l7tech.gateway.common.security.signer.SignerUtils#TRUSTED_CERTS_ALIAS_PREFIX}</li>
     * </ul>
     */
    public static final String SIGNED_DATA_ZIP_ENTRY = TestUtils.getFieldValue(SignerUtils.class, "SIGNED_DATA_ZIP_ENTRY", String.class);
    public static final String SIGNATURE_PROPS_ZIP_ENTRY = TestUtils.getFieldValue(SignerUtils.class, "SIGNATURE_PROPS_ZIP_ENTRY", String.class);
    public static final String SIGNING_CERT_PROPS = TestUtils.getFieldValue(SignerUtils.class, "SIGNING_CERT_PROPS", String.class);
    public static final String SIGNATURE_PROP = TestUtils.getFieldValue(SignerUtils.class, "SIGNATURE_PROP", String.class);
    private static final String TRUSTED_CERTS_ALIAS_PREFIX = TestUtils.getFieldValue(SignerUtils.class, "TRUSTED_CERTS_ALIAS_PREFIX", String.class);

    @Rule
    public TemporaryFolder kryStoreFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        xPath = XPathFactory.newInstance().newXPath();
        final HashMap<String, String> map = new HashMap<>();
        map.put("l7", "http://ns.l7tech.com/2010/04/gateway-management");
        xPath.setNamespaceContext(new NamespaceContextImpl(map));
    }

    @After
    public void tearDown() throws Exception {
        kryStoreFolder.delete();
    }

    private Pair<X509Certificate, PrivateKey> generateSelfSignedKeyPair() throws Exception {
        final TestCertificateGenerator gen = new TestCertificateGenerator();
        return gen.basicConstraintsCa(1).subject("cn=test1.apim.ca.com").keySize(1024).generateWithKey();
    }

    /**
     * Generates a PKCS12 keystore file with the specified signerDns.
     * Creates a new elliptic curve (P-256) key pairs for each signerDns and adds it to the keystore.
     */
    private Pair<List<X509Certificate>, File> generateKeyStore(
            final String ksType,
            final String ksPassword,
            final String aliasPrefix,
            final String ... signerDns
    ) throws Exception {
        // load root cert
        Assert.assertThat(signerDns, Matchers.allOf(Matchers.notNullValue(), Matchers.not(Matchers.<String>emptyArray())));

        final List<X509Certificate> certs = new ArrayList<>();

        // Create new empty KeyStore
        final KeyStore ks = KeyStore.getInstance(ksType);
        Assert.assertNotNull(ks);
        ks.load(null, null);

        for (final String dn : signerDns) {
            // gen self-signed cert
            final TestCertificateGenerator gen = new TestCertificateGenerator();
            final Pair<X509Certificate, PrivateKey> signerKeyPair = gen.basicConstraintsNoCa().subject(dn).curveName("P-256").generateWithKey();
            Assert.assertNotNull(signerKeyPair);
            Assert.assertNotNull(signerKeyPair.left);
            Assert.assertNotNull(signerKeyPair.right);

            final Set<String> usedAliases = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            final Enumeration<String> aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                final String alias = aliases.nextElement();
                usedAliases.add(alias);
            }

            // Generate an alias that isn't already used in the file
            int num = 1;
            String newAlias = aliasPrefix + num;
            while (usedAliases.contains(newAlias)) {
                num++;
                newAlias = aliasPrefix + num;
            }

            // Add the new cert to the trust store and safely overwrite the existing file in-place
            ks.setKeyEntry(newAlias, signerKeyPair.right, null, new X509Certificate[] { signerKeyPair.left} );

            certs.add(signerKeyPair.left);
        }

        final File signerKeyStoreFile = kryStoreFolder.newFile();
        Assert.assertNotNull(signerKeyStoreFile);
        Assert.assertTrue(signerKeyStoreFile.exists() && !signerKeyStoreFile.isDirectory());
        SignerUtils.storeKeyStoreToFile(signerKeyStoreFile, ks, ksPassword.toCharArray());

        return Pair.pair(certs, signerKeyStoreFile);
    }

    /**
     * Generates a PKCS12 TrustStore file (containing the trusted certs from signerDns) as well as private keystore files
     * for each individual signer cert.
     * Uses {@link #generateKeyStore(String, String, String, String...)} to create individual signer certs and private keystore files.
     */
    private Pair<File, List<Pair<X509Certificate, File>>> generateTrustStoreAndSignerKeyStores(
            final String ksType,
            final String ksPassword,
            final String ksAliasPrefix,
            final String tsType,
            final String tsPassword,
            final String ... signerDns
    ) throws Exception {
        // generate signer KetStore's
        final List<Pair<X509Certificate, File>> signerKeyStores = new ArrayList<>();
        for (final String dn : signerDns) {
            final Pair<List<X509Certificate>, File> signerKeyStore = generateKeyStore(ksType, ksPassword, ksAliasPrefix, dn);
            Assert.assertNotNull(signerKeyStore);
            Assert.assertNotNull(signerKeyStore.right);
            Assert.assertTrue(signerKeyStore.right.exists() && !signerKeyStore.right.isDirectory());
            Assert.assertThat(signerKeyStore.left, Matchers.allOf(Matchers.notNullValue(), Matchers.hasSize(1)));
            signerKeyStores.add(Pair.pair(signerKeyStore.left.get(0), signerKeyStore.right));
        }

        // generate TrustStore
        final File trustStoreFile = kryStoreFolder.newFile();
        Assert.assertNotNull(trustStoreFile);
        Assert.assertTrue(trustStoreFile.exists() && !trustStoreFile.isDirectory());
        final KeyStore ks = KeyStore.getInstance(tsType);
        // Create new empty trust store
        ks.load(null, null);
        SignerUtils.storeKeyStoreToFile(trustStoreFile, ks, tsPassword.toCharArray());
        for (final Pair<X509Certificate, File> signerCertAndFile : signerKeyStores) {
            SignerUtils.addTrustedCertificateToTrustStore(trustStoreFile, ksType, tsPassword.toCharArray(), signerCertAndFile.left);
        }

        return Pair.pair(trustStoreFile, signerKeyStores);
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

    /**
     * Extracts signed.dat and signature.properties bytes from the given signed Zip {@code InputStream}.
     *
     * @param signedZipStream    signed Zip {@code InputStream}.
     * @return a {@link SignerUtils.SignedZip.InnerPayload} containing signed.dat and signature.properties bytes.
     */
    private SignerUtils.SignedZip.InnerPayload extractSignedZipDataAndSignature(final InputStream signedZipStream) throws Exception {
        Assert.assertNotNull("Signed Zip InputStream is null", signedZipStream);

        // read signed zip file
        final SignerUtils.SignedZip.InnerPayload payload = execReadSignedZip(signedZipStream);
        Assert.assertNotNull(payload);

        // make sure both signed.dat and signature.properties bytes are read.
        final byte[] signedDatBytes = payload.getDataBytes();
        final byte[] sigPropBytes = payload.getSignaturePropertiesBytes();
        Assert.assertNotNull("signed.dat bytes are not null", signedDatBytes);
        Assert.assertThat("signed.dat bytes are not empty", signedDatBytes.length, Matchers.greaterThan(0));
        Assert.assertNotNull("signature.properties bytes are not null", sigPropBytes);
        Assert.assertThat("signature.properties bytes are not empty", sigPropBytes.length, Matchers.greaterThan(0));

        return payload;
    }

    private void doVerifySignature(final InputStream is, final byte[] rawBytes, final X509Certificate signerCert) throws Exception {
        Assert.assertNotNull(is);
        Assert.assertNotNull(rawBytes);
        Assert.assertThat(rawBytes.length, Matchers.greaterThan(0));
        Assert.assertNotNull(signerCert);

        // verify signed zip file
        try (final SignerUtils.SignedZip.InnerPayload payload = execReadSignedZip(is)) {
            Assert.assertNotNull("zipContent is not null", payload);
            // make sure both signed.dat and signature.properties bytes are read.
            final byte[] signedDataBytes = payload.getDataBytes();
            final byte[] sigPropBytes = payload.getSignaturePropertiesBytes();
            Assert.assertNotNull("signed.dat bytes are not null", signedDataBytes);
            Assert.assertThat("signed.dat bytes are not empty", signedDataBytes.length, Matchers.greaterThan(0));
            Assert.assertThat("signed.dat bytes are what expected", signedDataBytes, Matchers.equalTo(rawBytes));
            Assert.assertNotNull("signature.properties bytes are not null", sigPropBytes);
            Assert.assertThat("signature.properties bytes are not empty", sigPropBytes.length, Matchers.greaterThan(0));
            Assert.assertNotNull("signature.properties are not null", payload.getSignatureProperties());
            final String signerCertB64 = (String) payload.getSignatureProperties().get(SIGNING_CERT_PROPS);
            Assert.assertThat(HexUtils.decodeBase64(signerCertB64), Matchers.equalTo(signerCert.getEncoded()));

            // finally verify content signature
            Assert.assertThat("signature.properties are not null or empty", payload.getSignaturePropertiesString(), Matchers.not(Matchers.isEmptyOrNullString()));
            final X509Certificate sawCert = SignerUtils.verifySignature(new ByteArrayInputStream(rawBytes), payload.getSignaturePropertiesString());
            Assert.assertNotNull("sawCert cannot be null", sawCert);
            Assert.assertNotNull("sawCert.getEncoded() cannot be null", sawCert.getEncoded());
            Assert.assertThat(sawCert.getEncoded(), Matchers.equalTo(signerCert.getEncoded()));
        }
    }

    @Test
    public void testSignZip() throws Exception {
        final byte[] testData = "test data".getBytes(Charsets.UTF8);

        // test with RSA
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TestCertificateGenerator gen = new TestCertificateGenerator();
        Pair<X509Certificate, PrivateKey> keyPair = gen.basicConstraintsCa(1).subject("cn=test1.apim.ca.com").keySize(1024).generateWithKey();
        // sign content
        SignerUtils.signZip(
                keyPair.left,
                keyPair.right,
                new ByteArrayInputStream(testData),
                baos
        );
        doVerifySignature(new ByteArrayInputStream(baos.toByteArray()), testData, keyPair.left);

        // test with EC
        baos = new ByteArrayOutputStream();
        gen = new TestCertificateGenerator();
        keyPair = gen.basicConstraintsCa(1).subject("cn=test1.apim.ca.com").curveName("P-256").generateWithKey();
        // sign content
        SignerUtils.signZip(
                keyPair.left,
                keyPair.right,
                new ByteArrayInputStream(testData),
                baos
        );
        doVerifySignature(new ByteArrayInputStream(baos.toByteArray()), testData, keyPair.left);

        // test with DSA
        baos = new ByteArrayOutputStream();
        gen = new TestCertificateGenerator();
        keyPair = gen.basicConstraintsCa(1).subject("cn=test1.apim.ca.com").dsaKeySize(1024).generateWithKey();
        try {
            SignerUtils.signZip(
                    keyPair.left,
                    keyPair.right,
                    new ByteArrayInputStream(testData),
                    baos
            );
            Assert.fail("DSA not supported; signZip should have failed with KeyStoreException");
        } catch (KeyStoreException ignore) {
            // expected
        }
    }

    private String genDummySigProps(final String sig, final String signer) throws Exception {
        final Properties sigProps = new Properties();
        sigProps.setProperty(SIGNATURE_PROP, sig);
        sigProps.setProperty(SIGNING_CERT_PROPS, signer);
        final StringWriter writer = new StringWriter();
        sigProps.store(writer, "blah");
        writer.flush();
        final String sigPropsString = writer.toString();
        Assert.assertThat(sigPropsString, Matchers.allOf(Matchers.notNullValue(), Matchers.not(Matchers.isEmptyOrNullString())));
        return sigPropsString;
    }

    @Test
    public void testVerifySignature() throws Exception {
        final String ksType = "PKCS12";
        final String ksPass = "7layer";

        // generate keystore with 3 signer certs
        final Pair<List<X509Certificate>, File> certsFilePair = generateKeyStore(ksType, ksPass, "signer", "cn=test1.apim.ca.com", "cn=test2.apim.ca.com", "cn=test3.apim.ca.com");
        Assert.assertNotNull(certsFilePair);
        Assert.assertNotNull(certsFilePair.right);
        Assert.assertTrue(certsFilePair.right.exists() && !certsFilePair.right.isDirectory());
        Assert.assertThat(certsFilePair.left, Matchers.allOf(Matchers.notNullValue(), Matchers.hasSize(3)));

        // sign with "cn=test1.apim.ca.com"
        final ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
        final byte[] testData1 = "test data1".getBytes(Charsets.UTF8);
        SignerUtils.signWithKeyStore(
                certsFilePair.right,
                ksType,
                ksPass.toCharArray(),
                "signer1",
                null,
                new ByteArrayInputStream(testData1),
                baos1
        );
        // sign with "cn=test2.apim.ca.com"
        final ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        final byte[] testData2 = "test data2".getBytes(Charsets.UTF8);
        SignerUtils.signWithKeyStore(
                certsFilePair.right,
                ksType,
                ksPass.toCharArray(),
                "signer2",
                null,
                new ByteArrayInputStream(testData2),
                baos2
        );
        // sign with "cn=test3.apim.ca.com"
        final ByteArrayOutputStream baos3 = new ByteArrayOutputStream();
        final byte[] testData3 = "test data3".getBytes(Charsets.UTF8);
        SignerUtils.signWithKeyStore(
                certsFilePair.right,
                ksType,
                ksPass.toCharArray(),
                "signer3",
                null,
                new ByteArrayInputStream(testData3),
                baos3
        );

        final String sigPropsString1, sigPropsString2, sigPropsString3;
        final Properties sigProps1, sigProps2, sigProps3;
        try (final SignerUtils.SignedZip.InnerPayload payload = execReadSignedZip(new ByteArrayInputStream(baos1.toByteArray()))) {
            sigPropsString1 = payload.getSignaturePropertiesString();
            Assert.assertThat(sigPropsString1, Matchers.allOf(Matchers.notNullValue(), Matchers.not(Matchers.isEmptyOrNullString())));
            sigProps1 = payload.getSignatureProperties();
            Assert.assertNotNull(sigProps1);
            final byte[] rawBytes = payload.getDataBytes();
            Assert.assertNotNull(rawBytes);
            Assert.assertThat(rawBytes, Matchers.equalTo(testData1));
        }
        try (final SignerUtils.SignedZip.InnerPayload payload = execReadSignedZip(new ByteArrayInputStream(baos2.toByteArray()))) {
            sigPropsString2 = payload.getSignaturePropertiesString();
            Assert.assertThat(sigPropsString2, Matchers.allOf(Matchers.notNullValue(), Matchers.not(Matchers.isEmptyOrNullString())));
            sigProps2 = payload.getSignatureProperties();
            Assert.assertNotNull(sigProps2);
            final byte[] rawBytes = payload.getDataBytes();
            Assert.assertNotNull(rawBytes);
            Assert.assertThat(rawBytes, Matchers.equalTo(testData2));
        }
        try (final SignerUtils.SignedZip.InnerPayload payload = execReadSignedZip(new ByteArrayInputStream(baos3.toByteArray()))) {
            sigPropsString3 = payload.getSignaturePropertiesString();
            Assert.assertThat(sigPropsString3, Matchers.allOf(Matchers.notNullValue(), Matchers.not(Matchers.isEmptyOrNullString())));
            sigProps3 = payload.getSignatureProperties();
            Assert.assertNotNull(sigProps3);
            final byte[] rawBytes = payload.getDataBytes();
            Assert.assertNotNull(rawBytes);
            Assert.assertThat(rawBytes, Matchers.equalTo(testData3));
        }

        // test "cn=test1.apim.ca.com"
        X509Certificate sawCert = SignerUtils.verifySignature(new ByteArrayInputStream(testData1), sigPropsString1);
        Assert.assertNotNull("sawCert cannot be null", sawCert);
        Assert.assertNotNull("sawCert.getEncoded() cannot be null", sawCert.getEncoded());
        Assert.assertThat(sawCert.getEncoded(), Matchers.equalTo(certsFilePair.left.get(0).getEncoded()));

        // test "cn=test1.apim.ca.com"
        sawCert = SignerUtils.verifySignature(new ByteArrayInputStream(testData2), sigPropsString2);
        Assert.assertNotNull("sawCert cannot be null", sawCert);
        Assert.assertNotNull("sawCert.getEncoded() cannot be null", sawCert.getEncoded());
        Assert.assertThat(sawCert.getEncoded(), Matchers.equalTo(certsFilePair.left.get(1).getEncoded()));

        // test "cn=test1.apim.ca.com"
        sawCert = SignerUtils.verifySignature(new ByteArrayInputStream(testData3), sigPropsString3);
        Assert.assertNotNull("sawCert cannot be null", sawCert);
        Assert.assertNotNull("sawCert.getEncoded() cannot be null", sawCert.getEncoded());
        Assert.assertThat(sawCert.getEncoded(), Matchers.equalTo(certsFilePair.left.get(2).getEncoded()));

        // test with empty signature props
        try {
            SignerUtils.verifySignature(new ByteArrayInputStream(testData1), null);
            Assert.fail("verifySignature should have failed");
        } catch (SignatureException ignore) {
            // expected
        }
        try {
            SignerUtils.verifySignature(new ByteArrayInputStream(testData1), "");
            Assert.fail("verifySignature should have failed");
        } catch (SignatureException ignore) {
            // expected
        }
        try {
            SignerUtils.verifySignature(new ByteArrayInputStream(testData1), "      ");
            Assert.fail("verifySignature should have failed");
        } catch (SignatureException ignore) {
            // expected
        }
        try {
            SignerUtils.verifySignature(new ByteArrayInputStream(testData1), "blahblah");
            Assert.fail("verifySignature should have failed");
        } catch (SignatureException ignore) {
            // expected
        }
        try {
            SignerUtils.verifySignature(new ByteArrayInputStream(testData1), genDummySigProps("blah", "blah"));
            Assert.fail("verifySignature should have failed");
        } catch (SignatureException ignore) {
            // expected
        }
        try {
            SignerUtils.verifySignature(new ByteArrayInputStream(testData1), genDummySigProps(sigProps1.getProperty(SIGNATURE_PROP), "blah"));
            Assert.fail("verifySignature should have failed");
        } catch (SignatureException ignore) {
            // expected
        }
        try {
            SignerUtils.verifySignature(new ByteArrayInputStream(testData1), genDummySigProps("blah", sigProps1.getProperty(SIGNING_CERT_PROPS)));
            Assert.fail("verifySignature should have failed");
        } catch (SignatureException ignore) {
            // expected
        }

        // test with wrong bytes
        try {
            SignerUtils.verifySignature(new ByteArrayInputStream(testData1), sigPropsString2);
            Assert.fail("verifySignature should have failed");
        } catch (SignatureException ignore) {
            // expected
        }
        try {
            SignerUtils.verifySignature(new ByteArrayInputStream(testData1), sigPropsString3);
            Assert.fail("verifySignature should have failed");
        } catch (SignatureException ignore) {
            // expected
        }
        try {
            SignerUtils.verifySignature(new ByteArrayInputStream(testData2), sigPropsString1);
            Assert.fail("verifySignature should have failed");
        } catch (SignatureException ignore) {
            // expected
        }
        try {
            SignerUtils.verifySignature(new ByteArrayInputStream(testData2), sigPropsString3);
            Assert.fail("verifySignature should have failed");
        } catch (SignatureException ignore) {
            // expected
        }
        try {
            SignerUtils.verifySignature(new ByteArrayInputStream(testData3), sigPropsString1);
            Assert.fail("verifySignature should have failed");
        } catch (SignatureException ignore) {
            // expected
        }
        try {
            SignerUtils.verifySignature(new ByteArrayInputStream(testData3), sigPropsString2);
            Assert.fail("verifySignature should have failed");
        } catch (SignatureException ignore) {
            // expected
        }
    }

    @Test
    public void testSignWithKeyStore() throws Exception {
        final String ksType = "PKCS12";
        final String ksPass = "7layer";
        final byte[] testData = "test data".getBytes(Charsets.UTF8);

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // generate a single signer keystore
        Pair<List<X509Certificate>, File> certsFilePair = generateKeyStore(ksType, ksPass, "signer", "cn=test1.apim.ca.com");
        Assert.assertNotNull(certsFilePair);
        Assert.assertNotNull(certsFilePair.right);
        Assert.assertTrue(certsFilePair.right.exists() && !certsFilePair.right.isDirectory());
        Assert.assertThat(certsFilePair.left, Matchers.allOf(Matchers.notNullValue(), Matchers.hasSize(1)));

        // try signing providing the alias
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SignerUtils.signWithKeyStore(
                certsFilePair.right,
                ksType,
                ksPass.toCharArray(),
                "signer1",
                null,
                new ByteArrayInputStream(testData),
                baos
        );
        doVerifySignature(new ByteArrayInputStream(baos.toByteArray()), testData, certsFilePair.left.get(0));

        // try signing without providing the alias (null)
        baos = new ByteArrayOutputStream();
        SignerUtils.signWithKeyStore(
                certsFilePair.right,
                ksType,
                ksPass.toCharArray(),
                null,
                null,
                new ByteArrayInputStream(testData),
                baos
        );
        doVerifySignature(new ByteArrayInputStream(baos.toByteArray()), testData, certsFilePair.left.get(0));

        // try signing without providing the alias ("")
        baos = new ByteArrayOutputStream();
        SignerUtils.signWithKeyStore(
                certsFilePair.right,
                ksType,
                ksPass.toCharArray(),
                "",
                null,
                new ByteArrayInputStream(testData),
                baos
        );
        doVerifySignature(new ByteArrayInputStream(baos.toByteArray()), testData, certsFilePair.left.get(0));
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // generate keystore with 3 signers
        certsFilePair = generateKeyStore(ksType, ksPass, "signer", "cn=test1.apim.ca.com", "cn=test2.apim.ca.com", "cn=test3.apim.ca.com");
        Assert.assertNotNull(certsFilePair);
        Assert.assertNotNull(certsFilePair.right);
        Assert.assertTrue(certsFilePair.right.exists() && !certsFilePair.right.isDirectory());
        Assert.assertThat(certsFilePair.left, Matchers.allOf(Matchers.notNullValue(), Matchers.hasSize(3)));

        // try signing providing each alias
        int n = 1;
        for (final X509Certificate cert : certsFilePair.left) {
            baos = new ByteArrayOutputStream();
            SignerUtils.signWithKeyStore(
                    certsFilePair.right,
                    ksType,
                    ksPass.toCharArray(),
                    "signer" + n++,
                    null,
                    new ByteArrayInputStream(testData),
                    baos
            );
            doVerifySignature(new ByteArrayInputStream(baos.toByteArray()), testData, cert);
        }

        // try signing without providing the alias
        try {
            baos = new ByteArrayOutputStream();
            SignerUtils.signWithKeyStore(
                    certsFilePair.right,
                    ksType,
                    ksPass.toCharArray(),
                    null,
                    null,
                    new ByteArrayInputStream(testData),
                    baos
            );
            Assert.fail("signWithKeyStore should have failed for a keystore having multiple signers without providing one");
        } catch (KeyStoreException ignore) {
            // expected
        }

        // try with cert instead of private key
        final TestCertificateGenerator gen = new TestCertificateGenerator();
        final Pair<X509Certificate, PrivateKey> otherKeyPair = gen.basicConstraintsNoCa().subject("cn=other1.apim.ca.com").curveName("P-256").generateWithKey();
        Assert.assertNotNull(otherKeyPair);
        Assert.assertNotNull(otherKeyPair.left);
        SignerUtils.addTrustedCertificateToTrustStore(
                certsFilePair.right,
                ksType,
                ksPass.toCharArray(),
                otherKeyPair.left
        );
        try {
            baos = new ByteArrayOutputStream();
            SignerUtils.signWithKeyStore(
                    certsFilePair.right,
                    ksType,
                    ksPass.toCharArray(),
                    TRUSTED_CERTS_ALIAS_PREFIX + "1",
                    null,
                    new ByteArrayInputStream(testData),
                    baos
            );
            Assert.fail("signWithKeyStore should have failed with non private key");
        } catch (KeyStoreException ignore) {
            // expected
        }
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }

    /**
     * Signs (using a newly generated self-signed key pair) and extracts signed.dat and signature.properties bytes all in one step.
     *
     * @param testData    test data bytes to sign.
     * @return a {@link SignerUtils.SignedZip.InnerPayload} containing signed.dat and signature.properties bytes.
     */
    private SignerUtils.SignedZip.InnerPayload signAndExtractDataAndSignatureBytes(final byte[] testData) throws Exception {
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
        try (final SignerUtils.SignedZip.InnerPayload payload = signAndExtractDataAndSignatureBytes(testData)) {
            Assert.assertNotNull("zipContent is not null", payload);
            // verify dataAndSignatureBytes
            final byte[] signedDataBytes = payload.getDataBytes();
            final byte[] sigPropBytes = payload.getSignaturePropertiesBytes();
            Assert.assertNotNull("signed.dat bytes are not null", signedDataBytes);
            Assert.assertThat("signed.dat bytes are not empty", signedDataBytes.length, Matchers.greaterThan(0));
            Assert.assertThat("signed.dat bytes are what expected", signedDataBytes, Matchers.equalTo(testData));
            Assert.assertNotNull("signature.properties bytes are not null", sigPropBytes);
            Assert.assertThat("signature.properties bytes are not empty", sigPropBytes.length, Matchers.greaterThan(0));

            // test with valid signed zip having signed.dat as first entry and signature.properties as second
            doTestVerifyZip(
                    new Functions.UnaryVoidThrows<ZipOutputStream, Exception>() {
                        @Override
                        public void call(final ZipOutputStream zos) throws Exception {
                            Assert.assertNotNull(zos);
                            // first zip entry should be the signed data bytes
                            zos.putNextEntry(new ZipEntry(SIGNED_DATA_ZIP_ENTRY));
                            IOUtils.copyStream(new ByteArrayInputStream(signedDataBytes), zos);
                            // next zip entry is the signature information
                            zos.putNextEntry(new ZipEntry(SIGNATURE_PROPS_ZIP_ENTRY));
                            IOUtils.copyStream(new ByteArrayInputStream(sigPropBytes), zos);
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
                            IOUtils.copyStream(new ByteArrayInputStream(sigPropBytes), zos);
                            // next zip entry should be the signed data bytes
                            zos.putNextEntry(new ZipEntry(SIGNED_DATA_ZIP_ENTRY));
                            IOUtils.copyStream(new ByteArrayInputStream(signedDataBytes), zos);
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
                            IOUtils.copyStream(new ByteArrayInputStream(sigPropBytes), zos);
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
                            IOUtils.copyStream(new ByteArrayInputStream(signedDataBytes), zos);
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
                            IOUtils.copyStream(new ByteArrayInputStream(signedDataBytes), zos);
                            // next zip entry is the signature information
                            zos.putNextEntry(new ZipEntry(SIGNATURE_PROPS_ZIP_ENTRY));
                            IOUtils.copyStream(new ByteArrayInputStream(sigPropBytes), zos);
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
                            IOUtils.copyStream(new ByteArrayInputStream(signedDataBytes), zos);
                            // add some test file
                            zos.putNextEntry(new ZipEntry("test1.dat"));
                            IOUtils.copyStream(new ByteArrayInputStream("test1.dat data".getBytes(Charsets.UTF8)), zos);
                            // add another test file
                            zos.putNextEntry(new ZipEntry("test2.dat"));
                            IOUtils.copyStream(new ByteArrayInputStream("test2.dat data".getBytes(Charsets.UTF8)), zos);
                            // next zip entry is the signature information
                            zos.putNextEntry(new ZipEntry(SIGNATURE_PROPS_ZIP_ENTRY));
                            IOUtils.copyStream(new ByteArrayInputStream(sigPropBytes), zos);
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
                            IOUtils.copyStream(new ByteArrayInputStream(signedDataBytes), zos);
                            // next zip entry is the signature information
                            zos.putNextEntry(new ZipEntry(SIGNATURE_PROPS_ZIP_ENTRY));
                            IOUtils.copyStream(new ByteArrayInputStream(sigPropBytes), zos);
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
                            IOUtils.copyStream(new ByteArrayInputStream(signedDataBytes), zos);
                            // add some dir
                            zos.putNextEntry(new ZipEntry("folder1/"));
                            // next zip entry is the signature information
                            zos.putNextEntry(new ZipEntry(SIGNATURE_PROPS_ZIP_ENTRY));
                            IOUtils.copyStream(new ByteArrayInputStream(sigPropBytes), zos);
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
                            IOUtils.copyStream(new ByteArrayInputStream(signedDataBytes), zos);
                            // next zip entry is the signature information
                            zos.putNextEntry(new ZipEntry(SIGNATURE_PROPS_ZIP_ENTRY));
                            IOUtils.copyStream(new ByteArrayInputStream(sigPropBytes), zos);
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
                            IOUtils.copyStream(new ByteArrayInputStream(signedDataBytes), zos);
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
                            IOUtils.copyStream(new ByteArrayInputStream(sigPropBytes), zos);
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
                            IOUtils.copyStream(new ByteArrayInputStream(signedDataBytes), zos);
                            // next zip entry is the signature information
                            zos.putNextEntry(new ZipEntry(SIGNATURE_PROPS_ZIP_ENTRY));
                            IOUtils.copyStream(new ByteArrayInputStream(sigPropBytes), zos);
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
                            IOUtils.copyStream(new ByteArrayInputStream(signedDataBytes), zos);
                            // add some dir
                            zos.putNextEntry(new ZipEntry(""));
                            // next zip entry is the signature information
                            zos.putNextEntry(new ZipEntry(SIGNATURE_PROPS_ZIP_ENTRY));
                            IOUtils.copyStream(new ByteArrayInputStream(sigPropBytes), zos);
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
                            IOUtils.copyStream(new ByteArrayInputStream(signedDataBytes), zos);
                            // next zip entry is the signature information
                            zos.putNextEntry(new ZipEntry("dir1/" + SIGNATURE_PROPS_ZIP_ENTRY));
                            IOUtils.copyStream(new ByteArrayInputStream(sigPropBytes), zos);
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

    @Test
    public void testVerifySignerCertIsTrusted() throws Exception {
        final byte[] testData = "test data".getBytes(Charsets.UTF8);
        final String ksType = "PKCS12";
        final String ksPass = "7layer";
        final String ksAliasPrefix = "signer";
        final String tsType = "PKCS12";
        final String tsPass = "changeme";

        final Pair<File, List<Pair<X509Certificate, File>>> trustStoreAndSignerKeyStores = generateTrustStoreAndSignerKeyStores(
                ksType,
                ksPass,
                ksAliasPrefix,
                tsType,
                tsPass,
                "cn=test1.apim.ca.com", "cn=test2.apim.ca.com", "cn=test3.apim.ca.com"
        );
        Assert.assertNotNull(trustStoreAndSignerKeyStores);
        Assert.assertNotNull(trustStoreAndSignerKeyStores.left);
        Assert.assertTrue(trustStoreAndSignerKeyStores.left.exists() && !trustStoreAndSignerKeyStores.left.isDirectory());
        Assert.assertNotNull(trustStoreAndSignerKeyStores.right);
        Assert.assertThat(trustStoreAndSignerKeyStores.right, Matchers.allOf(Matchers.notNullValue(), Matchers.hasSize(3)));

        for (final Pair<X509Certificate, File> privateKeyStorePair : trustStoreAndSignerKeyStores.right) {
            Assert.assertNotNull(privateKeyStorePair);
            Assert.assertNotNull(privateKeyStorePair.left);
            Assert.assertNotNull(privateKeyStorePair.right);
            Assert.assertTrue(privateKeyStorePair.right.exists() && !privateKeyStorePair.right.isDirectory());
            // test that signer is trusted
            SignerUtils.verifySignerCertIsTrusted(
                    trustStoreAndSignerKeyStores.left,
                    tsType,
                    tsPass.toCharArray(),
                    privateKeyStorePair.left
            );
            final Collection<X509Certificate> trustedCerts = SignerUtils.loadTrustedCertsFromTrustStore(trustStoreAndSignerKeyStores.left, tsType, tsPass.toCharArray());
            SignerUtils.verifySignerCertIsTrusted(trustedCerts, privateKeyStorePair.left);
            // sign our test data
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            SignerUtils.signWithKeyStore(
                    privateKeyStorePair.right,
                    ksType,
                    ksPass.toCharArray(),
                    null,
                    null,
                    new ByteArrayInputStream(testData),
                    bos
            );
            // verify signature and issuer
            try (final SignerUtils.SignedZip.InnerPayload payload = execReadSignedZip(new ByteArrayInputStream(bos.toByteArray()))) {
                Assert.assertNotNull(payload);
                Assert.assertThat(payload.getSignaturePropertiesString(), Matchers.allOf(Matchers.notNullValue(), Matchers.not(Matchers.isEmptyOrNullString())));
                SignerUtils.verifySignatureAndIssuer(
                        new ByteArrayInputStream(testData),
                        payload.getSignaturePropertiesString(),
                        trustedCerts
                );
            }
        }

        final TestCertificateGenerator gen = new TestCertificateGenerator();
        final Pair<X509Certificate, PrivateKey> nonTrustedSignerPair = gen.basicConstraintsNoCa().subject("cn=test1.apim.ca.com").curveName("P-256").generateWithKey();
        try {
            SignerUtils.verifySignerCertIsTrusted(
                    trustStoreAndSignerKeyStores.left,
                    tsType,
                    tsPass.toCharArray(),
                    nonTrustedSignerPair.left
            );
            Assert.fail("verifySignerCertIsTrusted should have failed for nonTrustedSignerPair");
        } catch (Exception ignore) {
            // expected
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

    private void doTestInnerPayload(
            final SignerUtils.SignedZip.InnerPayload payload,
            final byte[] expectedBytes,
            final Either<Pair<byte[], Properties>, X509Certificate> eitherSignatureBytesORSignerCert
    ) throws Exception {
        Assert.assertNotNull(payload);
        Assert.assertNotNull(expectedBytes);
        Assert.assertThat(expectedBytes.length, Matchers.greaterThan(0));
        Assert.assertNotNull(eitherSignatureBytesORSignerCert);

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // bytes
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////
        Assert.assertThat(
                payload.getDataBytes(),
                Matchers.allOf(
                        Matchers.not(Matchers.sameInstance(expectedBytes)),  // make sure it is a copy of the buffer
                        Matchers.equalTo(expectedBytes)
                )
        );
        Assert.assertThat(payload.getDataSize(), Matchers.is(expectedBytes.length));

        Assert.assertThat(payload.getDataDigest(), Matchers.equalTo(ModuleDigest.digest(expectedBytes)));

        InputStream dataStream = payload.getDataStream();
        Assert.assertThat(
                IOUtils.slurpStream(dataStream),
                Matchers.allOf(
                        Matchers.not(Matchers.sameInstance(expectedBytes)),  // make sure it is a copy of the buffer
                        Matchers.equalTo(expectedBytes)
                )
        );
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // signature
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////
        if (eitherSignatureBytesORSignerCert.isLeft()) {
            final byte[] expectedSignature = eitherSignatureBytesORSignerCert.left().left;
            Assert.assertNotNull(expectedSignature);
            Assert.assertThat(expectedSignature.length, Matchers.greaterThan(0));
            final Properties sigProps = eitherSignatureBytesORSignerCert.left().right;
            Assert.assertNotNull(sigProps);

            Assert.assertThat(
                    payload.getSignaturePropertiesBytes(),
                    Matchers.allOf(
                            Matchers.not(Matchers.sameInstance(expectedSignature)),  // make sure it is a copy of the buffer
                            Matchers.equalTo(expectedSignature)
                    )
            );
            Assert.assertThat(payload.getSignaturePropertiesSize(), Matchers.is(expectedSignature.length));

            dataStream = payload.getSignaturePropertiesStream();
            Assert.assertThat(
                    IOUtils.slurpStream(dataStream),
                    Matchers.allOf(
                            Matchers.not(Matchers.sameInstance(expectedSignature)),  // make sure it is a copy of the buffer
                            Matchers.equalTo(expectedSignature)
                    )
            );

            Assert.assertThat(
                    payload.getSignaturePropertiesString(),
                    Matchers.equalTo(new String(expectedSignature, StandardCharsets.ISO_8859_1))
            );
            Assert.assertEquals(payload.getSignatureProperties(), sigProps);
        } else {
            final X509Certificate signerCert = eitherSignatureBytesORSignerCert.right();
            Assert.assertNotNull(signerCert);

            final Properties sigProps = payload.getSignatureProperties();
            Assert.assertNotNull(sigProps);
            Assert.assertThat(sigProps.getProperty(SIGNATURE_PROP), Matchers.allOf(Matchers.notNullValue(), Matchers.not(Matchers.isEmptyOrNullString())));
            Assert.assertThat(sigProps.getProperty(SIGNING_CERT_PROPS), Matchers.allOf(Matchers.notNullValue(), Matchers.not(Matchers.isEmptyOrNullString())));

            final byte[] sigerCertBytes = HexUtils.decodeBase64(sigProps.getProperty(SIGNING_CERT_PROPS));
            Assert.assertThat(sigerCertBytes, Matchers.equalTo(signerCert.getEncoded()));
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }

    @Test
    public void testInnerPayload() throws Exception {
        byte[] bytes = "test bytes".getBytes(Charsets.UTF8);
        PoolByteArrayOutputStream bytesPool = new PoolByteArrayOutputStream(bytes.length);
        IOUtils.copyStream(new ByteArrayInputStream(bytes), bytesPool);
        Properties sigProps = new Properties() {{
            setProperty("key1", "value1");
            setProperty("key2", "value2");
            setProperty("key3", "value3");
            setProperty("unicode_key", Character.toString('\u2202') + Character.toString('\uD840') + Character.toString('\u0436'));
        }};
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        sigProps.store(bos, "don't care");
        bos.flush();
        byte[] signature = bos.toByteArray();
        PoolByteArrayOutputStream signaturePool = new PoolByteArrayOutputStream(signature.length);
        IOUtils.copyStream(new ByteArrayInputStream(signature), signaturePool);

        try (final SignerUtils.SignedZip.InnerPayload payload = new SignerUtils.SignedZip.InnerPayload(bytesPool, ModuleDigest.digest(bytes), signaturePool)) {
            doTestInnerPayload(payload, bytes, Either.<Pair<byte[], Properties>, X509Certificate>left(Pair.pair(signature, sigProps)));
        } finally {
            ResourceUtils.closeQuietly(bytesPool);
            ResourceUtils.closeQuietly(signaturePool);
        }
    }

    @Test
    public void testInnerPayloadClose() throws Exception {
        // create sample data and signature
        PoolByteArrayOutputStream bytesPool = null, signaturePool = null;
        byte[] bytes = "test bytes".getBytes(Charsets.UTF8);
        bytesPool = new PoolByteArrayOutputStream(bytes.length);
        IOUtils.copyStream(new ByteArrayInputStream(bytes), bytesPool);
        Properties sigProps = new Properties() {{
            setProperty("key1", "value1");
            setProperty("key2", "value2");
            setProperty("key3", "value3");
            setProperty("unicode_key", Character.toString('\u2202') + Character.toString('\uD840') + Character.toString('\u0436'));
        }};
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        sigProps.store(bos, "don't care");
        bos.flush();
        byte[] signature = bos.toByteArray();
        signaturePool = new PoolByteArrayOutputStream(signature.length);
        IOUtils.copyStream(new ByteArrayInputStream(signature), signaturePool);

        try {
            // create the payload
            final SignerUtils.SignedZip.InnerPayload payload = Mockito.spy(new SignerUtils.SignedZip.InnerPayload(bytesPool, ModuleDigest.digest(bytes), signaturePool));
            doTestInnerPayload(payload, bytes, Either.<Pair<byte[], Properties>, X509Certificate>left(Pair.pair(signature, sigProps)));

            // close the payload
            Mockito.verify(payload, Mockito.never()).dispose();
            payload.close();
            Mockito.verify(payload, Mockito.times(1)).dispose();

            doTestPayloadIsClosed(payload);

            // close again
            Mockito.verify(payload, Mockito.times(1)).dispose();
            payload.close();
            Mockito.verify(payload, Mockito.times(1)).dispose(); // make sure dispose is never called

            doTestPayloadIsClosed(payload);
        } finally {
            ResourceUtils.closeQuietly(bytesPool);
            ResourceUtils.closeQuietly(signaturePool);
        }

        // test with try-with-resources block
        bytesPool = new PoolByteArrayOutputStream(bytes.length);
        IOUtils.copyStream(new ByteArrayInputStream(bytes), bytesPool);

        signaturePool = new PoolByteArrayOutputStream(signature.length);
        IOUtils.copyStream(new ByteArrayInputStream(signature), signaturePool);

        SignerUtils.SignedZip.InnerPayload mockPayload = Mockito.spy(new SignerUtils.SignedZip.InnerPayload(bytesPool, ModuleDigest.digest(bytes), signaturePool));
        try (final SignerUtils.SignedZip.InnerPayload payload = mockPayload) {
            doTestInnerPayload(payload, bytes, Either.<Pair<byte[], Properties>, X509Certificate>left(Pair.pair(signature, sigProps)));
            Mockito.verify(payload, Mockito.never()).dispose();
            Mockito.verify(mockPayload, Mockito.never()).dispose();
        } finally {
            ResourceUtils.closeQuietly(bytesPool);
            ResourceUtils.closeQuietly(signaturePool);
        }
        Mockito.verify(mockPayload, Mockito.times(1)).dispose();
        doTestPayloadIsClosed(mockPayload);
        // close again
        Mockito.verify(mockPayload, Mockito.times(1)).dispose();
        mockPayload.close();
        Mockito.verify(mockPayload, Mockito.times(1)).dispose(); // make sure dispose is never called
    }

    private void doTestPayloadIsClosed(final SignerUtils.SignedZip.InnerPayload payload) throws Exception {
        Assert.assertNotNull(payload);

        final byte[] expected = new byte[0];
        // make sure data is not leaked
        Assert.assertThat(
                payload.getDataBytes(),
                Matchers.allOf(
                        Matchers.not(Matchers.sameInstance(expected)),  // make sure it is a copy of the buffer
                        Matchers.equalTo(expected)
                )
        );
        Assert.assertThat(payload.getDataSize(), Matchers.is(expected.length));
        InputStream stream = payload.getDataStream();
        Assert.assertThat(
                IOUtils.slurpStream(stream),
                Matchers.allOf(
                        Matchers.not(Matchers.sameInstance(expected)),  // make sure it is a copy of the buffer
                        Matchers.equalTo(expected)
                )
        );

        // make sure signature is not leaked
        Assert.assertThat(
                payload.getSignaturePropertiesBytes(),
                Matchers.allOf(
                        Matchers.not(Matchers.sameInstance(expected)),  // make sure it is a copy of the buffer
                        Matchers.equalTo(expected)
                )
        );
        Assert.assertThat(payload.getSignaturePropertiesSize(), Matchers.is(expected.length));
        stream = payload.getSignaturePropertiesStream();
        Assert.assertThat(
                IOUtils.slurpStream(stream),
                Matchers.allOf(
                        Matchers.not(Matchers.sameInstance(expected)),  // make sure it is a copy of the buffer
                        Matchers.equalTo(expected)
                )
        );
        Assert.assertThat(payload.getSignaturePropertiesString(), Matchers.isEmptyString());
        Properties signatureProperties = payload.getSignatureProperties();
        Assert.assertNotNull(signatureProperties);
        Assert.assertEquals(0, signatureProperties.size());
    }

    @Test
    public void testSignedZip() throws Exception {
        final String ksType = "PKCS12";
        final String ksPass = "7layer";
        final byte[] testData = "test data".getBytes(Charsets.UTF8);

        final Pair<List<X509Certificate>, File> certsFilePair = generateKeyStore(ksType, ksPass, "signer", "cn=test1.apim.ca.com", "cn=test2.apim.ca.com", "cn=test3.apim.ca.com");
        Assert.assertNotNull(certsFilePair);
        Assert.assertNotNull(certsFilePair.right);
        Assert.assertTrue(certsFilePair.right.exists() && !certsFilePair.right.isDirectory());
        Assert.assertThat(certsFilePair.left, Matchers.allOf(Matchers.notNullValue(), Matchers.hasSize(3)));

        // trusted are the first two ("cn=test1.apim.ca.com" and "cn=test2.apim.ca.com")
        final SignerUtils.SignedZip signedZip = new SignerUtils.SignedZip(new X509Certificate[] {certsFilePair.left.get(0), certsFilePair.left.get(1)});
        final SignerUtils.SignedZip signedZipNoTrustedCerts = new SignerUtils.SignedZip(Collections.<X509Certificate>emptyList());

        // try with the first "cn=test1.apim.ca.com"
        {
            File signedZipFile = kryStoreFolder.newFile();
            try (final OutputStream os = new BufferedOutputStream(new FileOutputStream(signedZipFile))) {
                SignerUtils.signWithKeyStore(
                        certsFilePair.right,
                        ksType,
                        ksPass.toCharArray(),
                        "signer1",
                        null,
                        new ByteArrayInputStream(testData),
                        os
                );
                try (final SignerUtils.SignedZip.InnerPayload payload = signedZip.load(signedZipFile, SignerUtils.SignedZip.InnerPayload.FACTORY)) {
                    doTestInnerPayload(payload, testData, Either.<Pair<byte[], Properties>, X509Certificate>right(certsFilePair.left.get(0)));
                }
                //noinspection UnusedDeclaration
                try (final SignerUtils.SignedZip.InnerPayload payload = signedZipNoTrustedCerts.load(signedZipFile, SignerUtils.SignedZip.InnerPayload.FACTORY)) {
                    Assert.fail("SignedZip.load should have failed without any trusted certs");
                } catch (SignatureException ignore) {
                    // expected
                }
            }
        }
        // try with the second "cn=test2.apim.ca.com"
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            SignerUtils.signWithKeyStore(
                    certsFilePair.right,
                    ksType,
                    ksPass.toCharArray(),
                    "signer2",
                    null,
                    new ByteArrayInputStream(testData),
                    baos
            );
            try (final SignerUtils.SignedZip.InnerPayload payload = signedZip.load(new ByteArrayInputStream(baos.toByteArray()), SignerUtils.SignedZip.InnerPayload.FACTORY)) {
                doTestInnerPayload(payload, testData, Either.<Pair<byte[], Properties>, X509Certificate>right(certsFilePair.left.get(1)));
            }
            //noinspection UnusedDeclaration
            try (final SignerUtils.SignedZip.InnerPayload payload = signedZipNoTrustedCerts.load(new ByteArrayInputStream(baos.toByteArray()), SignerUtils.SignedZip.InnerPayload.FACTORY)) {
                Assert.fail("SignedZip.load should have failed without any trusted certs");
            } catch (SignatureException ignore) {
                // expected
            }
        }
        // try with the third "cn=test3.apim.ca.com" (untrusted)
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            SignerUtils.signWithKeyStore(
                    certsFilePair.right,
                    ksType,
                    ksPass.toCharArray(),
                    "signer3",
                    null,
                    new ByteArrayInputStream(testData),
                    baos
            );
            //noinspection UnusedDeclaration
            try (final SignerUtils.SignedZip.InnerPayload payload = signedZip.load(new ByteArrayInputStream(baos.toByteArray()), SignerUtils.SignedZip.InnerPayload.FACTORY)) {
                Assert.fail("SignedZip.load should have failed as cn=test3.apim.ca.com is not trusted");
            } catch (SignatureException ignore) {
                // expected
            }
            //noinspection UnusedDeclaration
            try (final SignerUtils.SignedZip.InnerPayload payload = signedZipNoTrustedCerts.load(new ByteArrayInputStream(baos.toByteArray()), SignerUtils.SignedZip.InnerPayload.FACTORY)) {
                Assert.fail("SignedZip.load should have failed without any trusted certs");
            } catch (SignatureException ignore) {
                // expected
            }
        }
    }
}

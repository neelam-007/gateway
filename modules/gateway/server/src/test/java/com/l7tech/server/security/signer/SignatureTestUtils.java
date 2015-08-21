package com.l7tech.server.security.signer;

import com.l7tech.common.io.NullOutputStream;
import com.l7tech.gateway.common.security.signer.SignedZipVisitor;
import com.l7tech.gateway.common.security.signer.SignerUtils;
import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.util.*;
import org.apache.commons.lang.StringUtils;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Ignore;
import sun.security.x509.X500Name;

import javax.security.auth.x500.X500Principal;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Utility class for testing signing and signature validation
 */
@Ignore
public class SignatureTestUtils {
    /**
     * IMPORTANT: Keep the values as per {@link com.l7tech.gateway.common.security.signer.SignerUtils#SIGNED_DATA_ZIP_ENTRY}
     */
    public static final String SIGNED_DATA_ZIP_ENTRY = "signed.dat";
    /**
     * IMPORTANT: Keep the values as per {@link com.l7tech.gateway.common.security.signer.SignerUtils#SIGNATURE_PROPS_ZIP_ENTRY}
     */
    public static final String SIGNATURE_PROPS_ZIP_ENTRY = "signature.properties";

    private static final String KEYSTORES_TMP_DIR = "l7tech-keyStores-Tmp";
    private static final String TRUST_STORE_TYPE = "jks";
    private static final char[] TRUST_STORE_PASS = "changeit".toCharArray();
    private static final String TRUST_STORE_FILE_PREFIX = "trusted_signers";
    private static final Principal ROOT_CA_DN = new X500Principal("cn=root.apim.ca.com");
    private static final String SIGNER_STORE_TYPE = "PKCS12";
    private static final char[] SIGNER_STORE_PASS = "7layer".toCharArray();
    private static final String SIGNER_STORE_CERT_ALIAS = "signer";
    private static final char[] SIGNER_STORE_ENTRY_PASS = "7layer".toCharArray();
    private static final int CSR_EXPIRY_DAYS = 7305;
    private static final String CSR_SIG_ALG = "SHA512withECDSA";
    private static final int SIGNER_CERT_EXPIRY_DAYS = 7305;
    private static final String ROOT_CA_KEYSTORE_FILE_PREFIX = "root_ca";
    private static final String ROOT_CA_STORE_TYPE = "PKCS12";
    private static final char[] ROOT_CA_STORE_PASS = "7layer".toCharArray();
    private static final String ROOT_CA_STORE_CERT_ALIAS = "root";
    private static final char[] ROOT_CA_STORE_ENTRY_PASS = "7layer".toCharArray();

    // randomly generated temporary folder for keeping generated key store files for this session.
    private static File keyStoresTmpFolder;

    // variable holding all temporary folder which are created by the unit-test,
    // so that they can be gracefully deleted @AfterClass
    private static final Map<String, File> tmpFiles = new HashMap<>();

    /**
     * Our stub {@link SignatureVerifier} holding trusted signers store file, Root CA private key file and a map of signer cert key store files.
     */
    @SuppressWarnings("UnusedDeclaration")
    static class SignatureVerifierStub extends SignatureVerifierImpl {
        private final File trustedSignersStore;
        private final File rootCAKeyStore;
        private final Map<String, File> signerCertKeyStores;

        private SignatureVerifierStub(
                final File trustedSignersStore,
                final String type,
                char[] password,
                final File rootCAKeyStore,
                final Map<String, File> signerCertKeyStores
        ) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
            super(trustedSignersStore, type, password);
            Assert.assertThat(trustedSignersStore, Matchers.notNullValue());
            Assert.assertThat(rootCAKeyStore, Matchers.notNullValue());
            Assert.assertThat(signerCertKeyStores, Matchers.notNullValue());
            this.trustedSignersStore = trustedSignersStore;
            this.rootCAKeyStore = rootCAKeyStore;
            this.signerCertKeyStores = Collections.unmodifiableMap(signerCertKeyStores);
        }

        public Map<String, File> getSignerCertKeyStores() {
            return signerCertKeyStores;
        }

        public File getTrustedSignersStore() {
            return trustedSignersStore;
        }

        public File getRootCAKeyStore() {
            return rootCAKeyStore;
        }
    }

    public static void beforeClass() throws Exception {
        cleanUpTemporaryFilesFromPreviousRuns(KEYSTORES_TMP_DIR);

        keyStoresTmpFolder = getTempFolder(KEYSTORES_TMP_DIR);
        Assert.assertThat(keyStoresTmpFolder, Matchers.notNullValue());
        Assert.assertThat(keyStoresTmpFolder.isDirectory() && keyStoresTmpFolder.exists(), Matchers.is(true));
    }

    public static void afterClass() throws Exception {
        for (final File tmpDir : tmpFiles.values()) {
            FileUtils.deleteDir(tmpDir);
        }
        tmpFiles.clear();
    }


    public static void cleanUpTemporaryFilesFromPreviousRuns(final String ... tmpFolderPrefixes) {
        //noinspection SpellCheckingInspection
        final String tmpDirPath = SyspropUtil.getProperty("java.io.tmpdir");
        final File tmpFolder = new File(tmpDirPath);

        for (final String tampFolderPrefix : tmpFolderPrefixes) {
            File[] files = tmpFolder.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(final File dir, final String name) {
                    return name != null && !name.isEmpty() && name.matches(tampFolderPrefix + ".*");
                }
            });
            for (final File file : files) {
                if (file.isDirectory()) {
                    if (!FileUtils.deleteDir(file)) {
                        System.err.println( "Can't remove " + file.getAbsolutePath() );
                    }
                }
            }
        }
    }

    private static File getTempFolder(final String prefix) {
        //noinspection SpellCheckingInspection
        final String tmpDirPath = SyspropUtil.getProperty("java.io.tmpdir");
        final File tmpDir = new File(tmpDirPath);
        final File moduleTmpDir = new File(
                tmpDir,
                prefix + "-" + Long.toHexString(System.currentTimeMillis())
        );

        if (moduleTmpDir.exists()) {
            if (!FileUtils.deleteDir(moduleTmpDir)) {
                return getTempFolder(prefix + "-" + Long.toHexString(Double.doubleToLongBits(Math.random())));
            }
        }
        //noinspection ResultOfMethodCallIgnored
        moduleTmpDir.mkdir();
        moduleTmpDir.deleteOnExit();

        // keep track of the new folder, so that it will be deleted @AfterClass.
        tmpFiles.put(moduleTmpDir.getAbsolutePath(), moduleTmpDir);

        return moduleTmpDir;
    }

    private static File generateRandomFile(final File parentDir, final String fileNamePrefix, final String fileNameSuffix) throws IOException {
        Assert.assertThat(parentDir, Matchers.notNullValue());
        Assert.assertThat(parentDir.isDirectory() && parentDir.exists(), Matchers.is(true));

        // generate random file name
        final byte[] rndBytes = new byte[8];
        RandomUtil.nextBytes(rndBytes);
        final String fileName =
                (StringUtils.isNotBlank(fileNamePrefix) ? fileNamePrefix + "_" : "") +
                Long.toHexString(System.currentTimeMillis()) + HexUtils.hexDump(rndBytes) +
                (StringUtils.isNotBlank(fileNameSuffix) ? "_" + fileNameSuffix : "");
        // create the trust store
        final File file = new File(parentDir, fileName);
        if (file.exists() && !file.isDirectory()) {
            // shouldn't happen but if exists simply delete and overwrite
            if (!file.delete()) {
                throw new IOException("Failed to delete file: " + file.getCanonicalPath());
            }
        }
        Assert.assertThat(!file.exists() && !file.isDirectory(), Matchers.is(true));
        return file;
    }

    /**
     * Generate a test signer cert.
     * <p>Go through the whole on-boarding process:<br/>
     * 1. SK Dev: prospective SK dev generates key pair, placeholder self-signed cert, and csr<br/>
     * 2. SK Dev: SK dev generates CSR, and sends the CSR to the API Gateway Solution Kit Code Signing Program Administrator (CA Admin) <br/>
     * 3. CA Admin: If application approved, create a Signer Cert using Root CA private key to sign new code signing cert  <br/>
     * 4. CA Admin: return the Signing Cert back to the prospective SK dev
     * </p>
     *
     * @param dn                    the DN for the prospective team
     * @param rootCa                the CA Root Certificate Public Key
     * @param rootCertPrivateKey    the CA Root Certificate Private Key
     * @return a Signer Cert.
     * @throws Exception
     */
    private static Pair<X509Certificate, File> generateTestSignerCert(
            final String dn,
            final X509Certificate rootCa,
            final PrivateKey rootCertPrivateKey
    ) throws Exception {
        // load root cert
        Assert.assertThat(dn, Matchers.not(Matchers.isEmptyOrNullString()));
        Assert.assertThat(rootCa, Matchers.notNullValue());
        Assert.assertThat(rootCertPrivateKey, Matchers.notNullValue());

        // prospective SK dev generates key pair, placeholder self-signed cert, and csr
        final KeyPair signerKeyPair = SignerUtils.generateNewKeyPair();
        Assert.assertThat(signerKeyPair, Matchers.notNullValue());

        // SK dev generates CSR, and sends CSR to the API Gateway Solution Kit Code Signing Program Administrator
        final byte[] signerCsr = SignerUtils.generatePkcs10CertificateSigningRequest(signerKeyPair, dn, CSR_EXPIRY_DAYS, CSR_SIG_ALG);
        Assert.assertThat(signerCsr, Matchers.notNullValue());

        // CA Admin: If application approved, CA Admin uses code signing CA private key to sign new code signing cert
        final X509Certificate signerCert = SignerUtils.createCertificateFromCsr(signerCsr, rootCa, rootCertPrivateKey, SIGNER_CERT_EXPIRY_DAYS);
        Assert.assertThat(signerCert, Matchers.notNullValue());

        // generate signer KeyStore
        Assert.assertThat(keyStoresTmpFolder, Matchers.notNullValue());
        Assert.assertThat(keyStoresTmpFolder.exists() && keyStoresTmpFolder.isDirectory(), Matchers.is(true));
        final File signerKeyStore = generateRandomFile(keyStoresTmpFolder, new X500Name(dn).getCommonName(), "keyStore.p12");
        SignerUtils.saveKeyAndCertChainToKeyStoreFile(
                signerKeyStore,
                SIGNER_STORE_TYPE,
                SIGNER_STORE_PASS,
                SIGNER_STORE_CERT_ALIAS,
                SIGNER_STORE_ENTRY_PASS,
                signerKeyPair.getPrivate(),
                new X509Certificate[]{signerCert, rootCa}
        );
        Assert.assertThat(signerKeyStore.exists() && !signerKeyStore.isDirectory(), Matchers.is(true));

        // return a pair of the signer cert and signing keystore
        return Pair.pair(signerCert, signerKeyStore);
    }

    /**
     *
     * @return a {@code Pair} of trusted store {@code File} and the root cert private key {@code File}.
     */
    public static Triple<
                   File,               // trusted store
                   File,               // keystore file containing the Root CA KeyPair (Public and Private Keys)
                   Map<String, File>   // a map of principle and keyStore files for optional trusted Signer Certs
                   >
    generateTrustedKeyStore(final String ... teamCertPrinciples) throws Exception {
        // generate root certificate pair
        final TestCertificateGenerator gen = new TestCertificateGenerator();
        final Pair<X509Certificate, PrivateKey> rootCa = gen.basicConstraintsCa(1).subject(ROOT_CA_DN.toString()).curveName("P-256").generateWithKey();

        Assert.assertThat(rootCa, Matchers.notNullValue());
        Assert.assertThat(rootCa.left, Matchers.notNullValue());
        Assert.assertThat(rootCa.right, Matchers.notNullValue());

        final Map<String, File> trustedCertKeyStores = new HashMap<>();

        // random file name for trust store
        Assert.assertThat(keyStoresTmpFolder, Matchers.notNullValue());
        Assert.assertThat(keyStoresTmpFolder.exists() && keyStoresTmpFolder.isDirectory(), Matchers.is(true));
        final File trustedStoreFile = generateRandomFile(keyStoresTmpFolder, TRUST_STORE_FILE_PREFIX, null);
        // create empty trust store
        SignerUtils.createEmptyKeyStoreFile(trustedStoreFile, TRUST_STORE_TYPE, TRUST_STORE_PASS);

        // generate Root CA KeyPair KeyStore
        Assert.assertThat(keyStoresTmpFolder, Matchers.notNullValue());
        Assert.assertThat(keyStoresTmpFolder.exists() && keyStoresTmpFolder.isDirectory(), Matchers.is(true));
        final File rootCaKeyStoreFile = generateRandomFile(keyStoresTmpFolder, ROOT_CA_KEYSTORE_FILE_PREFIX, "keystore.p12");
        SignerUtils.saveKeyAndCertChainToKeyStoreFile(
                rootCaKeyStoreFile,
                ROOT_CA_STORE_TYPE,
                ROOT_CA_STORE_PASS,
                ROOT_CA_STORE_CERT_ALIAS,
                ROOT_CA_STORE_ENTRY_PASS,
                rootCa.right,
                new X509Certificate[]{rootCa.left}
        );
        Assert.assertThat(rootCaKeyStoreFile.exists() && !rootCaKeyStoreFile.isDirectory(), Matchers.is(true));

        // add our test team trusted certs
        final Set<String> usedPrincipals = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        // skip any duplicates
        Collections.addAll(usedPrincipals, teamCertPrinciples);
        // create and add all team certs into the trust store
        for (String principle : usedPrincipals) {
            final Pair<X509Certificate, File> signerCertAnsKeyStore = generateTestSignerCert(principle, rootCa.left, rootCa.right);
            SignerUtils.addTrustedCertificateToTrustStore(trustedStoreFile, TRUST_STORE_TYPE, TRUST_STORE_PASS, signerCertAnsKeyStore.left);
            Assert.assertThat(trustedCertKeyStores, Matchers.not(Matchers.hasKey(principle)));
            trustedCertKeyStores.put(principle, signerCertAnsKeyStore.right);
        }
        // test that all certs are stored in the key store
        final Collection<X509Certificate> trustedCerts = SignerUtils.loadTrustedCertsFromTrustStore(trustedStoreFile, TRUST_STORE_TYPE, TRUST_STORE_PASS);
        Assert.assertThat(trustedCerts.size(), Matchers.is(usedPrincipals.size()));

        // return the trusted store file and the root cert private key
        return Triple.triple(trustedStoreFile, rootCaKeyStoreFile, trustedCertKeyStores);
    }

    /**
     * Create a new instance of {@code SignatureVerifier} with optional trusted Signer Certs.<br/>
     * The {@code SignatureVerifier} will be backed up with a newly created trusted store holding a Root CA certificate,
     * as well as any specified trusted Signer Certs (created with DN's specified from {@code trustedSignerCertPrinciples}).
     *
     *
     * @param trustedSignerCertPrinciples    optional array of trusted Signer Certs.
     */
    public static SignatureVerifier createSignatureVerifier(final String... trustedSignerCertPrinciples) throws Exception {
        final Triple<File, File, Map<String, File>> trustedStoreTriple = generateTrustedKeyStore(trustedSignerCertPrinciples);
        return new SignatureVerifierStub(
                trustedStoreTriple.left,
                TRUST_STORE_TYPE,
                TRUST_STORE_PASS,
                trustedStoreTriple.middle,
                trustedStoreTriple.right
        );
    }

    /**
     * Will sign the specified {@code dataStream} using the specified {@code signer} and {@code signerDn}.
     *
     * @return signed content raw-bytes.
     */
    public static byte[] sign(final SignatureVerifier signer, final InputStream dataStream, final String signerDn) throws Exception {
        Assert.assertThat(signer, Matchers.instanceOf(SignatureVerifierStub.class));
        final SignatureVerifierStub signerStub = (SignatureVerifierStub)signer;
        Assert.assertThat(dataStream, Matchers.notNullValue());
        Assert.assertThat(signerStub.getSignerCertKeyStores(), Matchers.notNullValue());
        Assert.assertThat(signerStub.getSignerCertKeyStores(), Matchers.hasKey(signerDn));
        final File teamKeyStoreFile = signerStub.getSignerCertKeyStores().get(signerDn);

        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            SignerUtils.signWithKeyStore(teamKeyStoreFile, SIGNER_STORE_TYPE, SIGNER_STORE_PASS, SIGNER_STORE_CERT_ALIAS, SIGNER_STORE_ENTRY_PASS, dataStream, baos);
            return baos.toByteArray();
        }
    }

    /**
     * A generic method for extracting the signature properties using the {@code valueConverter},
     * e.g. get the signature as {@code String} or as {@code Properties} etc.
     */
    public static <R> R getSignature(
            final byte[] signedZipContent,
            final Functions.UnaryThrows<R, InputStream, IOException> valueConverter
    ) throws Exception {
        Assert.assertNotNull(signedZipContent);
        Assert.assertNotNull(valueConverter);
        try (final InputStream is = new ByteArrayInputStream(signedZipContent)) {
            // process the signed zip file
            final Pair<Void, R> signaturePair = SignerUtils.walkSignedZip(
                    is,
                    new SignedZipVisitor<Void, R>() {
                        @Override
                        public Void visitData(@NotNull final InputStream inputStream) throws IOException {
                            // don't care
                            return null;
                        }

                        @Override
                        public R visitSignature(@NotNull final InputStream inputStream) throws IOException {
                            // read the signature
                            return valueConverter.call(inputStream);
                        }
                    },
                    true
            );

            // get the signature props
            final R signatureProps = signaturePair.right;
            if (signatureProps == null) {
                throw new IOException("Signature missing from signed Zip");
            }
            return signatureProps;
        }
    }

    /**
     * Convenient method for extracting the signature properties as raw-bytes
     */
    public static byte[] getSignatureBytes(final byte[] signedZipContent) throws Exception {
        return getSignature(
                signedZipContent,
                new Functions.UnaryThrows<byte[], InputStream, IOException>() {
                    @Override
                    public byte[] call(final InputStream inputStream) throws IOException {
                        return IOUtils.slurpStream(inputStream);
                    }
                }
        );
    }

    /**
     * Convenient method for extracting the signature properties as {@code Properties} object.
     */
    public static Properties getSignatureProperties(final byte[] signedZipContent) throws Exception {
        return getSignature(
                signedZipContent,
                new Functions.UnaryThrows<Properties, InputStream, IOException>() {
                    @Override
                    public Properties call(final InputStream inputStream) throws IOException {
                        final Properties sigProps = new Properties();
                        sigProps.load(inputStream);
                        return sigProps;
                    }
                }
        );
    }

    /**
     * Convenient method for extracting the signature properties as {@code String}.
     */
    public static String getSignatureString(final byte[] signedZipContent) throws Exception {
        return getSignature(
                signedZipContent,
                new Functions.UnaryThrows<String, InputStream, IOException>() {
                    @Override
                    public String call(final InputStream inputStream) throws IOException {
                        final StringWriter writer = new StringWriter();
                        try (final Reader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.ISO_8859_1))) {
                            IOUtils.copyStream(reader, writer);
                            writer.flush();
                        }
                        return writer.toString();
                    }
                }
        );
    }

    /**
     * A generic method for extracting the signed raw-data from the {@code signedZipContent}
     * and transform the data using {@code valueTransformer}, e.g. calculate raw-data digest etc.
     */
    public static <R> R getSignedData(
            final byte[] signedZipContent,
            final Functions.UnaryThrows<R, InputStream, IOException> valueTransformer
    ) throws Exception {
        Assert.assertNotNull(signedZipContent);
        Assert.assertNotNull(valueTransformer);
        try (final InputStream is = new ByteArrayInputStream(signedZipContent)) {
            // process the signed zip file
            final Pair<R, Void> rawBytesPair = SignerUtils.walkSignedZip(
                    is,
                    new SignedZipVisitor<R, Void>() {
                        @Override
                        public R visitData(@NotNull final InputStream inputStream) throws IOException {
                            //return IOUtils.slurpStream(inputStream);
                            return valueTransformer.call(inputStream);
                        }

                        @Override
                        public Void visitSignature(@NotNull final InputStream inputStream) throws IOException {
                            // don't care
                            return null;
                        }
                    },
                    true
            );

            // get the signature props
            final R signedDataRawBytes = rawBytesPair.left;
            if (signedDataRawBytes == null) {
                throw new IOException("signed data raw bytes missing from signed Zip");
            }
            return signedDataRawBytes;
        }
    }

    /**
     * Convenient method for extracting the signed raw-data from the {@code signedZipContent} and returning the calculated digest (SHA256 that is).
     */
    public static byte[] calcSignedDataDigest(final byte[] signedZipContent) throws Exception {
        final MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        return getSignedData(
                signedZipContent,
                new Functions.UnaryThrows<byte[], InputStream, IOException>() {
                    @Override
                    public byte[] call(final InputStream inputStream) throws IOException {
                        final DigestInputStream dis = new DigestInputStream(inputStream, messageDigest);
                        IOUtils.copyStream(dis, new NullOutputStream());
                        return dis.getMessageDigest().digest();
                    }
                }
        );
    }

    public static String signAndGetSignature(final SignatureVerifier signer, final byte[] bytes, final String teamDn) throws Exception {
        try (final ByteArrayInputStream bis = new ByteArrayInputStream(bytes)) {
            return getSignatureString(sign(signer, bis, teamDn));
        }
    }

    public static String signAndGetSignature(final SignatureVerifier signer, final InputStream dataStream, final String teamDn) throws Exception {
        return getSignatureString(sign(signer, dataStream, teamDn));
    }

    public static String signAndGetSignature(final SignatureVerifier signer, final File dataFile, final String teamDn) throws Exception {
        try (final BufferedInputStream is = new BufferedInputStream(new FileInputStream(dataFile))) {
            return getSignatureString(sign(signer, is, teamDn));
        }
    }

    /**
     * Sign specified {@code dataStream} using the specified {@code signer} and {@code signerDn}.
     * Next extract raw data bytes and signature properties and pass them to the specified {@code modifyCallback} for tampering/modification.
     * Returned {@code Pair} of data and signature properties are then written into the same zip.
     *
     * @return the modified signed zip bytes.
     */
    public static byte[] signAndTamperWithContent(
            final InputStream dataStream, final SignatureVerifier signer, final String signerDn,
            final Functions.BinaryThrows<Pair<byte[], Properties>, byte[], Properties, Exception> modifyCallback
    ) throws Exception {
        Assert.assertNotNull(signer);
        Assert.assertNotNull(dataStream);
        Assert.assertNotNull(modifyCallback);

        // first sign the dataStream using the specified signer and signer DN (i.e. the team cert)
        final byte[] signedContent = sign(signer, dataStream, signerDn);

        // process the signed content and extract the raw bytes and signature properties
        final Pair<
                byte[], // data bytes
                Properties  // signature properties
                > signedDataAndSignatureBytes = SignerUtils.walkSignedZip(
                new ByteArrayInputStream(signedContent),
                new SignedZipVisitor<byte[], Properties>() {
                    @Override
                    public byte[] visitData(@NotNull final InputStream inputStream) throws IOException {
                        final byte[] dataBytes = IOUtils.slurpStream(inputStream);
                        Assert.assertNotNull(dataBytes);
                        Assert.assertThat(dataBytes.length, Matchers.greaterThan(0));
                        return dataBytes;
                    }

                    @Override
                    public Properties visitSignature(@NotNull final InputStream inputStream) throws IOException {
                        final Properties sigProps = new Properties();
                        sigProps.load(inputStream);
                        return sigProps;
                    }
                },
                true
        );

        // get signed data and signature properties bytes
        final byte[] dataBytes = signedDataAndSignatureBytes.left;
        final Properties sigProps = signedDataAndSignatureBytes.right;
        // make sure both are read correctly
        if (dataBytes == null || sigProps == null) {
            Assert.fail("Invalid signed Zip file. Either 'Signed Data' or 'Signature Properties' or both are missing from signed Zip");
        }

        // do the modification
        final Pair<byte[], Properties> dataAndSignature = modifyCallback.call(dataBytes, sigProps);
        Assert.assertNotNull(dataAndSignature);
        Assert.assertNotNull(dataAndSignature.left);
        Assert.assertThat(dataAndSignature.left.length, Matchers.greaterThan(0));
        Assert.assertNotNull(dataAndSignature.right);

        // now repack the zip with modified bytes
        final ByteArrayOutputStream outputZip = new ByteArrayOutputStream(1024);
        try (final ZipOutputStream zos = new ZipOutputStream(outputZip)) {
            // first zip entry should be the signed data bytes
            zos.putNextEntry(new ZipEntry(SIGNED_DATA_ZIP_ENTRY));
            // write the modified bytes into the first zip entry
            IOUtils.copyStream(new ByteArrayInputStream(dataAndSignature.left), zos);

            // next zip entry is the signature information
            zos.putNextEntry(new ZipEntry(SIGNATURE_PROPS_ZIP_ENTRY));
            dataAndSignature.right.store(zos, "Signature");
        }

        // return the modified zip bytes
        return outputZip.toByteArray();
    }


    /**
     * As the name says it'll flip a byte, of the specified byte array, at the specified position.
     * @return the same (as not a copy of) but modified byte array.
     */
    public static byte[] flipByteAtPos(final byte[] bytes, final int byteToFlip) {
        Assert.assertNotNull(bytes);
        Assert.assertThat(bytes.length, Matchers.greaterThan(0));
        Assert.assertThat(byteToFlip, Matchers.allOf(Matchers.greaterThanOrEqualTo(0), Matchers.lessThan(bytes.length)));
        // flip specified byte byte
        bytes[byteToFlip] = (byte) (~(bytes[byteToFlip]) & 0xff);
        return bytes;
    }

    protected static final Random rnd = new Random();
    /**
     * As the name says it'll flip a random byte of the specified byte array.
     * @return the same (as not a copy of) but modified byte array.
     */
    public static byte[] flipRandomByte(final byte[] bytes) {
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
}

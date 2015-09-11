package com.l7tech.gateway.common.module;

import com.l7tech.gateway.common.security.signer.SignedZipVisitor;
import com.l7tech.gateway.common.security.signer.SignerUtils;
import com.l7tech.util.*;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.text.MessageFormat;
import java.util.*;
import java.util.jar.JarInputStream;

/**
 * {@link ServerModuleFile} utility class.
 * All {@link ServerModuleFile} utility methods should go here.
 */
public class ServerModuleFileUtils {
    private static final ResourceBundle resources = ResourceBundle.getBundle(ServerModuleFileUtils.class.getName());

    @NotNull
    private final CustomAssertionsScannerHelper customAssertionsScannerHelper;
    @NotNull
    private final ModularAssertionsScannerHelper modularAssertionsScannerHelper;

    public ServerModuleFileUtils(
            @NotNull final CustomAssertionsScannerHelper customAssertionsScannerHelper,
            @NotNull final ModularAssertionsScannerHelper modularAssertionsScannerHelper
    ) {
        this.customAssertionsScannerHelper = customAssertionsScannerHelper;
        this.modularAssertionsScannerHelper = modularAssertionsScannerHelper;
    }

    /**
     * Convenient method for creating a {@link ServerModuleFile} from the specified {@code signedFile}.
     *
     * @param signedFile    the specified signed modular or custom assertion file.  Required and cannot be {@code null}.
     * @return a {@code ServerModuleFile} from the specified {@code file}, never {@code null}.
     * @see #createFromStream(java.io.InputStream, String)
     */
    public ServerModuleFile createFromFile(@NotNull final File signedFile) throws IOException {
        try (final InputStream bis = new BufferedInputStream(new FileInputStream(signedFile))) {
            return createFromStream(bis, signedFile.getName());
        }
    }

    /**
     * Utility function for creating a {@link ServerModuleFile} from the specified modular or custom assertion {@code file}.
     * <p/>
     * The method will create a new {@link ServerModuleFile} object and set its properties according to the specified {@code file}.<br/>
     * The method will determine whether the specified {@code file} is a modular or custom assertion and set the type accordingly.<br/>
     * A proper {@link com.l7tech.gateway.common.module.ServerModuleFileData} object will be created based on the {@code file} bytes.
     *
     * @param signedModuleStream    the specified signed modular or custom assertion file stream.  Required and cannot be {@code null}.
     * @param fileName              the signed module file-name.  Required and cannot be {@code null}.
     * @return a {@code ServerModuleFile} from the specified {@code file}, never {@code null}.
     * @throws IOException if the module type cannot be determined, or if either signature verification failed or an error occurs while verifying it,
     * or if the specified {@code signedModuleStream} is unsigned, or if the {@code signedModuleStream} is not a valid modular or custom assertion file,
     * and finally if other IO error happens while reading the specified {@code signedModuleStream}.
     */
    public ServerModuleFile createFromStream(@NotNull final InputStream signedModuleStream, @NotNull final String fileName) throws IOException {
        // get bytes and signature properties
        final ModuleBytesAndSignature bytesAndSignature = getBytesAndSignatureFromSignedZip(signedModuleStream, fileName);

        // extract module bytes, digest and signature properties
        @NotNull final byte[] bytes = bytesAndSignature.bytesAndDigest.bytes;
        @NotNull final byte[] digest = bytesAndSignature.bytesAndDigest.digest;
        @NotNull final Properties signatureProps = bytesAndSignature.signature.signatureProps;
        @NotNull final String signaturePropsString = bytesAndSignature.signature.signaturePropsString;

        // get the actual raw bytes size
        long fileLength = bytes.length;
        // create new server module file instance
        final ServerModuleFile serverModuleFile = new ServerModuleFile();
        // make sure the module is either modular or custom assertion
        try (final AutoCloseStreams auto = new AutoCloseStreams()) {
            if (modularAssertionsScannerHelper.isModularAssertion(auto.add(new JarInputStream(new ByteArrayInputStream(bytes))))) {
                serverModuleFile.setModuleType(ModuleType.MODULAR_ASSERTION);
                serverModuleFile.setProperty(
                        ServerModuleFile.PROP_ASSERTIONS,
                        assertionsCollectionToCommaSeparatedString(
                                modularAssertionsScannerHelper.getAssertions(auto.add(new JarInputStream(new ByteArrayInputStream(bytes))))
                        )
                );
            } else if (customAssertionsScannerHelper.isCustomAssertion(auto.add(new JarInputStream(new ByteArrayInputStream(bytes))))) {
                serverModuleFile.setModuleType(ModuleType.CUSTOM_ASSERTION);
                serverModuleFile.setProperty(
                        ServerModuleFile.PROP_ASSERTIONS,
                        assertionsCollectionToCommaSeparatedString(
                                customAssertionsScannerHelper.getAssertions(auto.add(new JarInputStream(new ByteArrayInputStream(bytes))))
                        )
                );
            } else {
                throw new IOException(MessageFormat.format(resources.getString("error.cannot.determine.module.type"), trimFileName(fileName)));
            }
        }
        // set moduleFile props accordingly
        serverModuleFile.setProperty(ServerModuleFile.PROP_FILE_NAME, fileName);
        serverModuleFile.setProperty(ServerModuleFile.PROP_SIZE, String.valueOf(fileLength));

        // next create moduleFile data and verify that signature is valid

        // create module data (this will calculate module sha as well)
        serverModuleFile.createData(bytes, HexUtils.hexDump(digest), signaturePropsString);

        // make sure digest is properly set
        assert Arrays.equals(HexUtils.unHexDump(serverModuleFile.getModuleSha256()), digest);

        // finally verify module signature
        // Note this doesn't check whether the signer is trusted or not (a simple signature validation)
        try {
            SignerUtils.verifySignatureWithDigest(digest, signatureProps);
            // if all goes well return the moduleFile
            return serverModuleFile;
        } catch (final CertificateException | SignatureException | InvalidKeyException | NoSuchAlgorithmException e) {
            throw new IOException(MessageFormat.format(resources.getString("error.signature.verification"), trimFileName(fileName), ExceptionUtils.getMessage(e)), e);
        }
    }

    /**
     * Helper class holding module raw-bytes, digest (currently SHA-256) and signature properties, as both {@code Properties} and {@code String} objects.
     */
    static class ModuleBytesAndSignature {
        static class BytesAndDigest {
            @NotNull final byte[] bytes;
            @NotNull final byte[] digest;

            private BytesAndDigest(@NotNull final byte[] bytes, @NotNull final byte[] digest) {
                this.bytes = bytes;
                this.digest = digest;
            }
        }
        static class Signature {
            @NotNull final String signaturePropsString;
            @NotNull final Properties signatureProps;

            private Signature(@NotNull final String signaturePropsString, @NotNull final Properties signatureProps) {
                this.signaturePropsString = signaturePropsString;
                this.signatureProps = signatureProps;
            }
        }

        @NotNull final BytesAndDigest bytesAndDigest;
        @NotNull final Signature signature;

        private ModuleBytesAndSignature(@NotNull final BytesAndDigest bytesAndDigest, @NotNull final Signature signature) {
            this.bytesAndDigest = bytesAndDigest;
            this.signature = signature;
        }

        public static BytesAndDigest bytes(@NotNull final byte[] bytes, @NotNull final byte[] digest) {
            return new BytesAndDigest(bytes, digest);
        }
        public static Signature signature(@NotNull final String signature, @NotNull final Properties signatureProps) {
            return new Signature(signature, signatureProps);
        }
        public static ModuleBytesAndSignature bytesAndSignature(@NotNull final BytesAndDigest bytes, @NotNull final Signature signature) {
            return new ModuleBytesAndSignature(bytes, signature);
        }
    }

    /**
     * Utility method for walking through the signed zip content and extracting module raw-bytes,
     * digest (currently SHA-256) and signature properties, as both {@code Properties} and {@code String} objects.
     *
     * @param moduleStream    the signed zip file stream.  Required and cannot be {@code null}.
     * @param fileName        the signed module file-name.  Required and cannot be {@code null}.
     * @return a {@code ModuleBytesAndSignature} of raw-bytes, digest and signature properties (as both {@code Properties} and {@code String}), never {@code null}.
     * @throws IOException if an IO error occurs while walking through the signed zip file contents.
     */
    @NotNull
    private static ModuleBytesAndSignature getBytesAndSignatureFromSignedZip(@NotNull final InputStream moduleStream, @NotNull final String fileName) throws IOException {
        // get SHA-256 the message digest
        final MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (final NoSuchAlgorithmException e) {
            throw new IOException("Missing SHA-256 digest algorithm", e);
        }

        // walk through the signed zip and extract module raw bytes, module digest (SHA-256 encoded) and signature properties as both Properties and String
        final Pair<ModuleBytesAndSignature.BytesAndDigest, ModuleBytesAndSignature.Signature> moduleBytesAndSignature =
                SignerUtils.walkSignedZip(
                        moduleStream,
                        new SignedZipVisitor<ModuleBytesAndSignature.BytesAndDigest, ModuleBytesAndSignature.Signature>() {
                            @Override
                            public ModuleBytesAndSignature.BytesAndDigest visitData(@NotNull final InputStream inputStream) throws IOException {
                                // calc SHA-256 of SIGNED_DATA_ZIP_ENTRY
                                final DigestInputStream dis = new DigestInputStream(inputStream, messageDigest);
                                final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                IOUtils.copyStream(dis, bos);
                                return ModuleBytesAndSignature.bytes(bos.toByteArray(), dis.getMessageDigest().digest());
                            }

                            @Override
                            public ModuleBytesAndSignature.Signature visitSignature(@NotNull final InputStream inputStream) throws IOException {
                                // first read the whole stream in memory, as we need to return Properties and String
                                // TODO: perhaps skip this unnecessary read if Properties.load() is guarantied to read the entire bytes
                                final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                // copy all to bos
                                IOUtils.copyStream(inputStream, bos);
                                final byte[] signatureBytes = bos.toByteArray();

                                // read the signature properties as string
                                final StringWriter writer = new StringWriter();
                                try (final BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(signatureBytes), StandardCharsets.ISO_8859_1))) {
                                    IOUtils.copyStream(reader, writer);
                                    writer.flush();
                                }

                                // read the signature Properties
                                final Properties signatureProperties = new Properties();
                                signatureProperties.load(new ByteArrayInputStream(signatureBytes));

                                return ModuleBytesAndSignature.signature(writer.toString(), signatureProperties);
                            }
                        },
                        true
                );

        // check whether we have both signed data and signature properties.
        final ModuleBytesAndSignature.BytesAndDigest bytes = moduleBytesAndSignature.left;
        final ModuleBytesAndSignature.Signature signature = moduleBytesAndSignature.right;

        // if we have both then return
        if (bytes != null && signature != null) {
            return ModuleBytesAndSignature.bytesAndSignature(bytes, signature);
        }

        // this shouldn't happen, but in case it does throw IOException
        if (bytes == null && signature == null) {
            throw new IOException(MessageFormat.format(resources.getString("error.zip.missing.bytes.and.signature.properties"), "Signed Data", "Signature Properties", trimFileName(fileName)));
        }
        throw new IOException(MessageFormat.format(resources.getString("error.zip.missing.bytes.or.signature.properties"), (bytes == null) ? "Signed Data" : "Signature Properties", trimFileName(fileName)));
    }

    /**
     * Utility method to convert a collection of assertion class names into a comma separated string with assertion names.
     */
    private static String assertionsCollectionToCommaSeparatedString(@NotNull final Collection<String> assertionClassNames) {
        final StringBuilder result = new StringBuilder();
        for (final String str : assertionClassNames) {
            result.append(str.substring(str.lastIndexOf(".") + 1)); // strip the package name
            result.append(",");
        }
        return result.length() > 0 ? result.substring(0, result.length() - 1): "";
    }

    /**
     * Utility method to truncate filename if its too long.
     */
    public static String trimFileName(@NotNull final String fileName) {
        return TextUtils.truncateStringAtEnd(fileName, 70);
    }

    /**
     * Helper class to collect multiple streams and close all of them when {@link #close()} is invoked.
     */
    static class AutoCloseStreams implements Closeable {
        private final Collection<InputStream> streams = new ArrayList<>(5); // 5 should be more than enough

        @Override
        public void close() throws IOException {
            for (final InputStream is : streams) {
                is.close();
            }
            streams.clear();
        }

        public <IS extends InputStream> IS add(@NotNull final IS is) {
            streams.add(is);
            return is;
        }
    }
}

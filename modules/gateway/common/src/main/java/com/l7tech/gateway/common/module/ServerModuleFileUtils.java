package com.l7tech.gateway.common.module;

import com.l7tech.gateway.common.security.signer.SignatureVerifier;
import com.l7tech.gateway.common.security.signer.SignatureVerifierHelper;
import com.l7tech.gateway.common.security.signer.SignerUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.TextUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.security.SignatureException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ResourceBundle;
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
    @NotNull
    private final SignatureVerifier signatureVerifier;

    public ServerModuleFileUtils(
            @NotNull final CustomAssertionsScannerHelper customAssertionsScannerHelper,
            @NotNull final ModularAssertionsScannerHelper modularAssertionsScannerHelper,
            @NotNull final SignatureVerifier signatureVerifier
    ) {
        this.customAssertionsScannerHelper = customAssertionsScannerHelper;
        this.modularAssertionsScannerHelper = modularAssertionsScannerHelper;
        this.signatureVerifier = signatureVerifier;
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
        try (final SignerUtils.SignedZipContent zipContent = new SignatureVerifierHelper(signatureVerifier).verifyZip(signedModuleStream)) {
            // if it went this far the module is trustworthy ... so create new server module file instance
            final ServerModuleFile serverModuleFile = new ServerModuleFile();

            // make sure the module is either modular or custom assertion
            try (final AutoCloseStreams auto = new AutoCloseStreams()) {
                if (modularAssertionsScannerHelper.isModularAssertion(auto.add(new JarInputStream(zipContent.getDataStream())))) {
                    serverModuleFile.setModuleType(ModuleType.MODULAR_ASSERTION);
                    serverModuleFile.setProperty(
                            ServerModuleFile.PROP_ASSERTIONS,
                            assertionsCollectionToCommaSeparatedString(
                                    modularAssertionsScannerHelper.getAssertions(auto.add(new JarInputStream(zipContent.getDataStream())))
                            )
                    );
                } else if (customAssertionsScannerHelper.isCustomAssertion(auto.add(new JarInputStream(zipContent.getDataStream())))) {
                    serverModuleFile.setModuleType(ModuleType.CUSTOM_ASSERTION);
                    serverModuleFile.setProperty(
                            ServerModuleFile.PROP_ASSERTIONS,
                            assertionsCollectionToCommaSeparatedString(
                                    customAssertionsScannerHelper.getAssertions(auto.add(new JarInputStream(zipContent.getDataStream())))
                            )
                    );
                } else {
                    throw new IOException(MessageFormat.format(resources.getString("error.cannot.determine.module.type"), trimFileName(fileName)));
                }
            }
            // get the actual raw bytes size
            long fileLength = zipContent.getDataSize();

            // set moduleFile props accordingly
            serverModuleFile.setProperty(ServerModuleFile.PROP_FILE_NAME, fileName);
            serverModuleFile.setProperty(ServerModuleFile.PROP_SIZE, String.valueOf(fileLength));

            // create module data (this will calculate module sha as well)
            serverModuleFile.createData(zipContent.getDataBytes(), HexUtils.hexDump(zipContent.getDataDigest()), zipContent.getSignaturePropertiesString());

            // make sure digest is properly set
            assert Arrays.equals(HexUtils.unHexDump(serverModuleFile.getModuleSha256()), zipContent.getDataDigest());

            // if all goes well return the moduleFile
            return serverModuleFile;
        } catch (final SignatureException e) {
            throw new IOException(MessageFormat.format(resources.getString("error.signature.verification"), trimFileName(fileName), ExceptionUtils.getMessage(e)), e);
        }
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

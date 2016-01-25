package com.l7tech.gateway.common.module;

import com.l7tech.gateway.common.security.signer.InnerPayloadFactory;
import com.l7tech.gateway.common.security.signer.SignerUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.PoolByteArrayOutputStream;
import com.l7tech.util.TextUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.jar.JarInputStream;

/**
 * {@link ServerModuleFile} payload.
 */
public final class ServerModuleFilePayload extends SignerUtils.SignedZip.InnerPayload {
    /**
     * Override {@link com.l7tech.gateway.common.security.signer.SignerUtils.SignedZip.InnerPayload#FACTORY} to avoid accidental usage from base.
     */
    @SuppressWarnings("UnusedDeclaration")
    @Nullable
    private static final InnerPayloadFactory<ServerModuleFilePayload> FACTORY = null;

    private static final ResourceBundle resources = ResourceBundle.getBundle(ServerModuleFilePayload.class.getName());

    @NotNull
    private final CustomAssertionsScannerHelper customAssertionsScannerHelper;
    @NotNull
    private final ModularAssertionsScannerHelper modularAssertionsScannerHelper;
    @NotNull
    private final String fileName;

    /**
     * Package access constructor, used by {@link com.l7tech.gateway.common.module.ServerModuleFilePayloadFactory}.
     */
    ServerModuleFilePayload(
            @NotNull final PoolByteArrayOutputStream dataStream,
            @NotNull final byte[] dataDigest,
            @NotNull final PoolByteArrayOutputStream signaturePropsStream,
            @NotNull final CustomAssertionsScannerHelper customAssertionsScannerHelper,
            @NotNull final ModularAssertionsScannerHelper modularAssertionsScannerHelper,
            @NotNull final String fileName
    ) {
        super(dataStream, dataDigest, signaturePropsStream);
        this.customAssertionsScannerHelper = customAssertionsScannerHelper;
        this.modularAssertionsScannerHelper = modularAssertionsScannerHelper;
        this.fileName = fileName;
    }

    // todo: add more constructors for unsigned zips/content

    /**
     * Helper class to collect multiple streams and close all of them when {@link #close()} is invoked.
     */
    private static class AutoCloseStreams implements Closeable {
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

    /**
     * Creates a new {@link ServerModuleFile} using the payload within this class.
     * <p/>
     * Parses module data i.e. detects module type, read module registered assertions and sets module properties.
     *
     * @return a new {@link ServerModuleFile} out of this class payload.
     * @throws IOException if an error happens while parsing module stream, module type cannot be determined etc.
     */
    @NotNull
    public ServerModuleFile create() throws IOException {
        // create new server module file instance
        final ServerModuleFile serverModuleFile = new ServerModuleFile();

        // make sure the module is either modular or custom assertion
        try (final AutoCloseStreams auto = new AutoCloseStreams()) {
            if (modularAssertionsScannerHelper.isModularAssertion(auto.add(new JarInputStream(getDataStream(), false)))) {
                serverModuleFile.setModuleType(ModuleType.MODULAR_ASSERTION);
                serverModuleFile.setProperty(
                        ServerModuleFile.PROP_ASSERTIONS,
                        assertionsCollectionToCommaSeparatedString(
                                modularAssertionsScannerHelper.getAssertions(auto.add(new JarInputStream(getDataStream())))
                        )
                );
            } else if (customAssertionsScannerHelper.isCustomAssertion(auto.add(new JarInputStream(getDataStream())))) {
                serverModuleFile.setModuleType(ModuleType.CUSTOM_ASSERTION);
                serverModuleFile.setProperty(
                        ServerModuleFile.PROP_ASSERTIONS,
                        assertionsCollectionToCommaSeparatedString(
                                customAssertionsScannerHelper.getAssertions(auto.add(new JarInputStream(getDataStream())))
                        )
                );
            } else {
                throw new IOException(MessageFormat.format(resources.getString("error.cannot.determine.module.type"), trimFileName(fileName)));
            }
        }

        // get the actual raw bytes size
        long fileLength = getDataSize();

        // set moduleFile props accordingly
        serverModuleFile.setProperty(ServerModuleFile.PROP_FILE_NAME, fileName);
        serverModuleFile.setProperty(ServerModuleFile.PROP_SIZE, String.valueOf(fileLength));

        // create module data (this will calculate module sha as well)
        serverModuleFile.createData(getDataBytes(), HexUtils.hexDump(getDataDigest()), getSignaturePropertiesString());

        // make sure digest is properly set
        assert Arrays.equals(HexUtils.unHexDump(serverModuleFile.getModuleSha256()), getDataDigest());

        // if all goes well return the moduleFile
        return serverModuleFile;
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
    @NotNull
    public static String trimFileName(@NotNull final String fileName) {
        return TextUtils.truncateStringAtEnd(fileName, 70);
    }
}

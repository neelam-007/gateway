package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.module.*;
import com.l7tech.gateway.common.security.signer.SignedZipVisitor;
import com.l7tech.gateway.common.security.signer.SignerUtils;
import com.l7tech.gui.util.FileChooserUtil;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.text.MessageFormat;
import java.util.*;
import java.util.jar.JarInputStream;
import java.util.zip.ZipInputStream;

/**
 * Utility class for choosing a Modular or Custom Assertion Module from the local File System.
 */
public class ServerModuleFileChooser {
    private static final ResourceBundle resources = ResourceBundle.getBundle(ServerModuleFileChooser.class.getName());

    /**
     * Values in server module config is expected to change infrequently (e.g. almost never).
     * We only need one instance per Policy Manager (if config does change, a Policy Manager restart is required).
     */
    @Nullable private static ServerModuleConfig SERVER_MODULE_CONFIG;

    private final CustomAssertionsScannerHelper customAssertionsScannerHelper;
    private final ModularAssertionsScannerHelper modularAssertionsScannerHelper;
    private final FileFilter signedFilesFilter;

    /**
     * The parent component of the dialog, can be {@code null}
     */
    @Nullable private final Component parent;

    /**
     * Default constructor.
     *
     * @param parent    The parent component of the dialog.  Optional.
     */
    public ServerModuleFileChooser(@Nullable final Component parent) {
        this.parent = parent;

        if (SERVER_MODULE_CONFIG == null) {
            SERVER_MODULE_CONFIG = Registry.getDefault().getClusterStatusAdmin().getServerModuleConfig();
        }

        this.customAssertionsScannerHelper = new CustomAssertionsScannerHelper(SERVER_MODULE_CONFIG.getCustomAssertionPropertyFileName());
        this.modularAssertionsScannerHelper = new ModularAssertionsScannerHelper(SERVER_MODULE_CONFIG.getModularAssertionManifestAssertionListKey());
        this.signedFilesFilter = buildFileFilter(Collections.singleton(".signed"), resources.getString("signed.files.filter.desc"));
    }

    /**
     * Convenient method without specifying a startup folder.
     *
     * @see #choose(String)
     */
    public ServerModuleFile choose() throws IOException {
        return choose(null);
    }

    /**
     * Will popup a browse dialog for choosing Modular or Custom Assertion module file.
     *
     * @param startupFolder    Starting directory of the folder, must exist on file system, otherwise it will be ignored.  Optional.
     * @return instance of new {@link ServerModuleFile} created from the chosen file.
     * @throws IOException if an error happens while reading the chosen file.
     */
    public ServerModuleFile choose(@Nullable final String startupFolder) throws IOException {
        final JFileChooser fc = FileChooserUtil.doWithJFileChooser(
                new FileChooserUtil.FileChooserUser() {
                    @Override
                    public void useFileChooser(@NotNull final JFileChooser fc) {
                        fc.setDialogTitle(resources.getString("dialog.title"));
                        fc.setDialogType(JFileChooser.OPEN_DIALOG);
                        fc.setMultiSelectionEnabled(false);
                        fc.setAcceptAllFileFilterUsed(true);
                        fc.addChoosableFileFilter(signedFilesFilter);
                        fc.setFileFilter(signedFilesFilter);
                        if (StringUtils.isNotBlank(startupFolder)) {
                            //noinspection ConstantConditions
                            final File currentDir = new File(startupFolder);
                            if (currentDir.isDirectory()) {
                                fc.setCurrentDirectory(currentDir);
                            }
                        }
                    }
                }
        );

        if (fc == null) {
            throw new IOException(resources.getString("error.create.file.chooser.dialog"));
        }

        int result = fc.showOpenDialog(parent);
        if (JFileChooser.APPROVE_OPTION != result) {
            return null;
        }

        final File file = fc.getSelectedFile();
        if (file == null) {
            return null;
        }

        return createFromFile(file);
    }

    /**
     * Helper class to collect multiple streams and close them all when {@link #close()} is invoked.
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

    /**
     * Utility function for creating a {@link ServerModuleFile} from the specified {@code file}.
     * <p/>
     * The method will create a new {@link ServerModuleFile} object and set its properties according to the specified {@code file}.<br/>
     * The method will determine whether the specified {@code file} is a modular and custom assertion and set the type accordingly.<br/>
     * A proper {@link com.l7tech.gateway.common.module.ServerModuleFileData} object will be created based on the {@code file} bytes.
     *
     * @param file    the specified file.  Required.
     * @throws IOException if an error happens while reading the specified {@code file}
     */
    private ServerModuleFile createFromFile(@NotNull final File file) throws IOException {
        // sanity check the file
        if (file.isDirectory() || !file.isFile()) {
            throw new IOException(MessageFormat.format(resources.getString("error.invalid.file"), file.getName()));
        }
        final long fileLength = file.length();
        if (fileLength == 0L) {
            throw new IOException(MessageFormat.format(resources.getString("error.cannot.determine.file.size"), file.getName()));
        }
        final long maxModuleFileSize = ServerModuleFileClusterPropertiesReader.getInstance().getModulesUploadMaxSize();
        if (maxModuleFileSize != 0 && fileLength > maxModuleFileSize) {
            throw new IOException(MessageFormat.format(resources.getString("error.file.size"), ServerModuleFile.humanReadableBytes(maxModuleFileSize)));
        }

        // get bytes and signature properties
        final Pair<byte[], String> bytesAndSignature = getBytesAndSignatureFromSignedZip(file);
        final byte[] bytes = bytesAndSignature.left;
        final String signatureProperties = bytesAndSignature.right;

        // if both module bytes and signature props have been extracted
        if (bytes != null && signatureProperties != null) {
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
                    throw new IOException(MessageFormat.format(resources.getString("error.cannot.determine.module.type"), file.getName()));
                }
            }
            // set moduleFile props accordingly
            serverModuleFile.setProperty(ServerModuleFile.PROP_FILE_NAME, file.getName());
            serverModuleFile.setProperty(ServerModuleFile.PROP_SIZE, String.valueOf(fileLength));

            // next create moduleFile data and verify that signature is valid

            // create module data (this will calculate module sha as well)
            serverModuleFile.createData(bytes, signatureProperties);

            // finally verify module signature
            try (final StringReader reader = new StringReader(serverModuleFile.getData().getSignatureProperties())) {
                final Properties sigProps = new Properties();
                sigProps.load(reader);
                SignerUtils.verifySignatureWithDigest(
                        HexUtils.unHexDump(serverModuleFile.getModuleSha256()),
                        sigProps
                );

                // if all goes well return the moduleFile
                return serverModuleFile;
            } catch (final CertificateException | SignatureException | InvalidKeyException | NoSuchAlgorithmException e) {
                throw new IOException(MessageFormat.format(resources.getString("error.signature.verification"), file.getName(), ExceptionUtils.getMessage(e)), e);
            }
        }

        // shouldn't happen though, just in case throw IOException
        if (bytes == null && signatureProperties == null) {
            throw new IOException(MessageFormat.format(resources.getString("error.zip.missing.bytes.and.signature.properties"), "Signed Data", "Signature Properties", file.getName()));
        }
        throw new IOException(MessageFormat.format(resources.getString("error.zip.missing.bytes.or.signature.properties"), (bytes == null) ? "Signed Data" : "Signature Properties", file.getName()));
    }

    /**
     * Utility method for walking through the signed zip content and extracting the raw-bytes and signature properties {@code String}.
     *
     * @param file    the signed zip file.  Required and cannot be {@code null}.
     * @return a {@code Pair} of raw-bytes and signature properties {@code String}, never {@code null}.
     * @throws IOException if an IO error occurs while walking through the signed zip file contents.
     */
    private static Pair<byte[], String> getBytesAndSignatureFromSignedZip(@NotNull final File file) throws IOException {
        return SignerUtils.walkSignedZip(
                file,
                new SignedZipVisitor<byte[], String>() {
                    @Override
                    public byte[] visitData(@NotNull final ZipInputStream zis) throws IOException {
                        return IOUtils.slurpStream(zis);
                    }

                    @Override
                    public String visitSignature(@NotNull final ZipInputStream zis) throws IOException {
                        // read the signature properties
                        final StringWriter writer = new StringWriter();
                        try (
                                final BufferedReader reader = new BufferedReader(
                                        new InputStreamReader(
                                                new FilterInputStream(zis) {
                                                    @Override public void close() throws IOException {
                                                        // don't close the zip input stream
                                                    }
                                                },
                                                StandardCharsets.ISO_8859_1
                                        )
                                )
                        ) {
                            IOUtils.copyStream(reader, writer);
                            writer.flush();
                        }
                        return writer.toString();
                    }
                },
                true
        );
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
     * Utility method to create a file filter given a list of file extensions and a description.
     *
     * @param extensions     a list of file extensions such as ".jar" or ".aar" etc.
     * @param description    a file-type description pattern, having one argument for the comma separated extension list.
     * @return a {@link FileFilter} object.
     */
    private static FileFilter buildFileFilter(@NotNull final Collection<String> extensions, @NotNull final String description) {
        return new FileFilter() {
            @Override
            public boolean accept(@NotNull final File f) {
                if (f.isDirectory()) {
                    return true;
                } else {
                    for (final String extension : extensions) {
                        if (f.getName().toLowerCase().endsWith(extension)) {
                            return true;
                        }
                    }
                    return false;
                }
            }

            private String desc;
            @Override
            public String getDescription() {
                if (this.desc == null) {
                    this.desc = MessageFormat.format(description, joinAndPrependIfMissing(extensions.iterator(), ";", "*"));
                }
                return this.desc;
            }

            private String joinAndPrependIfMissing(final Iterator<String> iterator, final String separator, final String prefix) {
                if (iterator == null) {
                    return null;
                }
                if (!iterator.hasNext()) {
                    return StringUtils.EMPTY;
                }
                final String first = iterator.next();
                if (!iterator.hasNext()) {
                    return prependIfMissing(first, prefix);
                }

                final StringBuilder buf = new StringBuilder(256).append(prependIfMissing(first, prefix));
                while (iterator.hasNext()) {
                    if (StringUtils.isNotBlank(separator)) {
                        buf.append(separator);
                    }
                    buf.append(prependIfMissing(iterator.next(), prefix));
                }
                return buf.toString();
            }

            private String prependIfMissing(final String str, final String prefix) {
                if (StringUtils.isNotBlank(str)) {
                    if (StringUtils.isNotBlank(prefix)) {
                        return (StringUtils.startsWithIgnoreCase(str, prefix)) ? str : prefix + str;
                    } else {
                        return str;
                    }
                }
                return StringUtils.EMPTY;
            }
        };
    }
}

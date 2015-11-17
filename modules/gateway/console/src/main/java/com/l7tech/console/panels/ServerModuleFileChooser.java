package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.module.*;
import com.l7tech.gui.util.FileChooserUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;

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
    @NotNull private final ServerModuleFileUtils moduleFileUtils;

    private final FileFilter customAssertionFileFilter;
    private final FileFilter modularAssertionFileFilter;
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

        this.moduleFileUtils = new ServerModuleFileUtils(
                new CustomAssertionsScannerHelper(SERVER_MODULE_CONFIG.getCustomAssertionPropertyFileName()),
                new ModularAssertionsScannerHelper(SERVER_MODULE_CONFIG.getModularAssertionManifestAssertionListKey()),
                Registry.getDefault().getSignatureVerifierAdmin()
        );
        this.customAssertionFileFilter = buildFileFilter(SERVER_MODULE_CONFIG.getCustomAssertionModulesExt(), resources.getString("custom.assertion.file.filter.desc"));
        this.modularAssertionFileFilter = buildFileFilter(SERVER_MODULE_CONFIG.getModularAssertionModulesExt(), resources.getString("modular.assertion.file.filter.desc"));
        this.signedFilesFilter = buildFileFilter(Arrays.asList(".saar", ".sjar"), resources.getString("signed.files.filter.desc"));
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
                        fc.addChoosableFileFilter(modularAssertionFileFilter);
                        fc.addChoosableFileFilter(customAssertionFileFilter);
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
            throw new IOException(MessageFormat.format(resources.getString("error.invalid.file"), ServerModuleFileUtils.trimFileName(file.getName())));
        }
        // get the .signed file size
        long fileLength = file.length();
        if (fileLength == 0L) {
            throw new IOException(MessageFormat.format(resources.getString("error.cannot.determine.file.size"), ServerModuleFileUtils.trimFileName(file.getName())));
        }
        // the max-size check is done against the .signed file size, instead of the raw bytes.
        // the reason is that this way we can early fail if the input file is huge
        // the .signed bytes size and raw bytes size should be very similar
        final long maxModuleFileSize = ServerModuleFileClusterPropertiesReader.getInstance().getModulesUploadMaxSize();
        if (maxModuleFileSize != 0 && fileLength > maxModuleFileSize) {
            throw new IOException(MessageFormat.format(resources.getString("error.file.size"), ServerModuleFile.humanReadableBytes(maxModuleFileSize)));
        }

        return moduleFileUtils.createFromFile(file);
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

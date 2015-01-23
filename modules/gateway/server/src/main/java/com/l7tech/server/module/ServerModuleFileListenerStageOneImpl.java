package com.l7tech.server.module;

import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.gateway.common.module.ModuleState;
import com.l7tech.gateway.common.module.ModuleType;
import com.l7tech.gateway.common.module.ServerModuleFile;
import com.l7tech.gateway.common.module.ServerModuleFileState;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.event.system.ServerModuleFileSystemEvent;
import com.l7tech.server.policy.ServerAssertionRegistry;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Phase One.
 * <p/>
 * This class will provide solution without the OS Service i.e. without a acceptance-rejection mechanism.<br/>
 * All modules are accepted by default and the Gateway process would need to have write permission to the modules folder in order to deploy them.<br/>
 * Otherwise the modules will be copied into the staging folder awaiting future acceptance and installation, once the OS Service is put in place.
 */
public class ServerModuleFileListenerStageOneImpl extends ServerModuleFileListener {
    private static final Logger logger = Logger.getLogger(ServerModuleFileListenerStageOneImpl.class.getName());
    private static final ResourceBundle resources = ResourceBundle.getBundle(ServerModuleFileListenerStageOneImpl.class.getName());

    @Nullable private File stagingRootDir = null;
    @Nullable private File stagingTempDir = null;
    @Nullable private File stagingModularDir = null;
    @Nullable private File stagingCustomDir = null;

    /**
     * @see ServerModuleFileListener#ServerModuleFileListener(com.l7tech.server.util.ApplicationEventProxy, ServerModuleFileManager, org.springframework.transaction.PlatformTransactionManager, com.l7tech.util.Config, com.l7tech.server.policy.ServerAssertionRegistry, com.l7tech.gateway.common.custom.CustomAssertionsRegistrar)
     */
    public ServerModuleFileListenerStageOneImpl(
            @NotNull final ApplicationEventProxy eventProxy,
            @NotNull final ServerModuleFileManager serverModuleFileManager,
            final PlatformTransactionManager transactionManager,
            @NotNull final Config config,
            @NotNull final ServerAssertionRegistry modularAssertionRegistrar,
            @NotNull final CustomAssertionsRegistrar customAssertionRegistrar
    ) {
        super(eventProxy, serverModuleFileManager, transactionManager, config, modularAssertionRegistrar, customAssertionRegistrar);
    }

    @Override
    protected void onModuleChanged(@NotNull final ServerModuleFile moduleFile) throws ModuleStagingException {
        // do not process Module Change if upload is disabled
        if (!serverModuleFileManager.isModuleUploadEnabled()) {
            return;
        }

        // for debug purposes
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Module changed; goid \"" + moduleFile.getGoid() + "\", type \"" + moduleFile.getModuleType() + "\", name \"" + moduleFile.getName() + "\", file-name \"" + moduleFile.getProperty(ServerModuleFile.PROP_FILE_NAME) + "\"");
        }

        final ServerModuleFileState state = serverModuleFileManager.findStateForCurrentNode(moduleFile);
        // process only modules not having any state for this node
        // or modules with UPLOADED state for this node (having no errors)
        if (state == null || (StringUtils.isBlank(state.getErrorMessage()) && ModuleState.UPLOADED == state.getState())) {
            // for debug purposes
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, state == null ? "Module goid \"" + moduleFile.getGoid() + "\", state \"null\"" : "Module goid \"" + moduleFile.getGoid() + "\", state \"" + state.getState() + "\", error-message \"" + state.getErrorMessage() + "\"");
            }

            final ModuleType moduleType = moduleFile.getModuleType();
            if (ModuleType.MODULAR_ASSERTION == moduleType) {
                installModule(moduleFile, getModularAssertionDeployPath(), getModularAssertionStagingPath());
            } else if (ModuleType.CUSTOM_ASSERTION == moduleType) {
                installModule(moduleFile, getCustomAssertionDeployPath(), getCustomAssertionStagingPath());
            } else {
                throw new UnsupportedModuleTypeException(moduleType);
            }
        }
    }

    @Override
    protected void onModuleDeleted(@NotNull final ServerModuleFile moduleFile) throws ModuleStagingException {
        // do not process Module Delete if upload is disabled
        if (!serverModuleFileManager.isModuleUploadEnabled()) {
            return;
        }

        // for debug purposes
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Module deleted; goid \"" + moduleFile.getGoid() + "\", type \"" + moduleFile.getModuleType() + "\", name \"" + moduleFile.getName() + "\", file-name \"" + moduleFile.getProperty(ServerModuleFile.PROP_FILE_NAME) + "\"");
        }

        final ModuleType moduleType = moduleFile.getModuleType();
        if (ModuleType.MODULAR_ASSERTION == moduleType) {
            uninstallModule(moduleFile, getModularAssertionDeployPath(), getModularAssertionStagingPath());
        } else if (ModuleType.CUSTOM_ASSERTION == moduleType) {
            uninstallModule(moduleFile, getCustomAssertionDeployPath(), getCustomAssertionStagingPath());
        } else {
            throw new UnsupportedModuleTypeException(moduleType);
        }
    }

    /**
     * Will try to install the specified module, having its corresponding staging and deploy folders.
     * <p/>
     * The module is first downloaded, from the DB, in a staging temporary folder (needed for atomic move later on).<br/>
     * Next depending whether the Gateway process has write permission into the deploy folder, the module will be
     * atomically moved into the deploy folder, otherwise the module will be atomically moved into the staging folder.<br/>
     * Finally, upon successfully move the temporary file will be deleted.
     *
     * @param moduleFile     the module to install.  Required and cannot be {@link null}
     * @param deployPath     the module deploy folder, depending on its type (modular or custom assertion).  Required and cannot be {@link null}
     * @param stagingPath    the module staging folder, depending on its type (modular or custom assertion).  Required and cannot be {@link null}
     * @throws ModuleStagingException if an error happens while downloading and moving the module into appropriate staging or deploy folder.
     */
    private void installModule(
            @NotNull final ServerModuleFile moduleFile,
            @NotNull final String deployPath,
            @NotNull final String stagingPath
    ) throws ModuleStagingException {
        // for debug purposes
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Install module goid \"" + moduleFile.getGoid() + "\", deployPath \"" + deployPath + "\", stagingPath \"" + stagingPath + "\"");
        }

        // audit installation start
        logAndAudit(ServerModuleFileSystemEvent.Action.INSTALLING, moduleFile);

        // get module file name
        final String moduleFileName = getModuleFileName(moduleFile);

        // download the module from DB into the staging temporary folder
        final File tmpModuleFile = downloadModule(moduleFile, getStagingTemporaryDir());
        try {
            // check whether the deploy directory is writable
            if (isDirectoryWritable(new File(deployPath))) {
                // move downloaded module into the modules deploy folder
                moveModule(tmpModuleFile, new File(deployPath + File.separator + moduleFileName));
                updateModuleState(moduleFile.getGoid(), ModuleState.DEPLOYED);
                try {
                    // just in case there is a module inside the staging folder remove it
                    deleteModule(new File(stagingPath + File.separator + moduleFileName));
                } catch (final IOException ignore) { /* ignore */ }
                // audit deploying success
                logAndAudit(ServerModuleFileSystemEvent.Action.INSTALL_DEPLOYED, moduleFile);
            } else {
                // audit no modules deploy folder permissions
                logAndAudit(ServerModuleFileSystemEvent.Action.DEPLOY_PERMISSION, moduleFile);

                moveModule(tmpModuleFile, new File(stagingPath + File.separator + moduleFileName));
                updateModuleState(moduleFile.getGoid(), ModuleState.STAGED);

                // audit staging success
                logAndAudit(ServerModuleFileSystemEvent.Action.INSTALL_STAGED, moduleFile);
            }
        } finally {
            try {
                FileUtils.delete(tmpModuleFile);
            } catch (final IOException e) {
                logger.log(Level.WARNING, "Failed to remove temporary module file: " + tmpModuleFile, ExceptionUtils.getDebugException(e));
            }
        }
    }

    /**
     * Will try to download the file from the DB and save it into the specified staging temp folder.
     * <p/>
     * A temporary file will be created, with a deleteOnExit. On error the file will be deleted.
     *
     * @param moduleFile        the module to download.  Required and cannot be {@code null}.
     * @param stagingTempDir    the staging folder.  Required and cannot be {@code null}.
     * @return a temporary {@link File}, with a deleteOnExit flag set, holding the location of the downloaded module,
     * having a name prefixed as the module file-name (with extension removed) and suffixed with {@code .tmp}.
     * @throws ModuleStagingException if an error occurs while downloading the file from DB.
     */
    @NotNull
    private File downloadModule(
            @NotNull final ServerModuleFile moduleFile,
            @NotNull final File stagingTempDir
    ) throws ModuleStagingException {
        // for debug purposes
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Downloading module; goid \"" + moduleFile.getGoid() + "\", stagingTempDir \"" + stagingTempDir + "\"");
        }

        File tmpModuleFile = null;
        try {
            tmpModuleFile = File.createTempFile(stripExtension(getModuleFileName(moduleFile)), ".tmp", stagingTempDir);
            tmpModuleFile.deleteOnExit();
            try (final FileOutputStream out = new FileOutputStream(tmpModuleFile)) {
                out.write(getModuleDataBytes(moduleFile));
                out.flush();
            }

            // for debug purposes
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Module Downloaded Successfully; goid \"" + moduleFile.getGoid() + "\", temporary file-name is \"" + tmpModuleFile.getPath() + "\"");
            }

            return tmpModuleFile;
        } catch (final IOException e) {
            if (tmpModuleFile != null) {
                try {
                    FileUtils.delete(tmpModuleFile);
                } catch (final IOException ex) {
                    logger.log(Level.WARNING, "Failed to remove temporary module file: " + tmpModuleFile, ExceptionUtils.getDebugException(ex));
                }
            }
            throw new ModuleDownloadException(e);
        }
    }

    /**
     * Will try to atomically move specified, temporary downloaded, module file into the specified destination file.<br/>
     * The destination file is computed by combining the module destination folder and the module file.
     * <p/>
     * In order to accomplish atomic operation, if the destination module file already exists, it will be deleted first and
     * then the temporary downloaded module file will be moved into the specified destination file.
     * <p/>
     * Note on Windows platforms: If the destination module already exists and is targeting file inside deploy folder, rather then staging,
     * then delete operation will fail due to mandatory jar locking by JVM (having the module beaing loaded by the Gateway).<br/>
     * This means that this method will always throw {@link ModuleMoveException} on Window platform (under the above conditions, that is).
     *
     * @param tmpModuleFile     the temporary downloaded module file.  Required and cannot be {@code null}
     * @param destModuleFile    the destination file.  Required and cannot be {@code null}
     * @throws ModuleMoveException if an error happens while either deleteing the destination file (if already exists)
     * or moving the temporary downloaded file into the destination file.
     */
    @SuppressWarnings("SpellCheckingInspection")
    private void moveModule(
            @NotNull final File tmpModuleFile,
            @NotNull final File destModuleFile
    ) throws ModuleMoveException {
        // for debug purposes
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Moving module; source \"" + tmpModuleFile.getPath() + "\", dest \"" + destModuleFile.getPath() + "\"");
        }

        try {
            if (destModuleFile.exists()) {
                if (!destModuleFile.isFile()) {
                    throw new IOException(MessageFormat.format(resources.getString("error.install.move.destination.not.regular.file"), destModuleFile.getPath()));
                }

                // for debug purposes
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Destination module exist; Deleting file \"" + destModuleFile.getPath() + "\"");
                }

                FileUtils.delete(destModuleFile);
            }

            // for debug purposes
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Renaming module; source \"" + tmpModuleFile.getPath() + "\", dest \"" + destModuleFile.getPath() + "\"");
            }

            FileUtils.rename(tmpModuleFile, destModuleFile);
        } catch (final IOException e) {
            throw new ModuleMoveException(e);
        }
    }

    /**
     * Will try to uninstall the specified module module.
     * <p/>
     * If the Gateway process has write permission into the deploy folder then the module will be removed from the
     * modules deploy folder. In any case the file will also be attempted to be deleted from the staging folder,
     * as the entity is no longer present in the DB.<br/>
     * Note that, due to JVM mandatory jar locking, this will always fail under Window platform,
     * in case when the module is loaded by the Gateway.
     *
     * @param moduleFile        server module file.  Required and cannot be {@code null}
     * @param deployPath        the module deploy folder, depending on its type (modular or custom assertion).  Required and cannot be {@link null}
     * @param stagingPath       the module staging folder, depending on its type (modular or custom assertion).  Required and cannot be {@link null}
     * @throws ModuleUninstallException if an IO error occurs while deleting the module.
     */
    private void uninstallModule(
            @NotNull final ServerModuleFile moduleFile,
            @NotNull final String deployPath,
            @NotNull final String stagingPath
    ) throws ModuleStagingException {
        // extract module file-name
        final String moduleFileName = getModuleFileName(moduleFile);

        // for debug purposes
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Uninstalling module; file-name \"" + moduleFileName + "\", deployPath \"" + deployPath + "\", stagingPath \"" + stagingPath + "\"");
        }

        // audit un-installation start
        logAndAudit(ServerModuleFileSystemEvent.Action.UNINSTALLING, moduleFile);

        try {
            // check whether the deploy directory is writable
            if (isDirectoryWritable(new File(deployPath))) {
                deleteModule(new File(deployPath + File.separator + moduleFileName));
            }
            deleteModule(new File(stagingPath + File.separator + moduleFileName));
        } catch (final IOException e) {
            throw new ModuleUninstallException(moduleFileName, e);
        }

        // if it goes this far its a success
        // audit un-installation success
        logAndAudit(ServerModuleFileSystemEvent.Action.UNINSTALL_SUCCESS, moduleFile);
    }

    /**
     * Utility method for deleting the specified {@link File file} from the file system.<br/>
     * No operation will be executed if the specified file doesn't exist or is not a regular file.
     *
     * @param file    the file to delete.  Required and cannot be {@code null}
     */
    private void deleteModule(@NotNull final File file) throws IOException {
        // for debug purposes
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Deleting module file \"" + file.getPath() + "\"");
        }

        if (file.exists() && file.isFile()) {
            if (!file.delete()) {
                throw new IOException(MessageFormat.format(resources.getString("error.delete.module.fail"), file.getPath()));
            }

            // for debug purposes
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Module Deleted Successfully; file \"" + file.getPath() + "\"");
            }
        }
    }

    /**
     * Get the specified server module file-name trimmed.
     *
     * @param serverModuleFile    the specified server module file.  Required and cannot be {@code null}
     * @return a {@code String} containing the module file-name trimmed.  Never {@code null}.
     * @throws ModuleMissingFileNameException if the module file-name property is either missing or empty.
     */
    @NotNull
    private String getModuleFileName(@NotNull final ServerModuleFile serverModuleFile) throws ModuleMissingFileNameException {
        final String moduleFileName = serverModuleFile.getProperty(ServerModuleFile.PROP_FILE_NAME);
        if (StringUtils.isBlank(moduleFileName)) {
            throw new ModuleMissingFileNameException();
        }
        return moduleFileName.trim();
    }

    /**
     * Get the specified server module file data-bytes.
     * <p/>
     * Taking in count that {@link ServerModuleFile#data} is lazy fetched,
     * this method will throw {@link org.hibernate.LazyInitializationException LazyInitializationException} if the session
     * which generated {@code serverModuleFile} is closed or there was no session to begin with. <br/>
     * To ensure proper session is being withhold, {@link #onModuleChanged(com.l7tech.gateway.common.module.ServerModuleFile)}
     * and {@link #onModuleDeleted(com.l7tech.gateway.common.module.ServerModuleFile)} are executed within a transaction
     * (i.e. wrapped around with {@link #transactionIfAvailable(Runnable)}).<br/>
     * If the above is changed consider modifying the logic.
     *
     * @param serverModuleFile    the specified server module file.  Required and cannot be {@code null}
     * @return byte array holding the module bytes.  Never {@code null}
     * @throws ModuleMissingBytesException if the specified module doesn't have bytes.
     */
    @NotNull
    private byte[] getModuleDataBytes(@NotNull final ServerModuleFile serverModuleFile) throws ModuleMissingBytesException {
        final byte[] bytes = serverModuleFile.getData().getDataBytes();
        if (bytes == null || !(bytes.length > 0)) {
            throw new ModuleMissingBytesException();
        }
        return bytes;
    }

    /**
     * Helper method for getting Modular Assertions Deploy Directory path.
     *
     * @return a {@code String} having Modular Assertions deploy directory path.  Never {@code null}
     * @throws ConfigException if an I/O error occurs while constructing directory canonical path.
     */
    @NotNull
    private String getModularAssertionDeployPath() throws ConfigException {
        final String moduleFolder = config.getProperty(ServerConfigParams.PARAM_MODULAR_ASSERTIONS_DIRECTORY);
        if (StringUtils.isBlank(moduleFolder)) {
            throw new ConfigException(resources.getString("error.deploy.modular.dir.empty.or.not.configured"));
        }
        try {
            return new File(moduleFolder).getCanonicalPath();
        } catch (final IOException e) {
            throw new ConfigException(resources.getString("error.deploy.modular.dir.invalid"), e);
        }
    }

    /**
     * Helper method for getting Custom Assertions Deploy Directory path.
     *
     * @return a {@code String} having Custom Assertions deploy directory path.  Never {@code null}
     * @throws ConfigException if an I/O error occurs while constructing directory canonical path.
     */
    @NotNull
    private String getCustomAssertionDeployPath() throws ConfigException {
        final String moduleFolder = config.getProperty(ServerConfigParams.PARAM_CUSTOM_ASSERTIONS_MODULES_DIRECTORY);
        if (StringUtils.isBlank(moduleFolder)) {
            throw new ConfigException(resources.getString("error.deploy.custom.dir.empty.or.not.configured"));
        }
        try {
            return new File(moduleFolder.trim()).getCanonicalPath();
        } catch (final IOException e) {
            throw new ConfigException(resources.getString("error.deploy.custom.dir.invalid"), e);
        }
    }

    /**
     * Lazy get configured Modular Assertions Staging Directory.<br/>
     * This method ensures that the Modular Assertion Staging Directory is properly configured, exists in the File System and is
     * writable by the Gateway process.
     *
     * @return The Configured Modular Assertions Staging Directory.  Never {@code null}
     * @throws ConfigException if {@link #getRootStagingDir()} throws, Modular Assertions Staging Directory is not writable
     * by the Gateway process or an I/O error occurs while constructing directory canonical path.
     * @see #getRootStagingDir()
     */
    @NotNull
    private String getModularAssertionStagingPath() throws ConfigException {
        if (this.stagingModularDir == null) {
            final File stagingModularDir;
            try {
                stagingModularDir = new File(getRootStagingDir().getPath() + File.separator + MODULAR_MODULES_DIR).getCanonicalFile();
            } catch (final IOException e) {
                throw new ConfigException(resources.getString("error.staging.modular.dir.invalid"), e);
            }
            if (!(stagingModularDir.exists() && stagingModularDir.isDirectory())) {
                throw new ConfigException(MessageFormat.format(resources.getString("error.staging.modular.dir.does.not.exist"), stagingModularDir));
            }
            if (!isDirectoryWritable(stagingModularDir)) {
                throw new ConfigException(resources.getString("error.staging.modular.dir.not.writable"));
            }
            this.stagingModularDir = stagingModularDir;
        }
        return this.stagingModularDir.getPath();
    }

    /**
     * Lazy get configured Custom Assertions Staging Directory.<br/>
     * This method ensures that the Custom Assertion Staging Directory is properly configured, exists in the File System and is
     * writable by the Gateway process.
     *
     * @return The Configured Custom Assertions Staging Directory.  Never {@code null}
     * @throws ConfigException if {@link #getRootStagingDir()} throws, Custom Assertions Staging Directory is not writable
     * by the Gateway process or an I/O error occurs while constructing directory canonical path.
     * @see #getRootStagingDir()
     */
    @NotNull
    private String getCustomAssertionStagingPath() throws ConfigException {
        if (this.stagingCustomDir == null) {
            final File stagingCustomDir;
            try {
                stagingCustomDir = new File(getRootStagingDir().getPath() + File.separator + CUSTOM_MODULES_DIR).getCanonicalFile();
            } catch (final IOException e) {
                throw new ConfigException(resources.getString("error.staging.custom.dir.invalid"), e);
            }
            if (!(stagingCustomDir.exists() && stagingCustomDir.isDirectory())) {
                throw new ConfigException(MessageFormat.format(resources.getString("error.staging.custom.dir.does.not.exist"), stagingCustomDir));
            }
            if (!isDirectoryWritable(stagingCustomDir)) {
                throw new ConfigException(resources.getString("error.staging.custom.dir.not.writable"));
            }
            this.stagingCustomDir = stagingCustomDir;
        }
        return this.stagingCustomDir.getPath();
    }

    /**
     * Lazy create Staging Temporary Directory.<br/>
     * This method will crate the Staging Temporary Directory (if not already created).
     * This directory is used to "download" uploaded modules before moving them into their destination directory
     * (that being the deploy or staging propitiatory directory).
     *
     * @return the Staging Temporary directory.  Never {@code null}
     * @throws ConfigException if {@link #getRootStagingDir()} throws, generated Staging Temporary Directory is not writable
     * by the Gateway process or an I/O error occurs while constructing directory canonical path.
     * @see #getRootStagingDir()
     */
    @NotNull
    private File getStagingTemporaryDir() throws ConfigException {
        if (this.stagingTempDir == null) {
            final File stagingTempDir;
            try {
                stagingTempDir = FileUtils.createTempDirectory("modules", "temp", getRootStagingDir(), true).getCanonicalFile();
            } catch (final IOException e) {
                throw new ConfigException(resources.getString("unhandled.error.staging.dir"), e);
            }
            if (!(stagingTempDir.exists() && stagingTempDir.isDirectory())) {
                throw new ConfigException(MessageFormat.format(resources.getString("error.staging.tmp.dir.does.not.exist"), stagingTempDir));
            }
            if (!isDirectoryWritable(stagingTempDir)) {
                throw new ConfigException(resources.getString("error.staging.tmp.dir.not.writable"));
            }
            this.stagingTempDir = stagingTempDir;
        }
        return this.stagingTempDir;
    }

    /**
     * Lazy get configured Root Staging Directory.<br/>
     * This method ensures that the Root Staging Directory is properly configured, exists in the File System and is
     * writable by the Gateway process.
     *
     * @return The Configured Root Staging Directory.  Never {@code null}
     * @throws ConfigException if Staging Directory is not configured, doesn't exist or is not writable by the Gateway process.
     * Additionally if an I/O error occurs while constructing directory canonical path.
     */
    @NotNull
    private File getRootStagingDir() throws ConfigException {
        if (this.stagingRootDir == null) {
            final String rootStagingPath = getRootStagingPath();
            if (StringUtils.isBlank(rootStagingPath)) {
                throw new ConfigException(resources.getString("error.staging.dir.empty.or.not.configured"));
            }
            final File rootStagingDir;
            try {
                rootStagingDir = new File(rootStagingPath.trim()).getCanonicalFile();
            } catch (final IOException e) {
                throw new ConfigException(resources.getString("error.staging.dir.invalid"), e);
            }
            if (!(rootStagingDir.exists() && rootStagingDir.isDirectory())) {
                throw new ConfigException(MessageFormat.format(resources.getString("error.staging.dir.does.not.exist"), rootStagingDir));
            }
            if (!isDirectoryWritable(rootStagingDir)) {
                throw new ConfigException(MessageFormat.format(resources.getString("error.staging.dir.not.writable"), rootStagingDir));
            }
            this.stagingRootDir = rootStagingDir;
        }
        return this.stagingRootDir;
    }

    /**
     * Determine whether the specified directory is writable or not.
     *
     * @param dir    Directory to check.  Required and cannot be {@code null}
     * @return {@code true} if and only if the {@code dir} exists, is directory and the application is allowed to write to the directory; {@code false} otherwise.
     */
    static boolean isDirectoryWritable(final File dir) {
        if (dir == null) throw new IllegalArgumentException("dir cannot be null");
        boolean isWritable = false;
        try {
            isWritable = dir.exists() && dir.isDirectory() && dir.canWrite();
        } catch (final Throwable ex) {
            logger.log(Level.WARNING, "Error while determining whether directory \"" + dir + "\" is writable: " + ExceptionUtils.getMessage(ex), ExceptionUtils.getDebugException(ex));
        }
        // for debug purposes
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "isDirectoryWritable(" + dir.getPath() + "): " + isWritable);
        }
        return isWritable;
    }

    /**
     * Helper method to remove the extension from the specified {@code fileName}.<br/>
     * This method returns the textual part of the {@code fileName} before the last dot.
     *
     * @param fileName    the filename to query.  Required and cannot be {@code null}.
     * @return the {@code fileName} minus the extension.  Never {@code null}
     */
    @NotNull
    static String stripExtension(@NotNull String fileName) {
        int pos = fileName.lastIndexOf(".");
        if (pos > 0) {
            fileName = fileName.substring(0, pos);
        }
        return fileName;
    }


    // --- Exceptions ---

    /**
     * Indicates Invalid Server Module File, file-name property is missing.
     */
    @SuppressWarnings("serial")
    class ModuleMissingFileNameException extends ModuleStagingException {
        ModuleMissingFileNameException() {
            super(MessageFormat.format(resources.getString("error.invalid.module.property"), ServerModuleFile.PROP_FILE_NAME));
        }
    }

    /**
     * Indicates Invalid Server Module File, bytes are empty.
     */
    @SuppressWarnings("serial")
    class ModuleMissingBytesException extends ModuleStagingException {
        ModuleMissingBytesException() {
            super(resources.getString("error.invalid.module.bytes"));
        }
    }

    /**
     * Indicates Invalid Server Module File, unsupported type.
     */
    @SuppressWarnings("serial")
    class UnsupportedModuleTypeException extends ModuleStagingException {
        UnsupportedModuleTypeException(@NotNull final ModuleType moduleType) {
            super(MessageFormat.format(resources.getString("error.unsupported.module.type"), moduleType.toString()));
        }
    }

    /**
     * Indicates Configuration error, could be due to misconstrued staging or deploy folders.
     */
    @SuppressWarnings("serial")
    class ConfigException extends ModuleStagingException {
        ConfigException (@NotNull final  String message) {
            super(message);
        }
        ConfigException (@NotNull final  String message, @NotNull final  Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Indicates an error happen while trying to install module.
     */
    @SuppressWarnings("serial")
    class ModuleInstallException extends ModuleStagingException {
        ModuleInstallException(@NotNull final String message, @NotNull final Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Indicates an error happen while downloading the module bytes from the DB into the staging temporary folder.
     */
    @SuppressWarnings("serial")
    class ModuleDownloadException extends ModuleInstallException {
        ModuleDownloadException(@NotNull final Throwable cause) {
            super(resources.getString("error.module.download.fail"), cause);
        }
    }

    /**
     * Indicates an error happen while downloading the module bytes from the DB.
     */
    @SuppressWarnings("serial")
    class ModuleMoveException extends ModuleInstallException {
        ModuleMoveException(@NotNull final Throwable cause) {
            super(resources.getString("error.module.move.fail"), cause);
        }
    }

    /**
     * Indicates an error happen while uninstalling removed module.
     */
    @SuppressWarnings("serial")
    class ModuleUninstallException extends ModuleStagingException {
        ModuleUninstallException(@NotNull final String moduleFileName, @NotNull final Throwable cause) {
            super(MessageFormat.format(resources.getString("error.module.uninstall.fail"), moduleFileName), cause);
        }
    }
}

package com.l7tech.server.event.system;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.module.ModuleType;
import com.l7tech.gateway.common.module.ServerModuleFile;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.logging.Level;

/**
 */
public class ServerModuleFileSystemEvent extends SystemEvent {

    private final String action;

    /**
     * Indicate whether this Admin Event is for Uploading or Deleting Server Module File
     */
    public enum Action {
        /**
         * This is a Admin message indicating that the module has been successfully uploaded into the Database.
         * <p/>
         * Sample message: <br/>
         * {@code Installing Module #2d10078e12e0099191b41f672fd97af4 (SalesForce Connector), type "Custom Assertion", file-name "SalesForceConnector.jar"...}
         */
        INSTALLING("Installing Module #{0} ({1}), type \"{2}\", file-name \"{3}\"...", "Installing Server Module File", Level.INFO),
        INSTALL_FAIL("Failed to install Module #{0} ({1}), type \"{2}\", file-name \"{3}\".", "Installing Server Module File", Level.WARNING),
        INSTALL_SUCCESS("Module #{0} ({1}), type \"{2}\", file-name \"{3}\" installed successfully.", "Installing Server Module File", Level.INFO),
        DEPLOY_PERMISSION("The Gateway doesn't have write permission on the modules deploy folder, Module #{0} ({1}), type \"{2}\", file-name \"{3}\" will be staged instead.", "Installing Server Module File", Level.INFO),
        UNINSTALLING("Uninstalling Module #{0} ({1}), type \"{2}\", file-name \"{3}\"...", "Uninstalling Server Module File", Level.INFO),
        UNINSTALL_FAIL("Failed to uninstall Module #{0} ({1}), type \"{2}\", file-name \"{3}\".", "Uninstalling Server Module File", Level.WARNING),
        UNINSTALL_SUCCESS("Module #{0} ({1}), type \"{2}\", file-name \"{3}\" uninstalled successfully.", "Uninstalling Server Module File", Level.INFO),
        LOADED("Module with type \"{0}\" and file-name \"{1}\" loaded successfully.", "Loading Module", Level.INFO)
        ;

        private final String messageFormat;
        private final String action;
        private final Level logLevel;

        private Action(final String messageFormat, final String action, final Level logLevel) {
            this.messageFormat = messageFormat;
            this.action = action;
            this.logLevel = logLevel;
        }

        public String getMessageFormat() {
            return this.messageFormat;
        }

        public String getAction() {
            return this.action;
        }

        public Level getLogLevel() {
            return logLevel;
        }
    }

    private ServerModuleFileSystemEvent(Object source, @NotNull final Level level, @NotNull final String message, @NotNull final String action) {
        super(source, Component.GW_SERVER_MODULE_FILE, null, level, message);
        this.action = action;
    }

    @Override
    public String getAction() {
        return this.action;
    }


    public static ServerModuleFileSystemEvent createSystemEvent(Object source, @NotNull final Action action, @NotNull final ServerModuleFile moduleFile) {
        return new ServerModuleFileSystemEvent(source, action.getLogLevel(), formatMessage(action, moduleFile), action.getAction());
    }

    public static ServerModuleFileSystemEvent createLoadedSystemEvent(Object source, @NotNull final ModuleType moduleType, @NotNull final String moduleFileName) {
        return new ServerModuleFileSystemEvent(
                source,
                Action.LOADED.getLogLevel(),
                MessageFormat.format(
                        Action.LOADED.getMessageFormat(),
                        moduleType,
                        moduleFileName
                ),
                Action.LOADED.getAction()
        );
    }

    private static String formatMessage(@NotNull final Action action, @NotNull final ServerModuleFile moduleFile) {
        switch (action) {
            case INSTALLING:
            case INSTALL_FAIL:
            case INSTALL_SUCCESS:
            case DEPLOY_PERMISSION:
            case UNINSTALLING:
            case UNINSTALL_FAIL:
            case UNINSTALL_SUCCESS:
                return MessageFormat.format(
                        action.getMessageFormat(),
                        moduleFile.getGoid().toHexString(),
                        moduleFile.getName(),
                        moduleFile.getModuleType().toString(),
                        moduleFile.getProperty(ServerModuleFile.PROP_FILE_NAME)
                );
            default:
                throw new IllegalStateException("Unsupported action: " + action);
        }
    }
}

package com.l7tech.server.event.system;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.module.ModuleType;
import com.l7tech.gateway.common.module.ServerModuleFile;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.logging.Level;

/**
 * Server Module file System Event fired to indicate .
 */
public class ServerModuleFileSystemEvent extends SystemEvent {

    private final String action;

    /**
     * Different types of Server Module File audit actions.
     */
    public enum Action {
        /**
         * Indicates that a module is in the process of installing.
         * <p/>
         * Sample message: <br/>
         * {@code Installing Module #2d10078e12e0099191b41f672fd97af4 (SalesForce Connector), type "Custom Assertion", file-name "SalesForceConnector.jar"...}
         */
        INSTALLING("Installing Module #{0} ({1}), type \"{2}\", file-name \"{3}\"...", "Installing Server Module File", Level.INFO),
        /**
         * Indicates that a module has been successfully deployed into the Gateway modules deploy folder.
         * <p/>
         * Sample message: <br/>
         * {@code Module #2d10078e12e0099191b41f672fd97af4 (SalesForce Connector), type "Custom Assertion", file-name "SalesForceConnector.jar" deployed successfully.}
         */
        INSTALL_DEPLOYED("Module #{0} ({1}), type \"{2}\", file-name \"{3}\" deployed successfully.", "Installing Server Module File", Level.INFO),
        /**
         * Notifies that the gateway doesn't have write permission to the modules deploy folder, thus ths module is being staged instead..
         * <p/>
         * Sample message: <br/>
         * {@code Module #2d10078e12e0099191b41f672fd97af4 (SalesForce Connector), type "Custom Assertion", file-name "SalesForceConnector.jar" deployed successfully.}
         */
        DEPLOY_PERMISSION("The Gateway doesn''t have write permission on the modules deploy folder, Module #{0} ({1}), type \"{2}\", file-name \"{3}\" will be staged instead.", "Installing Server Module File", Level.INFO),
        /**
         * Indicates that a module has been staged.
         * <p/>
         * Sample message: <br/>
         * {@code Module #2d10078e12e0099191b41f672fd97af4 (SalesForce Connector), type "Custom Assertion", file-name "SalesForceConnector.jar" staged successfully.}
         */
        INSTALL_STAGED("Module #{0} ({1}), type \"{2}\", file-name \"{3}\" staged successfully.", "Installing Server Module File", Level.INFO),
        /**
         * Indicates that installation has failed for the module.
         * <p/>
         * Sample message: <br/>
         * {@code Failed to install Module #2d10078e12e0099191b41f672fd97af4 (SalesForce Connector), type "Custom Assertion", file-name "SalesForceConnector.jar".}
         */
        INSTALL_FAIL("Failed to install Module #{0} ({1}), type \"{2}\", file-name \"{3}\".", "Installing Server Module File", Level.WARNING),

        /**
         * Indicates that a module is in the process of un-installation.
         * <p/>
         * Sample message: <br/>
         * {@code Uninstalling Module #2d10078e12e0099191b41f672fd97af4 (SalesForce Connector), type "Custom Assertion", file-name "SalesForceConnector.jar"...}
         */
        UNINSTALLING("Uninstalling Module #{0} ({1}), type \"{2}\", file-name \"{3}\"...", "Uninstalling Server Module File", Level.INFO),
        /**
         * Indicates that a module has been successfully un-installed.
         * <p/>
         * Sample message: <br/>
         * {@code Module #2d10078e12e0099191b41f672fd97af4 (SalesForce Connector), type "Custom Assertion", file-name "SalesForceConnector.jar" uninstalled successfully.}
         */
        UNINSTALL_SUCCESS("Module #{0} ({1}), type \"{2}\", file-name \"{3}\" uninstalled successfully.", "Uninstalling Server Module File", Level.INFO),
        /**
         * Indicates that un-installation has failed for the module.
         * <p/>
         * Sample message: <br/>
         * {@code Failed to uninstall Module #2d10078e12e0099191b41f672fd97af4 (SalesForce Connector), type "Custom Assertion", file-name "SalesForceConnector.jar".}
         */
        UNINSTALL_FAIL("Failed to uninstall Module #{0} ({1}), type \"{2}\", file-name \"{3}\".", "Uninstalling Server Module File", Level.WARNING),

        /**
         * Indicates that the Gateway successfully loaded the module.
         * <p/>
         * Sample message: <br/>
         * {@code Failed to uninstall Module #2d10078e12e0099191b41f672fd97af4 (SalesForce Connector), type "Custom Assertion", file-name "SalesForceConnector.jar".}
         */
        LOADED("Module with type \"{0}\" and file-name \"{1}\" loaded successfully.", "Loading Module", Level.INFO)
        ;

        private final String messageFormat;
        private final String action;
        private final Level logLevel;

        /**
         * Create a Action enum item
         *
         * @param messageFormat    audit message format for this action.
         * @param action           audit action text for this action.
         * @param logLevel         audit log level for this action.
         */
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
            case INSTALL_DEPLOYED:
            case INSTALL_STAGED:
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

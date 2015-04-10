package com.l7tech.server.event.system;

import com.l7tech.gateway.common.Component;
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
         * {@code Installing Module #981ac894eed924fc779c841369433c91, name "SalesForce Connector", type "Custom Assertion"...}
         */
        INSTALLING("Installing Module #{0}, name \"{1}\", type \"{2}\"...", "Installing Server Module File", Level.INFO),
        /**
         * Indicates that a module signature was verified (i.e. the Gateway accepted the module) and the module was staged successfully.
         * <p/>
         * Sample message: <br/>
         * {@code Verified Module #2d10078e12e0099191b41f672fd97af4, name "SalesForce Connector", type "Custom Assertion"}
         */
        INSTALL_ACCEPTED("Verified Module #{0}, name \"{1}\", type \"{2}\"", "Installing Server Module File", Level.INFO),
        /**
         * Indicates that a module signature was not verified (i.e. the Gateway rejected the module).
         * <p/>
         * Sample message: <br/>
         * {@code Gateway rejected Module #2d10078e12e0099191b41f672fd97af4 , name "SalesForce Connector", type "Custom Assertion", as signature cannot be verified}
         */
        INSTALL_REJECTED("Gateway rejected Module #{0}, name \"{1}\", type \"{2}\", as signature cannot be verified", "Installing Server Module File", Level.INFO),
        /**
         * Indicates that installation has failed for the module.
         * <p/>
         * Sample message: <br/>
         * {@code Failed to install Module #2d10078e12e0099191b41f672fd97af4, name "SalesForce Connector", type "Custom Assertion"}
         */
        INSTALL_FAIL("Failed to install Module #{0}, name \"{1}\", type \"{2}\"", "Installing Server Module File", Level.WARNING),

        /**
         * Indicates that a module is in the process of un-installation.
         * <p/>
         * Sample message: <br/>
         * {@code Uninstalling Module #2d10078e12e0099191b41f672fd97af4 name "SalesForce Connector", type "Custom Assertion"...}
         */
        UNINSTALLING("Uninstalling Module #{0}, name \"{1}\", type \"{2}\"...", "Uninstalling Server Module File", Level.INFO),
        /**
         * Indicates that a module has been successfully un-installed.
         * <p/>
         * Sample message: <br/>
         * {@code Successfully uninstalled Module #2d10078e12e0099191b41f672fd97af4 , name "SalesForce Connector", type "Custom Assertion"}
         */
        UNINSTALL_SUCCESS("Successfully uninstalled Module #{0}, name \"{1}\", type \"{2}\"", "Uninstalling Server Module File", Level.INFO),
        /**
         * Indicates that un-installation has failed for the module.
         * <p/>
         * Sample message: <br/>
         * {@code Failed to uninstall Module #2d10078e12e0099191b41f672fd97af4 , name "SalesForce Connector", type "Custom Assertion"}
         */
        UNINSTALL_FAIL("Failed to uninstall Module #{0}, name \"{1}\", type \"{2}\"", "Uninstalling Server Module File", Level.WARNING),

        /**
         * Indicates that the Gateway successfully loaded the module.
         * <p/>
         * Sample message: <br/>
         * {@code Successfully loaded Module #2d10078e12e0099191b41f672fd97af4 , name "SalesForce Connector", type "Custom Assertion"}
         */
        LOADED("Successfully loaded Module #{0}, name \"{1}\", type \"{2}\"", "Loading Module", Level.INFO)
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

    private static String formatMessage(@NotNull final Action action, @NotNull final ServerModuleFile moduleFile) {
        switch (action) {
            case INSTALLING:
            case INSTALL_FAIL:
            case INSTALL_ACCEPTED:
            case INSTALL_REJECTED:
            case UNINSTALLING:
            case UNINSTALL_FAIL:
            case UNINSTALL_SUCCESS:
            case LOADED:
                return MessageFormat.format(
                        action.getMessageFormat(),
                        moduleFile.getGoid().toHexString(),
                        moduleFile.getName(),
                        moduleFile.getModuleType().toString()
                );
            default:
                throw new IllegalStateException("Unsupported action: " + action);
        }
    }
}

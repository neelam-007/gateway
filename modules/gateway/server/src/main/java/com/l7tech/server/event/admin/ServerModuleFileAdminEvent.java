package com.l7tech.server.event.admin;

import com.l7tech.gateway.common.module.ServerModuleFile;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.logging.Level;

/**
 * Event fired when Server Module file is successfully uploaded or deleted.
 */
public class ServerModuleFileAdminEvent extends AdminEvent {

    /**
     * Indicate whether this Admin Event is for Uploading or Deleting Server Module File
     */
    public enum Action {
        /**
         * This is a Admin message indicating that the module has been successfully uploaded into the Database.
         * <p/>
         * Sample message: <br/>
         * {@code Module #2d10078e12e0099191b41f672fd97af4 (SalesForce Connector), type = "Custom Assertion", file-name = "SalesForceConnector.jar", size "234KB" uploaded.}
         */
        UPLOADED("Module #{0} ({1}), type \"{2}\", file-name \"{3}\", size \"{4}\" uploaded."),

        /**
         * This is a Admin message indicating that the module has been successfully deleted from the Database.
         * <p/>
         * Sample message: <br/>
         * {@code Module #2d10078e12e0099191b41f672fd97af4 (SalesForce Connector), type = "Custom Assertion", file-name = "SalesForceConnector.jar" deleted.}
         *
         */
        DELETED("Module #{0} ({1}), type \"{2}\", file-name \"{3}\" deleted.")
        ;

        private final String messageFormat;

        private Action(final String messageFormat) {
            this.messageFormat = messageFormat;
        }

        public String getMessageFormat() {
            return this.messageFormat;
        }
    }

    /**
     * Creates {@code ServerModuleFileAdminEvent}.
     *
     * @param source        the source.
     * @param action        which actions should be audit.
     * @param moduleFile    the server module file.
     * @param level         the log level.
     */
    public ServerModuleFileAdminEvent(
            final Object source,
            @NotNull final Action action,
            @NotNull final ServerModuleFile moduleFile,
            @NotNull final Level level
    ) {
        super(source, formatMessage(action, moduleFile), level);
    }

    /**
     * Convenience constructor with default {@link Level#INFO} log level.
     *
     * @param source        the source.
     * @param action        which actions should be audit.
     * @param moduleFile    the server module file.
     */
    public ServerModuleFileAdminEvent(
            final Object source,
            @NotNull final Action action,
            @NotNull final ServerModuleFile moduleFile
    ) {
        this(source, action, moduleFile, Level.INFO);
    }

    private static String formatMessage(@NotNull final Action action, @NotNull final ServerModuleFile moduleFile) {
        switch (action) {
            case UPLOADED:
                return MessageFormat.format(
                        action.getMessageFormat(),
                        moduleFile.getGoid().toHexString(),
                        moduleFile.getName(),
                        moduleFile.getModuleType().toString(),
                        moduleFile.getProperty(ServerModuleFile.PROP_FILE_NAME),
                        moduleFile.getHumanReadableFileSize()
                );
                case DELETED:
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

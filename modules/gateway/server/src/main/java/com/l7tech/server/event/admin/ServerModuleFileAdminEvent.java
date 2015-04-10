package com.l7tech.server.event.admin;

import com.l7tech.gateway.common.audit.AdminAuditRecord;
import com.l7tech.gateway.common.module.ServerModuleFile;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.TextUtils;
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
         * {@code Uploaded Module #2d10078e12e0099191b41f672fd97af4, name "SalesForce Connector", type "Custom Assertion", size "234KB"}
         */
        UPLOADED("Uploaded Module #{0}, name \"{1}\", type \"{2}\", size \"{3}\" uploaded"),

        /**
         * This is a Admin message indicating that the module has been successfully deleted from the Database.
         * <p/>
         * Sample message: <br/>
         * {@code Deleted Module #2d10078e12e0099191b41f672fd97af4, name "SalesForce Connector", type "Custom Assertion"}
         *
         */
        DELETED("Deleted Module #{0}, name \"{1}\", type \"{2}\"")
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
     * Represents the {@link ServerModuleFile#goid}
     */
    private final Goid moduleGoid;
    /**
     * Represents the {@link ServerModuleFile#_name}
     */
    private final String moduleName;
    /**
     * Represents the audit action.<br/>
     * Can either be {@link com.l7tech.gateway.common.audit.AdminAuditRecord#ACTION_CREATED ACTION_CREATED} or
     * {@link com.l7tech.gateway.common.audit.AdminAuditRecord#ACTION_DELETED ACTION_DELETED}
     *
     * @see #actionToAdminAuditRecordAction(com.l7tech.server.event.admin.ServerModuleFileAdminEvent.Action)
     */
    private final char action;

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
        this.moduleGoid = moduleFile.getGoid();
        this.moduleName = moduleFile.getName();
        this.action = actionToAdminAuditRecordAction(action);
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

    /**
     * Getter for {@link #moduleGoid}
     */
    public Goid getModuleGoid() {
        return moduleGoid;
    }

    /**
     * Getter for {@link #moduleName}
     */
    public String getModuleName() {
        return moduleName;
    }

    /**
     * Getter for {@link #action}
     */
    public char getAction() {
        return action;
    }

    public String getEntityClassName() {
        return ServerModuleFile.class.getName();
    }

    private static String formatMessage(@NotNull final Action action, @NotNull final ServerModuleFile moduleFile) {
        int maxDetailLength;
        switch (action) {
            case UPLOADED:
                // make sure we do not overshot admin audit MESSAGE_MAX_LENGTH
                maxDetailLength = MESSAGE_MAX_LENGTH - action.getMessageFormat().length() -
                        moduleFile.getGoid().toHexString().length() -
                        moduleFile.getModuleType().toString().length() -
                        moduleFile.getHumanReadableFileSize().length();
                return TextUtils.truncateStringAtEnd(
                        MessageFormat.format(
                                action.getMessageFormat(),
                                moduleFile.getGoid().toHexString(),
                                TextUtils.truncateStringAtEnd(moduleFile.getName(), maxDetailLength),
                                moduleFile.getModuleType().toString(),
                                moduleFile.getHumanReadableFileSize()
                        ),
                        MESSAGE_MAX_LENGTH
                );
            case DELETED:
                // make sure we do not overshot admin audit MESSAGE_MAX_LENGTH
                maxDetailLength = MESSAGE_MAX_LENGTH - action.getMessageFormat().length() -
                        moduleFile.getGoid().toHexString().length() -
                        moduleFile.getModuleType().toString().length();
                return TextUtils.truncateStringAtEnd(
                        MessageFormat.format(
                                action.getMessageFormat(),
                                moduleFile.getGoid().toHexString(),
                                TextUtils.truncateStringAtEnd(moduleFile.getName(), maxDetailLength),
                                moduleFile.getModuleType().toString()
                        ),
                        MESSAGE_MAX_LENGTH
                );
            default:
                throw new IllegalStateException("Unsupported action: " + action);
        }
    }

    private static char actionToAdminAuditRecordAction(@NotNull final Action action) {
        switch (action) {
            case UPLOADED:
                return AdminAuditRecord.ACTION_CREATED;
            case DELETED:
                return AdminAuditRecord.ACTION_DELETED;
            default:
                throw new IllegalStateException("Unsupported action: " + action);
        }
    }
}

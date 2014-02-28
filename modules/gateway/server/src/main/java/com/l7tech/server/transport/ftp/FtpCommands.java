package com.l7tech.server.transport.ftp;

import java.io.IOException;

import org.apache.ftpserver.command.AbstractCommand;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.impl.FtpIoSession;
import org.apache.ftpserver.impl.FtpServerContext;

/**
 * FTP Commands that are overridden, but should not routed to the MessageProcessor.
 *
 * @author Steve Jones
 * @author jwilliams
 */
public class FtpCommands {

    /**
     * Implements FEAT command which advertises suppported extensions.
     */
    public static class FEAT extends AbstractCommand {
        private final String extensionsMessage;

        public FEAT(String[] extensions) {
            super();

            this.extensionsMessage = generateExtensionsMessage(extensions);
        }

        @Override
        public void execute(FtpIoSession session, FtpServerContext context,
                            FtpRequest request) throws IOException, FtpException {
            // reset state variables
            session.resetState();

            // send implemented features reply
            session.write(new DefaultFtpReply(FtpReply.REPLY_211_SYSTEM_STATUS_REPLY, extensionsMessage));
        }

        private String generateExtensionsMessage(String[] extensions) {
            StringBuilder sb = new StringBuilder();

            sb.append("211-Extensions supported:");

            for (String extension : extensions) {
                sb.append("\n ");
                sb.append(extension);
            }

            sb.append("\n211 End\r\n");

            return sb.toString();
        }
    }

    /**
     * LANG command implementation that only supports English.
     */
    public static class LANG extends AbstractCommand {
        private static final String LANG_EN = "en";

        @Override
        public void execute(FtpIoSession session, FtpServerContext context,
                            FtpRequest request) throws IOException, FtpException {
            // reset state
            session.resetState();

            // default language
            String language = request.getArgument();

            if (language == null || language.toLowerCase().startsWith(LANG_EN)) {
                session.setLanguage(null);
                session.write(new DefaultFtpReply(FtpReply.REPLY_200_COMMAND_OKAY, "Command LANG okay."));
            } else {
                // not found - send error message
                session.write(new DefaultFtpReply(FtpReply.REPLY_504_COMMAND_NOT_IMPLEMENTED_FOR_THAT_PARAMETER,
                        "Command LANG not implemented for this parameter."));
            }
        }
    }
}

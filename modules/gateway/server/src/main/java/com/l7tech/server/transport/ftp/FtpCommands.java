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
     * FEAT command implementation that advertises the cut-down features.
     */
    public static class FEAT extends AbstractCommand {

        @Override
        public void execute(FtpIoSession session, FtpServerContext context,
                            FtpRequest request) throws IOException, FtpException {
            // reset state variables
            session.resetState();

            session.write(new FtpReply() {
                public int getCode() {
                    return FtpReply.REPLY_211_SYSTEM_STATUS_REPLY;
                }

                public String getMessage() {
                    /**
                     * Add this to the supported extensions if/when explicit FTPS is needed:
                     *
                     *   \n AUTH SSL\n AUTH TLS
                     */
                    return "211-Extensions supported\n SIZE\n MDTM\n REST STREAM\n LANG en\n MLST Size;Modify;Type;Perm\n MODE Z\n UTF8\n EPRT\n EPSV\n PASV\n TVFS\n211 End\r\n"; // TODO jwilliams: needs to be updated
                }

                public String toString() {
                    return getMessage();
                }
            });
        }
    }

    /**
     * LANG command implementation that only supports English.
     */
    public static class LANG extends AbstractCommand {
        private static final String LANG_EN = "en";

        @Override
        public void execute(FtpIoSession session, FtpServerContext context, FtpRequest request) throws IOException, FtpException {
            // reset state
            session.resetState();

            // default language
            String language = request.getArgument();

            if (language == null || language.toLowerCase().startsWith(LANG_EN)) {
                session.setLanguage(null);
                session.write(new DefaultFtpReply(FtpReply.REPLY_200_COMMAND_OKAY, "Command LANG okay."));
                return;
            }

            // not found - send error message
            session.write(new DefaultFtpReply(FtpReply.REPLY_504_COMMAND_NOT_IMPLEMENTED_FOR_THAT_PARAMETER, "Command LANG not implemented for this parameter."));
        }
    }
}

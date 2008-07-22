package com.l7tech.server.transport.ftp;

import java.io.IOException;

import org.apache.ftpserver.listener.Connection;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpReplyOutput;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpReply;
import org.apache.ftpserver.FtpSessionImpl;
import org.apache.ftpserver.DefaultFtpReply;
import org.apache.ftpserver.command.AbstractCommand;

/**
 * FTP Commands that are overridden.
 *
 * <p>Probably should have done this for STOR/STOU instead of using an Ftplet.</p>
 *
 * @author Steve Jones
 */
public class FtpCommands {

    /**
     * FEAT command implementation that advertises the cut-down features.
     */
    public static class FEAT extends AbstractCommand {

        public void execute(Connection connection,
                            FtpRequest request,
                            FtpSessionImpl session,
                            FtpReplyOutput out) throws IOException, FtpException {

            // reset state variables
            session.resetState();

            out.write(new FtpReply(){
                public int getCode() {
                    return FtpReply.REPLY_211_SYSTEM_STATUS_REPLY;
                }

                public String getMessage() {
                    /**
                     * Add this to the supported extensions if/when explicit FTPS is needed:
                     *
                     *   \n AUTH SSL\n AUTH TLS
                     */
                    return "211-Extensions supported\n SIZE\n MDTM\n REST STREAM\n LANG en\n MLST Size;Modify;Type;Perm\n MODE Z\n UTF8\n EPRT\n EPSV\n PASV\n TVFS\n211 End\r\n";
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
        public void execute(Connection connection,
                            FtpRequest request,
                            FtpSessionImpl session,
                            FtpReplyOutput out) throws IOException, FtpException {

            // reset state
            session.resetState();

            // default language
            String language = request.getArgument();
            if(language == null || language.toLowerCase().startsWith("en")) {
                session.setLanguage(null);
                out.write(new DefaultFtpReply(FtpReply.REPLY_200_COMMAND_OKAY, "Command LANG okay."));
                return;
            }

            // not found - send error message
            out.write(new DefaultFtpReply(FtpReply.REPLY_504_COMMAND_NOT_IMPLEMENTED_FOR_THAT_PARAMETER, "Command LANG not implemented for this parameter."));
        }
    }
}

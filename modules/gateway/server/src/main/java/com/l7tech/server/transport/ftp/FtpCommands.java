package com.l7tech.server.transport.ftp;

import java.io.IOException;

import com.l7tech.gateway.common.transport.ftp.FtpMethod;
import org.apache.ftpserver.listener.Connection;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpReplyOutput;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpReply;
import org.apache.ftpserver.FtpSessionImpl;
import org.apache.ftpserver.DefaultFtpReply;

/**
 * FTP Commands that are overridden.
 *
 * <p>Probably should have done this for STOR/STOU instead of using an Ftplet.</p>
 *
 * @author Steve Jones
 * @author jwilliams
 */
public class FtpCommands { // TODO jwilliams: investigate refactoring of Ftplet functionality into this class, as suggested above in class javadoc

    /**
     * ACCT command implementation. Abort an active file transfer
     */
    public static class ACCT extends BaseCommand {
        
        ACCT() {
            super(FtpMethod.FTP_ACCT);
        }

        @Override
        public void execute(Connection connection,
                            FtpRequest request,
                            FtpSessionImpl session,
                            FtpReplyOutput out)
                throws IOException, FtpException {
            // reset state variables
            session.resetState();

            // and abort any data connection
            out.write(new DefaultFtpReply(FtpReply.REPLY_202_COMMAND_NOT_IMPLEMENTED, "ACCT"));
        }
    }

    /**
     * APPE command implementation. Abort an active file transfer
     */
    public static class APPE extends BaseCommand {
        
        APPE() {
            super(FtpMethod.FTP_APPE);
        }
        
        @Override
        public void execute(Connection connection,
                            FtpRequest request,
                            FtpSessionImpl session,
                            FtpReplyOutput out)
                throws IOException, FtpException {
            MessageProcessingFtplet ftplet = (MessageProcessingFtplet) connection.getServerContext().getFtpletContainer();

            ftplet.onListStart(session, request, out, FtpMethod.FTP_APPE);
        }
    }

    /**
     * LIST command implementation.
     */
    public static class LIST extends BaseCommand {

        LIST() {
            super(FtpMethod.FTP_LIST);
        }
        
        @Override
        public void execute(Connection connection,
                            FtpRequest request,
                            FtpSessionImpl session,
                            FtpReplyOutput out)
                throws IOException, FtpException {
            MessageProcessingFtplet ftplet = (MessageProcessingFtplet) connection.getServerContext().getFtpletContainer();

            ftplet.onListStart(session, request, out, FtpMethod.FTP_LIST);
        }
    }

    /**
     * MDTM command implementation. Return the last modified time of a specified file
     */
    public static class MDTM extends BaseCommand {

        MDTM() {
            super(FtpMethod.FTP_MDTM);
        }
    }

    /**
     * MLST  command implementation
     */
    public static class MLST extends BaseCommand {

        MLST() {
            super(FtpMethod.FTP_MLST);
        }
    }

    /**
     * NLST command implementation
     */
    public static class NLST extends BaseCommand {

        NLST() {
            super(FtpMethod.FTP_NLST);
        }

        @Override
        public void execute(Connection connection,
                            FtpRequest request,
                            FtpSessionImpl session,
                            FtpReplyOutput out)
                throws IOException, FtpException {
            MessageProcessingFtplet ftplet = (MessageProcessingFtplet) connection.getServerContext().getFtpletContainer();

            ftplet.onListStart(session, request, out, FtpMethod.FTP_NLST);
        }
    }

    /**
     * PASS command implementation. Set passive mode
     */
    public static class PASS extends BaseCommand {

        PASS() {
            super(FtpMethod.FTP_PASS);
        }
    }

    /**
     * RMD command implementation. Remove directory
     */
    public static class RMD extends BaseCommand {

        RMD() {
            super(FtpMethod.FTP_RMD);
        }
    }

    /**
     * RNFR command implementation. Rename file from
     */
    public static class RNFR extends BaseCommand {

        RNFR() {
            super(FtpMethod.FTP_RNFR);
        }
    }

    /**
     * RNTO  command implementation. Rename file to
     */
    public static class RNTO extends BaseCommand {

        RNTO() {
            super(FtpMethod.FTP_RNTO);
        }
    }

    /**
     * SIZE  command implementation. Get size of the file
     */
    public static class SIZE extends BaseCommand {

        SIZE() {
            super(FtpMethod.FTP_SIZE);
        }
    }

    /**
     * MKD Create a directory
     */
    public static class MKD extends BaseCommand {

        MKD() {
            super(FtpMethod.FTP_MKD);
        }
    }

    /**
     * CWD Change working directory
     */
    public static class CWD extends BaseCommand {

        CWD() {
            super(FtpMethod.FTP_CWD);
        }
    }

    /**
     * CDUP Change working directory
     */
    public static class CDUP extends BaseCommand {

        CDUP() {
            super(FtpMethod.FTP_CDUP);
        }
    }

    /**
     * PWD Change working directory
     */
    public static class PWD extends BaseCommand {

        PWD() {
            super(FtpMethod.FTP_PWD);
        }
    }

    /**
     * MLSD command implementation.
     */
    public static class MLSD extends BaseCommand {

        MLSD() {
            super(FtpMethod.FTP_MLSD);
        }

        @Override
        public void execute(Connection connection,
                            FtpRequest request,
                            FtpSessionImpl session,
                            FtpReplyOutput out)
                throws IOException, FtpException {
            MessageProcessingFtplet ftplet = (MessageProcessingFtplet) connection.getServerContext().getFtpletContainer();

            ftplet.onListStart(session, request, out, FtpMethod.FTP_MLSD);
        }
    }

    /**
     * DELE command implementation.
     */
    public static class DELE extends BaseCommand {

        DELE() {
            super(FtpMethod.FTP_DELE);
        }

        @Override
        public void execute(Connection connection,
                            FtpRequest request,
                            FtpSessionImpl session,
                            FtpReplyOutput out)
                throws IOException, FtpException {
            MessageProcessingFtplet ftplet = (MessageProcessingFtplet) connection.getServerContext().getFtpletContainer();

            ftplet.onDeleteStart(session, request, out); // TODO jwilliams: move delete code from Ftplet to here?
        }
    }

    /**
     * FEAT command implementation that advertises the cut-down features.
     */
    public static class FEAT extends BaseCommand {

        FEAT() {
            super(FtpMethod.FTP_FEAT);
        }

        @Override
        public void execute(Connection connection,
                            FtpRequest request,
                            FtpSessionImpl session,
                            FtpReplyOutput out) throws IOException, FtpException {
            // reset state variables
            session.resetState();

            out.write(new FtpReply() {
                public int getCode() {
                    return FtpReply.REPLY_211_SYSTEM_STATUS_REPLY;
                }

                public String getMessage() {
                    /**
                     * Add this to the supported extensions if/when explicit FTPS is needed:
                     *
                     *   \n AUTH SSL\n AUTH TLS
                     */
                    return "211-Extensions supported\n SIZE\n MDTM\n REST STREAM\n LANG en\n MLST Size;Modify;Type;Perm\n MODE Z\n UTF8\n EPRT\n EPSV\n PASV\n TVFS\n211 End\r\n"; // TODO jwilliams: needs review/updating?
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
    public static class LANG extends BaseCommand {
        private static final String LANG_EN = "en";

        LANG() {
            super(FtpMethod.FTP_LANG);
        }

        @Override
        public void execute(Connection connection,
                            FtpRequest request,
                            FtpSessionImpl session,
                            FtpReplyOutput out) throws IOException, FtpException {
            // reset state
            session.resetState();

            // default language
            String language = request.getArgument();

            if (language == null || language.toLowerCase().startsWith(LANG_EN)) {
                session.setLanguage(null);
                out.write(new DefaultFtpReply(FtpReply.REPLY_200_COMMAND_OKAY, "Command LANG okay."));
                return;
            }

            // not found - send error message
            out.write(new DefaultFtpReply(FtpReply.REPLY_504_COMMAND_NOT_IMPLEMENTED_FOR_THAT_PARAMETER, "Command LANG not implemented for this parameter."));
        }
    }
}

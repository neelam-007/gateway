package com.l7tech.external.assertions.ftprouting.server;

import org.apache.ftpserver.DefaultFtpReply;
import org.apache.ftpserver.FtpSessionImpl;
import org.apache.ftpserver.command.AbstractCommand;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.listener.Connection;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author nilic
 */
public class FtpCommands {

    /**
     * ACCT command implementation . Abort an active file transfer
     */
    public static class ACCT extends BaseCommand {

        private static final Logger logger = Logger.getLogger(ACCT.class.getName());

        /**
         * Execute command.
         */
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
     * APPE command implementation . Abort an active file transfer
     */
    public static class APPE extends AbstractCommand {

        private static final Logger logger = Logger.getLogger(APPE.class.getName());

        /**
         * Execute command.
         */
        public void execute(Connection connection,
                            FtpRequest request,
                            FtpSessionImpl session,
                            FtpReplyOutput out)
                throws IOException, FtpException {

            MessageProcessingFtpletSubsystem ftplet = (MessageProcessingFtpletSubsystem) connection.getServerContext().getFtpletContainer();
            ftplet.onListStart(session, request, out, FtpMethod.FTP_APPE);
        }
    }

    /**
     * LIST command implementation .
     */
    public static class LIST extends AbstractCommand {

        private static final Logger logger = Logger.getLogger(LIST.class.getName());

        /**
         * Execute command.
         */
        public void execute(Connection connection,
                            FtpRequest request,
                            FtpSessionImpl session,
                            FtpReplyOutput out)
                throws IOException, FtpException {

            MessageProcessingFtpletSubsystem ftplet = (MessageProcessingFtpletSubsystem) connection.getServerContext().getFtpletContainer();
            ftplet.onListStart(session, request, out, FtpMethod.FTP_LIST);
        }
    }

    /**
     * MDTM command implementation. Return the last modified time of a specified file
     */
    public static class MDTM extends BaseCommand {

        private static final Logger logger = Logger.getLogger(MDTM.class.getName());

        MDTM(){
            super(FtpMethod.FTP_MDTM);
        }
    }

    /**
     * MLST  command implementation
     */
    public static class MLST extends BaseCommand {

        private static final Logger logger = Logger.getLogger(MLST.class.getName());

        MLST(){
            super(FtpMethod.FTP_MLST);
        }

    }

    /**
     * NLST command implementation
     */
    public static class NLST extends BaseCommand {

        private static final Logger logger = Logger.getLogger(NLST.class.getName());

        /**
         * Execute command.
         */
        public void execute(Connection connection,
                            FtpRequest request,
                            FtpSessionImpl session,
                            FtpReplyOutput out)
                throws IOException, FtpException {

            MessageProcessingFtpletSubsystem ftplet = (MessageProcessingFtpletSubsystem) connection.getServerContext().getFtpletContainer();
            ftplet.onListStart(session, request, out, FtpMethod.FTP_NLST);
        }

    }

    /**
     * PASS command implementation . Set passive mode
     */
    public static class PASS extends BaseCommand {

        private static final Logger logger = Logger.getLogger(PASS.class.getName());

        PASS() {
            super(FtpMethod.FTP_PASS);
        }
    }

    /**
     * RMD command implementation. Remove directory
     */
    public static class RMD extends BaseCommand {

        private static final Logger logger = Logger.getLogger(RMD.class.getName());

        RMD(){
            super(FtpMethod.FTP_RMD);
        }
    }

    /**
     * RNFR command implementation. Rename file from
     */
    public static class RNFR extends BaseCommand {

        private static final Logger logger = Logger.getLogger(RNFR.class.getName());

        RNFR(){
            super(FtpMethod.FTP_RNFR);
        }
    }

    /**
     * RNTO  command implementation. Rename file to
     */
    public static class RNTO extends BaseCommand {

        private static final Logger logger = Logger.getLogger(RNTO.class.getName());

        RNTO(){
            super(FtpMethod.FTP_RNTO);
        }
    }

    /**
     * SIZE  command implementation. Get size of the file
     */
    public static class SIZE extends BaseCommand {

        private static final Logger logger = Logger.getLogger(SIZE.class.getName());

        SIZE(){
            super(FtpMethod.FTP_SIZE);
        }
    }

    /**
     * MKD Create a directory
     */
    public static class MKD extends BaseCommand {

        private static final Logger logger = Logger.getLogger(MKD.class.getName());

        MKD(){
            super(FtpMethod.FTP_MKD);
        }
    }

    /**
     * CWD Change working directory
     */
    public static class CWD extends BaseCommand {

        private static final Logger logger = Logger.getLogger(CWD.class.getName());

        CWD(){
            super(FtpMethod.FTP_CWD);
        }
    }

    /**
     * CDUP Change working directory
     */
    public static class CDUP extends BaseCommand {

        private static final Logger logger = Logger.getLogger(CDUP.class.getName());

        CDUP(){
            super(FtpMethod.FTP_CDUP);
        }
    }

    /**
     * PWD Change working directory
     */
    public static class PWD extends BaseCommand {

        private static final Logger logger = Logger.getLogger(PWD.class.getName());

        PWD(){
            super(FtpMethod.FTP_PWD);
        }
    }

    /**
     * MLSD command implementation .
     */
    public static class MLSD extends AbstractCommand {

        private static final Logger logger = Logger.getLogger(MLSD.class.getName());

        /**
         * Execute command.
         */
        public void execute(Connection connection,
                            FtpRequest request,
                            FtpSessionImpl session,
                            FtpReplyOutput out)
                throws IOException, FtpException {

            MessageProcessingFtpletSubsystem ftplet = (MessageProcessingFtpletSubsystem) connection.getServerContext().getFtpletContainer();
            ftplet.onListStart(session, request, out, FtpMethod.FTP_MLSD);
        }
    }

    /**
     * MLSD command implementation .
     */
    public static class DELE extends AbstractCommand {

        private static final Logger logger = Logger.getLogger(DELE.class.getName());

        /**
         * Execute command.
         */
        public void execute(Connection connection,
                            FtpRequest request,
                            FtpSessionImpl session,
                            FtpReplyOutput out)
                throws IOException, FtpException {

            MessageProcessingFtpletSubsystem ftplet = (MessageProcessingFtpletSubsystem) connection.getServerContext().getFtpletContainer();
            ftplet.onDeleteStart(session, request, out);
        }
    }

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

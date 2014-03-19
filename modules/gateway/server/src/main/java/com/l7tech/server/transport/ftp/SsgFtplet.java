package com.l7tech.server.transport.ftp;

import org.apache.ftpserver.ftplet.DefaultFtpReply;
import org.apache.ftpserver.ftplet.DefaultFtplet;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpReply;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.ftplet.FtpletResult;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ftplet implementation for SSG.
 * Currently only used to enforce licensing of FTP(S) Transport Module.
 *
 * @author jwilliams
 */
public class SsgFtplet extends DefaultFtplet {
    private static final Logger logger = Logger.getLogger(SsgFtplet.class.getName());

    private final FtpServerManager ftpServerManager;

    /*
     * Bean constructor
     */
    public SsgFtplet(final FtpServerManager ftpServerManager) {
        this.ftpServerManager = ftpServerManager;
    }

    @Override
    public FtpletResult onLogin(FtpSession ftpSession, FtpRequest ftpRequest) throws FtpException, IOException  {
        FtpletResult result = FtpletResult.SKIP;

        if (!ftpServerManager.isLicensed()) {
            logger.log(Level.WARNING, "Login failed: FTP server not licensed.");

            ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                    "Service not available (not licensed)."));

            result =  FtpletResult.DISCONNECT;
        }

        return result;
    }
}

package com.l7tech.server.transport.ftp;

import com.l7tech.common.log.HybridDiagnosticContext;
import com.l7tech.gateway.common.log.GatewayDiagnosticContextKeys;
import com.l7tech.objectmodel.Goid;
import org.apache.ftpserver.ftplet.DataConnectionFactory;
import org.apache.ftpserver.ftplet.DefaultFtpReply;
import org.apache.ftpserver.ftplet.DefaultFtplet;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpReply;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.ftplet.FtpletResult;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.impl.ServerDataConnectionFactory;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ftplet implementation for SSG.
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

    /**
     * Ensure that on initial connection the data connection is secure if the control connection is.
     */
    @Override
    public FtpletResult onConnect(FtpSession ftpSession) throws FtpException, IOException {
        // TODO jwilliams: is this functionality still required?
        DataConnectionFactory dataConnectionFactory = ftpSession.getDataConnection();

        if (dataConnectionFactory instanceof ServerDataConnectionFactory) {
            ServerDataConnectionFactory connectionFactory = (ServerDataConnectionFactory) dataConnectionFactory;

            boolean controlSecure = false;

//            if (ftpSession instanceof FtpServerSession) {
//                FtpServerSession ftpServerSession = (FtpServerSession) ftpSession;
//                controlSecure = ftpServerSession.getListener().isImplicitSsl();
//            }

            // init data connection security to the same as the control connection
            connectionFactory.setSecure(controlSecure);
        }

        return super.onConnect(ftpSession);
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

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

    private static final int STORE_RESULT_OK = 0;
    private static final int STORE_RESULT_FAULT = 1;
    private static final int STORE_RESULT_DROP = 2;

    private final FtpServerManager ftpServerManager;
    private final Goid connectorGoid;
    private final String initServiceUri;

    /*
     * Bean constructor
     */
    public SsgFtplet(final FtpServerManager ftpServerManager,
                     final Goid connectorGoid,
                     final String initServiceUri) {
        this.ftpServerManager = ftpServerManager;
        this.connectorGoid = connectorGoid;
        this.initServiceUri = initServiceUri;
    }

    /**
     * Ensure that on initial connection the data connection is secure if the control connection is.
     */
    @Override
    public FtpletResult onConnect(FtpSession ftpSession) throws FtpException, IOException {
        // TODO jwilliams: perform licensing check here instead of for every command

        // TODO jwilliams: preserve this functionality if required
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
        return handleLogin(ftpSession, ftpRequest, false);
    }

    private FtpletResult handleLogin(FtpSession ftpSession, FtpRequest ftpRequest, boolean unique) throws FtpException, IOException {
    // TODO jwilliams: separate processing from other stuff, move to RequestProcessor
        FtpletResult result = FtpletResult.SKIP;

        String fileName = ftpRequest.getArgument();

        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "Handling " + ftpRequest.getCommand() + " for file ''{0}'' (unique:{1}).", new Object[] {fileName, unique});

        if (!ftpServerManager.isLicensed()) {
            if (logger.isLoggable(Level.INFO))
                logger.log(Level.INFO, "Failing " + ftpRequest.getCommand() + " (FTP server not licensed).");

            ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                    "Service not available (not licensed)."));
        } else {
            HybridDiagnosticContext.put(GatewayDiagnosticContextKeys.LISTEN_PORT_ID, connectorGoid.toString()); // TODO jwilliams: handle HybridDiagnostic stuff in before/after Ftplet methods?
            HybridDiagnosticContext.put(GatewayDiagnosticContextKeys.CLIENT_IP, ftpSession.getClientAddress().getAddress().getHostAddress());
            User user = ftpSession.getUser();
            String file = ftpRequest.getArgument();

            String path = initServiceUri;

            if (unique) {
                ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_250_REQUESTED_FILE_ACTION_OKAY, file + ": Transfer started."));
            }

            int storeResult = -1; // TODO jwilliams: temporarily added - remove when moving processing to FtpRequestProcessor

//            int storeResult = process(ftpSession, user, path, file, // TODO jwilliams: uncomment this when process method is available
//                    isSecureSession(ftpSession), unique, FtpMethod.FTP_LOGIN.getWspName(), "/");

            if (storeResult == STORE_RESULT_FAULT) {
                result = FtpletResult.DISCONNECT;
            } else {
                result = null;
            }

            HybridDiagnosticContext.remove(GatewayDiagnosticContextKeys.LISTEN_PORT_ID);
            HybridDiagnosticContext.remove(GatewayDiagnosticContextKeys.CLIENT_IP);
        }

        return result;
    }
}

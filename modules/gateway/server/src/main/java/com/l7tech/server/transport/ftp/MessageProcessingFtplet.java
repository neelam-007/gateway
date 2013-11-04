package com.l7tech.server.transport.ftp;

import com.l7tech.common.log.HybridDiagnosticContext;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.gateway.common.log.GatewayDiagnosticContextKeys;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.transport.ftp.FtpMethod;
import com.l7tech.message.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.event.FaultProcessed;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.PolicyVersionException;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.util.EventChannel;
import com.l7tech.server.util.SoapFaultManager;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InetAddressUtil;
import com.l7tech.util.ResourceUtils;
import org.apache.ftpserver.ftplet.DataConnection;
import org.apache.ftpserver.ftplet.DataConnectionFactory;
import org.apache.ftpserver.ftplet.DefaultFtpReply;
import org.apache.ftpserver.ftplet.DefaultFtplet;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpReply;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.ftplet.FtpletResult;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.ftpletcontainer.impl.DefaultFtpletContainer;
import org.apache.ftpserver.impl.FtpIoSession;
import org.apache.ftpserver.impl.ServerDataConnectionFactory;
import org.apache.ftpserver.listener.nio.AbstractListener;
import org.w3c.dom.Document;

import java.io.*;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ftplet implementation backed by our MessageProcessor.
 *
 * @author Steve Jones
 * @author jwilliams
 */
public class MessageProcessingFtplet extends DefaultFtplet {
    private static final Logger logger = Logger.getLogger(MessageProcessingFtplet.class.getName());

    private static final int STORE_RESULT_OK = 0;
    private static final int STORE_RESULT_FAULT = 1;
    private static final int STORE_RESULT_DROP = 2;

    private final FtpServerManager ftpServerManager;
    private final MessageProcessor messageProcessor;
    private final SoapFaultManager soapFaultManager;
    private final StashManagerFactory stashManagerFactory;
    private final EventChannel messageProcessingEventChannel;
    private final ContentTypeHeader overriddenContentType;
    private final Goid hardwiredServiceGoid;
    private final long maxRequestSize;
    private final Goid connectorGoid;
    private final ServiceManager serviceManager;
    private final String initServiceUri;

    /*
     * Bean constructor
     */
    public MessageProcessingFtplet(final FtpServerManager ftpServerManager,
                                   final MessageProcessor messageProcessor,
                                   final SoapFaultManager soapFaultManager,
                                   final StashManagerFactory stashManagerFactory,
                                   final EventChannel messageProcessingEventChannel,
                                   final ContentTypeHeader overriddenContentType,
                                   final Goid hardwiredServiceGoid,
                                   final long maxRequestSize,
                                   final Goid connectorGoid,
                                   final ServiceManager serviceManager,
                                   final String initServiceUri) {
        this.ftpServerManager = ftpServerManager;
        this.messageProcessor = messageProcessor;
        this.soapFaultManager = soapFaultManager;
        this.stashManagerFactory = stashManagerFactory;
        this.messageProcessingEventChannel = messageProcessingEventChannel;
        this.overriddenContentType = overriddenContentType;
        this.hardwiredServiceGoid = hardwiredServiceGoid;
        this.maxRequestSize = maxRequestSize;
        this.connectorGoid = connectorGoid;
        this.serviceManager = serviceManager;
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
            logger.log(Level.FINE, "Handling " + FtpMethod.FTP_LOGIN.getWspName() + " for file ''{0}'' (unique:{1}).", new Object[] {fileName, unique});

        if (!ftpServerManager.isLicensed()) {
            if (logger.isLoggable(Level.INFO))
                logger.log(Level.INFO, "Failing " + FtpMethod.FTP_LOGIN.getWspName() + " (FTP server not licensed).");

            ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                    "Service not available (not licensed)."));
        } else {
            HybridDiagnosticContext.put(GatewayDiagnosticContextKeys.LISTEN_PORT_ID, connectorGoid.toString());
            HybridDiagnosticContext.put(GatewayDiagnosticContextKeys.CLIENT_IP, ftpSession.getClientAddress().getAddress().getHostAddress());
            User user = ftpSession.getUser();
            String file = ftpRequest.getArgument();

            String path = initServiceUri;

            if (unique) {
                ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_250_REQUESTED_FILE_ACTION_OKAY, file + ": Transfer started."));
            }

//            int storeResult = process(ftpSession, user, path, file, // TODO jwilliams: uncomment this when process method is available
//                    isSecureSession(ftpSession), unique, FtpMethod.FTP_LOGIN.getWspName(), "/");
//
//            if (storeResult == STORE_RESULT_FAULT) {
//                result = FtpletResult.DISCONNECT;
//            } else {
//                result = null;
//            }

            HybridDiagnosticContext.remove(GatewayDiagnosticContextKeys.LISTEN_PORT_ID);
            HybridDiagnosticContext.remove(GatewayDiagnosticContextKeys.CLIENT_IP);
        }

        return result;
    }
}

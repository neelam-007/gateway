package com.l7tech.server.transport.ftp;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.server.transport.ListenerException;
import com.l7tech.server.transport.tls.SsgConnectorSslHelper;
import com.l7tech.util.ExceptionUtils;
import org.apache.ftpserver.ssl.ClientAuth;
import org.springframework.beans.factory.annotation.Autowired;

import java.security.GeneralSecurityException;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class FtpSslFactory {

    @Autowired
    private FtpServerManager ftpServerManager;

    public FtpSsl create(SsgConnector connector) throws ListenerException {
        ClientAuth clientAuth;

        switch (connector.getClientAuth()) {
            case SsgConnector.CLIENT_AUTH_ALWAYS:
                clientAuth = ClientAuth.NEED;
                break;
            case SsgConnector.CLIENT_AUTH_OPTIONAL:
                clientAuth = ClientAuth.WANT;
                break;
            default:
                clientAuth = ClientAuth.NONE;
        }

        SsgConnectorSslHelper sslHelper;

        try {
            sslHelper = new SsgConnectorSslHelper(ftpServerManager, connector);
        } catch (GeneralSecurityException e) {
            throw new ListenerException("Unable to create SSL context: " + ExceptionUtils.getMessage(e), e);
        }

        return new FtpSsl(sslHelper, clientAuth);
    }
}

package com.l7tech.gateway.common.transport.ftp;

import org.springframework.transaction.annotation.Transactional;

/**
 * Stub-mode FTP admin interface.
 *
 * @author rmak
 * @since SecureSpan 4.0
 */
public class FtpAdminStub implements FtpAdmin {
    @Override
    @Transactional(readOnly = true)
    public void testConnection(boolean isFtps,
                               boolean isExplicit,
                               boolean isVerifyServerCert,
                               String hostName,
                               int port,
                               String userName,
                               String password,
                               boolean useClientCert,
                               long clientCertKeystoreId,
                               String clientCertKeyAlias,
                               String directory,
                               int timeout) throws FtpTestException {
    }
}

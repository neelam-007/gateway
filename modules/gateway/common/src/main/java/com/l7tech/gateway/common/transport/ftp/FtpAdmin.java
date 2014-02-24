/*
 * Copyright (C) 2007 Layer 7 Technologies, Inc.
 */

package com.l7tech.gateway.common.transport.ftp;

import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;

/**
 * Remote interface for supporting FTP routing assertion.
 *
 * @author rmak
 * @since SecureSpan 4.0
 */
@Administrative
@Secured
public interface FtpAdmin {
    /**
     * Tests connection to FTP(S) server and tries "cd" into remote directory.
     *
     * @param isFtps                true if FTPS; false if FTP (unsecured)
     * @param isExplicit            if FTPS: true if explicit FTPS, false if implicit FTPS
     * @param isVerifyServerCert    whether to verify FTPS server certificate using trusted certificate store; applies only if isFtps is true
     * @param hostName              host name of FTP(S) server
     * @param port                  port number of FTP(S) server
     * @param userName              user name to login in as
     * @param password              password to login with
     * @param useClientCert         whether to use client cert and private key for authentication
     * @param clientCertKeystoreId  ID of keystore to use if useClientCert is true; must be a valid ID if useClientCert is true
     * @param clientCertKeyAlias    key alias in keystore to use if useClientCert is true; must not be null if useClientCert is true
     * @param directory             remote directory to "cd" into; supply empty string if no "cd" wanted
     * @param timeout               connection timeout in milliseconds
     * @throws FtpTestException if connection test failed
     */
    @Transactional(readOnly=true)
    @Secured(stereotype = MethodStereotype.UNCHECKED_WIDE_OPEN)
    void testConnection(boolean isFtps,
                        boolean isExplicit,
                        boolean isVerifyServerCert,
                        String hostName,
                        int port,
                        String userName,
                        String password,
                        Goid passwordGoid,
                        boolean useClientCert,
                        @Nullable Goid clientCertKeystoreId,
                        String clientCertKeyAlias,
                        String directory,
                        int timeout) throws FtpTestException;
}

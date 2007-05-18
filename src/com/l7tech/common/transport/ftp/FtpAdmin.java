/*
 * Copyright (C) 2007 Layer 7 Technologies, Inc.
 */

package com.l7tech.common.transport.ftp;

import com.l7tech.common.security.rbac.Secured;
import org.springframework.transaction.annotation.Transactional;

import java.rmi.RemoteException;

/**
 * Remote interface for supporting FTP routing assertion.
 *
 * @author rmak
 * @since SecureSpan 4.0
 */
@Secured
public interface FtpAdmin {
    @Transactional(readOnly=true)
    void testConnection(boolean isFtps,
                        boolean isExplicit,
                        boolean isVerifyServerCert,
                        String hostName,
                        int port,
                        String userName,
                        String password,
                        String directory,
                        int timeout) throws RemoteException, FtpTestException;
}

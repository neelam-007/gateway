/*
 * Copyright (C) 2007 Layer 7 Technologies, Inc.
 */

package com.l7tech.service;

import com.l7tech.common.transport.ftp.FtpAdmin;
import com.l7tech.common.transport.ftp.FtpTestException;
import org.springframework.transaction.annotation.Transactional;

import java.rmi.RemoteException;

/**
 * Stub-mode FTP admin interface.
 *
 * @author rmak
 * @since SecureSpan 4.0
 */
public class FtpAdminStub implements FtpAdmin {
    @Transactional(readOnly = true)
    public void testConnection(boolean isFtps, boolean isExplicit, boolean isVerifyServerCert, String hostName, int port, String userName, String password, String directory, int timeout) throws RemoteException, FtpTestException {
    }
}

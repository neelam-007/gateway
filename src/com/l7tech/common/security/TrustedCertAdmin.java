/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security;

import com.l7tech.objectmodel.*;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * @author alex
 * @version $Revision$
 */
public interface TrustedCertAdmin extends Remote {
    public List findAllCerts() throws FindException, RemoteException;
    public TrustedCert findCertByPrimaryKey(long oid) throws FindException, RemoteException;
    public long saveCert(TrustedCert cert) throws SaveException, UpdateException, VersionException, RemoteException;
    public void deleteCert(long oid) throws FindException, DeleteException, RemoteException;
    public X509Certificate[] retrieveCertFromUrl(String url) throws CertificateException, RemoteException, IOException;
    public X509Certificate[] retrieveCertFromUrl(String url, boolean ignoreHostname) throws CertificateException, RemoteException, IOException;
}

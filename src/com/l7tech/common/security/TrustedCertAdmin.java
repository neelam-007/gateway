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
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.util.List;

/**
 * @author alex
 * @version $Revision$
 */
public interface TrustedCertAdmin extends Remote {

    /**
     * Retrieves all {@link TrustedCert}s from the database.
     * @return a {@link List} of {@link TrustedCert}s
     * @throws FindException
     * @throws RemoteException
     */
    public List findAllCerts() throws FindException, RemoteException;

    /**
     * Retrieves the {@link TrustedCert} with the specified oid.
     * @param oid the oid of the {@link TrustedCert} to retrieve
     * @return
     * @throws FindException
     * @throws RemoteException
     */
    public TrustedCert findCertByPrimaryKey(long oid) throws FindException, RemoteException;

    /**
     * Retrieves the {@link TrustedCert} with the specified subject DN.
     * @param dn the Subject DN of the {@link TrustedCert} to retrieve
     * @return
     * @throws FindException
     * @throws RemoteException
     */
    public TrustedCert findCertBySubjectDn(String dn) throws FindException, RemoteException;

    /**
     * Saves a new or existing {@link TrustedCert} to the database.
     * @param cert the {@link TrustedCert} to be saved
     * @return
     * @throws SaveException
     * @throws UpdateException
     * @throws VersionException
     * @throws RemoteException
     */
    public long saveCert(TrustedCert cert) throws SaveException, UpdateException, VersionException, RemoteException;

    /**
     * Removes the specified {@link TrustedCert} from the database.
     * @param oid the oid of the {@link TrustedCert} to be deleted
     * @throws FindException if the {@link TrustedCert} cannot be found
     * @throws DeleteException if the {@link TrustedCert} cannot be deleted
     * @throws RemoteException
     */
    public void deleteCert(long oid) throws FindException, DeleteException, RemoteException;

    public static class HostnameMismatchException extends Exception {
        public HostnameMismatchException(String certName, String hostname) {
            super("SSL Certificate with DN '" + certName + "' does not match the expected hostname '" + hostname + "'");
        }
    }

    /**
     * Retrieves the {@link X509Certificate} chain from the specified URL.
     * @param url the url from which to retrieve the cert.
     * @return an {@link X509Certificate} chain.
     * @throws IOException if the certificate cannot be retrieved for whatever reason.
     * @throws IllegalArgumentException if the URL does not start with "https://"
     * @throws RemoteException
     */
    public X509Certificate[] retrieveCertFromUrl(String url) throws RemoteException, IOException, HostnameMismatchException;
    public X509Certificate[] retrieveCertFromUrl(String url, boolean ignoreHostname) throws RemoteException, IOException, HostnameMismatchException;

    /**
     * Get the ssg's root cert
     */
    public X509Certificate getSSGRootCert() throws IOException, CertificateException, RemoteException;
}

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
 * Remote interface to get/save/delete certs trusted by the gateway.
 */
public interface TrustedCertAdmin extends Remote {

    /**
     * Retrieves all {@link TrustedCert}s from the database.
     * @return a {@link List} of {@link TrustedCert}s
     * @throws FindException if there was a server-side problem accessing the requested information
     * @throws RemoteException on remote communication error
     */
    public List findAllCerts() throws FindException, RemoteException;

    /**
     * Retrieves the {@link TrustedCert} with the specified oid.
     * @param oid the oid of the {@link TrustedCert} to retrieve
     * @return the TrustedCert or null if no cert for that oid
     * @throws FindException if there was a server-side problem accessing the requested information
     * @throws RemoteException on remote communication error
     */
    public TrustedCert findCertByPrimaryKey(long oid) throws FindException, RemoteException;

    /**
     * Retrieves the {@link TrustedCert} with the specified subject DN.
     * @param dn the Subject DN of the {@link TrustedCert} to retrieve
     * @return the TrustedCert or null if no cert for that oid
     * @throws FindException
     * @throws RemoteException on remote communication error
     */
    public TrustedCert findCertBySubjectDn(String dn) throws FindException, RemoteException;

    /**
     * Saves a new or existing {@link TrustedCert} to the database.
     * @param cert the {@link TrustedCert} to be saved
     * @return the object id (oid) of the newly saved cert
     * @throws SaveException if there was a server-side problem saving the cert
     * @throws UpdateException if there was a server-side problem updating the cert
     * @throws VersionException if the updated cert was not up-to-date (updating an old version)
     * @throws RemoteException on remote communication error
     */
    public long saveCert(TrustedCert cert) throws SaveException, UpdateException, VersionException, RemoteException;

    /**
     * Removes the specified {@link TrustedCert} from the database.
     * @param oid the oid of the {@link TrustedCert} to be deleted
     * @throws FindException if the {@link TrustedCert} cannot be found
     * @throws DeleteException if the {@link TrustedCert} cannot be deleted
     * @throws RemoteException on remote communication error
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
     * @throws RemoteException on remote communication error
     * @throws IOException if the certificate cannot be retrieved for whatever reason.
     * @throws IllegalArgumentException if the URL does not start with "https://"
     * @throws HostnameMismatchException if the hostname did not match the cert's subject
     */
    public X509Certificate[] retrieveCertFromUrl(String url) throws RemoteException, IOException, HostnameMismatchException;

    /**
     * Retrieves the {@link X509Certificate} chain from the specified URL.
     * @param url the url from which to retrieve the cert.
     * @param ignoreHostname whether or not the hostname match should be ignored when doing ssl handshake
     * @return an {@link X509Certificate} chain.
     * @throws RemoteException on remote communication error
     * @throws IOException if the certificate cannot be retrieved for whatever reason.
     * @throws HostnameMismatchException if the hostname did not match the cert's subject
     */
    public X509Certificate[] retrieveCertFromUrl(String url, boolean ignoreHostname) throws RemoteException, IOException, HostnameMismatchException;

    /**
     * Get the gateway's root cert.
     * @return the gateway's root cert
     * @throws IOException if the certificate cannot be retrieved
     * @throws CertificateException if the certificate cannot be retrieved
     * @throws RemoteException on remote communication error
     */
    public X509Certificate getSSGRootCert() throws IOException, CertificateException, RemoteException;
}

/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.common.security;

import static com.l7tech.common.security.rbac.EntityType.TRUSTED_CERT;
import static com.l7tech.common.security.rbac.EntityType.SSG_KEY_ENTRY;
import static com.l7tech.common.security.rbac.MethodStereotype.*;
import com.l7tech.common.security.rbac.Secured;
import com.l7tech.common.security.rbac.MethodStereotype;
import com.l7tech.objectmodel.*;
import com.l7tech.server.security.keystore.SsgKeyEntry;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.KeyStoreException;
import java.security.InvalidAlgorithmParameterException;
import java.security.GeneralSecurityException;
import java.util.List;

/**
 * Remote interface to get/save/delete certs trusted by the gateway.
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
@Secured(types=TRUSTED_CERT)
public interface TrustedCertAdmin  {
    /**
     * Retrieves all {@link TrustedCert}s from the database.
     * @return a {@link List} of {@link TrustedCert}s
     * @throws FindException if there was a server-side problem accessing the requested information
     * @throws RemoteException on remote communication error
     */
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_ENTITIES)
    public List<TrustedCert> findAllCerts() throws FindException, RemoteException;

    /**
     * Retrieves the {@link TrustedCert} with the specified oid.
     * @param oid the oid of the {@link TrustedCert} to retrieve
     * @return the TrustedCert or null if no cert for that oid
     * @throws FindException if there was a server-side problem accessing the requested information
     * @throws RemoteException on remote communication error
     */
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_BY_PRIMARY_KEY)
    public TrustedCert findCertByPrimaryKey(long oid) throws FindException, RemoteException;

    /**
     * Retrieves the {@link TrustedCert} with the specified subject DN.
     * @param dn the Subject DN of the {@link TrustedCert} to retrieve
     * @return the TrustedCert or null if no cert for that oid
     * @throws FindException
     * @throws RemoteException on remote communication error
     */
    @Transactional(readOnly=true)
    @Secured(stereotype= FIND_ENTITY_BY_ATTRIBUTE)
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
    @Secured(stereotype=SAVE_OR_UPDATE)
    public long saveCert(TrustedCert cert) throws SaveException, UpdateException, VersionException, RemoteException;

    /**
     * Removes the specified {@link TrustedCert} from the database.
     * @param oid the oid of the {@link TrustedCert} to be deleted
     * @throws FindException if the {@link TrustedCert} cannot be found
     * @throws DeleteException if the {@link TrustedCert} cannot be deleted
     * @throws RemoteException on remote communication error
     */
    @Secured(stereotype= DELETE_BY_ID)
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
    @Transactional(propagation=Propagation.SUPPORTS)
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
    @Transactional(propagation=Propagation.SUPPORTS)
    public X509Certificate[] retrieveCertFromUrl(String url, boolean ignoreHostname) throws RemoteException, IOException, HostnameMismatchException;

    /**
     * Get the gateway's root cert.
     * @return the gateway's root cert
     * @throws IOException if the certificate cannot be retrieved
     * @throws CertificateException if the certificate cannot be retrieved
     * @throws RemoteException on remote communication error
     */
    @Transactional(propagation=Propagation.SUPPORTS)
    public X509Certificate getSSGRootCert() throws IOException, CertificateException, RemoteException;

    /**
     * Get the gateway's SSL cert.
     * @return the gateway's SSL cert
     * @throws IOException if the certificate cannot be retrieved
     * @throws CertificateException if the certificate cannot be retrieved
     * @throws RemoteException on remote communication error
     */
    @Transactional(propagation=Propagation.SUPPORTS)
    public X509Certificate getSSGSslCert() throws IOException, CertificateException, RemoteException;

    /**
     * Represents general information about a Keystore instance available on this Gateway.
     */
    public static class KeystoreInfo implements Serializable {
        private static final long serialVersionUID = 2340872398471981L;
        public final long id;
        public final String name;
        public final String type;
        public final boolean readonly;

        public KeystoreInfo(long id, String name, String type, boolean readonly) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.readonly = readonly;
        }
    }

    /**
     * Find all keystore instances available on this Gateway.
     *
     * @return a List of KeystoreInfo.  Always contains at least one keystore, although it may be read-only.
     * @throws IOException if there is a problem reading necessary keystore data
     * @throws RemoteException on remote communication error
     * @throws FindException if there is a problem getting info from the database
     * @throws java.security.KeyStoreException if a keystore is corrupt
     */
    @Transactional(propagation=Propagation.SUPPORTS)
    @Secured(stereotype=FIND_ENTITIES)
    public List<KeystoreInfo> findAllKeystores() throws IOException, FindException, KeyStoreException;

    /**
     * Retrieves all SsgKeyEntry instances available on this Gateway node.
     *
     * @param keystoreId the key store in which to find the key entries.
     * @return a List of SsgKeyEntry.  May be empty but never null.
     * @throws IOException if there is a problem reading necessary keystore data
     * @throws CertificateException if the keystore contents are corrupt
     * @throws RemoteException on remote communication error
     * @throws FindException if there is a problem getting info from the database
     */
    @Transactional(propagation=Propagation.SUPPORTS)
    @Secured(stereotype=FIND_ENTITIES, types=SSG_KEY_ENTRY)
    public List<SsgKeyEntry> findAllKeys(long keystoreId) throws IOException, CertificateException, FindException;


    /**
     * Destroys an SsgKeyEntry identified by its keystore ID and entry alias.
     *
     * @param keystoreId  the keystore in which to destroy an entry.  Required.
     * @param keyAlias    the alias of hte entry which is to be destroyed.  Required.
     */
    @Transactional(propagation=Propagation.REQUIRED)
    @Secured(stereotype=DELETE_MULTI, types=SSG_KEY_ENTRY)
    void deleteKey(long keystoreId, String keyAlias) throws IOException, CertificateException, DeleteException;

    /**
     * Generate a new RSA key pair and self-signed certificate in the specified keystore with the specified
     * settings.
     *
     * @param keystoreId the key store in which to create the new key pair and self-signed cert.
     * @param alias the alias to use when saving the new key pair and self-signed cert.  Required.
     * @param dn the DN to use in the new self-signed cert.  Required.
     * @param keybits number of bits for the new RSA key, ie 512, 768, 1024 or 2048.  Required.
     * @param expiryDays number of days the self-signed cert should be valid.  Required.
     * @return the new self-signed certificate, with a public key corresponding to the new private key.  Never null.
     * @throws RemoteException on remote communication error
     * @throws FindException if there is a problem getting info from the database
     * @throws java.security.GeneralSecurityException if there is a problem generating or signing the cert
     * @throws IllegalArgumentException if the keybits or dn are improperly specified
     */
    @Transactional(propagation=Propagation.REQUIRED)
    @Secured(stereotype= MethodStereotype.SET_PROPERTY_BY_UNIQUE_ATTRIBUTE, types=SSG_KEY_ENTRY)
    public X509Certificate generateKeyPair(long keystoreId, String alias, String dn, int keybits, int expiryDays) throws RemoteException, FindException, GeneralSecurityException;
}

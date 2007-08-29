/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Provides a programmatic interface to the Bridge's message processor.  Use {@link SecureSpanBridgeFactory} to
 * obtain an implementation of this interface.
 *
 * @author mike
 * @version 1.0
 */
public interface SecureSpanBridge {
    /**
     * This is the result returned from a call to send().
     */
    public static interface Result {
        /**
         * Check the HTTP status of the result.
         * @return the HTTP status, e.g., 200 for success or 500 for a fault
         */
        int getHttpStatus();

        /**
         * Get the response document, if the response was XML.
         *
         * @return the response as a DOM document.  Never null.
         * @throws IOException if there is a problem reading the response
         * @throws SAXException if the response isn't declared as XML, or if the XML is not well-formed
         */
        Document getResponse() throws IOException, SAXException;
    }

    /**
     * Extended result information returned from SSB API version 4.2.0 and higher.
     * This allows for non-XML responses.
     */
    public static interface MimeResult extends Result {
        /**
         * Check if the response was declared as XML.
         *
         * @return true if a call to getResponse() would return a Document.
         * @throws IOException if there is a problem reading the response
         */
        boolean isResponseXml() throws IOException;

        /**
         * Get the reponse content type.
         *
         * @return the full content type of the response, e.g., "text/xml; charset=utf-8".
         * @throws IOException if there is a problem reading the response
         */
        String getContentType() throws IOException;

        /**
         * Get the response content length.
         *
         * @return the length of the response body in bytes, or -1 if not known in advance.
         * @throws IOException if there is a problem reading the response
         */
        long getContentLength() throws IOException;

        /**
         * Get the raw response bytes as a stream.
         *
         * @return an InputStream that will produce the response bytes.  Never null.
         * @throws IOException if there is a problem reading the response
         */
        InputStream getResponseStream() throws IOException;

        /**
         * Get the response as a byte array.  This requires that the entire response
         * can fit into memory.
         *
         * @return the response body as a byte array.  Never null, but may be empty.
         * @throws IOException if there is a problem reading the response
         */
        byte[] getResponseBytes() throws IOException;
    }

    /** Thrown if there is a problem sending a message to the Gateway. */
    public static class SendException extends Exception {}

    /** Thrown if a bad username or password was detected. */
    public static class BadCredentialsException extends Exception {}

    /**
     * Thrown if a new client certificate cannot be obtained because the Gateway has already issued
     * a client certificate for this account.
     */
    public static class CertificateAlreadyIssuedException extends Exception {}

    /**
     * Set a static policy to use for subsequent requests.
     * Setting a static policy overrides the internal policy cache and disables
     * dynamic policy discovery.
     * <p>
     * Internally, a cache of policy XML String instances to parsed policy trees is kept.  Thus, switching
     * among a small set of policy XML strings by repeatedly calling this method does not require the same policies
     * XML String to be reparsed on subsequent calls.
     *
     * @param policyXml  a new static policy, or null to disable static policies and re-enable dynamic policy discovery.
     * @throws SAXException if the specified policy XML cannot be parsed, or is not a valid policy document.
     */
    void setStaticPolicy(String policyXml) throws SAXException;

    /**
     * Send a message to the service through the Gateway.
     * <p>
     * If the Gateway indicates that a specific security policy is required for this request, the Bridge will
     * download the policy and cache it, applying it to this and future requests with the same SOAPAction, body
     * namespace URI, and URI local part.
     * <p>
     * If the Gateway SSL server certificate is required but has not yet been discovered or imported, Gateway SSL
     * server certificate discovery will be attempted using the currently configured credentials.
     * <p>
     * The Bridge will apply for a client certificate if necessary.
     *
     * @param soapAction the SOAPAction header to use for this message, including surrounding double-quote characters
     * @param message the SOAPEnvelope containing the message to send, as a DOM tree
     * @return the {@link Result} returned by the Gateway.  Never null.
     *         As of SecureSpanBridge version 4.2.0, this can always by safely downcast to a {@link MimeResult}.
     * @throws SendException                if the operation failed due to one of the following problems:
     *      a client certificate was required but could not be obtained;
     *      this request cannot succeed until the client or server configuration is changed;
     *      we were unable to conform to the policy, and did not get any useful SOAP fault from the Gateway;
     *      the Gateways's SSL certificate could not be obtained, validated, and/or saved;
     *      the response from the Gateway was signed, but the signature did not validate.
     * @throws BadCredentialsException      if the username or password was not accepted by the Gateway
     * @throws IOException                  if information couldn't be obtained from the SSG due to network trouble; or,
     *                                      if a certificate could not be saved to disk
     * @throws CertificateAlreadyIssuedException if we need a client cert but the Gateway has already issued us one
     */
    Result send(String soapAction, Document message) throws SendException, IOException, BadCredentialsException,
                                                            CertificateAlreadyIssuedException;

    /**
     * Send a message to the service through the Gateway.  This method behaves identically to
     * {@link #send(String, org.w3c.dom.Document)},
     * except it takes the message as an XML string instead of a DOM Document.
     *
     * @param soapAction the SOAPAction header to use for this message, including surrounding double-quote characters
     * @param message the SOAPEnvelope containing the message to send, as a String containing XML
     * @return the {@link Result} returned by the Gateway.  Never null.
     * @throws SAXException if the provided message was not well-formed XML
     * @throws SendException see {@link #send(String, org.w3c.dom.Document)}
     * @throws BadCredentialsException see {@link #send(String, org.w3c.dom.Document)}
     * @throws IOException see {@link #send(String, org.w3c.dom.Document)}
     * @throws CertificateAlreadyIssuedException if we need a client cert but the Gateway has already issued us one
     */
    Result send(String soapAction, String message) throws SAXException, SendException, IOException, BadCredentialsException,
                                                          CertificateAlreadyIssuedException;
    /**
     * Get the local part of the URI that will be reported to the Gateway as the L7-Original-URI.
     *
     * @return the local part of the L7-Original-URI: header.
     */
    String getUriLocalPart();

    /**
     * Set the local part of the URI to report to the Gateway as the L7-Original-URI header.  When using the
     * SecureSpan Bridge API, this defaults to a dummy URI.  This may need to be customized to provide
     * extra information to the Gateway's service resolver in the case of a non-SOAP XML web application, whose
     * messages typically do not have SOAP bodies or SOAPAction headers.
     *
     * @param uriLocalPart the new local part to report in the L7-Original-URI header in subsequent requests.
     */
    void setUriLocalPart(String uriLocalPart);

    /**
     * Get the Gateway's CA certificate, or null if we don't yet know it.
     * @return The Gateway's CA certificate, or null if we don't yet know it.
     * @throws IOException if the certificate was not recoverable due to a corrupt keystore.
     */
    X509Certificate getServerCert() throws IOException;

    /**
     * Get the Bridge client certificate, if any.
     *
     * @return The client certificate, or null if there is no client certificate configured.
     * @throws IOException if the certificate was not recoverable due to a corrupt keystore.
     */
    X509Certificate getClientCert() throws IOException;

    /**
     * Get the private key for the client certificate, if any.
     *
     * @return The private key, or null if there is no client certificate configured.
     * @throws BadCredentialsException if the private key could not be decrypted due to a bad password
     * @throws IOException if the keystore was corrupt
     * @throws RuntimeException if the cryptographic algorithm needed for this key is not available
     */
    PrivateKey getClientCertPrivateKey() throws BadCredentialsException, IOException;

    /**
     * Ensure the Gateway SSL certificate and Bridge's client ceritificate are available locally.  If the Gateway
     * SSL certificate has not yet been discovered or imported, Gateway SSL certificate discovery will be attempted.
     * If no Bridge client certificate has yet been obtained or imported, the Bridge will generate a new private key
     * and send a certificate signing request to the Gateway to apply for a new client certificate.
     *
     * @throws IOException if there was a network problem downloading the server cert; or,
     *                     if there was a problem reading or writing the keystore file; or,
     *                     if the keystore was corrupt
     * @throws BadCredentialsException if the downloaded Gateway SSL cert could not be verified with the configured username and password
     * @throws GeneralSecurityException (specifically, CertificateException) if the Gateway provides an invalid certificate; or,
     *                                  for miscellaneous and mostly unlikely certificate or key store problems
     * @throws CertificateAlreadyIssuedException if the Gateway has already issued a client certificate to this account
     */
    void ensureCertificatesAreAvailable() throws IOException, BadCredentialsException, GeneralSecurityException, CertificateAlreadyIssuedException;

    /**
     * Manually import the Gateway SSL certificate for this Gateway.  This may not be needed in some configurations:
     * the SecureSpan Bridge can safely discover the Gateway SSL automatically in cases where the Gateway has access
     * to the plaintext password for your account.
     * <p>
     * This method will replace the server certificate in the current Cert Store with the specified certificate.
     * This method does not require the ability to reach the Gateway via the network.
     *
     * @param serverCert the X509 certificate to use for this Gateway.  Will be saved to the Bridge's Cert Store, and
     *                   used for trusting future SSL connections to the Gateway, and for WS-Security encryption of
     *                   messages bound for the Gateway.
     * @throws IOException if there is a problem with the specified certificate; or,
     *                     if the Cert Store cannot be written; or,
     *                     if the Cert Store is corrupt
     */
    void importServerCert(X509Certificate serverCert) throws IOException;

    /**
     * Manually import a client certificate to use with this Gateway.  This may not be needed in some configurations:
     * if a client certificate is needed and has not been manually imported, the SecureSpan Bridge will automatically
     * try to apply for one from the Gateway using the currently configured username and password.
     * <p>
     * The client certificate and corresponding private key Will be saved to the Bridge Key Store and
     * the public portion (the certificate) will also be cached in the Cert Store.
     * <p>
     * The currently configured password will be used as the encryption password for the Key Store.
     *
     * @param clientCert the X509 certificate to use for this account
     * @param clientKey the private key corresponding to the public key in getCachedClientCert
     * @throws IOException if there is a problem with the specified certificate; or,
     *                     if the Key Store or Cert Store cannot be written; or,
     *                     if the Key Store or Cert Store is corrupt; or,
     *                     if there is an existing Key Store encrypted with a password other than the current password
     */
    void importClientCert(X509Certificate clientCert, PrivateKey clientKey) throws IOException;

    /**
     * Manually import a client certificate from the specified PKCS#12 file.  This may not be needed in some
     * configurations: if a client certificate is needed and has not been manually imported, the SecureSpan Bridge
     * will automatically try to apply for one from the Gateway using the currently configured username and password.
     * <p>
     * The specified PKCS#12 file will be decrypted with the specified password, and will be searched for an
     * entry with the specified alias.  This entry will be read, and the X509 certificate and corresponding
     * private key will be copied into the Bridge Key Store.  The public portion (the certificate) will also
     * be cached in the Cert Store.
     * <p>
     * The currently configured Bridge password will be used as the encryption password for the Bridge Key Store.
     *
     * @param pkcs12Path a File identifying a readable PKCS#12 file on disk
     * @param alias the name of the entry within this file to be imported, or null to import the first entry
     *              that contains a certificate and private key
     * @param pkcs12Password the password to use to decrypt the specified PKCS#12 file
     * @throws IOException if the specified key store file cannot be read; or,
     *                     if the specified alias is not present in the file, or does not contain a private key; or,
     *                     if the Key Store or Cert Store cannot be written; or,
     *                     if the Key Store or Cert Store is corrupt; or,
     *                     if there is an existing Key Store encrypted with a password other than the current password
     */
    void importClientCert(File pkcs12Path, String alias, char[] pkcs12Password) throws IOException;

    /**
     * Delete your client certificate from the keystore, perhaps so that you can apply for a new one.
     * <b><i>Note that this only deletes your local client-side copy of your certificate.
     * The Gateway will not allow you to obtain a new client certificate until a Gateway
     * administrator has revoked your server-side copy of the client certificate.</i></b>
     * @throws IOException if the client certificate could not be removed, or
     *                     if there was a problem saving the keystore to disk.
     */
    void destroyClientCertificate() throws IOException;
}

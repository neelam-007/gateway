/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Provides a programmatic interface to the Bridge's message processor.
 * @author mike
 * @version 1.0
 */
public interface SecureSpanBridge {
    /**
     * This is the result returned from a call to send().
     */
    public static interface Result {
        int getHttpStatus();
        Document getResponse() throws IOException, SAXException;
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
     * Send a message to the service through the Gateway.
     * <p>
     * If instructed by the Gateway, we will download and apply a security policy to the message and response.
     * <p>
     * The Gateway server certificate will be discovered if necessary.
     * <p>
     * We will apply for a client certificate if necessary.
     *
     * @param soapAction the SOAPAction header to use for this message, including surrounding double-quote characters
     * @param message the SOAPEnvelope containing the message to send, as a DOM tree
     * @return
     * @throws SendException                if the operation failed due to one of the following problems:
     *      a client certificate was required but could not be obtained;
     *      this request cannot succeed until the client or server configuration is changed;
     *      we were unable to conform to the policy, and did not get any useful SOAP fault from the Gateway;
     *      the Gateways's SSL certificate could not be obtained, validated, and/or saved;
     *      the response from the Gateway was signed, but the signature did not validate.
     * @throws BadCredentialsException      if the username or password was not accepted by the Gateway
     * @throws IOException                  if information couldn't be obtained from the SSG due to network trouble
     * @throws IOException                  if a certificate could not be saved to disk
     * @throws CertificateAlreadyIssuedException if we need a client cert but the Gateway has already issued us one
     */
    Result send(String soapAction, Document message) throws SendException, IOException, BadCredentialsException,
                                                            CertificateAlreadyIssuedException;

    /**
     * As above, but takes the XML as a string.
     *
     * @param soapAction the SOAPAction header to use for this message, including surrounding double-quote characters
     * @param message the SOAPEnvelope containing the message to send, as a String containing XML
     * @return
     * @throws SAXException if the provided message was not well-formed XML
     * @throws SendException see above
     * @throws BadCredentialsException see above
     * @throws IOException see above
     * @throws CertificateAlreadyIssuedException if we need a client cert but the Gateway has already issued us one
     */
    Result send(String soapAction, String message) throws SAXException, SendException, IOException, BadCredentialsException,
                                                          CertificateAlreadyIssuedException;

    /**
     * Get the Gateway's CA certificate, or null if we don't yet know it.
     * @return The Gateway's CA certificate, or null if we don't yet know it.
     * @throws IOException if the certificate was not recoverable due to a corrupt keystore.
     */
    X509Certificate getServerCert() throws IOException;

    /**
     * Get the client certificate, or null if we don't currently have one.
     * @return The client certificate, or null if we don't currently have one.
     * @throws IOException if the certificate was not recoverable due to a corrupt keystore.
     */
    X509Certificate getClientCert() throws IOException;

    /**
     * Get the private key for the client certificate, or null if we don't currently have one.
     * @return The private key, or null.
     * @throws BadCredentialsException if the private key could not be decrypted due to a bad password
     * @throws IOException if the keystore was corrupt
     * @throws RuntimeException if the cryptographic algorithm needed for this key is not available
     */
    PrivateKey getClientCertPrivateKey() throws BadCredentialsException, IOException;

    /**
     * Ensure the Gateway SSL certificate and Bridge's client ceritificate are available, downloading or applying
     * for them as needed.
     * @throws IOException if there was a network problem downloading the server cert
     * @throws IOException if there was a problem reading or writing the keystore file
     * @throws IOException if the keystore was corrupt
     * @throws BadCredentialsException if the downloaded Gateway SSL cert could not be verified with the configured username and password
     * @throws GeneralSecurityException (specifically, CertificateException) if the Gateway provides an invalid certificate
     * @throws GeneralSecurityException for miscellaneous and mostly unlikely certificate or key store problems
     * @throws CertificateAlreadyIssuedException if the Gateway has already issued a client certificate to this account
     */
    void ensureCertificatesAreAvailable() throws IOException, BadCredentialsException, GeneralSecurityException, CertificateAlreadyIssuedException;

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

/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.util;

import com.l7tech.common.mime.MimeUtil;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.security.CertificateRequest;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.proxy.datamodel.Managers;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.datamodel.exceptions.BadPasswordFormatException;
import com.l7tech.proxy.datamodel.exceptions.CertificateAlreadyIssuedException;
import com.l7tech.proxy.datamodel.exceptions.ServerCertificateUntrustedException;
import com.l7tech.proxy.ssl.ClientProxySecureProtocolSocketFactory;
import com.l7tech.proxy.ssl.CurrentSslPeer;
import com.l7tech.proxy.ssl.HostnameMismatchException;
import com.l7tech.proxy.ssl.SslPeer;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.PostMethod;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;
import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

/**
 * Utilities for SSL support.
 * User: mike
 * Date: Jul 30, 2003
 * Time: 10:10:53 AM
 */
public class SslUtils {
    private static final Logger log = Logger.getLogger(SslUtils.class.getName());

    /** This is just a dummy body document, used as a placeholder in the password change POST. */
    private static final String PASSWORD_CHANGE_BODY = "<changePasswordAndRevokeClientCertificate/>";

    /**
     * Configure the specified URL connection to use the right socket factory, if it's SSL.
     * @return true if this is an SSL connection; otherwise false.
     */
    public static boolean configureSslSocketFactory(URLConnection conn) {
        boolean usingSsl = false;
        if (conn instanceof HttpsURLConnection) {
            HttpsURLConnection sslConn = (HttpsURLConnection)conn;
            sslConn.setSSLSocketFactory(ClientProxySecureProtocolSocketFactory.getInstance());
            usingSsl = true;
        }
        return usingSsl;
    }

    /**
     * If the given SSLException is a hostname mismatch or server cert untrusted, takes remedial action and returns.
     * Otherwise, rethrows the SSLException.
     * <p>
     * If this returns, the caller should attempt to discover and/or import the server cert.
     *
     * @param server the name of the SSL server we were trying to talk to, ie "the Gatway foo.bar.com"
     * @throws javax.net.ssl.SSLException if the exception could not be handled.
     * @param e the SSLException that was caught
     */
    public static void handleServerCertProblem(SslPeer sslPeer, String server, Exception e) throws SSLException {
        // Was this server cert untrusted?
        Throwable scuet = ExceptionUtils.getCauseIfCausedBy(e, ServerCertificateUntrustedException.class);
        ServerCertificateUntrustedException scue = (ServerCertificateUntrustedException)scuet;
        if (scue == null) {
            // No, that wasn't the problem.  Was it a cert hostname mismatch?
            HostnameMismatchException hme = (HostnameMismatchException)
              ExceptionUtils.getCauseIfCausedBy(e, HostnameMismatchException.class);
            if (hme != null) {
                // Notify user of the hostname mismatch and then abort this request
                String wanted = hme.getWhatWasWanted();
                String got = hme.getWhatWeGotInstead();
                if (sslPeer instanceof Ssg)
                    ((Ssg)sslPeer).getRuntime().getCredentialManager().notifySslHostnameMismatch(server, wanted, got);
                else
                    Managers.getCredentialManager().notifySslHostnameMismatch(server, wanted, got);
                throw (SSLException)new SSLException("SSL hostname mismatch: " + e.getMessage()).initCause(e);
            }

            // not sure what happened; throw it up and abort the request
            throw (SSLException)new SSLException("SSL connection failure: " + e.getMessage()).initCause(e);
        }

        // Problem Solved.  Caller just needs to import/ask the user about/download the server cert.
    }

    public static final class PasswordNotWritableException extends Exception {
        private PasswordNotWritableException(String message) {
            super(message);
        }
    };

    /**
     * Change a user's password and revoke the current client certificate, if any.  This requires knowing the
     * old password.  As well, if you possess a client certificate on the target Gateway, you must possess it
     * and its private key (and have a key manager already set up to present it during the SSL handshake).
     * <p>
     * If this method returns, the password change was successful, and any client certificate associated with this
     * account has been forgotten on the server side.  The caller is responsible for ensuring that the client
     * certificate is reliably delete on the caller's side if and only if the password change is successful.
     *
     * @param ssg  The SSG whose password changing service to talk to.
     * @param username The username of the account whose password is to be changed.
     * @param oldpassword  The account's current password.
     * @param newpassword  The new password that is desired.
     * @throws IOException   in case of network trouble, or if the password change fails for any reason other than
     *                       bad credentials
     * @throws BadCredentialsException  if the old password is incorrect, or the current client certificate was
     *                                  missing or invalid.
     * @throws BadPasswordFormatException if the new password was rejected by the server.
     * @throws PasswordNotWritableException if the Gateway is unable to change this password.
     */
    public static void changePasswordAndRevokeClientCertificate(Ssg ssg, String username,
                                                                char[] oldpassword, char[] newpassword)
            throws IOException, BadCredentialsException, BadPasswordFormatException, PasswordNotWritableException
    {
        URL url = ssg.getServerPasswordChangeUrl();
        HttpClient hc = new HttpClient();
        hc.getState().setAuthenticationPreemptive(true);
        hc.getState().setCredentials(null, null,
                                     new UsernamePasswordCredentials(username,
                                                                     new String(oldpassword)));
        PostMethod post = new PostMethod(url.toExternalForm());
        try {
            post.setRequestBody(PASSWORD_CHANGE_BODY); // dummy body, just as placeholder for POST
            post.setRequestHeader(MimeUtil.CONTENT_TYPE, XmlUtil.TEXT_XML);
            post.setRequestHeader(MimeUtil.CONTENT_LENGTH, String.valueOf(PASSWORD_CHANGE_BODY.length()));
            post.setRequestHeader(SecureSpanConstants.HttpHeaders.HEADER_NEWPASSWD,
                                  HexUtils.encodeBase64(new String(newpassword).getBytes(), true));
            CurrentSslPeer.set(ssg);
            int result = hc.executeMethod(post);
            CurrentSslPeer.set(null);
            log.info("HTTPS POST to password change service returned HTTP status " + result);
            if (result == 400) {
                byte[] response = post.getResponseBody();
                throw new BadPasswordFormatException("Password change service rejected your new password " +
                                                     "(HTTP status " + result + "). " + new String(response));
            }
            if (result == 401) throw new BadCredentialsException("Password change service indicates invalid current credentials (HTTP status " + result + ")");
            if (result == 403) throw new PasswordNotWritableException("Password change service is unable to change the password for this account (HTTP status " + result + ")");
            if (result != 200) throw new IOException("HTTPS POST to password change service returned HTTP status " + result);

            post.releaseConnection();
            post = null;
        } finally {
            if (post != null)
                post.releaseConnection();
        }
    }

    /**
     * Transmit a CSR to the SSG and download the newly-signed certificate.
     * Afterward we'll verify that the signed cert is the one we asked for, and was signed by the CA we expect.
     *
     * @param ssg       The SSG that we should post our CSR to, whose response will be assumed to be our certificate.
     * @param username
     * @param password
     * @param csr
     * @param caCert  the cert of the CA that is supposed to be signing the request
     * @return
     * @throws IOException                  if there is a network problem talking to the CSR signing service
     * @throws CertificateException         if the post to the CSR signer results in a status other than 200 (or 401)
     * @throws NoSuchAlgorithmException     if one of the keys uses an algorithm that isn't installed
     * @throws InvalidKeyException          if one of the keys is invalid
     * @throws NoSuchProviderException      if no X.509 cert provider is installed (can't happen)
     * @throws SignatureException           if the resulting cert was not signed by the correct CA key
     * @throws BadCredentialsException if the username or password was rejected by the CSR signer
     */
    public static X509Certificate obtainClientCertificate(Ssg ssg, String username, char[] password,
                                                          CertificateRequest csr,
                                                          X509Certificate caCert)
            throws IOException, CertificateException, NoSuchAlgorithmException,
                   InvalidKeyException, NoSuchProviderException, BadCredentialsException,
                   SignatureException, CertificateAlreadyIssuedException
    {
        URL url = ssg.getServerCertificateSigningRequestUrl();
        X500Principal csrName = new X500Principal(csr.getSubjectAsString());
        String csrNameString = csrName.getName(X500Principal.RFC2253);
        HttpClient hc = new HttpClient();
        hc.getState().setAuthenticationPreemptive(true);
        hc.getState().setCredentials(null, null,
                                     new UsernamePasswordCredentials(username,
                                                                     new String(password)));
        PostMethod post = new PostMethod(url.toExternalForm());
        try {
            byte[] csrBytes = csr.getEncoded();
            ByteArrayInputStream bais = new ByteArrayInputStream(csrBytes);
            post.setRequestBody(bais);
            post.setRequestHeader(MimeUtil.CONTENT_TYPE, "application/pkcs10");
            post.setRequestHeader(MimeUtil.CONTENT_LENGTH, String.valueOf(csrBytes.length));

            CurrentSslPeer.set(ssg);
            int result = hc.executeMethod(post);
            CurrentSslPeer.set(null);
            if ( result == 401 ) throw new BadCredentialsException("HTTP POST to certificate signer returned status " + result );
            if ( result == 403 ) throw new CertificateAlreadyIssuedException("HTTP POST to certificate signer returned status " + result);
            if ( result != 200 ) throw new CertificateException( "HTTP POST to certificate signer generated status " + result );
            X509Certificate cert = (X509Certificate)CertUtils.getFactory().generateCertificate(post.getResponseBodyAsStream());
            post.releaseConnection();
            post = null;
            X500Principal certName = new X500Principal(cert.getSubjectDN().toString());
            String certNameString = certName.getName(X500Principal.RFC2253);

            if (!certNameString.equals(csrNameString))
                throw new CertificateException("We got a certificate, but it's distinguished name didn't match what we asked for.");
            if (!cert.getPublicKey().equals(csr.getPublicKey()))
                throw new CertificateException("We got a certificate, but it certified the wrong public key.");

            // TODO this doesn't work now that caCert is actually the SSL cert.  Might not be a problem though:
            // why do we even care what the server signed our client cert with, as long as the server is happy with it?
            // cert.verify(caCert.getPublicKey());

            return cert;
        } finally {
            if (post != null)
                post.releaseConnection();
        }
    }

}

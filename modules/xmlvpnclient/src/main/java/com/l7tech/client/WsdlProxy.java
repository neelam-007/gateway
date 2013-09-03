/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.client;

import com.l7tech.common.http.GenericHttpException;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.SimpleHttpClient;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.wsdl.WSDLException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Downloads WSDL documents from the Ssg, and rewrites the target URLs.
 *
 * User: mike
 * Date: Sep 18, 2003
 * Time: 5:12:42 PM
 */
class WsdlProxy {
    private static final Logger log = Logger.getLogger(WsdlProxy.class.getName());

    static class DownloadException extends Exception {
        private DownloadException(String message) {
            super(message);
        }

        private DownloadException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    static class ServiceNotFoundException extends Exception {
        ServiceNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Download the WSDL for the given published service from the given Ssg.  The downloaded Wsdl
     * will be returned exactly as the Ssg returned it -- specifically, the port URL will be whatever
     * the Ssg gave us (probably pointing at itself), and will need to be rewritten to point at the Bridge instead.
     *
     * @param ssg           the Ssg to contact
     * @param serviceId    the ID (GOID) of the service whose WSDL to obtain
     * @param queryParameters additional query string parameters for the request
     * @return the Wsdl document obtained from the Ssg.
     * @throws OperationCanceledException       if the user canceled the Logon dialog
     * @throws IOException                      if there was a network problem
     * @throws DownloadException            if we got a status code other than 200, 401 or 404; or,
     *                                      if we got three bad password attempts in a row
     * @throws WSDLException                    if the Ssg gave us back something other than a valid Wsdl document
     * @throws ServiceNotFoundException     if we got back a 404 from the Wsdl service
     */
    static Document obtainWsdlForService(Ssg ssg, String serviceId, final Map<String,String[]> queryParameters)
            throws OperationCanceledException, WSDLException, IOException, DownloadException, ServiceNotFoundException, HttpChallengeRequiredException {
        try {
            return doDownload(ssg, serviceId, queryParameters);
        } catch (SAXException e) {
            throw new RuntimeException("impossible exception", e);
        }
    }

    /**
     * Download the WSIL for the service list from the given Ssg.  The downloaded WSIL
     * will be returned exactly as the Ssg returned it -- specifically, the WSDL URLs will be whatever
     * the Ssg gave us (probably pointing at itself), and will need to be rewritten to point at the Bridge instead.
     *
     * @param ssg           the Ssg to contact
     * @return the Wsdl document obtained from the Ssg.
     * @throws OperationCanceledException       if the user canceled the Logon dialog
     * @throws IOException                      if there was a network problem
     * @throws DownloadException            if we got a status code other than 200, 401 or 404; or,
     *                                      if we got three bad password attempts in a row
     * @throws SAXException                 if we got back invalid XML
     * @throws ServiceNotFoundException     if we got back a 404
     */
    static Document obtainWsilForServices(Ssg ssg)
            throws OperationCanceledException, DownloadException, IOException, SAXException, ServiceNotFoundException, HttpChallengeRequiredException {
        try {
            return doDownload(ssg, null, Collections.<String, String[]>emptyMap());
        } catch (WSDLException e) {
            throw new RuntimeException("impossible exception", e); // this can't happen
        }
    }

    private static Document doDownload( final Ssg ssg,
                                        @Nullable final String serviceId,
                                        final Map<String,String[]> queryParameters )
            throws OperationCanceledException, IOException, DownloadException,
            WSDLException, SAXException, ServiceNotFoundException, HttpChallengeRequiredException {
        StringBuilder file = new StringBuilder(SecureSpanConstants.WSDL_PROXY_FILE);
        if (serviceId != null) {
            file.append("?serviceoid=").append(serviceId);

            for ( final Map.Entry<String,String[]> parameterEntry : queryParameters.entrySet() ) {
                if ( parameterEntry.getKey() != null &&
                     parameterEntry.getKey().startsWith( SecureSpanConstants.HttpQueryParameters.PARAM_WSDL_PREFIX ) ) {
                    for ( final String value : parameterEntry.getValue() ) {
                        file.append( "&" );
                        file.append( URLEncoder.encode( parameterEntry.getKey(), "UTF-8") );
                        file.append( "=" );
                        file.append( URLEncoder.encode( value, "UTF-8") );
                    }
                }
            }
        }
        URL url;
        try {
            url = new URL("https", ssg.getSsgAddress(), ssg.getSslPort(), file.toString());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e); // can't happen
        }

        int status = 0;

        // The reason for the last failure
        Exception error = null;

        SimpleHttpClient client = ssg.getRuntime().getHttpClient();
        GenericHttpRequestParams params = new GenericHttpRequestParams(url);
        params.setPreemptiveAuthentication(true);

        // Password retries for WSDL download
        PasswordAuthentication pw;
        for (int retries = 0; retries < 3; ++retries) {
            pw = ssg.getRuntime().getCredentialManager().getCredentials(ssg);
            if (isAnonCreds(pw)) {
                pw = null;
            } else {
                // Prepare private key
                try {
                    ssg.getRuntime().getSsgKeyStoreManager().getClientCertPrivateKey(pw);
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                } catch (BadCredentialsException e) {
                    ssg.getRuntime().getCredentialManager().getNewCredentials(ssg, true);
                    continue;
                } catch (KeyStoreCorruptException e) {
                    log.log(Level.WARNING, "Unable to prepare client cert private key: " + ExceptionUtils.getMessage(e), e);
                    ssg.getRuntime().getCredentialManager().getNewCredentials(ssg, true);
                    continue;
                }
            }
            params.setPasswordAuthentication(pw);
            log.info("WsdlProxy: Attempting download from Gateway from URL: " + url);
            SimpleHttpClient.SimpleHttpResponse result = null;
            try {
                result = client.get(params);
                status = result.getStatus();
            } catch (GenericHttpException e) {
                ServerCertificateUntrustedException scue = ExceptionUtils.getCauseIfCausedBy(e, ServerCertificateUntrustedException.class);
                if (scue != null) {
                    error = scue;
                    try {
                        ssg.getRuntime().discoverServerCertificate(pw);
                        status = -1; // hack hack hack: fake status meaning "try again"
                        continue;
                    } catch (GeneralSecurityException e1) {
                        throw new DownloadException("Unable to discover Gateway's server certificate", e1);
                    } catch (BadCredentialsException e1) {
                        log.log(Level.INFO, "Certificate discovery service indicates bad password; setting status to 401 artificially");
                        status = 401; // hack hack hack: fake up a 401 status to trigger password dialog below
                        //noinspection ThrowableInstanceNeverThrown
                        error = new BadCredentialsException("Unable to automatically establish authenticity of the Gateway SSL certificate using the current username and password");
                        // FALLTHROUGH -- get new credentials and try again
                    }
                } else
                    throw e;
            }

            log.info("WsdlProxy: connection HTTP status " + status);
            if (status == 200 && result != null) {
                final InputSource inputSource = new InputSource();
                inputSource.setSystemId( url.toString() );
                inputSource.setByteStream( new ByteArrayInputStream(result.getBytes()) );
                return XmlUtil.parse( inputSource, false );
            } else if (status == 404) {
                throw new ServiceNotFoundException("No service was found on Gateway " + ssg + " with serviceoid " + serviceId);
            } else if (status == 401 || status == 403) {
                ssg.getRuntime().getCredentialManager().getNewCredentials(ssg, true);
                // FALLTHROUGH - continue and try again with new password
            } else if (status == -1) {
                // FALLTHROUGH -- continue and try again with empty keystore
            } else {
                error = null;
                break;
            }
        }
        if (error != null)
            throw new DownloadException("WsdlProxy: " + error.getMessage(), error);
        throw new DownloadException("WsdlProxy: download from Gateway failed with HTTP status " + status);
    }

    private static boolean isAnonCreds(PasswordAuthentication pw) {
        return pw == null || pw.getUserName() == null || pw.getUserName().length() < 1;
    }
}

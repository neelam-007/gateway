/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.proxy.datamodel.Managers;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.datamodel.exceptions.ServerCertificateUntrustedException;
import com.l7tech.service.Wsdl;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Category;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.net.ssl.SSLHandshakeException;
import javax.wsdl.WSDLException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.security.GeneralSecurityException;

/**
 * Downloads WSDL documents from the Ssg, and rewrites the target URLs.
 *
 * User: mike
 * Date: Sep 18, 2003
 * Time: 5:12:42 PM
 */
public class WsdlProxy {
    private static final Category log = Category.getInstance(WsdlProxy.class);

    private interface StreamReader {
        Object readStream(InputStream is) throws WSDLException, IOException, SAXException;
    }

    public static class DownloadException extends Exception {
        private DownloadException(String message) {
            super(message);
        }

        private DownloadException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class ServiceNotFoundException extends Exception {
        public ServiceNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Download the WSDL for the given published service from the given Ssg.  The downloaded Wsdl
     * will be returned exactly as the Ssg returned it -- specifically, the port URL will be whatever
     * the Ssg gave us (probably pointing at itself), and will need to be rewritten to point at the Agent instead.
     *
     * @param ssg           the Ssg to contact
     * @param serviceoid    the OID of the service whose WSDL to obtain
     * @return the Wsdl document obtained from the Ssg.
     * @throws OperationCanceledException       if the user canceled the Logon dialog
     * @throws IOException                      if there was a network problem
     * @throws DownloadException            if we got a status code other than 200, 401 or 404
     * @throws DownloadException            if we got three bad password attempts in a row
     * @throws WSDLException                    if the Ssg gave us back something other than a valid Wsdl document
     * @throws ServiceNotFoundException     if we got back a 404 from the Wsdl service
     */
    public static Wsdl obtainWsdlForService(Ssg ssg, long serviceoid)
            throws OperationCanceledException, WSDLException, IOException, DownloadException, ServiceNotFoundException
    {
        try {
            return (Wsdl) doDownload(ssg, serviceoid, new StreamReader() {
                public Object readStream(InputStream is) throws WSDLException {
                    return Wsdl.newInstance(null, new InputStreamReader(is));
                }
            });
        } catch (SAXException e) {
            throw new RuntimeException("impossible exception", e);
        }
    }

    /**
     * Download the WSIL for the service list from the given Ssg.  The downloaded WSIL
     * will be returned exactly as the Ssg returned it -- specifically, the WSDL URLs will be whatever
     * the Ssg gave us (probably pointing at itself), and will need to be rewritten to point at the Agent instead.
     *
     * @param ssg           the Ssg to contact
     * @return the Wsdl document obtained from the Ssg.
     * @throws OperationCanceledException       if the user canceled the Logon dialog
     * @throws IOException                      if there was a network problem
     * @throws DownloadException            if we got a status code other than 200, 401 or 404
     * @throws DownloadException            if we got three bad password attempts in a row
     * @throws SAXException                 if we got back invalid XML
     * @throws ServiceNotFoundException     if we got back a 404
     */
    public static Document obtainWsilForServices(Ssg ssg)
            throws OperationCanceledException, DownloadException, IOException, SAXException, ServiceNotFoundException
    {
        try {
            return (Document) doDownload(ssg, 0, new StreamReader() {
                public Object readStream(InputStream is) throws IOException, SAXException {
                    return XmlUtil.parse(is);
                }
            });
        } catch (WSDLException e) {
            throw new RuntimeException("impossible exception", e); // this can't happen
        }
    }

    private static Object doDownload(Ssg ssg, long serviceoid, StreamReader sr)
            throws OperationCanceledException, IOException, DownloadException,
                   WSDLException, SAXException, ServiceNotFoundException
    {
        HttpClient httpClient = new HttpClient();
        String file = SecureSpanConstants.WSDL_PROXY_FILE;
        if (serviceoid != 0)
            file += "?serviceoid=" + serviceoid;
        URL url;
        try {
            url = new URL("https", ssg.getSsgAddress(), ssg.getSslPort(), file);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e); // can't happen
        }
        httpClient.getState().setAuthenticationPreemptive(true);

        int status = 0;

        // Password retries for WSDL download
        PasswordAuthentication pw;
        for (int retries = 0; retries < 3; ++retries) {
            GetMethod getMethod = null;
            try {
                pw = Managers.getCredentialManager().getCredentials(ssg);
                getMethod = new GetMethod(url.toString());
                httpClient.getState().setCredentials(null,
                                                     null,
                                                     new UsernamePasswordCredentials(pw.getUserName(),
                                                                                     new String(pw.getPassword())));
                log.info("WsdlProxy: Attempting download from SSG from URL: " + url);
                try {
                    status = httpClient.executeMethod(getMethod);
                } catch (SSLHandshakeException e) {
                    if (e.getCause() instanceof ServerCertificateUntrustedException) {
                        try {
                            log.info("Attempting to discover Ssg server certificate for " + ssg);
                            ClientProxy.installSsgServerCertificate(ssg);
                            continue;
                        } catch (GeneralSecurityException e1) {
                            throw new DownloadException("Unable to discover Gateway's server certificate", e1);
                        } catch (BadCredentialsException e1) {
                            log.warn("Certificate discovery service indicates bad password; setting status to 401 arificially");
                            status = 401; // hack hack hack: fake up a 401 status to trigger password dialog below
                        }
                    }
                }
                log.info("WsdlProxy: connection HTTP status " + status);
                if (status == 200) {
                    return sr.readStream(getMethod.getResponseBodyAsStream());
                } else if (status == 404) {
                    throw new ServiceNotFoundException("No service was found on Ssg " + ssg + " with serviceoid " + serviceoid);
                } else if (status == 401) {
                    pw = Managers.getCredentialManager().getNewCredentials(ssg);
                    // FALLTHROUGH - continue and try again with new password
                } else
                    break;
            } finally {
                if (getMethod != null) {
                    getMethod.releaseConnection();
                    getMethod = null;
                }
            }
        }
        throw new DownloadException("WsdlProxy: download from Gateway failed with HTTP status " + status);
    }
}

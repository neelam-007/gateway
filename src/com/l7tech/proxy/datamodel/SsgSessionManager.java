/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.security.xml.Session;
import com.l7tech.common.util.HexUtils;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.datamodel.exceptions.ServerCertificateUntrustedException;
import com.l7tech.proxy.util.ClientLogger;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;

/**
 * Keep track of open security sessions with SSGs
 * User: mike
 * Date: Aug 28, 2003
 * Time: 1:11:49 PM
 */
public class SsgSessionManager {
    private static final ClientLogger log = ClientLogger.getInstance(SsgSessionManager.class);

    private static class InvalidSessionIdException extends IOException {
        public InvalidSessionIdException(String s, Throwable cause) {
            super(s);
            initCause(cause);
        }
    }

    /**
     * Quickly check whether we have a session open with the specified Ssg.  If this returns true, the
     * Ssg may still reject the session next time we refer to it.
     */
    public static boolean isSessionAvailable(Ssg ssg) {
        return ssg.session() != null;
    }

    /**
     * Get the session with the specified Ssg, or null if no session currently exists.
     */
    public static Session getSession(Ssg ssg) {
        return ssg.session();
    }

    /**
     * Get a session with the specified Ssg.  If no session currently exists, one will be created.
     * @param ssg
     * @param serviceId the identifier of the published service whose policy is requiring us to get a session.
     * @return the Session, which may or may have been newly (re)established.
     */
    public static Session getOrCreateSession(Ssg ssg, String serviceId)
            throws OperationCanceledException, IOException,
            ServerCertificateUntrustedException, BadCredentialsException
    {
        synchronized (ssg) {
            Session session = ssg.session();
            if (session != null)
                return session;
        }
        PasswordAuthentication pw = Managers.getCredentialManager().getCredentials(ssg);
        synchronized (ssg) {
            Session session = ssg.session();
            if (session != null)
                return session;

            session = establishNewSession(ssg, pw, serviceId);
            ssg.session(session);
            return session;
        }
    }

    private static String header(HttpMethod method, String name) throws IOException {
        Header header = method.getResponseHeader(name);
        if (header == null)
            throw new IOException("Required HTTP header " + name + " was missing from response");
        return header.getValue();
    }

    /**
     *
     * @param ssg   the Gateway with which to establish the session.
     * @param pw    the username and password to use.
     * @param serviceId  the identifier of the published service whose policy required us to establish the session.
     * @return the newly-established Session.  Any old session that may have existed with the Gateway is no longer valid.
     * @throws MalformedURLException
     * @throws IOException
     * @throws ServerCertificateUntrustedException
     * @throws BadCredentialsException
     */
    private static Session establishNewSession(Ssg ssg, PasswordAuthentication pw, String serviceId)
            throws MalformedURLException, IOException,
            ServerCertificateUntrustedException, BadCredentialsException
    {
        HttpClient client = new HttpClient();
        client.getState().setAuthenticationPreemptive(true);
        client.getState().setCredentials(null, null,
                                         new UsernamePasswordCredentials(pw.getUserName(),
                                                                         new String(pw.getPassword())));
        HttpMethod getMethod = new GetMethod(new URL("https",
                                                     ssg.getSsgAddress(),
                                                     ssg.getSslPort(),
                                                     SecureSpanConstants.SESSION_SERVICE_FILE + "?" +
                                                     SecureSpanConstants.HttpQueryParameters.PARAM_SERVICEOID + "=" +
                                                     serviceId).toString());
        getMethod.setDoAuthentication(true);
        try {
            int status = client.executeMethod(getMethod);
            if (status == 401) {
                log.info("Got 401 status establishing session");
                throw new BadCredentialsException("Got 401 status while establishing session");
            }
            log.info("Session establishment completed with status " + status);

            String idstr = header(getMethod, SecureSpanConstants.HttpHeaders.XML_SESSID_HEADER_NAME);
            String b64keyreq = header(getMethod, SecureSpanConstants.HttpHeaders.HEADER_KEYREQ);

            byte[] keyreq = HexUtils.decodeBase64(b64keyreq);
            if (keyreq == null)
                throw new IOException("Gateway sent invalid request key in session: base64 keyreq=" + b64keyreq);
            String b64keyres = header(getMethod, SecureSpanConstants.HttpHeaders.HEADER_KEYRES);
            byte[] keyres = HexUtils.decodeBase64(b64keyres);
            if (keyres == null)
                throw new IOException("Gateway sent invalid response key in session: base64 keyres=" + b64keyres);

            return new Session(Long.parseLong(idstr), System.currentTimeMillis(), keyreq, keyres, 0);
        } catch (SSLHandshakeException e) {
            if (e.getCause() instanceof ServerCertificateUntrustedException)
                throw (ServerCertificateUntrustedException) e.getCause();
            throw e;
        } catch (NumberFormatException e) {
            throw new InvalidSessionIdException("Gateway sent invalid session ID", e);
        } finally {
            getMethod.releaseConnection();
        }
    }

    public static void invalidateSession(Ssg ssg) {
        ssg.session(null);
    }

}

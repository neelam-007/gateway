/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.common.security.xml.Session;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.proxy.datamodel.exceptions.ServerCertificateUntrustedException;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;

import javax.net.ssl.SSLHandshakeException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Category;
import org.apache.axis.encoding.Base64;

import java.net.URL;
import java.net.MalformedURLException;
import java.io.IOException;

/**
 * Keep track of open security sessions with SSGs
 * User: mike
 * Date: Aug 28, 2003
 * Time: 1:11:49 PM
 */
public class SsgSessionManager {
    private static final Category log = Category.getInstance(SsgSessionManager.class);

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
     * @return
     */
    public static Session getOrCreateSession(Ssg ssg)
            throws OperationCanceledException, IOException,
            ServerCertificateUntrustedException, BadCredentialsException
    {
        synchronized (ssg) {
            Session session = ssg.session();
            if (session != null)
                return session;

            session = establishNewSession(ssg);
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

    private static Session establishNewSession(Ssg ssg)
            throws OperationCanceledException, MalformedURLException, IOException,
            ServerCertificateUntrustedException, BadCredentialsException
    {
        HttpClient client = new HttpClient();
        if (!ssg.isCredentialsConfigured())
            Managers.getCredentialManager().getCredentials(ssg);

        client.getState().setAuthenticationPreemptive(true);

        String username = ssg.getUsername();
        char[] password = ssg.password();
        client.getState().setCredentials(null, null, new UsernamePasswordCredentials(username, new String(password)));
        HttpMethod getMethod = new GetMethod(new URL("https",
                                                     ssg.getSsgAddress(),
                                                     ssg.getSslPort(),
                                                     SecureSpanConstants.SESSION_SERVICE_FILE).toString());
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
            byte[] keyreq = Base64.decode(b64keyreq);
            if (keyreq == null)
                throw new IOException("SSG sent invalid request key in session: base64 keyreq=" + b64keyreq);
            String b64keyres = header(getMethod, SecureSpanConstants.HttpHeaders.HEADER_KEYRES);
            byte[] keyres = Base64.decode(b64keyres);
            if (keyres == null)
                throw new IOException("SSG sent invalid response key in session: base64 keyres=" + b64keyres);

            return new Session(Long.parseLong(idstr), System.currentTimeMillis(), keyreq, keyres, 0);
        } catch (SSLHandshakeException e) {
            if (e.getCause() instanceof ServerCertificateUntrustedException)
                throw (ServerCertificateUntrustedException) e.getCause();
            throw e;
        } catch (NumberFormatException e) {
            throw new InvalidSessionIdException("SSG sent invalid session ID", e);
        } finally {
            getMethod.releaseConnection();
        }
    }

    public static void invalidateSession(Ssg ssg) {
        ssg.session(null);
    }

}

package com.l7tech.server.processcontroller;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CXF Interceptor that enforces ESM or localhost access controls.
 */
public class SecurityInterceptor extends AbstractPhaseInterceptor<Message> {

    //- PUBLIC

    public SecurityInterceptor() {
        super(Phase.READ);
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public void handleMessage( final Message message ) throws Fault {
        final HttpServletRequest req = (HttpServletRequest)message.getContextualProperty("HTTP.REQUEST");
        if (req == null) throw new IllegalStateException("Couldn't get HttpServletRequest");

        try {
            final InetAddress addr = InetAddress.getByName(req.getRemoteAddr()); // TODO maybe special-case "127.0.0.1" and "localhost" to avoid the DNS lookup?
            if (addr.isLoopbackAddress()) {
                logger.fine("Allowing connection from localhost with no client certificate");
                return;
            }
        } catch (UnknownHostException e) {
            throw new IllegalStateException("Couldn't get client address", e);
        }

        final Object maybeCert = req.getAttribute("javax.servlet.request.X509Certificate");
        final X509Certificate certificate;
        if (maybeCert instanceof X509Certificate) {
            certificate = (X509Certificate)maybeCert;
        } else if (maybeCert instanceof X509Certificate[]) {
            X509Certificate[] certs = (X509Certificate[])maybeCert;
            certificate = certs[0];
        } else if (maybeCert != null) {
            logger.warning( "Client certificate was a " + maybeCert.getClass().getName() + ", not an X509Certificate" );
            throw new IllegalStateException(AUTH_FAILURE);
        } else {
            logger.fine( "Client certificate missing in request." );
            throw new IllegalArgumentException(AUTH_FAILURE);
        }

        if (!configService.getTrustedRemoteNodeManagementCerts().contains(certificate)) {
            logger.fine( "Client certificate was not trusted for remote management '" + certificate.getSubjectDN().toString() + "' from '"+req.getRemoteAddr()+"'." );
            throw new IllegalArgumentException(AUTH_FAILURE);
        }

        logger.log(Level.FINE, "Accepted client certificate {0}", certificate.getSubjectDN().getName());
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(SecurityInterceptor.class.getName());

    private static final String AUTH_FAILURE = "Authentication Required";

    @Resource
    private ConfigService configService;
}
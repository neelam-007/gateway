package com.l7tech.server.processcontroller;

import com.l7tech.common.http.CookieUtils;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.io.CertUtils;
import com.l7tech.util.ExceptionUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

import javax.inject.Inject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CXF Interceptor that enforces ESM or localhost access controls.
 */
public class SecurityInterceptor extends AbstractPhaseInterceptor<Message> {

    //- PUBLIC

    public SecurityInterceptor( final boolean allowLocalAccess ) {
        super(Phase.READ);
        this.allowLocalAccess = allowLocalAccess;
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public void handleMessage( final Message message ) throws Fault {
        final HttpServletRequest req = (HttpServletRequest)message.getContextualProperty("HTTP.REQUEST");
        if (req == null) throw new IllegalStateException("Couldn't get HttpServletRequest");

        if ( allowLocalAccess ) {
            try {
                final InetAddress addr = InetAddress.getByName(req.getRemoteAddr()); // TODO maybe special-case "127.0.0.1" and "localhost" to avoid the DNS lookup?
                if (addr.isLoopbackAddress()) {
                    final String secret = configService.getHostSecret();
                    if ( secret == null ) {
                        logger.fine("Allowing connection from localhost with no client certificate");
                        return;
                    } else if ( validCookie( req.getCookies(), "PC-AUTH", secret ) ) {
                        logger.fine("Allowing connection from localhost with cookie");
                        return;
                    }
                }
            } catch (UnknownHostException e) {
                throw new IllegalStateException("Couldn't get client address", e);
            }
        }

        final Object maybeCert = req.getAttribute("javax.servlet.request.X509Certificate");
        final X509Certificate certificate;
        if (maybeCert instanceof X509Certificate) {
            certificate = (X509Certificate)maybeCert;
        } else if (maybeCert instanceof X509Certificate[]) {
            X509Certificate[] certs = (X509Certificate[])maybeCert;
            certificate = certs.length> 0 ? certs[0] : null;
        } else if (maybeCert != null) {
            logger.warning( "Client certificate was a " + maybeCert.getClass().getName() + ", not an X509Certificate" );
            throw new IllegalStateException(AUTH_FAILURE);
        } else {
            certificate = null;            
        }

        if ( certificate == null ) {
            logger.fine( "Client certificate missing in request." );
            throw new IllegalArgumentException(AUTH_FAILURE);
        }

        if ( !isTrustedForRemoteManagement(certificate) ) {
            logger.fine( "Client certificate was not trusted for remote management '" + certificate.getSubjectDN().toString() + "' from '" + req.getRemoteAddr() + "'." );
            throw new IllegalArgumentException(AUTH_FAILURE);
        }

        logger.log(Level.FINE, "Accepted client certificate {0}", certificate.getSubjectDN().getName());
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(SecurityInterceptor.class.getName());

    private static final String AUTH_FAILURE = "Authentication Required";

    @Inject
    private ConfigService configService;

    private final boolean allowLocalAccess;

    /**
     * Check if the given certificate is trusted for remote management.
     *
     * This method will accept the given certificate if it matches the thumbprint
     * of the trusted ESM certificate.
     */
    private boolean isTrustedForRemoteManagement( final X509Certificate certificate ) {
        boolean trusted = configService.getTrustedRemoteNodeManagementCerts().contains(certificate);

        if ( !trusted ) {
            try {
                final String thumbprint = CertUtils.getCertificateFingerprint(certificate, "SHA1").substring(5);
                for ( String partialThumbprint : configService.getTrustedRemoteNodeManagementCertThumbprints() ) {
                    if ( thumbprint.startsWith( partialThumbprint ) ) {
                        configService.acceptTrustedRemoteNodeManagementCert( certificate );
                        trusted = true;
                        break;
                    }
                }
            } catch ( CertificateEncodingException e ) {
                logger.log(Level.INFO, "Error checking if certificate is trusted : " + ExceptionUtils.getMessage( e ), ExceptionUtils.getDebugException( e ));
            } catch ( NoSuchAlgorithmException e ) {
                logger.log(Level.WARNING,  "Error checking if certificate is trusted : " + ExceptionUtils.getMessage( e ), ExceptionUtils.getDebugException( e ));
            }
        }

        return trusted;
    }

    private boolean validCookie( final Cookie[] servletCookies,
                                 final String name,
                                 final String value ) {
        boolean valid = false;

        if ( servletCookies != null ) {
            final HttpCookie[] cookies = CookieUtils.fromServletCookies(servletCookies, false);
            final String cookieValue = getCookieValue( cookies, name );
            valid = value.equals( cookieValue );
        }

        return valid;
    }

    private String getCookieValue( final HttpCookie[] cookies, final String name ) {
        String value = null;

        try {
            final HttpCookie valueCookie = CookieUtils.findSingleCookie(cookies, name);
            if ( valueCookie != null ) {
                value = valueCookie.getCookieValue();
            } else {
                value = null;
            }
        } catch ( IOException ioe) {
            // duplicate cookie
            logger.log( Level.FINE, "Error processing cookies '"+ ExceptionUtils.getMessage(ioe)+"'.", ExceptionUtils.getDebugException( ioe ) );
        }

        return value;
    }
}
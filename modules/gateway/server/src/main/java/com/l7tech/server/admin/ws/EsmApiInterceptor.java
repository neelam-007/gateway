package com.l7tech.server.admin.ws;

import com.l7tech.common.http.CookieUtils;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.io.CertUtils;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.esmtrust.TrustedEsm;
import com.l7tech.gateway.common.esmtrust.TrustedEsmUser;
import com.l7tech.gateway.common.spring.remoting.RemoteUtils;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.server.GatewayFeatureSets;
import com.l7tech.server.TrustedEsmManager;
import com.l7tech.server.TrustedEsmUserManager;
import com.l7tech.server.admin.AdminSessionManager;
import com.l7tech.server.transport.http.HttpTransportModule;
import com.l7tech.server.util.JaasUtils;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

import javax.security.auth.Subject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CXF Interceptor that enforces ESM/Gateway trust.
 */
public class EsmApiInterceptor extends AbstractPhaseInterceptor<Message> {

    //- PUBLIC

    public EsmApiInterceptor( final LicenseManager licenseManager,
                              final TrustedEsmManager trustedEmsManager,
                              final TrustedEsmUserManager trustedEmsUserManager,
                              final AdminSessionManager adminSessionManager ){
        super(Phase.READ);
        this.licenseManager = licenseManager;
        this.trustedEmsManager = trustedEmsManager;
        this.trustedEmsUserManager = trustedEmsUserManager;
        this.adminSessionManager = adminSessionManager;
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public void handleMessage( final Message message ) throws Fault {
        enforceLicensed();

        HttpServletRequest hreq = RemoteUtils.getHttpServletRequest();
        if (hreq == null) {
            logger.warning("Error in remote management, servlet info not found.");
            throw new SoapFault("Server Error", SoapFault.FAULT_CODE_SERVER);
        }

        SsgConnector connector = HttpTransportModule.getConnector(hreq);
        if (connector == null) {
            logger.warning("Error in remote management, connector info not found.");
            throw new SoapFault("Server Error", SoapFault.FAULT_CODE_SERVER);
        }
        if (!connector.offersEndpoint(SsgConnector.Endpoint.ADMIN_REMOTE_ESM))
            throw new SoapFault("Server Error", SoapFault.FAULT_CODE_SERVER);

        Subject subject = JaasUtils.getCurrentSubject();
        if (subject == null) {
            logger.warning("Error in remote management, subject not found.");
            throw new SoapFault("Server Error", SoapFault.FAULT_CODE_SERVER);
        }

        Cookie[] servletCookies = hreq.getCookies();
        if ( servletCookies == null || servletCookies.length == 0 ) {
            throw new SoapFault("Authentication Required", SoapFault.FAULT_CODE_CLIENT);
        }

        HttpCookie[] cookies = CookieUtils.fromServletCookies(servletCookies, false);
        String esmId = getCookieValue( cookies, "EM-UUID", true );
        String esmUserId = getCookieValue( cookies, "EM-USER-UUID", false );


        // check mapping and set the user for later use when checking permissions
        // the user is not required for all actions
        User user = validateMapping( esmId, esmUserId );
        if ( user != null && subject.getPrincipals().isEmpty() ) {
            subject.getPrincipals().add(user);
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(EsmApiInterceptor.class.getName());

    private final LicenseManager licenseManager;
    private final TrustedEsmManager trustedEmsManager;
    private final TrustedEsmUserManager trustedEmsUserManager;
    private final AdminSessionManager adminSessionManager;

    private void enforceLicensed() {
        try {
            licenseManager.requireFeature( GatewayFeatureSets.SERVICE_REMOTE_MANAGEMENT );
        } catch ( LicenseException e) {
            logger.log( Level.WARNING, "License check failed for remote management.");
            throw new SoapFault("Not Licensed", SoapFault.FAULT_CODE_SERVER);
        }
    }

    private String getCookieValue( final HttpCookie[] cookies, final String name, final boolean required ) {
        String value;

        try {
            HttpCookie valueCookie = CookieUtils.findSingleCookie(cookies, name);

            if ( required && (valueCookie == null || valueCookie.getCookieValue()==null || valueCookie.getCookieValue().trim().isEmpty()) ) {
                throw new SoapFault("Authentication Required", SoapFault.FAULT_CODE_CLIENT);
            }

            if ( valueCookie != null ) {
                value = valueCookie.getCookieValue();
            } else {
                value = null;
            }
        } catch (IOException ioe) {
            // duplicate cookie
            throw new SoapFault("Authentication Required", SoapFault.FAULT_CODE_CLIENT);
        }

        return value;
    }

    private User validateMapping( final String esmId, final String esmUserId  ) {
        User user = null;

        try {
            // Find ESM registration info and check it
            TrustedEsm esm = trustedEmsManager.findEsmById( esmId );
            if ( esm == null ) {
                throw new SoapFault("Authentication Required", SoapFault.FAULT_CODE_CLIENT);
            }

            X509Certificate esmCertificate = RemoteUtils.getClientCertificate();
            if ( esmCertificate==null || !CertUtils.certsAreEqual( esmCertificate, esm.getTrustedCert().getCertificate() ) ) {
                throw new SoapFault("Authentication Required", SoapFault.FAULT_CODE_CLIENT);
            }

            if ( esmUserId != null ) {
                TrustedEsmUser emsUser = trustedEmsUserManager.findByEsmIdAndUserUUID( esm.getOid(), esmUserId );
                if ( emsUser == null ) {
                    throw new SoapFault("Access Denied", SoapFault.FAULT_CODE_CLIENT);
                }

                Goid providerId = emsUser.getProviderGoid();
                String userId = emsUser.getSsgUserId();

                user = adminSessionManager.authorize( providerId, userId );
                if ( user == null ) {
                    throw new SoapFault("Access Denied", SoapFault.FAULT_CODE_CLIENT);
                }
            }
        } catch ( ObjectModelException fe ) {
            logger.log( Level.WARNING, "Error checking ESM remote credentials.", fe );
            throw new SoapFault("Server Error", SoapFault.FAULT_CODE_SERVER);
        }

        return user;
    }
}

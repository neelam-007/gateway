package com.l7tech.server.admin.ws;

import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.binding.soap.SoapFault;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.security.cert.X509Certificate;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.spring.remoting.RemoteUtils;
import com.l7tech.gateway.common.emstrust.TrustedEms;
import com.l7tech.gateway.common.emstrust.TrustedEmsUser;
import com.l7tech.gateway.common.admin.LicenseRuntimeException;
import com.l7tech.server.TrustedEmsManager;
import com.l7tech.server.TrustedEmsUserManager;
import com.l7tech.server.GatewayFeatureSets;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.server.transport.http.HttpTransportModule;
import com.l7tech.server.admin.AdminSessionManager;
import com.l7tech.common.http.ParameterizedString;
import com.l7tech.common.io.CertUtils;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.identity.User;

import javax.servlet.http.HttpServletRequest;
import javax.security.auth.Subject;

/**
 *
 */
public class EsmApiInterceptor extends AbstractPhaseInterceptor<Message> {

    //- PUBLIC

    public EsmApiInterceptor( final LicenseManager licenseManager,
                              final TrustedEmsManager trustedEmsManager,
                              final TrustedEmsUserManager trustedEmsUserManager,
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
        Map<String,List<String>> headers = (Map<String,List<String>>) message.get(Message.PROTOCOL_HEADERS);

        enforceLicensed( getHeader(headers, "SOAPAction") );

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
        if (!connector.offersEndpoint(SsgConnector.Endpoint.ADMIN_REMOTE))
            throw new SoapFault("Server Error", SoapFault.FAULT_CODE_SERVER);

        Subject subject = JaasUtils.getCurrentSubject();
        if (subject == null) 
            throw new SoapFault("Server Error", SoapFault.FAULT_CODE_SERVER);

        String cookieHeader = getHeader(headers, "cookie");
        if ( cookieHeader == null ) {
            throw new SoapFault("Authentication Required", SoapFault.FAULT_CODE_CLIENT);
        }

        //TODO [steve] fix separator (should be cookie style, not query string)
        ParameterizedString paramString = new ParameterizedString(cookieHeader, true);
        String esmId = paramString.getParameterValue("EM-UUID");
        String esmUserId = paramString.getParameterValue("EM-USER-UUID");

        if ( esmId == null ) {
            throw new SoapFault("Authentication Required", SoapFault.FAULT_CODE_CLIENT);
        }

        try {
            // Find ESM registration info and check it
            TrustedEms esm = trustedEmsManager.findEmsById( esmId );
            if ( esm == null ) {
                throw new SoapFault("Authentication Required", SoapFault.FAULT_CODE_CLIENT);
            }

            X509Certificate esmCertificate = RemoteUtils.getClientCertificate();
            if ( esmCertificate==null || !CertUtils.certsAreEqual( esmCertificate, esm.getTrustedCert().getCertificate() ) ) {
                throw new SoapFault("Authentication Required", SoapFault.FAULT_CODE_CLIENT);
            }

            if ( esmUserId != null ) {
                TrustedEmsUser emsUser = trustedEmsUserManager.findByEmsIdAndUserUUID( esm.getOid(), esmUserId );
                if ( emsUser == null ) {
                    throw new SoapFault("Authentication Required", SoapFault.FAULT_CODE_CLIENT);
                }

                long providerId = emsUser.getProviderOid();
                String userId = emsUser.getSsgUserId();

                User user = adminSessionManager.authorize( providerId, userId );
                if ( user == null ) {
                    throw new SoapFault("Authentication Required", SoapFault.FAULT_CODE_CLIENT);
                }

                // set the user for later use when checking permissions
                if ( subject.getPrincipals().isEmpty() ) {
                    subject.getPrincipals().add(user);
                }
            }
        } catch ( ObjectModelException fe ) {
            logger.log( Level.WARNING, "Error checking ESM remote credentials.", fe );
            throw new SoapFault("Server Error", SoapFault.FAULT_CODE_SERVER);
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(EsmApiInterceptor.class.getName());

    private final LicenseManager licenseManager;
    private final TrustedEmsManager trustedEmsManager;
    private final TrustedEmsUserManager trustedEmsUserManager;
    private final AdminSessionManager adminSessionManager;

    private void enforceLicensed( final String operation ) {
        try {
            licenseManager.requireFeature( GatewayFeatureSets.SERVICE_REMOTE_MANAGEMENT );
        } catch ( LicenseException e) {
            logger.log( Level.WARNING, "License checking failed when invoking the operation '" + operation + "'.");
            throw new LicenseRuntimeException(e);
        }
    }

    private String getHeader( final Map<String,List<String>> headers, final String name ) {
        String value = null;

        for ( Map.Entry<String,List<String>> header : headers.entrySet() ) {
            if ( name.equalsIgnoreCase(header.getKey()) ) {
                List<String> values = header.getValue();
                if ( values.size() > 0 ) {
                    value = values.get(0);                    
                }
                break;
            }
        }

        return value;
    }

}

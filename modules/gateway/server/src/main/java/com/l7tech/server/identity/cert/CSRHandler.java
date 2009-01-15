/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.identity.cert;

import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.IOUtils;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.identity.*;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.prov.RsaSignerEngine;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.server.AuthenticatableHttpServlet;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.GatewayFeatureSets;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.server.event.system.CertificateSigningServiceEvent;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.transport.TransportModule;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;

/**
 * Servlet which handles the CSR requests coming from the XML VPN Client. Must come
 * through ssl and must contain valid credentials embedded in basic auth header.
 * <p/>
 * When the node handling the csr request does not have access to the master key, it tries
 * to forward the request to a node that has it.
 */
public class CSRHandler extends AuthenticatableHttpServlet {

    @Override
    public void init( final ServletConfig config ) throws ServletException {
        super.init(config);
        defaultKey = (DefaultKey)getApplicationContext().getBean("defaultKey", DefaultKey.class);
        providerConfigManager = (IdentityProviderConfigManager)getApplicationContext().getBean("identityProviderConfigManager", IdentityProviderConfigManager.class);
        auditContext = (AuditContext)getApplicationContext().getBean("auditContext", AuditContext.class);
    }

    @Override
    protected String getFeature() {
        return GatewayFeatureSets.SERVICE_CSRHANDLER;
    }

    @Override
    protected SsgConnector.Endpoint getRequiredEndpoint() {
        return SsgConnector.Endpoint.CSRHANDLER;
    }

    @Override
    protected void doPost( final HttpServletRequest request,
                           final HttpServletResponse response)
      throws ServletException, IOException {

        // make sure we come in through ssl
        if (!request.isSecure()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "CSR requests must come through ssl port");
            return;
        }

        // Authentication
        final AuthenticationResult[] results;
        try {
            results = authenticateRequestBasic(request);
        } catch (BadCredentialsException e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "must provide valid credentials");
            logger.log(Level.SEVERE, "Failed authentication", e);
            return;
        } catch (MissingCredentialsException e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "must provide valid credentials");
            logger.log(Level.SEVERE, "Failed authentication", e);
            return;
        } catch (IssuedCertNotPresentedException e) {
            logger.log(Level.WARNING, "Requestor is refused csr");
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "CSR Forbidden." +
              " Contact your administrator for more info.");
            return;
        } catch ( LicenseException e) {
            logger.log(Level.WARNING, "Service is unlicensed, returning 500", e);
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Gateway CA service not enabled by license");
            return;
        } catch (TransportModule.ListenerException e) {
            logger.log(Level.WARNING, "Request not permitted on this port, returning 500", e);
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Gateway CA service not enabled on this port");
            return;
        }

        if (results == null || results.length < 1) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "must provide valid credentials");
            logger.warning("CSR Handler called without credentials");
            return;
        } else if (results.length > 1) {
            String msg = "Ambiguous authentication - credentials valid in more than one identity provider.";
            response.sendError(HttpServletResponse.SC_CONFLICT, msg);
            logger.warning(msg);
            return;
        }

        final AuthenticationResult authResult = results[0];
        final User authenticatedUser = authResult.getUser();
        final X509Certificate requestCert = authResult.getAuthenticatedCert();

        try {
            if (!clientCertManager.userCanGenCert(authenticatedUser, requestCert)) {
                logger.log(Level.SEVERE, "user is refused csr: " + authenticatedUser.getLogin());
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "CSR Forbidden." +
                  " Contact your administrator for more info.");
                return;
            }

            final long oid  = authenticatedUser.getProviderId();
            final IdentityProviderConfig conf = providerConfigManager.findByPrimaryKey(oid);
            if (conf instanceof LdapIdentityProviderConfig) {
                LdapIdentityProviderConfig ldapIdentityProviderConfig = (LdapIdentityProviderConfig) conf;
                if (ldapIdentityProviderConfig.isUserCertsEnabled()) {
                   logger.log(Level.WARNING,
                        "The LDAP Identity provider \"" + ldapIdentityProviderConfig.getName() + "\" allows client certs. Cannot grant a CSR for users with certs in LDAP.");
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "CSR Forbidden." +
                        " Contact your administrator for more info.");
                    return;
                }
            }
        } catch (FindException e) {
            logger.log(Level.SEVERE,
                    "Couldn't look up the identity provider for the user : " + authenticatedUser.getLogin() + " CSR denied");
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "CSR Forbidden." +
              " Contact your administrator for more info.");
            return;
        }

        final byte[] csr = readCSRFromRequest(request);
        final Certificate cert;

        String certSubject = "cn=" + authenticatedUser.getLogin();
        // todo: perhaps we should use the real dn in the case of ldap users but then we
        // would need to change our runtime authentication mechanism

        // sign request
        try {
            // for internal users, if an account expiration is specified, make sure the cert created matches it
            if (authenticatedUser instanceof InternalUser) {
                InternalUser iu = (InternalUser)authenticatedUser;
                if (iu.getExpiration() != -1) {
                    cert = sign(csr, certSubject, iu.getExpiration());
                } else {
                    cert = sign(csr, certSubject);
                }
            } else {
                cert = sign(csr, certSubject);
            }
        } catch (NoCaException ncae) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Gateway CA service not available");
            logger.log(Level.WARNING, "This Gateway does not currently have a default CA key configured.");
            return;
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            logger.log(Level.SEVERE, e.getMessage(), e);
            return;
        }

        // record new cert
        final boolean wasSystem = auditContext.isSystem();
        auditContext.setSystem(true);
        try {
            clientCertManager.recordNewUserCert(authenticatedUser, cert, authResult.isCertSignedByStaleCA());
            final String message = buildIssuedMessage(authenticatedUser);
            getApplicationContext().publishEvent(new CertificateSigningServiceEvent(this, Level.INFO 
                                                , request.getRemoteAddr()
                                                , message, authenticatedUser.getProviderId()
                                                , getName(authenticatedUser), authenticatedUser.getId()));
        } catch (UpdateException e) {
            final String msg = "Could not record cert. " + e.getMessage();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
            logger.log(Level.SEVERE, msg, e);
            return;
        } finally {
            auditContext.setSystem(wasSystem);
        }
        
        // send cert back
        try {
            final byte[] certbytes = cert.getEncoded();
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/x-x509-ca-cert");
            response.setContentLength(certbytes.length);
            response.getOutputStream().write(certbytes);
            response.flushBuffer();
            logger.fine("sent new cert to user " + authenticatedUser.getLogin() +
              ". Subject DN=" + ((X509Certificate)(cert)).getSubjectDN().toString());
        } catch (CertificateEncodingException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private String buildIssuedMessage( User user ) {                 
        final StringBuilder message = new StringBuilder();
        message.append("Issued certificate for user ");
        message.append(getName(user));
        message.append(" (#");
        message.append(user.getId());
        message.append("), ");
        message.append(getIdentityProviderName(user.getProviderId()));
        message.append(" (#");
        message.append(user.getProviderId());
        message.append(")");

        return message.toString();
    }

    private String getName( final User user ) {
        return user.getLogin()!=null ? user.getLogin() : user.getId();
    }

    private byte[] readCSRFromRequest( final HttpServletRequest request ) throws IOException {
        // csr request might be based64 or not, we need to see what format we are getting
        byte[] contents = IOUtils.slurpStream(request.getInputStream());
        try {
            return CertUtils.csrPemToBinary(contents);
        } catch (IOException e) {
            // PEM decoding failed -- assume it was already binary
            return contents;
        }
    }

    private RsaSignerEngine getSigner() throws NoCaException {
        final SignerInfo ca = defaultKey.getCaInfo();
        if (ca == null)
            throw new NoCaException();
        return JceProvider.createRsaSignerEngine(ca.getPrivate(), ca.getCertificateChain());
    }

    private Certificate sign( final byte[] csr, final String subject ) throws Exception {
        return getSigner().createCertificate(csr, subject);
    }

    private Certificate sign( final byte[] csr, final String subject, final long expiration ) throws Exception {
        return getSigner().createCertificate(csr, subject, expiration);
    }

    private static final class NoCaException extends Exception {}

    private DefaultKey defaultKey;
    private IdentityProviderConfigManager providerConfigManager;
    private AuditContext auditContext;

    public static final String AUTH_HEADER_NAME = "Authorization";
    public static final String ROUTED_FROM_PEER = "Routed-From-Peer";
}

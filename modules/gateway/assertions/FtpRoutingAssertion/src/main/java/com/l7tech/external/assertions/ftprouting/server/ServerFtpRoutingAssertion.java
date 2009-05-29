/*
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.ftprouting.server;

import com.jscape.inet.ftp.FtpException;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.gateway.common.transport.ftp.*;
import com.l7tech.util.CausedIOException;
import com.l7tech.external.assertions.ftprouting.FtpRoutingAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerRoutingAssertion;
import com.l7tech.server.transport.ftp.FtpClientUtils;
import com.l7tech.server.DefaultKey;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Assertion that routes the request to an FTP server.
 *
 * @since SecureSpan 4.0
 * @author rmak
 */
public class ServerFtpRoutingAssertion extends ServerRoutingAssertion<FtpRoutingAssertion> {
    private static final Logger _logger = Logger.getLogger(ServerFtpRoutingAssertion.class.getName());
    private final Auditor _auditor;
    private final X509TrustManager _trustManager;
    private final DefaultKey _keyFinder;

    public ServerFtpRoutingAssertion(FtpRoutingAssertion assertion, ApplicationContext applicationContext) {
        super(assertion, applicationContext, _logger);
        _auditor = new Auditor(this, applicationContext, _logger);
        _trustManager = (X509TrustManager)applicationContext.getBean("routingTrustManager", X509TrustManager.class);
        _keyFinder = (DefaultKey)applicationContext.getBean("defaultKey", DefaultKey.class);

    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final Message request = context.getRequest();

        final MimeKnob mimeKnob = request.getKnob(MimeKnob.class);

        // DELETE CURRENT SECURITY HEADER IF NECESSARY
        try {
            handleProcessedSecurityHeader(request);
        } catch(SAXException se) {
            _logger.log(Level.INFO, "Error processing security header, request XML invalid ''{0}''", se.getMessage());
        }

        String userName = null;
        String password = null;
        if (assertion.getCredentialsSource() == FtpCredentialsSource.PASS_THRU) {
            final LoginCredentials credentials = context.getDefaultAuthenticationContext().getLastCredentials();
            if (credentials != null) {
                userName = credentials.getName();
                password = new String(credentials.getCredentials());
            }
            if (userName == null) {
                _auditor.logAndAudit(AssertionMessages.FTP_ROUTING_PASSTHRU_NO_USERNAME);
                return AssertionStatus.FAILED;
            }
        } else if (assertion.getCredentialsSource() == FtpCredentialsSource.SPECIFIED) {
            userName = assertion.getUserName();
            password = assertion.getPassword();
        }

        String fileName = null;
        if (assertion.getFileNameSource() == FtpFileNameSource.AUTO) {
            // Cannot use STOU because
            // {@link com.jscape.inet.ftp.Ftp.uploadUnique(InputStream, String)}
            // sends a parameter as filename seed, which causes IIS to respond
            // with "500 'STOU seed': Invalid number of parameters".
            // This was reported (2007-05-07) to JSCAPE, who said they will add
            // a method to control STOU parameter.
            fileName = context.getRequestId().toString();
        } else if (assertion.getFileNameSource() == FtpFileNameSource.PATTERN) {
            fileName = expandVariables(context, assertion.getFileNamePattern());
        }

        try {
            final InputStream messageBodyStream = mimeKnob.getEntireMessageBodyAsInputStream();
            final FtpSecurity security = assertion.getSecurity();
            if (security == FtpSecurity.FTP_UNSECURED) {
                doFtp(context, userName, password, messageBodyStream, fileName);
            } else if (security == FtpSecurity.FTPS_EXPLICIT) {
                doFtps(context, true, userName, password, messageBodyStream, fileName);
            } else if (security == FtpSecurity.FTPS_IMPLICIT) {
                doFtps(context, false, userName, password, messageBodyStream, fileName);
            }
            return AssertionStatus.NONE;
        } catch (NoSuchPartException e) {
            throw new CausedIOException("Unable to get request body.", e);
        } catch (FtpException e) {
            _auditor.logAndAudit(AssertionMessages.FTP_ROUTING_FAILED_UPLOAD, assertion.getHostName(), e.getMessage());
            return AssertionStatus.FAILED;
        }
    }

    private String getDirectory(PolicyEnforcementContext context, FtpRoutingAssertion assertion) {
        return expandVariables(context, assertion.getDirectory());
    }

    private void doFtp(PolicyEnforcementContext context,
                       String userName,
                       String password,
                       InputStream is,
                       String fileName) throws FtpException {

        String hostName = assertion.getHostName();
        String directory = getDirectory(context, assertion);

        assert(hostName != null);
        assert(userName != null);
        assert(password != null);
        assert(directory != null);

        FtpClientConfig config = FtpClientUtils.newConfig(hostName);
        config.setPort(assertion.getPort()).setUser(userName).setPass(password).
                setDirectory(directory).setTimeout(assertion.getTimeout());

        FtpClientUtils.upload(config, is, fileName);
    }

    private void doFtps(PolicyEnforcementContext context,
                        boolean isExplicit,
                        String userName,
                        String password,
                        InputStream is,
                        String fileName) throws FtpException {

        boolean verifyServerCert = assertion.isVerifyServerCert();
        String hostName = assertion.getHostName();
        boolean useClientCert = assertion.isUseClientCert();
        long clientCertKeystoreId = assertion.getClientCertKeystoreId();
        String clientCertKeyAlias = assertion.getClientCertKeyAlias();
        String directory = getDirectory(context, assertion);

        assert(!verifyServerCert || _trustManager != null);
        assert(hostName != null);
        assert(userName != null);
        assert(password != null);
        assert(!useClientCert || (clientCertKeystoreId != -1 && clientCertKeyAlias != null));
        assert(directory != null);

        FtpClientConfig config = FtpClientUtils.newConfig(hostName);
        config.setPort(assertion.getPort()).setUser(userName).setPass(password).setDirectory(directory).
                setTimeout(assertion.getTimeout()).setSecurity(isExplicit ? FtpSecurity.FTPS_EXPLICIT : FtpSecurity.FTPS_IMPLICIT);

        X509TrustManager trustManager = null;
        if (verifyServerCert) {
            config.setVerifyServerCert(true);
            trustManager = _trustManager;
        }

        DefaultKey keyFinder = null;
        if (useClientCert) {
            config.setUseClientCert(true).setClientCertId(clientCertKeystoreId).setClientCertAlias(clientCertKeyAlias);
            keyFinder = _keyFinder;
        }

        FtpClientUtils.upload(config, is, fileName, keyFinder, trustManager);
    }

    private String expandVariables(PolicyEnforcementContext context, String pattern) {
        final String[] variablesUsed = Syntax.getReferencedNames(pattern);
        final Map<String, Object> vars = context.getVariableMap(variablesUsed, _auditor);
        return ExpandVariables.process(pattern, vars, _auditor);
    }
}

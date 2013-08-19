package com.l7tech.external.assertions.ftprouting.server;

import com.jscape.inet.ftp.FtpException;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.gateway.common.transport.ftp.*;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.util.CausedIOException;
import com.l7tech.external.assertions.ftprouting.FtpRoutingAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerRoutingAssertion;
import com.l7tech.server.transport.ftp.FtpClientUtils;
import com.l7tech.server.DefaultKey;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import javax.net.ssl.X509TrustManager;
import javax.net.ssl.HostnameVerifier;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;

/**
 * Assertion that routes the request to an FTP server.
 *
 * @since SecureSpan 4.0
 * @author rmak
 */
public class ServerFtpRoutingAssertion extends ServerRoutingAssertion<FtpRoutingAssertion> {
    private final X509TrustManager _trustManager;
    private final HostnameVerifier _hostnameVerifier;
    private final DefaultKey _keyFinder;

    public ServerFtpRoutingAssertion(FtpRoutingAssertion assertion, ApplicationContext applicationContext) {
        super(assertion, applicationContext);
        _trustManager = applicationContext.getBean("routingTrustManager", X509TrustManager.class);
        _hostnameVerifier = applicationContext.getBean("hostnameVerifier", HostnameVerifier.class);
        _keyFinder = applicationContext.getBean("defaultKey", DefaultKey.class);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {

        Message request;
        try {
            request = context.getTargetMessage(assertion.getRequestTarget());
        } catch (NoSuchVariableException e) {
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, e.getMessage(), e);
        }

        final MimeKnob mimeKnob = request.getKnob(MimeKnob.class);
        if (mimeKnob == null || !request.isInitialized()) {
            // Uninitialized request
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Request is not initialized; nothing to route");
            return AssertionStatus.BAD_REQUEST;
        }

        // DELETE CURRENT SECURITY HEADER IF NECESSARY
        try {
            handleProcessedSecurityHeader(request);
        } catch(SAXException se) {
            logger.log(Level.INFO, "Error processing security header, request XML invalid ''{0}''", se.getMessage());
        }

        final VariableExpander variableExpander = getVariableExpander( context );

        String userName = null;
        String password = null;
        if (assertion.getCredentialsSource() == FtpCredentialsSource.PASS_THRU) {
            final LoginCredentials credentials = context.getDefaultAuthenticationContext().getLastCredentials();
            if (credentials != null) {
                userName = credentials.getName();
                password = new String(credentials.getCredentials());
            }
            if (userName == null) {
                logAndAudit(AssertionMessages.FTP_ROUTING_PASSTHRU_NO_USERNAME);
                return AssertionStatus.FAILED;
            }
        } else if (assertion.getCredentialsSource() == FtpCredentialsSource.SPECIFIED) {
            userName = getUserName(variableExpander);
            password = getPassword(variableExpander);
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
            fileName = variableExpander.expandVariables( assertion.getFileNamePattern() );
        }

        try {
            final InputStream messageBodyStream = mimeKnob.getEntireMessageBodyAsInputStream();
            final long bodyBytes = mimeKnob.getContentLength();
            final FtpSecurity security = assertion.getSecurity();
            if (security == FtpSecurity.FTP_UNSECURED) {
                doFtp(variableExpander, userName, password, messageBodyStream, bodyBytes, fileName);
            } else if (security == FtpSecurity.FTPS_EXPLICIT) {
                doFtps(variableExpander, true, userName, password, messageBodyStream, bodyBytes, fileName);
            } else if (security == FtpSecurity.FTPS_IMPLICIT) {
                doFtps(variableExpander, false, userName, password, messageBodyStream, bodyBytes, fileName);
            }
            return AssertionStatus.NONE;
        } catch (NoSuchPartException e) {
            throw new CausedIOException("Unable to get request body.", e);
        } catch (FtpException e) {
            logAndAudit(AssertionMessages.FTP_ROUTING_FAILED_UPLOAD, getHostName(variableExpander), e.getMessage());
            return AssertionStatus.FAILED;
        }
    }

    private String getHostName(final VariableExpander variableExpander) {
        return variableExpander.expandVariables( assertion.getHostName() );
    }

    private int getPort(final VariableExpander variableExpander) {
        try {
            return Integer.parseInt(variableExpander.expandVariables( assertion.getPort() ));
        } catch (NumberFormatException e) {
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, e.getMessage(), e);
        }
    }

    private String getDirectory(final VariableExpander variableExpander) {
        return variableExpander.expandVariables( assertion.getDirectory() );
    }

    private String getUserName(final VariableExpander variableExpander) {
        return variableExpander.expandVariables( assertion.getUserName() );
    }

    private String getPassword(final VariableExpander variableExpander) {
        return assertion.isPasswordUsesContextVariables() ? variableExpander.expandVariables( assertion.getPassword() ) : assertion.getPassword();
    }

    private void doFtp(final VariableExpander variableExpander,
                       String userName,
                       String password,
                       InputStream is,
                       long count,
                       String fileName) throws FtpException {

        String hostName = getHostName(variableExpander);
        String directory = getDirectory(variableExpander);

        assert(hostName != null);
        assert(userName != null);
        assert(password != null);
        assert(directory != null);

        FtpClientConfig config = FtpClientUtils.newConfig(hostName);
        config.setPort(getPort(variableExpander)).setUser(userName).setPass(password).
                setDirectory(directory).setTimeout(assertion.getTimeout());

        FtpClientUtils.upload(config, is, count, fileName);
    }

    private void doFtps(final VariableExpander variableExpander,
                        boolean isExplicit,
                        String userName,
                        String password,
                        InputStream is,
                        long count,
                        String fileName) throws FtpException {

        boolean verifyServerCert = assertion.isVerifyServerCert();
        String hostName = getHostName(variableExpander);
        boolean useClientCert = assertion.isUseClientCert();
        Goid clientCertKeystoreId = assertion.getClientCertKeystoreId();
        String clientCertKeyAlias = assertion.getClientCertKeyAlias();
        String directory = getDirectory(variableExpander);

        assert(!verifyServerCert || _trustManager != null);
        assert(hostName != null);
        assert(userName != null);
        assert(password != null);
        assert(!useClientCert || (!Goid.isDefault(clientCertKeystoreId) && clientCertKeyAlias != null));
        assert(directory != null);

        FtpClientConfig config = FtpClientUtils.newConfig(hostName);
        config.setPort(getPort(variableExpander)).setUser(userName).setPass(password).setDirectory(directory).
                setTimeout(assertion.getTimeout()).setSecurity(isExplicit ? FtpSecurity.FTPS_EXPLICIT : FtpSecurity.FTPS_IMPLICIT);

        X509TrustManager trustManager = null;
        HostnameVerifier hostnameVerifier = null;
        if (verifyServerCert) {
            config.setVerifyServerCert(true);
            trustManager = _trustManager;
            hostnameVerifier = _hostnameVerifier;
        }

        DefaultKey keyFinder = null;
        if (useClientCert) {
            config.setUseClientCert(true).setClientCertId(clientCertKeystoreId).setClientCertAlias(clientCertKeyAlias);
            keyFinder = _keyFinder;
        }

        FtpClientUtils.upload(config, is, count, fileName, keyFinder, trustManager, hostnameVerifier);
    }
}

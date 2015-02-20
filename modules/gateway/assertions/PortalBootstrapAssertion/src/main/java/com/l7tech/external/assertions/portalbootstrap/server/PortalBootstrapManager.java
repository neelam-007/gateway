package com.l7tech.external.assertions.portalbootstrap.server;

import com.l7tech.common.io.SingleCertX509KeyManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.identity.User;
import com.l7tech.message.AbstractHttpResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.token.OpaqueSecurityToken;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.event.AdminInfo;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.util.ApplicationContextInjector;
import com.l7tech.util.*;
import org.springframework.context.ApplicationContext;

import javax.inject.Inject;
import javax.net.ssl.*;
import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class that provides portal bootstrap services.
 * Created by module load listener.
 * Invoked by extension interface.
 */
public class PortalBootstrapManager {
    private static final Logger logger = Logger.getLogger( PortalBootstrapManager.class.getName() );

    private static PortalBootstrapManager instance = null;

    private final ApplicationContext applicationContext;
    private boolean initialized;

    @Inject
    private DefaultKey defaultKey;

    @Inject
    private ServerPolicyFactory serverPolicyFactory;

    @Inject
    private WspReader wspReader;

    private PortalBootstrapManager( ApplicationContext context ) {
        this.applicationContext = context;
    }

    static void initialize( ApplicationContext context ) {
        if ( instance != null )
            throw new IllegalStateException( "PortalBootstrapManager has already been created" );
        instance = new PortalBootstrapManager( context );
    }

    public static PortalBootstrapManager getInstance() {
        if ( instance == null )
            throw new IllegalStateException( "PortalBootstrapManager has not been initialized" );
        instance.ensureInitialized();
        return instance;
    }

    private synchronized void ensureInitialized() {
        if ( !initialized ) {
            ApplicationContextInjector injector = applicationContext.getBean( "injector", ApplicationContextInjector.class );
            injector.inject( this );
            initialized = true;
        }
    }

    public void enrollWithPortal( String enrollmentUrl ) throws IOException {
        URL url = new URL( enrollmentUrl );

        if ( !"https".equals( url.getProtocol() ) )
            throw new IOException( "Enrollment URL must begin with https" );

        String query = url.getQuery();
        Pattern pinExtractor = Pattern.compile( "sckh=([a-zA-Z0-9\\_\\-]+)" );
        Matcher pinMatcher = pinExtractor.matcher( query );
        if ( !pinMatcher.find() )
            throw new IOException( "Enrollment URL does not contain a server certificate key hash (sckh) parameter" );

        String sckh = pinMatcher.group( 1 );
        final byte[] pinBytes = HexUtils.decodeBase64Url( sckh );

        X509TrustManager tm = new X509TrustManager() {
            @Override
            public void checkClientTrusted( X509Certificate[] chain, String authType ) throws CertificateException {
                throw new CertificateException( "Method not supported" );
            }

            @Override
            public void checkServerTrusted( X509Certificate[] chain, String authType ) throws CertificateException {
                X509Certificate subjectCert = chain[0];
                byte[] encodedPublicKey = subjectCert.getPublicKey().getEncoded();
                byte[] keyHash = HexUtils.getSha256Digest( encodedPublicKey );
                if ( !Arrays.equals( keyHash, pinBytes) )
                    throw new CertificateException( "Server certificate key hash does not match pin value" );
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };

        final SSLSocketFactory socketFactory;
        try {
            SsgKeyEntry dk = defaultKey.getSslInfo();
            X509KeyManager km = new SingleCertX509KeyManager( dk.getCertificateChain(), dk.getPrivate() );

            SSLContext sslContext = SSLContext.getInstance( "TLS" );
            sslContext.init( new KeyManager[] { km }, new TrustManager[] { tm }, JceProvider.getInstance().getSecureRandom() );
            socketFactory = sslContext.getSocketFactory();
        } catch ( NoSuchAlgorithmException | KeyManagementException | UnrecoverableKeyException e ) {
            throw new IOException( e );
        }

        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setSSLSocketFactory( socketFactory );
        byte[] bundleBytes = IOUtils.slurpStream( connection.getInputStream() );

        installBundle( bundleBytes );
    }

    private void installBundle( byte[] bundleBytes ) throws IOException {
        String policyXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:RESTGatewayManagement>\n" +
                "            <L7p:OtherTargetMessageVariable stringValue=\"mess\"/>\n" +
                "            <L7p:Target target=\"OTHER\"/>\n" +
                "        </L7p:RESTGatewayManagement>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n";
        Assertion assertion = wspReader.parseStrictly( policyXml, WspReader.Visibility.omitDisabled );

        ServerAssertion sph = null;
        PolicyEnforcementContext context = null;
        try {
            sph = serverPolicyFactory.compilePolicy( assertion, false );

            Message mess = new Message();
            mess.initialize( ContentTypeHeader.XML_DEFAULT, bundleBytes );

            context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( new Message(), new Message() );
            context.getResponse().attachHttpResponseKnob( new AbstractHttpResponseKnob() {} );
            context.setVariable( "mess", mess );
            context.setVariable( "restGatewayMan.action", "PUT" );
            context.setVariable( "restGatewayMan.uri", "1.0/bundle" );
            User adminUser = AdminInfo.find( false ).user;
            if ( adminUser != null ) {
                context.getDefaultAuthenticationContext().addAuthenticationResult( new AuthenticationResult( adminUser, new OpaqueSecurityToken() ) );
            }

            AssertionStatus result = sph.checkRequest( context );
            int httpStatus = context.getResponse().getHttpResponseKnob().getStatus();

            String resp = new String( IOUtils.slurpStream( context.getResponse().getMimeKnob().getEntireMessageBodyAsInputStream() ), Charsets.UTF8 );

            logger.log( Level.INFO, "Enrollment response from local RESTMAN bundle install: result=" + result + " httpStatus=" + httpStatus + " body:\n" + resp );

            if ( !AssertionStatus.NONE.equals( result ) || httpStatus != 200 ) {
                throw new IOException( "RESTMAN failed with result=" + result + " httpStatus=" + httpStatus + ": " + resp );
            }

        } catch ( ServerPolicyException | LicenseException e ) {
            throw new IOException( "Unable to prepare RESTMAN policy: " + ExceptionUtils.getMessage( e ), e );
        } catch ( PolicyAssertionException|NoSuchPartException e ) {
            throw new IOException( "Unable to invoke RESTMAN policy: " + ExceptionUtils.getMessage( e ), e );
        } finally {
            ResourceUtils.closeQuietly( context );
            ResourceUtils.closeQuietly( sph );
        }
    }
}

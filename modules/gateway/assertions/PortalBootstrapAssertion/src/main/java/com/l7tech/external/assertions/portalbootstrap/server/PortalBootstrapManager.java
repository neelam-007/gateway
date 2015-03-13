package com.l7tech.external.assertions.portalbootstrap.server;

import com.l7tech.common.io.CertGenParams;
import com.l7tech.common.io.KeyGenParams;
import com.l7tech.common.io.SingleCertX509KeyManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.identity.User;
import com.l7tech.message.AbstractHttpResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.token.OpaqueSecurityToken;
import com.l7tech.server.cluster.ClusterInfoManager;
import com.l7tech.server.event.AdminInfo;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.security.keystore.SsgKeyFinder;
import com.l7tech.server.security.keystore.SsgKeyStore;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.util.ApplicationContextInjector;
import com.l7tech.util.*;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;

import javax.inject.Inject;
import javax.net.ssl.*;
import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
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
    private ServerPolicyFactory serverPolicyFactory;

    @Inject
    private WspReader wspReader;

    @Inject
    private SsgKeyStoreManager ssgKeyStoreManager;

    @Inject
    private RbacServices rbacServices;

    @Inject
    private ClusterInfoManager clusterInfoManager;

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
        User adminUser = AdminInfo.find( false ).user;
        if ( null == adminUser )
            throw new IllegalStateException( "No administrative user authenticated" );

        URL url = new URL( enrollmentUrl );

        if ( !"https".equals( url.getProtocol() ) )
            throw new IOException( "Enrollment URL must begin with https" );

        String query = url.getQuery();
        Pattern pinExtractor = Pattern.compile( "sckh=([a-zA-Z0-9\\_\\-]+)" );
        Matcher pinMatcher = pinExtractor.matcher( query );
        if ( !pinMatcher.find() )
            throw new IOException( "Enrollment URL does not contain a server certificate key hash (sckh) parameter" );

        byte[] postBody = buildEnrollmentPostBody( adminUser );

        String sckh = pinMatcher.group( 1 );
        final byte[] pinBytes = HexUtils.decodeBase64Url( sckh );

        final SsgKeyEntry clientCert = prepareClientCert( adminUser );

        X509TrustManager tm = new PinCheckingX509TrustManager( pinBytes );
        X509KeyManager km;

        final SSLSocketFactory socketFactory;
        try {
            km = new SingleCertX509KeyManager( clientCert.getCertificateChain(), clientCert.getPrivateKey(), clientCert.getAlias() );

            SSLContext sslContext = SSLContext.getInstance( "TLS" );
            sslContext.init( new KeyManager[] { km }, new TrustManager[] { tm }, JceProvider.getInstance().getSecureRandom() );
            socketFactory = sslContext.getSocketFactory();
        } catch ( NoSuchAlgorithmException | KeyManagementException | UnrecoverableKeyException e ) {
            throw new IOException( e );
        }

        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestMethod( "POST" );
        connection.setSSLSocketFactory( socketFactory );
        connection.setRequestProperty( "Content-Type", "application/json" );
        connection.setDoOutput( true );
        connection.getOutputStream().write( postBody );
        byte[] bundleBytes = IOUtils.slurpStream( connection.getInputStream() );

        installBundle( bundleBytes, adminUser );
    }

    private byte[] buildEnrollmentPostBody( User adminUser ) throws IOException {
        Config config = ConfigFactory.getCachedConfig();
        String clusterName = config.getProperty( "clusterHost", "" );
        String buildInfo = BuildInfo.getLongBuildString();
        String ssgVersion = BuildInfo.getProductVersion();

        final Collection<ClusterNodeInfo> infos;
        try {
            infos = clusterInfoManager.retrieveClusterStatus();
        } catch ( FindException e ) {
            throw new IOException( "Unable to load cluster info: " + ExceptionUtils.getMessage( e ), e );
        }

        String nodeCount = String.valueOf( infos.size() );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonFactory jsonFactory = new JsonFactory();
        JsonGenerator gen = jsonFactory.createJsonGenerator( baos );
        gen.writeStartObject();
        gen.writeStringField( "cluster_name", clusterName );
        gen.writeStringField( "version", ssgVersion );
        gen.writeStringField( "build_info", buildInfo );
        gen.writeStringField( "adminuser_providerid", adminUser.getProviderId().toString() );
        gen.writeStringField( "adminuser_id", adminUser.getId() );
        gen.writeStringField( "node_count", nodeCount );
        gen.writeFieldName( "nodeInfo" );
        gen.writeStartArray();
        gen.flush();

        for ( ClusterNodeInfo info : infos ) {
            gen.writeStartObject();
            gen.writeStringField( "name", info.getName() );
            gen.writeEndObject();
        }

        gen.writeEndArray();
        gen.writeEndObject();
        gen.flush();

        return baos.toByteArray();
    }

    @NotNull
    private SsgKeyEntry prepareClientCert( @NotNull User adminUser ) {
        // TODO allow key to be specified by admin user in GUI.  For now, will always use "portalman" key
        // if exists, otherwise will try and create one.
        // If allowing alias to be configured, ensure it does not contain any characters that are invalid when
        // used without quoting inside an X500Name.
        String alias = "portalman";

        try {
            return ssgKeyStoreManager.lookupKeyByKeyAlias( alias, PersistentEntity.DEFAULT_GOID );
        } catch ( ObjectNotFoundException e ) {
            /* FALLTHROUGH and try to create new */
        } catch ( FindException|KeyStoreException e ) {
            throw new RuntimeException( "Unable to look up portalman key: " + ExceptionUtils.getMessage( e ), e );
        }

        SsgKeyStore ks = findFirstMutableKeyStore();

        // Check permissions before creating key
        try {
            if ( !rbacServices.isPermittedForEntity( adminUser, ks, OperationType.UPDATE, null ) )
                throw new PermissionDeniedException( OperationType.UPDATE, ks, null );
            if ( !rbacServices.isPermittedForAnyEntityOfType( adminUser, OperationType.CREATE, EntityType.SSG_KEY_ENTRY ) )
                throw new PermissionDeniedException( OperationType.CREATE, EntityType.SSG_KEY_ENTRY );
        } catch ( FindException e ) {
            throw new RuntimeException( "Unable to check permission for private key creation: " + ExceptionUtils.getMessage( e ), e );
        }

        // TODO include tenant cluster name in this DN to make things easier to manage inside the portal DB
        String dn = "cn=" + alias;
        KeyGenParams keyGenParams = new KeyGenParams( 2048 );
        CertGenParams certGenParams = new CertGenParams( new X500Principal( dn ), 365 * 25, false, null );

        try {
            Future<X509Certificate> future = ks.generateKeyPair( null, alias, keyGenParams, certGenParams, null );
            future.get();

            return ks.getCertificateChain( alias );

        } catch ( GeneralSecurityException | InterruptedException | ExecutionException | ObjectNotFoundException e ) {
            throw new RuntimeException( "Unable to generate new portalman private key: " + ExceptionUtils.getMessage( e ), e );
        }
    }

    @NotNull
    private SsgKeyStore findFirstMutableKeyStore() {
        try {
            List<SsgKeyFinder> keystores = ssgKeyStoreManager.findAll();
            for ( SsgKeyFinder keyFinder : keystores ) {
                if ( keyFinder.isMutable() ) {
                    return keyFinder.getKeyStore();
                }
            }
        } catch ( FindException|KeyStoreException e ) {
            throw new RuntimeException( "Unable to access key stores: " + ExceptionUtils.getMessage( e ), e );
        }

        throw new RuntimeException( "portalman private key does not already exist, and no mutable keystore exists in which to create a new one." );
    }

    private void installBundle( @NotNull byte[] bundleBytes, @NotNull User adminUser ) throws IOException {
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
            context.getDefaultAuthenticationContext().addAuthenticationResult( new AuthenticationResult( adminUser, new OpaqueSecurityToken() ) );

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

    private static class PinCheckingX509TrustManager implements X509TrustManager {
        private final byte[] pinBytes;

        public PinCheckingX509TrustManager( byte[] pinBytes ) {
            this.pinBytes = pinBytes;
        }

        @Override
        public void checkClientTrusted( X509Certificate[] chain, String authType ) throws CertificateException {
            throw new CertificateException( "Method not supported" );
        }

        @Override
        public void checkServerTrusted( X509Certificate[] chain, String authType ) throws CertificateException {
            // TODO for now pin required to match server subject cert, rather than some intermediate cert
            X509Certificate subjectCert = chain[0];
            byte[] encodedPublicKey = subjectCert.getPublicKey().getEncoded();
            byte[] keyHash = HexUtils.getSha256Digest( encodedPublicKey );
            if ( !Arrays.equals( keyHash, pinBytes ) )
                throw new CertificateException( "Server certificate key hash does not match pin value" );
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}

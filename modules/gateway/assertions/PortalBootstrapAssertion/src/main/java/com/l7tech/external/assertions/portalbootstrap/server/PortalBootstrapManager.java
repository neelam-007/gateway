package com.l7tech.external.assertions.portalbootstrap.server;

import com.l7tech.common.io.CertGenParams;
import com.l7tech.common.io.KeyGenParams;
import com.l7tech.common.io.SSLSocketFactoryWrapper;
import com.l7tech.common.io.SingleCertX509KeyManager;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.identity.User;
import com.l7tech.message.AbstractHttpResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.token.OpaqueSecurityToken;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.cassandra.CassandraConnectionHolder;
import com.l7tech.server.cassandra.CassandraConnectionManager;
import com.l7tech.server.cluster.ClusterInfoManager;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.jdbc.JdbcConnectionManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.EncapsulatedAssertionConfigManager;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.security.keystore.SsgKeyFinder;
import com.l7tech.server.security.keystore.SsgKeyStore;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.util.ApplicationContextInjector;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.util.BuildInfo;
import com.l7tech.util.Charsets;
import com.l7tech.util.Config;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.RandomUtil;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.Triple;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import javax.inject.Inject;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Class that provides portal bootstrap services.
 * Created by module load listener.
 * Invoked by extension interface.
 */
public class PortalBootstrapManager {
    private static final Logger logger = Logger.getLogger( PortalBootstrapManager.class.getName() );
    static final String CLUSTER_NAME = "cluster_name";
    static final String VERSION = "version";
    static final String BUILD_INFO = "build_info";
    static final String ADMINUSER_PROVIDERID = "adminuser_providerid";
    static final String ADMINUSER_ID = "adminuser_id";
    static final String OTK_CLIENT_DB_GET_POLICY = "otk_client_db_get_policy";
    static final String OTK_REQUIRE_OAUTH_2_TOKEN_ENCASS = "otk_require_oauth_2_token_encass";
    static final String OTK_JDBC_CONN = "otk_jdbc_conn";
    static final String NODE_COUNT = "node_count";
    static final String NODE_INFO = "nodeInfo";
    static final String NAME = "name";
    static final String OTK_CLIENT_DB_GET = "OTK Client DB GET";
    static final String OTK_REQUIRE_OAUTH_2_0_TOKEN = "OTK Require OAuth 2.0 Token";
    static final String OAUTH = "OAuth";
    static final String OTK_VERSION = "otk_version";
    /** OTK_GUID OTK solution kit is always fixed as a6f9bbee-87c2-43d8-b613-5c072f7b6f7d */
    static final String OTK_GUID = "a6f9bbee-87c2-43d8-b613-5c072f7b6f7d";
    static final String OTK_VERSION_TAG = "l7:SolutionKitVersion";

    // For restman calls
    static final String MESS = "mess";
    static final String POLICY_XML = String.format(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:RESTGatewayManagement>\n" +
            "            <L7p:OtherTargetMessageVariable stringValue=\"%s\"/>\n" +
            "            <L7p:Target target=\"OTHER\"/>\n" +
            "        </L7p:RESTGatewayManagement>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>\n", MESS );
    static final String REST_GATEWAY_MAN_URI = "restGatewayMan.uri";
    static final String REST_GATEWAY_MAN_ACTION= "restGatewayMan.action";


    //For Cassandra
    static final String OTK_CLIENT_NOSQL_GET = "OTK Client NoSQL GET";
    static final String OAUTH_CASSANDRA = "OAuth_Cassandra";

    private static PortalBootstrapManager instance = null;
    private static final int ENROLL_PORT = 9446;
    private static final String SKAR_ID_HEADER_FIELD = "L7-skar-id";

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

    @Inject
    private PolicyManager policyManager;

    @Inject
    private EncapsulatedAssertionConfigManager encapsulatedAssertionConfigManager;

    @Inject
    private JdbcConnectionManager jdbcConnectionManager;

    @Inject
    private CassandraConnectionManager cassandraConnectionManager;

    @Inject
    private StashManagerFactory stashManagerFactory;

    @Inject
    private ClusterPropertyManager clusterPropertyManager;

    @Inject
    private TrustedCertManager trustedCertManager;

    private Config config = ConfigFactory.getCachedConfig();

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

    public void enrollWithPortal(String enrollmentUrl) throws IOException, FindException {
        final User user = JaasUtils.getCurrentUser();
        if ( null == user )
            throw new IllegalStateException( "No administrative user authenticated" );

        URL url = new URL( enrollmentUrl + "&action=enroll" );

        if ( !"https".equals( url.getProtocol() ) )
            throw new IOException( "Enrollment URL must begin with https" );

        String query = url.getQuery();
        Pattern pinExtractor = Pattern.compile( "sckh=([a-zA-Z0-9\\_\\-]+)" );
        Matcher pinMatcher = pinExtractor.matcher( query );
        if ( !pinMatcher.find() )
            throw new IOException( "Enrollment URL does not contain a server certificate key hash (sckh) parameter" );

        byte[] postBody = buildEnrollmentPostBody( user );

        String sckh = pinMatcher.group( 1 );
        final byte[] pinBytes = HexUtils.decodeBase64Url( sckh );

        final SsgKeyEntry clientCert = prepareClientCert( user );

        PinChecker pinChecker = new PinChecker( pinBytes );
        X509KeyManager km;

        final SSLSocketFactory socketFactory;
        try {
            km = new SingleCertX509KeyManager( clientCert.getCertificateChain(), clientCert.getPrivateKey(), clientCert.getAlias() );

            SSLContext sslContext = SSLContext.getInstance( "TLS" );
            sslContext.init( new KeyManager[] { km }, new TrustManager[] { pinChecker }, JceProvider.getInstance().getSecureRandom() );
            socketFactory = sslContext.getSocketFactory();
        } catch ( NoSuchAlgorithmException | KeyManagementException | UnrecoverableKeyException e ) {
            throw new IOException( e );
        }

        InputStream input = null;
        OutputStream output = null;
        PolicyEnforcementContext context = null;
        HttpsURLConnection connection;

        try {
            connection = (HttpsURLConnection) url.openConnection();
            connection.setHostnameVerifier( pinChecker );
            connection.setRequestMethod("POST");
            enableSni( connection, socketFactory, url );
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            output = connection.getOutputStream();
            output.write(postBody);

            final int responseCode = connection.getResponseCode();

            if (responseCode != HttpsURLConnection.HTTP_OK) {
                String errMsg = "Unknown error.";
                if (connection.getErrorStream() != null) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                        errMsg = br.lines().collect(Collectors.joining(" "));
                    }
                }
                throw new IOException(errMsg);
            }

            // If the TSSG has already enrolled with a different portal pssg, it should not be allowed to re-enrolled
            String new_pssg_identifier = connection.getHeaderField("portal-config-identifier");
            String current_pssg_identifier = clusterPropertyManager.getProperty("portal.config.identifier");
            if ((new_pssg_identifier!=null) && (current_pssg_identifier!=null) && (! new_pssg_identifier.equals(current_pssg_identifier))){
                logger.log( Level.WARNING, "Unable to enroll: This gateway has already enrolled with SaaS portal which is identified by , '"+current_pssg_identifier+"'. Enrollment has been aborted.");
                throw new IOException( "This gateway has already enrolled with a different portal." );
            }
            ContentTypeHeader contentTypeHeader = ContentTypeHeader.parseValue(connection.getContentType());
            boolean isBinary = contentTypeHeader.matches(ContentTypeHeader.OCTET_STREAM_DEFAULT);
            if (isBinary) {
                input = new GZIPInputStream(connection.getInputStream());
            } else {
                input = connection.getInputStream();
            }

            // context is created here and passed into installBundle Mockito doesn't support mocking static method.
            context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
            installBundle(input, connection.getHeaderField(SKAR_ID_HEADER_FIELD), contentTypeHeader, user, context);
        } finally {
            if (input != null) {
                input.close();
            }
            if (output != null) {
                output.close();
            }

            ResourceUtils.closeQuietly( context );
        }

        // Bundle installed, post back status
        try {
            connection = (HttpsURLConnection) new URL(enrollmentUrl + "&action=enroll&status=success").openConnection();
            connection.setHostnameVerifier( pinChecker );
            connection.setRequestMethod("POST");
            connection.setSSLSocketFactory(socketFactory);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            output = connection.getOutputStream();
            output.write(postBody);
            final int responseCode = connection.getResponseCode();

            if (responseCode != HttpsURLConnection.HTTP_OK) {
                throw new IOException("Failed to post enrollment status to the portal.");
            }
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    /**
     * Work around Oracle JDK bug JDK-8072464 where outbound server_name indication is not included with ClientHello
     * in JDK 8 if a custom HostnameVerifier is in use.
     *
     * @param connection the HttpsURLConnection to customize
     * @param socketFactory the SSLSocketFactory that is to be used
     * @param url the URL that is to be connected to
     */
    private static void enableSni( @NotNull HttpsURLConnection connection, @NotNull SSLSocketFactory socketFactory, @NotNull final URL url ) {
        connection.setSSLSocketFactory( new SSLSocketFactoryWrapper( socketFactory ) {
            @Override
            protected Socket notifySocket( final Socket socket ) {
                if ( socket instanceof SSLSocket && url.getHost() != null ) {
                    final SSLSocket sslSocket = (SSLSocket) socket;
                    SSLParameters params = sslSocket.getSSLParameters();
                    params.setServerNames( Collections.singletonList( new SNIHostName( url.getHost() ) ) );
                    sslSocket.setSSLParameters( params );
                }

                return socket;
            }
        } );
    }

    public void upgradePortal() throws IOException, FindException {
        final User user = JaasUtils.getCurrentUser();
        if ( null == user )
            throw new IllegalStateException( "No administrative user authenticated" );

        final String pssgHost = clusterPropertyManager.getProperty("portal.config.pssg.host");
        final String tenantId = clusterPropertyManager.getProperty("portal.config.name");
        final String nodeId = clusterPropertyManager.getProperty("portal.config.node.id");
        final String bundleVersion = clusterPropertyManager.getProperty("portal.bundle.version");

        //v2 OSE uses a specified port, ENROLL_PORT for backwards compatibility
        String port = clusterPropertyManager.getProperty("portal.config.pssg.enroll.port");
        final String enrollPort = (StringUtils.isEmpty(port)? "" + ENROLL_PORT : port);

        URL url = new URL("https://" + pssgHost + ":" + enrollPort + "/enroll/" + tenantId + "?action=upgrade&nodeId=" + nodeId + "&bundleVersion=" + bundleVersion);

        byte[] postBody = buildEnrollmentPostBody( user );

        final SsgKeyEntry clientCert = prepareClientCert(user);

        final SSLSocketFactory socketFactory;
        X509KeyManager km;
        PinChecker pinChecker;

        try {

            Collection<TrustedCert> trustedCerts = trustedCertManager.findByName(pssgHost);
            if (trustedCerts != null && trustedCerts.size() > 0) {
                TrustedCert cert = trustedCerts.iterator().next();
                byte[] encodedPublicKey = cert.getCertificate().getPublicKey().getEncoded();
                byte[] keyHash = HexUtils.getSha256Digest( encodedPublicKey );
                pinChecker = new PinChecker(keyHash);
            } else {
                throw new IOException("Certificate not valid");
            }


            km = new SingleCertX509KeyManager( clientCert.getCertificateChain(), clientCert.getPrivateKey(), clientCert.getAlias() );

            SSLContext sslContext = SSLContext.getInstance( "TLS" );
            sslContext.init( new KeyManager[] { km }, new TrustManager[] { pinChecker }, JceProvider.getInstance().getSecureRandom() );
            socketFactory = sslContext.getSocketFactory();
        } catch ( NoSuchAlgorithmException | KeyManagementException | UnrecoverableKeyException e ) {
            throw new IOException( e );
        }

        InputStream input = null;
        OutputStream output = null;
        PolicyEnforcementContext context = null;
        HttpsURLConnection connection;

        try {
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setHostnameVerifier(pinChecker);
            enableSni( connection, socketFactory, url );
            connection.setSSLSocketFactory(socketFactory);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            output = connection.getOutputStream();
            output.write(postBody);
            // If the TSSG has already enrolled with a different portal pssg, it should not be allowed to upgrade
            String new_pssg_identifier = connection.getHeaderField("portal-config-identifier");
            String current_pssg_identifier = clusterPropertyManager.getProperty("portal.config.identifier");
            if ((new_pssg_identifier != null) && (current_pssg_identifier != null) && (!new_pssg_identifier.equals(current_pssg_identifier))) {
                logger.log(Level.WARNING, "Unable to upgrade: This gateway is enrolled with SaaS portal which is identified by , '" + current_pssg_identifier + "'. Upgrade has been aborted.");
                throw new IOException("This gateway is enrolled with a different portal.");
            }

            if (connection.getResponseCode() == 204) {
                throw new IOException("This Gateway already has the latest Portal integration software.");
            }

            ContentTypeHeader contentTypeHeader = ContentTypeHeader.parseValue(connection.getContentType());
            boolean isBinary = contentTypeHeader.matches(ContentTypeHeader.OCTET_STREAM_DEFAULT);
            if (isBinary) {
                input = new GZIPInputStream(connection.getInputStream());
            } else {
                input = connection.getInputStream();
            }

            // context is created here and passed into installBundle Mockito doesn't support mocking static method.
            context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
            installBundle(input, connection.getHeaderField(SKAR_ID_HEADER_FIELD), contentTypeHeader, user, context);
        } finally {
            if (input != null) {
                input.close();
            }
            if (output != null) {
                output.close();
            }

            ResourceUtils.closeQuietly( context );
        }
    }

    public Triple<Policy,EncapsulatedAssertionConfig,JdbcConnection> getOtkEntities() throws IOException {

        Policy otkPolicy;
        EncapsulatedAssertionConfig otkEncass;
        JdbcConnection jdbcConnection;

        //        check for OTK policy to update for portal configuration
        try {
            otkPolicy = policyManager.findByUniqueName(OTK_CLIENT_DB_GET);
            if(otkPolicy == null){
                otkPolicy = policyManager.findByUniqueName(OTK_CLIENT_NOSQL_GET);
                if (otkPolicy == null) {
                    throw new IOException("OTK policy not found: " + OTK_CLIENT_DB_GET + " or " + OTK_CLIENT_NOSQL_GET);
                }
            }
        } catch (FindException e) {
            throw new IOException( "Error finding OTK policy: " + OTK_CLIENT_DB_GET + " or " + OTK_CLIENT_NOSQL_GET, ExceptionUtils.getDebugException(e) );
        }


        // check for OTK encapsulated assertion used by portal entity
        try {
            otkEncass = encapsulatedAssertionConfigManager.findByUniqueName(OTK_REQUIRE_OAUTH_2_0_TOKEN);
            if(otkEncass == null){
                throw new IOException( "OTK encapsulated assertion not found: "+OTK_REQUIRE_OAUTH_2_0_TOKEN+"" );
            }
        } catch (FindException e) {
            throw new IOException( "Error finding OTK encapsulated assertion: "+OTK_REQUIRE_OAUTH_2_0_TOKEN+"", ExceptionUtils.getDebugException(e) );
        }

        // At least 1 connection (Jdbc or Cassandra) should be exits
        try {
            jdbcConnection = jdbcConnectionManager.findByUniqueName(OAUTH);

            CassandraConnectionHolder connectionHolder = cassandraConnectionManager.getConnection(OAUTH_CASSANDRA);

            //Atleast one connection should be exists
            if (jdbcConnection == null && (connectionHolder == null || connectionHolder.getCassandraConnectionEntity() == null)) {
                throw new IOException("Cannot find jdbc or cassandra connection");
            }
        } catch (FindException e) {
            throw new IOException("Error finding OTK db connection", ExceptionUtils.getDebugException(e));
        }

        return Triple.triple(otkPolicy,otkEncass,jdbcConnection);
    }

    void setConfig(final Config config) {
        this.config = config;
    }

    private Document toDocument(final InputStream inputStream) throws IOException {
        try {
            return XmlUtil.parse(inputStream);
        } catch (SAXException e) {
            throw new IOException(e);
        }

    }

    byte[] buildEnrollmentPostBody( User adminUser ) throws IOException {
        String clusterName = config.getProperty( "clusterHost", "" );
        String buildInfo = BuildInfo.getLongBuildString();
        String ssgVersion = BuildInfo.getProductVersion();
        String otkVersion = getOtkVersion(adminUser);

        final Collection<ClusterNodeInfo> infos;
        try {
            infos = clusterInfoManager.retrieveClusterStatus();
        } catch ( FindException e ) {
            throw new IOException( "Unable to load cluster info: " + ExceptionUtils.getMessage( e ), e );
        }

        final Triple<Policy,EncapsulatedAssertionConfig,JdbcConnection> otkEntities = getOtkEntities();

        String nodeCount = String.valueOf( infos.size() );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonFactory jsonFactory = new JsonFactory();
        JsonGenerator gen = jsonFactory.createJsonGenerator( baos );
        gen.writeStartObject();
        gen.writeStringField(CLUSTER_NAME, clusterName );
        gen.writeStringField(VERSION, ssgVersion );
        gen.writeStringField(OTK_VERSION, otkVersion);
        gen.writeStringField(BUILD_INFO, buildInfo );
        gen.writeStringField(ADMINUSER_PROVIDERID, adminUser.getProviderId().toString() );
        gen.writeStringField(ADMINUSER_ID, adminUser.getId() );
        gen.writeStringField(OTK_CLIENT_DB_GET_POLICY, otkEntities.left.getId());
        gen.writeStringField(OTK_REQUIRE_OAUTH_2_TOKEN_ENCASS, otkEntities.middle.getId());
        //In case of cassandra, we don't need JDBC connection so we are pass dummy id so that existing
        //enrolment bundle or old otk can work.
        gen.writeStringField(OTK_JDBC_CONN, (otkEntities.right == null) ? Goid.DEFAULT_GOID.toString(): otkEntities.right.getId());
        gen.writeStringField(NODE_COUNT, nodeCount );
        gen.writeFieldName(NODE_INFO);
        gen.writeStartArray();
        gen.flush();

        for ( ClusterNodeInfo info : infos ) {
            gen.writeStartObject();
            gen.writeStringField(NAME, info.getName() );
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
        char[] unique = new char[20];
        RandomUtil.nextChars( unique, 'a', 'z' );
        String dn = "cn=" + alias + "." + new String( unique );
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

    void installBundle(@NotNull InputStream responseInputStream,
                       @Nullable final String skarId,
                       @NotNull ContentTypeHeader responseContentType,
                       @NotNull User adminUser,
                       @NotNull PolicyEnforcementContext context) throws IOException {
        Assertion assertion = wspReader.parseStrictly( POLICY_XML, WspReader.Visibility.omitDisabled );

        ServerAssertion sph = null;
        try {
            sph = serverPolicyFactory.compilePolicy( assertion, false );
            Message mess = new Message();

            if (responseContentType.isMultipart()) {
                // SKAR bundle
                logger.log( Level.INFO, "SKAR bundle detected");

                String queryParam = "";
                if (skarId != null) {
                    // perform a SKAR upgrade
                    queryParam = "?id=" + skarId;
                }

                mess.initialize( stashManagerFactory.createStashManager(), responseContentType, responseInputStream );
                context.setVariable( REST_GATEWAY_MAN_ACTION, "POST" );
                context.setVariable( REST_GATEWAY_MAN_URI, "1.0/solutionKitManagers" + queryParam );
            } else {
                // restman bundle
                Document bundleDoc;
                bundleDoc = toDocument(responseInputStream);
                mess.initialize(bundleDoc, ContentTypeHeader.XML_DEFAULT);
                context.setVariable( REST_GATEWAY_MAN_ACTION, "PUT");
                context.setVariable( REST_GATEWAY_MAN_URI, "1.0/bundle" );
            }

            context.getResponse().attachHttpResponseKnob( new AbstractHttpResponseKnob() {} );
            context.setVariable( "mess", mess );
            context.getDefaultAuthenticationContext().addAuthenticationResult( new AuthenticationResult( adminUser, new OpaqueSecurityToken() ) );

            AssertionStatus result = sph.checkRequest( context );
            int httpStatus = context.getResponse().getHttpResponseKnob().getStatus();

            String resp = new String( IOUtils.slurpStream( context.getResponse().getMimeKnob().getEntireMessageBodyAsInputStream() ), Charsets.UTF8 );

            logger.log( Level.INFO, "Enrollment response from local RESTMAN bundle install: result: {0}, httpStatus: {1}, body: {2}\n", new Object[]{result, httpStatus, resp});

            if ( !AssertionStatus.NONE.equals( result ) || httpStatus != 200 ) {
                throw new IOException(MessageFormat.format("RESTMAN failed with result: {0}, httpStatus: {1}, body: {2}", result, httpStatus, resp));
            }

        } catch ( ServerPolicyException | LicenseException e ) {
            throw new IOException( "Unable to prepare RESTMAN policy: " + ExceptionUtils.getMessage( e ), e );
        } catch ( PolicyAssertionException|NoSuchPartException e ) {
            throw new IOException( "Unable to invoke RESTMAN policy: " + ExceptionUtils.getMessage( e ), e );
        } finally {
            ResourceUtils.closeQuietly( sph );
        }
    }

    /**
     * An X509TrustManager and HostnameVerifier that passes checks if the peer subject cert matches the
     * specified pin value.
     */
    static class PinChecker implements X509TrustManager, HostnameVerifier {
        private final byte[] pinBytes;

        public PinChecker( byte[] pinBytes ) {
            this.pinBytes = pinBytes;
        }

        @Override
        public void checkClientTrusted( X509Certificate[] chain, String authType ) throws CertificateException {
            throw new CertificateException( "Method not supported" );
        }

        @Override
        public void checkServerTrusted( X509Certificate[] chain, String authType ) throws CertificateException {
            // TODO for now pin required to match server subject cert, rather than some intermediate cert
            ensurePinMatches( chain[0] );
        }

        private void ensurePinMatches( Certificate certificate ) throws CertificateException {
            byte[] encodedPublicKey = certificate.getPublicKey().getEncoded();
            byte[] keyHash = HexUtils.getSha256Digest( encodedPublicKey );
            if ( !Arrays.equals( keyHash, pinBytes ) )
                throw new CertificateException( "Server certificate key hash does not match pin value" );
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

        @Override
        public boolean verify( String s, SSLSession sslSession ) {
            try {
                java.security.cert.Certificate[] serverChain = sslSession.getPeerCertificates();
                if ( serverChain.length < 1 ) {
                    logger.warning( "Server cert chain is empty" );
                    return false;
                }
                ensurePinMatches( serverChain[0] );
                return true;

            } catch ( SSLPeerUnverifiedException e ) {
                logger.log( Level.WARNING, "Unable to check server cert chain: " + ExceptionUtils.getMessage( e ), ExceptionUtils.getDebugException( e ) );
                return false;
            } catch ( CertificateException e ) {
                logger.log( Level.WARNING, "Pin check failed: " + ExceptionUtils.getMessage( e ), ExceptionUtils.getDebugException( e ) );
                return false;
            }
        }
    }

    public boolean isGatewayEnrolled() {
        // Do our best guess to determine if gateway is enrolled
        try {
            ssgKeyStoreManager.lookupKeyByKeyAlias("portalman", PersistentEntity.DEFAULT_GOID);
            String nodeId = clusterPropertyManager.getProperty("portal.config.node.id");
            String pssgHost = clusterPropertyManager.getProperty("portal.config.pssg.host");
            String tenantId = clusterPropertyManager.getProperty("portal.config.name");
            String bundleVersion = clusterPropertyManager.getProperty("portal.bundle.version");
            if (nodeId == null || pssgHost == null || tenantId == null || bundleVersion == null ||
                    nodeId.isEmpty() || pssgHost.isEmpty() || tenantId.isEmpty() || bundleVersion.isEmpty()) {
                return false;
            }
        } catch (FindException | KeyStoreException e) {
            return false;
        }

        return true;
    }

    public String getOtkVersion(@NotNull User adminUser) throws IOException{
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        String version;
        Assertion assertion = wspReader.parseStrictly( POLICY_XML, WspReader.Visibility.omitDisabled );

        ServerAssertion sph;
        try {
            sph = serverPolicyFactory.compilePolicy(assertion, false);
            Message mess = new Message();
            context.setVariable( REST_GATEWAY_MAN_ACTION, "GET" );
            context.setVariable( REST_GATEWAY_MAN_URI, "1.0/solutionKits?guid=" + OTK_GUID );

            context.getResponse().attachHttpResponseKnob( new AbstractHttpResponseKnob() {} );
            context.setVariable( MESS, mess );
            context.getDefaultAuthenticationContext().addAuthenticationResult( new AuthenticationResult( adminUser, new OpaqueSecurityToken() ) );

            AssertionStatus result = sph.checkRequest( context );
            String resp = new String( IOUtils.slurpStream( context.getResponse().getMimeKnob().getEntireMessageBodyAsInputStream()), Charsets.UTF8 );
            int httpStatus = context.getResponse().getHttpResponseKnob().getStatus();
            logger.log( Level.INFO, "Enrollment response from local RESTMAN bundle install: result: {0}, httpStatus: {1}, body: {2}\n", new Object[]{result, httpStatus, resp});

            if ( !AssertionStatus.NONE.equals( result ) || httpStatus != 200 ) {
                throw new IOException(MessageFormat.format("RESTMAN failed with result: {0}, httpStatus: {1}, body: {2}", result, httpStatus, resp));
            }

            Document document = toDocument(context.getResponse().getMimeKnob().getEntireMessageBodyAsInputStream());
            version = document.getElementsByTagName(OTK_VERSION_TAG).item(0).getTextContent();

        } catch ( ServerPolicyException | LicenseException e ) {
            throw new IOException( "Unable to prepare RESTMAN policy: " + ExceptionUtils.getMessage( e ), e );
        } catch ( PolicyAssertionException|NoSuchPartException e ) {
            throw new IOException( "Unable to invoke RESTMAN policy: " + ExceptionUtils.getMessage( e ), e );
        }

        return version;
    }
}

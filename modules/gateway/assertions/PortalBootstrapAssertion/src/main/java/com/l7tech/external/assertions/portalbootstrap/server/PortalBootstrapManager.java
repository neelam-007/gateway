package com.l7tech.external.assertions.portalbootstrap.server;

import com.l7tech.common.io.*;
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
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.token.OpaqueSecurityToken;
import com.l7tech.server.StashManagerFactory;
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
import com.l7tech.util.*;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.net.ssl.*;
import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

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
    private StashManagerFactory stashManagerFactory;

    @Inject
    private ClusterPropertyManager clusterPropertyManager;

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

        InputStream input = null;
        OutputStream output = null;
        PolicyEnforcementContext context = null;
        HttpsURLConnection connection;

        try {
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setSSLSocketFactory(socketFactory);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            output = connection.getOutputStream();
            output.write(postBody);
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
        final X509TrustManager tm = new PermissiveX509TrustManager();
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

        InputStream input = null;
        OutputStream output = null;
        PolicyEnforcementContext context = null;
        HttpsURLConnection connection;

        try {
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
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
                throw new IOException( "OTK policy not found: "+OTK_CLIENT_DB_GET+"" );
            }
        } catch (FindException e) {
            throw new IOException( "Error finding OTK policy: "+OTK_CLIENT_DB_GET+"", ExceptionUtils.getDebugException(e) );
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

        // assume only 1 jdbc connection and its for the OTK db
        try {
            jdbcConnection = jdbcConnectionManager.findByUniqueName(OAUTH);
            if (jdbcConnection == null) {
                throw new IOException("Cannot find jdbc connection: " + OAUTH);
            }
        } catch (FindException e) {
            throw new IOException("Error finding OTK jdbc connection", ExceptionUtils.getDebugException(e));
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
        gen.writeStringField(BUILD_INFO, buildInfo );
        gen.writeStringField(ADMINUSER_PROVIDERID, adminUser.getProviderId().toString() );
        gen.writeStringField(ADMINUSER_ID, adminUser.getId() );
        gen.writeStringField(OTK_CLIENT_DB_GET_POLICY, otkEntities.left.getId());
        gen.writeStringField(OTK_REQUIRE_OAUTH_2_TOKEN_ENCASS, otkEntities.middle.getId());
        gen.writeStringField(OTK_JDBC_CONN, otkEntities.right.getId());
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
                context.setVariable( "restGatewayMan.action", "POST" );
                context.setVariable( "restGatewayMan.uri", "1.0/solutionKitManagers" + queryParam );
            } else {
                // restman bundle
                Document bundleDoc;
                bundleDoc = toDocument(responseInputStream);
                mess.initialize(bundleDoc, ContentTypeHeader.XML_DEFAULT);
                context.setVariable("restGatewayMan.action", "PUT");
                context.setVariable( "restGatewayMan.uri", "1.0/bundle" );
            }

            context.getResponse().attachHttpResponseKnob( new AbstractHttpResponseKnob() {} );
            context.setVariable( "mess", mess );
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
}

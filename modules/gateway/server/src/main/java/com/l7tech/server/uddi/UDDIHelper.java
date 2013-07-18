package com.l7tech.server.uddi;

import com.l7tech.common.io.SingleCertX509KeyManager;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.gateway.common.admin.UDDIRegistryAdmin;
import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.uddi.UDDIRegistry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.policy.variable.ServerVariables;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.transport.SsgConnectorActivationListener;
import com.l7tech.uddi.*;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.InetAddressUtil;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.jaxws.JaxWsClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStoreException;
import java.security.UnrecoverableKeyException;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * UDDI utility methods.
 */
public class UDDIHelper implements SsgConnectorActivationListener {

    //- PUBLIC

    /**
     * Property key for UDDI Urls (use .1 .2 suffix)
     */
    public static final String PROP_INQUIRY_URLS = "uddi.url.inquiry";

    /**
     * Property key for the maximum rows to retrieve from the registry.
     */
    public static final String PROP_RESULT_ROWS_MAX = "uddi.result.max_rows";

    /**
     * Property key for the number of rows to retrieve from the registry with each call.
     */
    public static final String PROP_RESULT_BATCH_SIZE = "uddi.result.batch_size";

    public UDDIHelper( final ServerConfig serverConfig,
                       final SsgKeyStoreManager ssgKeyStoreManager,
                       final TrustManager trustManager,
                       final HostnameVerifier hostnameVerifier,
                       final UDDITemplateManager uddiTemplateManager,
                       final Properties properties ) {
        this.serverConfig = serverConfig;
        this.ssgKeyStoreManager = ssgKeyStoreManager;
        this.trustManager = trustManager;
        this.hostnameVerifier = hostnameVerifier;
        this.uddiTemplateManager = uddiTemplateManager;

        String rowsMax = properties.getProperty(PROP_RESULT_ROWS_MAX, "100");     // default 100 rows max
        int resultRowsMax = Integer.parseInt(rowsMax);
        String batchSize = properties.getProperty(PROP_RESULT_BATCH_SIZE, "100");
        int resultBatchSize = Integer.parseInt(batchSize);

        if(resultBatchSize > resultRowsMax){
            resultBatchSize = resultRowsMax;
        }

        this.defaultResultBatchSize = resultBatchSize;
        this.defaultResultRowsMax = resultRowsMax;
    }

    /**
     * Get a single cluster endpoint pair.
     * @param scheme HTTP or HTTPS
     * @param serviceOid oid of the Published Service
     * @return EndpointPair containing the service consumption URL and WSDL URL
     * @throws com.l7tech.server.uddi.UDDIHelper.EndpointNotDefinedException if no endpoint for the request scheme exists.
     */
    public EndpointPair getEndpointForScheme(final UDDIRegistryAdmin.EndpointScheme scheme,
                                             final long serviceOid) throws EndpointNotDefinedException{
        final Map<Goid,String> connectorProtocols;
        synchronized (activeConnectorProtocols) {
            connectorProtocols = new HashMap<Goid,String>( activeConnectorProtocols );
        }

        switch (scheme){
            case HTTP:
                if(!connectorProtocols.values().contains("HTTP"))
                    throw new EndpointNotDefinedException("Cannot get endpoint for HTTP as no HTTP listener is defined.");
                return new EndpointPair(
                        doGetExternalUrlForService(serviceOid, false),
                        doGetExternalWsdlUrlForService(serviceOid, false));
            case HTTPS:
                if(!connectorProtocols.values().contains("HTTPS"))
                    throw new EndpointNotDefinedException("Cannot get endpoint for HTTPS as no HTTPS listener is defined.");
                return new EndpointPair(
                        doGetExternalUrlForService(serviceOid, true),
                        doGetExternalWsdlUrlForService(serviceOid, false));
            default:
                throw new EndpointNotDefinedException("Unknown scheme: " + scheme.toString());

        }
    }

    public static class EndpointNotDefinedException extends Exception{
        public EndpointNotDefinedException(String message) {
            super(message);
        }
    }

    /**
     * For a cluster, this should return a pair for each configured http(s) listener configured on the cluster.
     *
     * The left side is the external URL which can be used to consume a service. The right side is the WSDL URL from
     * which the WSDL for the published service can be downloaded
     *
     * All URL's returned are specific to the published service with the serviceOid supplied.
     *
     * @param serviceOid long oid of the published service to generated external URLs for
     * @return Collection&lt;EndpointPair&gt; Collection of Pairs where each pair is a distinct end point URL for the
     * service. There should only ever be one http pair and one https pair
     */
    public Set<EndpointPair> getAllExternalEndpointAndWsdlUrls( long serviceOid ){
        final Set<EndpointPair> endpointsAndWsdlUrls = new HashSet<EndpointPair>();

        final Map<Goid,String> connectorProtocols;
        synchronized (activeConnectorProtocols) {
            connectorProtocols = new HashMap<Goid,String>( activeConnectorProtocols );
        }

        final EndpointPair httpPair = new EndpointPair(
                doGetExternalUrlForService( serviceOid, false ),
                doGetExternalWsdlUrlForService( serviceOid, false ) );

        if ( connectorProtocols.values().contains("HTTP") ) {
            endpointsAndWsdlUrls.add( httpPair );
        }

        if ( connectorProtocols.values().contains("HTTPS") ) {
            endpointsAndWsdlUrls.add( new EndpointPair(
                doGetExternalUrlForService( serviceOid, true),
                doGetExternalWsdlUrlForService( serviceOid, false )
            ) );
        }

        if ( endpointsAndWsdlUrls.isEmpty() ) {
            // Configure HTTP if nothing is available
            endpointsAndWsdlUrls.add( httpPair );
        }

        return endpointsAndWsdlUrls;
    }

    /**
     * Get the WSDL download URL for the given service identifier (OID)
     *
     * @param serviceOid The oid for the service
     * @return The URL
     */
    public String getExternalWsdlUrlForService( final long serviceOid )  {
        return doGetExternalWsdlUrlForService( serviceOid, false );
    }

    /**
     * Get the base WSDL URL
     *
     * @return String base WSDL URL
     */
    public String getBaseWsdlUrl(){
        return buildCompleteGatewayUrl(SecureSpanConstants.WSDL_PROXY_FILE, false);
    }

    /**
     * Get the query string portion of the WSDL URL
     *
     * @return String query portion of WSDL URL
     */
    public String getQueryWsdlString(){
        return SecureSpanConstants.WSDL_PROXY_FILE + "?" + SecureSpanConstants.HttpQueryParameters.PARAM_SERVICEOID;
    }

    public String getExternalPolicyUrlForService( final long serviceOid,
                                                  final boolean fullPolicy,
                                                  final boolean inlinePolicy ){
        final String policyUrlTemplate = serverConfig.getProperty(
                "uddi.policyUrlTemplate",
                "http://{0}:{1}/ssg/policy/disco?serviceoid={3}&fulldoc={4}&inline={5}" );

        return MessageFormat.format(policyUrlTemplate,
                getExternalHostName(),
                getExternalPort(),
                getExternalHttpsPort(),
                Long.toString(serviceOid),
                fullPolicy ? "yes" : "no",
                inlinePolicy ? "yes" : "no");
    }

    public String getExternalUrlForService( final long serviceOid ) {
        return doGetExternalUrlForService( serviceOid, false );
    }

    /**
     * Determine if the hostname part of endPoint points to this Gateway
     * @param endPoint String URL, can contain more than just the hostname
     * @return true if the hostname points to the this Gateway
     */
    public boolean isGatewayUrl( final String endPoint ){ 
        boolean gateway = false;

        try {
            URL url = new URL(endPoint);
            if ( url.getHost() != null ) {
                final String urlHost = url.getHost();
                gateway = InetAddressUtil.isLocalSystemAddress( urlHost );

                if ( !gateway ) {
                    for ( String host : getClusterNodeHostnames() ) {
                        if ( urlHost.equalsIgnoreCase( host )) {
                            gateway = true;
                            break;
                        }
                    }
                }
            }
        } catch (MalformedURLException e) {
           // not a gateway url
        }

        return gateway;
    }

    /**
     * If the search was interuppted the returned array may be empty, never null
     */
    public UDDINamedEntity[] getMatchingBusinesses( final UDDIClient uddiClient,
                                                    final String namePattern,
                                                    final boolean caseSensitive ) throws UDDIException {
        final List<UDDINamedEntity> uddiNamedEntities = new ArrayList<UDDINamedEntity>();
        final int resultRowsMax = getResultRowsMax();
        final int resultBatchSize = getResultBatchSize();

        for (int i=0; uddiNamedEntities.size() < resultRowsMax; i++) {
            int head = i*resultBatchSize;
            if (head > 0) head++; // one based

            Collection<UDDINamedEntity> businesses =
                    uddiClient.listBusinessEntities(namePattern, caseSensitive, head, resultBatchSize);

            //this will happen if search was interrupted
            if(businesses == null) return uddiNamedEntities.toArray(new UDDINamedEntity[uddiNamedEntities.size()]);

            uddiNamedEntities.addAll(businesses);

            if (!uddiClient.listMoreAvailable())
                break;
        }

        boolean maxedOutSearch = uddiNamedEntities.size()>=resultRowsMax || uddiClient.listMoreAvailable();

        if (maxedOutSearch) {
            uddiNamedEntities.subList(resultRowsMax, uddiNamedEntities.size()).clear();
            uddiNamedEntities.add( WsdlPortInfoImpl.MAXED_OUT_SEARCH_RESULT_ENTITY );
        }

        return uddiNamedEntities.toArray(new UDDINamedEntity[uddiNamedEntities.size()]);
    }

    /**
     * If the search was interuppted the returned array will empty, never null
     */
    public WsdlPortInfo[] getWsdlInfoForServiceKey(final UDDIClient uddiClient, final String serviceKey, final boolean getFirstOnly) throws UDDIException {
        final Collection<WsdlPortInfo> infoCollection = uddiClient.listWsdlPortsForService(serviceKey, getFirstOnly);
        //this will happen if search was interrupted
        if(infoCollection == null) return new WsdlPortInfo[]{};

        return infoCollection.toArray(new WsdlPortInfo[infoCollection.size()]);
    }

    /**
     * If the search was interuppted the returned array may be empty, never null
     */
    public WsdlPortInfo[] getWsdlByServiceName(final UDDIClient uddiClient,
                                               final String namePattern,
                                               final boolean caseSensitive,
                                               final boolean getWsdlURL) throws UDDIException {
        // % denotes wildcard of string (any number of characters), underscore denotes wildcard of a single character

        final List<WsdlPortInfo> wsdlPortInfos = new ArrayList<WsdlPortInfo>();
        final int resultRowsMax = getResultRowsMax();
        final int resultBatchSize = getResultBatchSize();

        final String queryUrl = getQueryWsdlString();

        for (int i = 0; wsdlPortInfos.size() < resultRowsMax; i++) {
            int head = i * resultBatchSize;
            if (head > 0) head++; // one based

            Collection<WsdlPortInfo> foundWsdlPortInfos =
                    uddiClient.listServiceWsdls(namePattern, caseSensitive, head, resultBatchSize, getWsdlURL);
            //this will happen if search was interrupted
            if(foundWsdlPortInfos == null) return wsdlPortInfos.toArray(new WsdlPortInfo[wsdlPortInfos.size()]);

            for(WsdlPortInfo wsdlPortInfo: foundWsdlPortInfos){
                final String wsdl = wsdlPortInfo.getWsdlUrl();
                if(wsdl == null) continue;
                if(isGatewayUrl(wsdl)){
                    if(wsdlPortInfo instanceof WsdlPortInfoImpl){
                        WsdlPortInfoImpl impl = (WsdlPortInfoImpl) wsdlPortInfo;
                        impl.setGatewayWsdl(true);//ssm will not allow this wsdl to be used
                    }
                } else if(wsdl.indexOf(queryUrl) != -1){
                    if(wsdlPortInfo instanceof WsdlPortInfoImpl){
                        WsdlPortInfoImpl impl = (WsdlPortInfoImpl) wsdlPortInfo;
                        impl.setLikelyGatewayWsdl(true);//ssm will warn the user
                    }
                }
            }

            wsdlPortInfos.addAll(foundWsdlPortInfos);

            if (!uddiClient.listMoreAvailable())
                break;
        }


        boolean maxedOutSearch = wsdlPortInfos.size() >= resultRowsMax || uddiClient.listMoreAvailable();

        if (maxedOutSearch) {
            wsdlPortInfos.subList(resultRowsMax, wsdlPortInfos.size()).clear();
            wsdlPortInfos.add(WsdlPortInfoImpl.MAXED_OUT_SEARCH_RESULT);
        }

        return wsdlPortInfos.toArray(new WsdlPortInfo[wsdlPortInfos.size()]);
    }

    /**
     * Create a UDDIClientConfig for accessing the given registry.
     *
     * @param uddiRegistry The registry to access.
     * @return The UDDIClientConfig
     * @throws com.l7tech.objectmodel.FindException if a password reference cannot be expanded
     */
    public UDDIClientConfig newUDDIClientConfig( final UDDIRegistry uddiRegistry ) throws FindException {
        boolean closeSession = true;

        UDDITemplate template = uddiTemplateManager.getUDDITemplate( uddiRegistry.getUddiRegistryType() );
        if ( template != null ) {
            closeSession = template.isCloseSession();
        }

        return new UDDIClientConfig(
                uddiRegistry.getInquiryUrl(),
                uddiRegistry.getPublishUrl(),
                uddiRegistry.getSubscriptionUrl(),
                uddiRegistry.getSecurityUrl(),
                uddiRegistry.getRegistryAccountUserName(),
                ServerVariables.expandSinglePasswordOnlyVariable(new LoggingAudit(logger), uddiRegistry.getRegistryAccountPassword()),
                closeSession,
                buildTLSConfig(uddiRegistry) );
    }

    /**
     * Create a UDDIClient for accessing the given registry.
     *
     * @param uddiRegistry The registry to access.
     * @return The UDDIClient
     * @throws com.l7tech.objectmodel.FindException if a password reference cannot be expanded
     */
    public UDDIClient newUDDIClient( final UDDIRegistry uddiRegistry ) throws FindException {
        UDDIClientFactory factory = UDDIClientFactory.getInstance();
        return factory.newUDDIClient( newUDDIClientConfig(uddiRegistry) );
    }

    @Override
    public void notifyActivated( final SsgConnector connector ) {
        if ( connector.offersEndpoint( SsgConnector.Endpoint.MESSAGE_INPUT ) ) {
            synchronized( activeConnectorProtocols ) {
                activeConnectorProtocols.put( connector.getGoid(), connector.getScheme() );
            }
        }
    }

    @Override
    public void notifyDeactivated( final SsgConnector connector ) {
        synchronized( activeConnectorProtocols ) {
            activeConnectorProtocols.remove( connector.getGoid() );
        }
    }

    //- PROTECTED

    /**
     * Get the hostname from the clusterHost cluster property.
     *
     * <p>If it's not set, then the SSG's host name will be returned</p>
     *
     * @return String cluster hostname or hostname if the cluster property "clusterHost" is not set
     * @throws com.l7tech.objectmodel.FindException if the hostname cannot be found
     */
    String getExternalHostName() {
        final String hostName;
        final String clusterHost = serverConfig.getProperty("clusterHost");
        if(clusterHost == null || clusterHost.trim().isEmpty()){
            hostName = serverConfig.getHostname();
        }else{
            hostName = clusterHost;
        }
        return hostName;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( UDDIHelper.class.getName() );

    private static final String PROP_UDDI_CONNECT_TIMEOUT = "uddi.connectTimeout";
    private static final String PROP_UDDI_READ_TIMEOUT = "uddi.timeout";

    static {
        UDDIClientTLSConfig.addDefaultAdapter( new CxfTLSConfigAdapter() );
    }

    private final ServerConfig serverConfig;
    private final SsgKeyStoreManager ssgKeyStoreManager;
    private final TrustManager trustManager;
    private final HostnameVerifier hostnameVerifier;
    private final UDDITemplateManager uddiTemplateManager;
    private final int defaultResultRowsMax;
    private final int defaultResultBatchSize;
    private final Map<Goid,String> activeConnectorProtocols = new HashMap<Goid,String>();

    private int getResultRowsMax() {
        return serverConfig.getIntProperty( PROP_RESULT_ROWS_MAX, defaultResultRowsMax );
    }

    private int getResultBatchSize() {
        return serverConfig.getIntProperty( PROP_RESULT_BATCH_SIZE, defaultResultBatchSize );
    }

    private String doGetExternalUrlForService( final long serviceOid, final boolean secure ) {
        return buildCompleteGatewayUrl(SecureSpanConstants.SERVICE_FILE, secure) + serviceOid;
    }

    private String doGetExternalWsdlUrlForService( final long serviceOid, final boolean secure )  {
        String query = SecureSpanConstants.HttpQueryParameters.PARAM_SERVICEOID + "=" + serviceOid;
        return buildCompleteGatewayUrl(SecureSpanConstants.WSDL_PROXY_FILE, secure) + "?" + query;
    }

    private String buildCompleteGatewayUrl( final String relativeUri, final boolean secure ) {
        if ( secure ) {
            return "https://" + InetAddressUtil.getHostForUrl(getExternalHostName()) + getPrefixedExternalHttpsPort() + relativeUri;
        } else {
            return "http://" + InetAddressUtil.getHostForUrl(getExternalHostName()) + getPrefixedExternalPort() + relativeUri;
        }
    }

    private String getExternalPort() {
        return serverConfig.getProperty("clusterhttpport", "8080");
    }

    private String getExternalHttpsPort() {
        return serverConfig.getProperty("clusterhttpsport", "8443");
    }

    private String getPrefixedExternalPort() {
        String port = serverConfig.getProperty("clusterhttpport", "");
        if ( port.length() > 0 ) {
            port = ":" + port;
        }
        return port;
    }

    private String getPrefixedExternalHttpsPort() {
        String port = serverConfig.getProperty("clusterhttpsport", "");
        if ( port.length() > 0 ) {
            port = ":" + port;
        }
        return port;
    }

    private Collection<String> getClusterNodeHostnames(){
        Collection<String> hostnames = new ArrayList<String>();
        hostnames.add( getExternalHostName() );
        hostnames.add( serverConfig.getHostname() );
        return hostnames;
    }

    private UDDIClientTLSConfig buildTLSConfig( final UDDIRegistry uddiRegistry ) {
        KeyManager keyManager = null;

        if ( uddiRegistry.isClientAuth() ) {
            final long keystoreOid = uddiRegistry.getKeystoreOid();
            final String alias = uddiRegistry.getKeyAlias();
            if ( keystoreOid>=-1 && alias != null ) {
                try {
                    SsgKeyEntry entry = ssgKeyStoreManager.lookupKeyByKeyAlias(alias, keystoreOid);
                    keyManager = new SingleCertX509KeyManager(entry.getCertificateChain(), entry.getPrivateKey());
                } catch (FindException e) {
                    logger.log( Level.WARNING, "Error configuring UDDI X.509 key manager.", e );
                } catch (KeyStoreException e) {
                    logger.log( Level.WARNING, "Error configuring UDDI X.509 key manager.", e );
                } catch (UnrecoverableKeyException e) {
                    logger.log( Level.WARNING, "Error configuring UDDI X.509 key manager.", e );
                }
            }
        }

        final int defaultConnectTimeout = 30000;
        long connectTimeout = serverConfig.getLongProperty( PROP_UDDI_CONNECT_TIMEOUT, defaultConnectTimeout);
        if(connectTimeout == 0) {
            connectTimeout = defaultConnectTimeout;
            logger.log(Level.WARNING, "A value of 0 is not allowed for cluster property: '" + PROP_UDDI_CONNECT_TIMEOUT+"'. Using default of " + defaultConnectTimeout);
        }else if(connectTimeout < 0){
            connectTimeout = defaultConnectTimeout;
            logger.log(Level.WARNING, "Illegal negative value supplied for cluster property: '" + PROP_UDDI_CONNECT_TIMEOUT+"'. Using default of " + defaultConnectTimeout);
        }

        final int defaultReadTimeout = 60000;
        long readTimeout = serverConfig.getLongProperty( PROP_UDDI_READ_TIMEOUT, defaultReadTimeout);
        if(readTimeout == 0) {
            readTimeout = defaultReadTimeout;
            logger.log(Level.WARNING, "A value of 0 is not allowed for cluster property: '" + PROP_UDDI_READ_TIMEOUT+"'. Using default of " + defaultReadTimeout);
        }else if(readTimeout < 0){
            readTimeout = defaultReadTimeout;
            logger.log(Level.WARNING, "Illegal negative value supplied for cluster property: '" + PROP_UDDI_READ_TIMEOUT+"'. Using default of " + defaultReadTimeout);
        }

        return new UDDIClientTLSConfig( keyManager, trustManager, hostnameVerifier, connectTimeout, readTimeout );
    }

    private static final class CxfTLSConfigAdapter implements UDDIClientTLSConfig.TLSConfigAdapter {
        private static final boolean VERIFY_HOSTNAME = ConfigFactory.getBooleanProperty( "com.l7tech.server.uddi.verifyHostname", true );

        @Override
        public boolean configure( final Object target,
                                  final UDDIClientTLSConfig config,
                                  final boolean configureTLS ) {
            boolean processed = false;

            if ( Proxy.isProxyClass( target.getClass() ) ) {
                Object proxyHander = Proxy.getInvocationHandler( target );
                if ( proxyHander instanceof JaxWsClientProxy ) {
                    processed = true;

                    final JaxWsClientProxy jaxWsClientProxy = (JaxWsClientProxy) proxyHander;
                    final HTTPConduit httpConduit = (HTTPConduit)jaxWsClientProxy.getClient().getConduit();
                    if ( configureTLS ) {
                        httpConduit.setTlsClientParameters( new TLSClientParameters(){
                            //TODO When CXF is upgraded change this to override getSSLSocketFactory() and return a hostname verifying socket factory

                            @Override
                            public boolean isDisableCNCheck() {
                                return !VERIFY_HOSTNAME;
                            }

                            @Override
                            public KeyManager[] getKeyManagers() {
                                return config.getKeyManagers();
                            }

                            @Override
                            public TrustManager[] getTrustManagers() {
                                return config.getTrustManagers();
                            }
                        } );
                    }
                    if ( httpConduit.getClient() != null ) {
                        httpConduit.getClient().setConnectionTimeout( config.getConnectionTimeout() );
                        httpConduit.getClient().setReceiveTimeout( config.getReadTimeout() );
                    }
                }
            }

            return processed;
        }
    }

}

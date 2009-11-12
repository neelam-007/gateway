package com.l7tech.server.uddi;

import com.l7tech.uddi.WsdlPortInfoImpl;
import com.l7tech.uddi.*;
import com.l7tech.gateway.common.uddi.UDDIRegistry;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.io.SingleCertX509KeyManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.SyspropUtil;

import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.reflect.Proxy;
import java.security.KeyStoreException;
import java.security.UnrecoverableKeyException;
import java.text.MessageFormat;

import org.apache.cxf.jaxws.JaxWsClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.configuration.jsse.TLSClientParameters;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import javax.net.ssl.HostnameVerifier;

/**
 * UDDI utility methods.
 */
public class UDDIHelper {

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
                       final Properties properties ) {
        this.serverConfig = serverConfig;
        this.ssgKeyStoreManager = ssgKeyStoreManager;
        this.trustManager = trustManager;
        this.hostnameVerifier = hostnameVerifier;

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
     * Get the WSDL download URL for the given service identifier (OID)
     *
     * @param serviceOid The oid for the service
     * @return The URL
     */
    public String getExternalWsdlUrlForService( final long serviceOid )  {
        String query = SecureSpanConstants.HttpQueryParameters.PARAM_SERVICEOID + "=" + serviceOid;
        return buildCompleteGatewayUrl(SecureSpanConstants.WSDL_PROXY_FILE) + "?" + query;
    }

    /**
     * Get the base WSDL URL
     *
     * @return String base WSDL URL
     */
    public String getBaseWsdlUrl(){
        return buildCompleteGatewayUrl(SecureSpanConstants.WSDL_PROXY_FILE);
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
        return buildCompleteGatewayUrl(SecureSpanConstants.SERVICE_FILE) + serviceOid;
    }

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

    public WsdlPortInfo[] getWsdlByServiceName(final UDDIClient uddiClient,
                                               final String namePattern,
                                               final boolean caseSensitive) throws UDDIException {
        // % denotes wildcard of string (any number of characters), underscore denotes wildcard of a single character

        final List<WsdlPortInfo> wsdlPortInfos = new ArrayList<WsdlPortInfo>();
        final int resultRowsMax = getResultRowsMax();
        final int resultBatchSize = getResultBatchSize();

        final String wsdlUrl = getBaseWsdlUrl();
        final String queryUrl = getQueryWsdlString();
        final String hostName = ServerConfig.getInstance().getHostname();
        final String hName;
        if(hostName.indexOf("[") != -1){
            hName = hostName.substring(0, hostName.indexOf("["));
        }else if(hostName.indexOf(".") != -1){
            hName = hostName.substring(0, hostName.indexOf("."));
        }else{
            hName = hostName;
        }

        for (int i = 0; wsdlPortInfos.size() < resultRowsMax; i++) {
            int head = i * resultBatchSize;
            if (head > 0) head++; // one based

            Collection<WsdlPortInfo> foundWsdlPortInfos =
                    uddiClient.listServiceWsdls(namePattern, caseSensitive, head, resultBatchSize);
            for(WsdlPortInfo wsdlPortInfo: foundWsdlPortInfos){
                final String wsdl = wsdlPortInfo.getWsdlUrl();
                if(wsdl.indexOf(wsdlUrl) != -1){
                    if(wsdlPortInfo instanceof WsdlPortInfoImpl){
                        WsdlPortInfoImpl impl = (WsdlPortInfoImpl) wsdlPortInfo;
                        impl.setGatewayWsdl(true);//ssm will not allow this wsdl to be used
                    }
                } else if(wsdl.indexOf(queryUrl) != -1){
                    if(wsdlPortInfo instanceof WsdlPortInfoImpl){
                        WsdlPortInfoImpl impl = (WsdlPortInfoImpl) wsdlPortInfo;
                        impl.setLikelyGatewayWsdl(true);//ssm will warn the user
                    }
                }else if(wsdl.indexOf(hName) != -1){
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
     */
    public UDDIClientConfig newUDDIClientConfig( final UDDIRegistry uddiRegistry ) {
        return new UDDIClientConfig(
                uddiRegistry.getInquiryUrl(),
                uddiRegistry.getPublishUrl(),
                uddiRegistry.getSubscriptionUrl(),
                uddiRegistry.getSecurityUrl(),
                uddiRegistry.getRegistryAccountUserName(),
                uddiRegistry.getRegistryAccountPassword(),
                buildTLSConfig(uddiRegistry) );
    }

    /**
     * Create a UDDIClient for accessing the given registry.
     *
     * @param uddiRegistry The registry to access.
     * @return The UDDIClient
     */
    public UDDIClient newUDDIClient( final UDDIRegistry uddiRegistry ) {
        UDDIClientFactory factory = UDDIClientFactory.getInstance();
        return factory.newUDDIClient( newUDDIClientConfig(uddiRegistry) );
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
    private final int defaultResultRowsMax;
    private final int defaultResultBatchSize;

    private int getResultRowsMax() {
        return serverConfig.getIntProperty( PROP_RESULT_ROWS_MAX, defaultResultRowsMax );
    }

    private int getResultBatchSize() {
        return serverConfig.getIntProperty( PROP_RESULT_BATCH_SIZE, defaultResultBatchSize );
    }

    private String buildCompleteGatewayUrl( final String relativeUri ) {
        return "http://" + getExternalHostName() + getPrefixedExternalPort() + relativeUri;
    }

    /**
     * Get the hostname from the clusterHost cluster property.
     *
     * <p>If it's not set, then the SSG's host name will be returned</p>
     *
     * @return String cluster hostname or hostname if the cluster property "clusterHost" is not set
     * @throws com.l7tech.objectmodel.FindException if the hostname cannot be found
     */
    private String getExternalHostName() {
        final String hostName;
        final String clusterHost = serverConfig.getProperty("clusterHost");
        if(clusterHost == null || clusterHost.trim().isEmpty()){
            hostName = serverConfig.getHostname();
        }else{
            hostName = clusterHost;
        }
        return hostName;
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

        long connectTimeout = serverConfig.getLongProperty( PROP_UDDI_CONNECT_TIMEOUT, 30000);
        long readTimeout = serverConfig.getLongProperty( PROP_UDDI_READ_TIMEOUT, 60000);

        return new UDDIClientTLSConfig( keyManager, trustManager, hostnameVerifier, connectTimeout, readTimeout );
    }

    private static final class CxfTLSConfigAdapter implements UDDIClientTLSConfig.TLSConfigAdapter {
        private static final boolean VERIFY_HOSTNAME = SyspropUtil.getBoolean( "com.l7tech.server.uddi.verifyHostname", true );

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

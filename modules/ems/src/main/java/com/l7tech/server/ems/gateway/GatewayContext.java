package com.l7tech.server.ems.gateway;

import com.l7tech.server.management.api.node.GatewayApi;
import com.l7tech.server.DefaultKey;
import com.l7tech.util.SyspropUtil;

import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.text.MessageFormat;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.KeyManager;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.jaxws.JaxWsClientFactoryBean;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.interceptor.LoggingInInterceptor;

/**
 * GatewayContext provides access to a Gateway from the ESM.
 *
 * @author steve
 */
public class GatewayContext {

    //- PUBLIC

    /**
     * Create a GatewayContext that uses the given host/port for services.
     *
     * @param host The gateway host
     * @param port The gateway port
     * @param esmId The ID for the EM
     * @param userId The ID for the EM user (null for none)
     */
    public GatewayContext( final DefaultKey defaultKey, final String host, final int port, final String esmId, final String userId ) {
        if ( host == null ) throw new IllegalArgumentException("host is required");
        if ( esmId == null ) throw new IllegalArgumentException("esmId is required"); 
        this.cookie = buildCookie( esmId, userId );
        this.api = initApi( GatewayApi.class, defaultKey, MessageFormat.format(GATEWAY_URL, host, Integer.toString(port)));
    }

    public GatewayApi getApi() {
        return api;
    }

    //- PRIVATE

    private static final String PROP_GATEWAYAPI_URL = "com.l7tech.esm.gatewayUrl";
    private static final String GATEWAY_URL = SyspropUtil.getString(PROP_GATEWAYAPI_URL, "https://{0}:{1}/ssg/services/gatewayApi");    

    private final String cookie;
    private final GatewayApi api;

    private String buildCookie( final String esmId, final String userId ) {
        String cookie =  "EM-UUID=" + esmId;
        if ( userId != null ) {
            //TODO [steve] fix separator (should be cookie style, not query string)
            cookie += "&EM-USER-UUID=" + userId;
        }
        return cookie;
    }

    @SuppressWarnings({"unchecked"})
    private <T> T initApi( final Class<T> apiClass, final DefaultKey defaultKey, final String url ) {
        JaxWsClientFactoryBean cfb = new JaxWsClientFactoryBean();
        cfb.setServiceClass( apiClass );
        cfb.setAddress( url );

        Client c = cfb.create();

        c.getInInterceptors().add( new LoggingInInterceptor() );

        HTTPConduit hc = (HTTPConduit) c.getConduit();
        HTTPClientPolicy policy = hc.getClient();
        policy.setCookie( cookie );
        hc.setTlsClientParameters(new TLSClientParameters() {
            @Override
            public TrustManager[] getTrustManagers() {
                return new TrustManager[] { new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {}
                    @Override
                    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {}
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {return new X509Certificate[0];}
                }};
            }

            @Override
            public KeyManager[] getKeyManagers() {
                return defaultKey.getSslKeyManagers();
            }

            @Override
            public boolean isDisableCNCheck() {
                return true;
            }
        });

        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean(cfb);
        return (T) factory.create();
    }
}

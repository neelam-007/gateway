package com.l7tech.server.ems.gateway;

import com.l7tech.server.management.api.node.GatewayApi;
import com.l7tech.server.management.api.node.NodeManagementApi;
import com.l7tech.server.management.api.node.ReportApi;
import com.l7tech.server.management.api.node.MigrationApi;
import com.l7tech.server.DefaultKey;
import com.l7tech.util.SyspropUtil;
import com.l7tech.util.ExceptionUtils;

import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.text.MessageFormat;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.KeyManager;
import javax.xml.ws.soap.SOAPFaultException;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.jaxws.JaxWsClientFactoryBean;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
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
        this.reportApi = initApi( ReportApi.class, defaultKey, MessageFormat.format(REPORT_URL, host, Integer.toString(port)));
        this.managementApi = initApi( NodeManagementApi.class, defaultKey, MessageFormat.format(CONTROLLER_URL, host, "8765")); //TODO this should be passed in
        this.migrationApi = initApi(MigrationApi.class, defaultKey, MessageFormat.format(MIGRATION_URL, host, Integer.toString(port)));
    }

    public GatewayApi getApi() {
        return api;
    }

    public ReportApi getReportApi() {
        return reportApi;
    }

    public NodeManagementApi getManagementApi() {
        return managementApi;
    }

    public MigrationApi getMigrationApi() {
        return migrationApi;
    }

    public static boolean isNetworkException( final SOAPFaultException sfe ) {
        return ExceptionUtils.causedBy( sfe, ConnectException.class ) ||
               ExceptionUtils.causedBy( sfe, NoRouteToHostException.class ) ||
               ExceptionUtils.causedBy( sfe, UnknownHostException.class );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(GatewayContext.class.getName());

    private static final String PROP_GATEWAY_URL = "com.l7tech.esm.gatewayUrl";
    private static final String PROP_REPORT_URL = "com.l7tech.esm.reportUrl";
    private static final String PROP_CONTROLLER_URL = "com.l7tech.esm.controllerUrl";
    private static final String PROP_MIGRATION_URL = "com.l7tech.esm.migrationUrl";
    private static final String GATEWAY_URL = SyspropUtil.getString(PROP_GATEWAY_URL, "https://{0}:{1}/ssg/services/gatewayApi");
    private static final String REPORT_URL = SyspropUtil.getString(PROP_REPORT_URL, "https://{0}:{1}/ssg/services/reportApi");
    private static final String CONTROLLER_URL = SyspropUtil.getString(PROP_CONTROLLER_URL, "https://{0}:{1}/services/nodeManagementApi");
    private static final String MIGRATION_URL = SyspropUtil.getString(PROP_MIGRATION_URL, "https://{0}:{1}/ssg/services/migrationApi");

    private final String cookie;
    private final GatewayApi api;
    private final ReportApi reportApi;
    private final NodeManagementApi managementApi;
    private final MigrationApi migrationApi;

    private String buildCookie( final String esmId, final String userId ) {
        String cookie =  "EM-UUID=" + esmId;
        if ( userId != null ) {
            cookie += "; EM-USER-UUID=" + userId;
        }
        return cookie;
    }

    @SuppressWarnings({"unchecked"})
    private <T> T initApi( final Class<T> apiClass, final DefaultKey defaultKey, final String url ) {
        JaxWsClientFactoryBean cfb = new JaxWsClientFactoryBean();
        cfb.setServiceClass( apiClass );
        cfb.setAddress( url );

        Client c = cfb.create();
        if ( logger.isLoggable( Level.FINEST ) ) {
            c.getInInterceptors().add( new LoggingInInterceptor() );
            c.getOutInterceptors().add( new LoggingOutInterceptor() );
            c.getInFaultInterceptors().add( new LoggingInInterceptor() );
            c.getOutFaultInterceptors().add( new LoggingOutInterceptor() );
        }

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
                return defaultKey==null ? new KeyManager[0] : defaultKey.getSslKeyManagers();
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

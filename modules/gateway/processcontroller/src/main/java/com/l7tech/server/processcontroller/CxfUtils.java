package com.l7tech.server.processcontroller;

import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.databinding.DataBinding;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.ArrayList;

/**
 * @author jbufu
 */
public class CxfUtils {

    // - PUBLIC

    public static class ApiBuilder {

        // - PUBLIC

        public ApiBuilder(String endpoint) {
            this.endpoint = endpoint;
            this.tlsClientParams = new TLSClientParameters() {
                @Override
                public boolean isDisableCNCheck() {
                    return true;
                }

                @Override
                public TrustManager[] getTrustManagers() {
                    return new TrustManager[] { new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {}
                        @Override
                        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {}
                        @Override
                        public X509Certificate[] getAcceptedIssuers() {return new X509Certificate[0];}
                    }};
                }
            };
        }

        public ApiBuilder tlsClientParameters(TLSClientParameters tlsClientParams) {
            this.tlsClientParams = tlsClientParams;
            return this;
        }

        public ApiBuilder inInterceptor(Interceptor<? extends Message> interceptor) {
            inInterceptors.add(interceptor);
            return this;
        }

        public ApiBuilder outInterceptor(Interceptor<? extends Message> interceptor) {
            outInterceptors.add(interceptor);
            return this;
        }

        public ApiBuilder inFaultInterceptor(Interceptor<? extends Message> interceptor) {
            inFaultInterceptors.add(interceptor);
            return this;
        }

        public ApiBuilder outFaultInterceptor(Interceptor<? extends Message> interceptor) {
            outFaultInterceptors.add(interceptor);
            return this;
        }

        public ApiBuilder clientPolicy(HTTPClientPolicy clientPolicy) {
            this.clientPolicy = clientPolicy;
            return this;
        }

        public ApiBuilder dataBinding(DataBinding dataBinding) {
            this.dataBinding = dataBinding;
            return this;
        }

        public <T> T build(Class<T> apiType) {
            final JaxWsProxyFactoryBean pfb = new JaxWsProxyFactoryBean(){
                @Override
                protected ClientProxy clientClientProxy( final Client c ) {
                    final HTTPConduit httpConduit = (HTTPConduit)c.getConduit();
                    httpConduit.setTlsClientParameters(tlsClientParams);
                    if (clientPolicy != null) {
                        httpConduit.setClient(clientPolicy);
                    }
                    
                    return super.clientClientProxy( c );
                }
            };
            pfb.setServiceClass(apiType);
            pfb.setAddress(endpoint);
            if (dataBinding != null) {
                pfb.setDataBinding(dataBinding);
            }
            pfb.getInInterceptors().addAll(inInterceptors);
            pfb.getOutInterceptors().addAll(outInterceptors);
            pfb.getInFaultInterceptors().addAll(inFaultInterceptors);
            pfb.getOutFaultInterceptors().addAll(outFaultInterceptors);

            return apiType.cast(pfb.create());
        }

        // - PRIVATE

        private final String endpoint;
        private TLSClientParameters tlsClientParams;
        private List<Interceptor<? extends Message>> inInterceptors = new ArrayList<Interceptor<? extends Message>>();
        private List<Interceptor<? extends Message>> outInterceptors = new ArrayList<Interceptor<? extends Message>>();
        private List<Interceptor<? extends Message>> inFaultInterceptors = new ArrayList<Interceptor<? extends Message>>();
        private List<Interceptor<? extends Message>> outFaultInterceptors = new ArrayList<Interceptor<? extends Message>>();

        HTTPClientPolicy clientPolicy;
        private DataBinding dataBinding;
    }

    // - PRIVATE

    private CxfUtils() {}
}

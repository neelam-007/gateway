package com.l7tech.gateway.api.examples.impl;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.examples.CreateService;
import com.l7tech.gateway.api.examples.EnumerateServices;
import com.l7tech.gateway.api.examples.ExportPrivateKey;
import com.l7tech.gateway.api.impl.TransportFactory;
import com.l7tech.gateway.api.impl.TransportFactory.ActionAwareHTTPTransportClient;
import com.l7tech.util.ResourceUtils;
import com.sun.ws.management.addressing.Addressing;
import com.sun.ws.management.client.impl.ConfigurableHTTPTransportClient;
import com.sun.ws.management.client.impl.TransportClient;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import javax.xml.bind.JAXBException;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.util.Map;

/**
 * Utility to generate the request / response Messages for Web Service API examples.
 */
public class ExampleGenerator {

    //- PUBLIC

    public static void main( final String[] args ) {
        traceInfo( "service_create" );
        CreateService.main( new String[0] );

        traceInfo( "service_enumerate" );
        EnumerateServices.main( new String[0] );

        traceInfo( "private_key_export" );
        ExportPrivateKey.main( new String[0] );
    }

    //- PRIVATE

    static {
        TransportFactory.setTransportStrategy( new TransportFactory.TransportStrategy(){
            @Override
            public TransportClient newTransportClient( final int connectTimeout,
                                                       final int readTimeout,
                                                       final PasswordAuthentication passwordAuthentication,
                                                       final HostnameVerifier hostnameVerifier,
                                                       final SSLSocketFactory sslSocketFactory ) {
                final ConfigurableHTTPTransportClient client = new  LoggingTransportClient();
                client.setConnectTimeout( connectTimeout );
                client.setReadTimeout( readTimeout );
                client.setPasswordAuthentication( passwordAuthentication );
                client.setHostnameVerifier( hostnameVerifier );
                client.setSslSocketFactory( sslSocketFactory );
                client.setUserAgent( "Testing-Client" );
                return client;
            }
        } );
    }

    private static final ThreadLocal<TraceInfo> traceInfo = new ThreadLocal<TraceInfo>();
    private static final String directory = "";

    private static void traceInfo( final String name ) {
        traceInfo.set( new TraceInfo( name ) );
    }

    private static final class TraceInfo {
        private final String filename;
        private long  exchangeCount = 1;

        private TraceInfo( final String filename ) {
            this.filename = filename;
        }

        private String getFilename( final String type ) {
            return filename + "_" + type + "_" + exchangeCount + ".xml";
        }
    }

    private static class LoggingTransportClient extends ActionAwareHTTPTransportClient {
        @Override
        public Addressing sendRequest( final Addressing addressing,
                                       final Map.Entry<String, String>... entries ) throws IOException, SOAPException, JAXBException {
            logMessage( addressing, "request" );
            Addressing response = super.sendRequest( addressing, entries );
            logMessage( response, "response" );
            traceInfo.get().exchangeCount++;
            return response;
        }

        @Override
        public Addressing sendRequest( final SOAPMessage soapMessage,
                                       final String s,
                                       final Map.Entry<String, String>... entries ) throws IOException, SOAPException, JAXBException {
            return super.sendRequest( soapMessage, s, entries );
        }

        private void logMessage( final Addressing addressing,
                                 final String type ) {
            FileOutputStream fout = null;
            try {
                fout = new FileOutputStream( traceInfo.get().getFilename(type) );
                XmlUtil.nodeToFormattedOutputStream( addressing.getEnvelope(), fout );
            } catch ( Exception e ) {
                e.printStackTrace();
            } finally {
                ResourceUtils.closeQuietly( fout );
            }
        }
    };
}

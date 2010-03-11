package com.l7tech.gateway.api.impl;

import com.l7tech.util.BuildInfo;
import com.sun.ws.management.Message;
import com.sun.ws.management.addressing.Addressing;
import com.sun.ws.management.client.impl.ConfigurableHTTPTransportClient;
import com.sun.ws.management.client.impl.TransportClient;
import com.sun.ws.management.transport.ContentType;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import javax.xml.bind.JAXBException;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import java.net.PasswordAuthentication;

/**
 *
 */
public class TransportFactory {

    //- PUBLIC

    public static TransportClient newTransportClient( final int connectTimeout,
                                                      final int readTimeout,
                                                      final PasswordAuthentication passwordAuthentication,
                                                      final HostnameVerifier hostnameVerifier,
                                                      final SSLSocketFactory sslSocketFactory ) {
        return transportStrategy.newTransportClient(
                connectTimeout,
                readTimeout,
                passwordAuthentication,
                hostnameVerifier,
                sslSocketFactory );
    }

    public static void setTransportStrategy( final TransportStrategy transportStrategy ) {
        TransportFactory.transportStrategy = transportStrategy;
    }

    public interface TransportStrategy {
        TransportClient newTransportClient(
                int connectTimeout,
                int readTimeout,
                PasswordAuthentication passwordAuthentication,
                HostnameVerifier hostnameVerifier,
                SSLSocketFactory sslSocketFactory );
    }

    //- PRIVATE

    private static final String USER_AGENT = "SecureSpan-Gateway-API/" + BuildInfo.getBuildNumber();
    private static TransportStrategy transportStrategy = new DefaultTransportStrategy();

    private static final class DefaultTransportStrategy implements TransportStrategy {
        @Override
        public TransportClient newTransportClient( final int connectTimeout,
                                                   final int readTimeout,
                                                   final PasswordAuthentication passwordAuthentication,
                                                   final HostnameVerifier hostnameVerifier,
                                                   final SSLSocketFactory sslSocketFactory ) {

            final ConfigurableHTTPTransportClient client = new ActionAwareHTTPTransportClient();
            client.setConnectTimeout( connectTimeout );
            client.setReadTimeout( readTimeout );
            client.setPasswordAuthentication( passwordAuthentication );
            client.setHostnameVerifier( hostnameVerifier );
            client.setSslSocketFactory( sslSocketFactory );
            client.setUserAgent( USER_AGENT );
            return client;
        }
    }

    private static final class ActionAwareHTTPTransportClient extends ConfigurableHTTPTransportClient {
        @Override
        protected String getContentType( final SOAPMessage soapMessage,
                                         final Message message,
                                         final ContentType suggestedContentType ) {
            String contentType = null;

            if ( message instanceof Addressing ) {
                final Addressing addressing = (Addressing) message;
                contentType = suggestedContentType == null ?
                        ContentType.DEFAULT_CONTENT_TYPE.toString() :
                        suggestedContentType.toString();

                try {
                    if ( !contentType.contains( "action" ) && addressing.getHeader() != null && addressing.getAction() != null ) {
                        // Add SOAP 1.2 action parameter
                        contentType += ";action=" + addressing.getAction();
                    }
                } catch ( SOAPException e ) {
                    // don't add action parameter
                } catch ( JAXBException e ) {
                    // don't add action parameter
                }
            }

            if ( contentType == null ) {
                contentType =  suggestedContentType == null ? null : suggestedContentType.toString();
            }

            return contentType;
        }
    }
}

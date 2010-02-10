package com.l7tech.server.service;

import com.l7tech.common.io.ByteLimitInputStream;
import com.l7tech.common.io.ByteOrderMarkInputStream;
import com.l7tech.util.IOUtils;
import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * WSDL document access refactored from ServiceAdminImpl in 5.3
 */
public class ServiceDocumentResolver {

    //- PUBLIC

    public ServiceDocumentResolver( final X509TrustManager trustManager ) {
        this.trustManager = trustManager;
    }

    public String resolveWsdlTarget(String url) throws IOException {
        GetMethod get = null;
        try {
            URL urltarget = new URL(url);
            HttpClient client = new HttpClient();
            HttpClientParams clientParams = client.getParams();
            HostConfiguration hconf = getHostConfigurationWithTrustManager(urltarget);
            // bugfix for 1857 (next 3 lines)
            clientParams.setVersion( HttpVersion.HTTP_1_1);
            StringBuilder hostval = new StringBuilder(urltarget.getHost());
            if (urltarget.getPort() > 0)
                hostval.append(':').append(urltarget.getPort());
            get = hconf == null ? new GetMethod(url) : new GetMethod(urltarget.getFile());
            get.setRequestHeader("HOST", hostval.toString());

            // support for passing username and password in the url from the ssm
            String userinfo = urltarget.getUserInfo();
            if (userinfo != null && userinfo.indexOf(':') > -1) {
                String login = userinfo.substring(0, userinfo.indexOf(':'));
                String passwd = userinfo.substring(userinfo.indexOf(':')+1, userinfo.length());
                HttpState state = client.getState();
                get.setDoAuthentication(true);
                clientParams.setAuthenticationPreemptive(true);
                state.setCredentials( AuthScope.ANY, new UsernamePasswordCredentials(login, passwd));
            }

            int ret;
            if (hconf != null) {
                ret = client.executeMethod(hconf, get);
            } else {
                ret = client.executeMethod(get);
            }
            if (ret == 200) {
                //noinspection IOResourceOpenedButNotSafelyClosed
                byte[] body = IOUtils.slurpStream(new ByteOrderMarkInputStream(new ByteLimitInputStream(get.getResponseBodyAsStream(), 16, 10*1024*1024)));
                String charset = get.getResponseCharSet();
                return new String(body, charset);
            } else {
                String msg = "The URL '" + url + "' is returning status code " + ret;
                throw new IOException(msg);
            }
        } catch ( HttpException e) {
            String msg = "Http error getting " + url;
            IOException ioe =new  IOException(msg);
            ioe.initCause(e);
            throw ioe;
        } finally {
            if (get != null) {
                get.releaseConnection();
            }
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( ServiceDocumentResolver.class.getName() );

    private SSLContext sslContext;

    private final X509TrustManager trustManager;


    private HostConfiguration getHostConfigurationWithTrustManager(URL url) {
        HostConfiguration hconf = null;
        if ("https".equals(url.getProtocol())) {
            final int fport = url.getPort() == -1 ? 443 : url.getPort();
            hconf = new HostConfiguration();
            Protocol protocol = new Protocol(url.getProtocol(), (ProtocolSocketFactory) new SecureProtocolSocketFactory() {
                @Override
                public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
                    return getSSLContext().getSocketFactory().createSocket(socket, host, port, autoClose);
                }

                @Override
                public Socket createSocket(String host, int port, InetAddress clientAddress, int clientPort) throws IOException {
                    return getSSLContext().getSocketFactory().createSocket(host, port, clientAddress, clientPort);
                }

                @Override
                public Socket createSocket(String host, int port) throws IOException {
                    return getSSLContext().getSocketFactory().createSocket(host, port);
                }

                @Override
                public Socket createSocket(String host, int port, InetAddress clientAddress, int clientPort, HttpConnectionParams httpConnectionParams) throws IOException {
                    Socket socket = getSSLContext().getSocketFactory().createSocket();
                    int connectTimeout = httpConnectionParams.getConnectionTimeout();

                    socket.bind(new InetSocketAddress(clientAddress, clientPort));

                    try {
                        socket.connect(new InetSocketAddress(host, port), connectTimeout);
                    }
                    catch( SocketTimeoutException ste) {
                        throw new ConnectTimeoutException("Timeout when connecting to host '"+host+"'.", ste);
                    }

                    return socket;
                }
            }, fport);
            hconf.setHost(url.getHost(), fport, protocol);
        }
        return hconf;
    }

    private synchronized SSLContext getSSLContext() {
        if (sslContext == null) {
            try {
                sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, new TrustManager[]{trustManager}, null);
            } catch ( NoSuchAlgorithmException e) {
                logger.log( Level.SEVERE, "Unable to get sslcontext", e);
                throw new RuntimeException(e);
            } catch ( KeyManagementException e) {
                logger.log(Level.SEVERE, "Unable to get sslcontext", e);
                throw new RuntimeException(e);
            }
        }
        return sslContext;
    }


}

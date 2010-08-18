package com.l7tech.skunkworks.console;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;

/**
 * Manager that uses the Applet code without requiring a web browser.
 *
 * <p>This is not secure, it is for internal use on a trusted network only.</p>
 *
 * <p>This will not work if you have any console classes on your classpath, the
 * run script can be used to launch - ./run.sh nomanager host:port user pass</p>
 *
 * <p>This should allow management of any SSG with an an applet, but additional
 * URLs may need to be added (e.g. a different name for the applet Jar or a
 * different path).</p>
 *
 * <p>Error messages are unfriendly, e.g. ClassNotFoundException for a bad
 * password.</p>
 */
public class NoManager {

    @SuppressWarnings({ "ResultOfMethodCallIgnored" })
    public static void main( final String[] args ) throws Exception {
        String host = "127.0.0.1:8443";
        if ( args.length > 0 ) {
            host = args[0];
        }

        String username = "admin";
        String password = "password";
        if ( args.length > 2 ) {
            username = args[1];
            password = args[2];    
        }

        CookieHandler.setDefault( new CookieManager() );
        HttpsURLConnection.setDefaultSSLSocketFactory( new PermissiveSSLSocketFactory() );
        HttpsURLConnection.setDefaultHostnameVerifier( new HostnameVerifier(){
            @Override
            public boolean verify( final String s, final SSLSession sslSession ) {
                return true;
            }
        } );

        final URL url = new URL( "https://"+host+"/ssg/webadmin/" );
        final HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setConnectTimeout( 15000 );
        urlConnection.setReadTimeout( 30000 );
        urlConnection.setRequestMethod( "POST" );
        urlConnection.setInstanceFollowRedirects( false );
        urlConnection.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded" );
        urlConnection.setDoOutput( true );
        urlConnection.getOutputStream().write( ("username="+username+"&password="+password).getBytes() );
        urlConnection.getInputStream().read( new byte[10240] );

        addURLs( Arrays.asList(
                new URL("https://"+host+"/ssg/webadmin/applet/layer7-gateway-console-applet.jar"),
                new URL("https://"+host+"/ssg/webadmin/applet/")
        ) );
        final Class consoleMain = Class.forName( "com.l7tech.console.Main" );
        final Method method = consoleMain.getMethod( "main", String[].class );
        method.invoke( null, new Object[]{args} );
    }

    private static void addURLs( final Collection<URL> jarUrls ) {
        URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        try {
            Method method = URLClassLoader.class.getDeclaredMethod( "addURL", new Class[]{ URL.class } );
            method.setAccessible( true );
            for ( URL jarUrl : jarUrls ) {
                method.invoke( classLoader, jarUrl );
            }
        } catch ( NoSuchMethodException e ) {
            throw new RuntimeException( "Error configuring classpath", e );
        } catch ( IllegalAccessException e ) {
            throw new RuntimeException( "Error configuring classpath", e );
        } catch ( InvocationTargetException e ) {
            throw new RuntimeException( "Error configuring classpath", e.getCause() );
        }
    }
    
    private static class PermissiveSSLSocketFactory extends SSLSocketFactory {
        private final SSLSocketFactory defaultSslSocketFactory = (SSLSocketFactory)SSLSocketFactory.getDefault();
        private final SSLContext sslContext;

        private PermissiveSSLSocketFactory() {
            try {
                sslContext = SSLContext.getInstance("TLS");
                X509TrustManager trustManager = new X509TrustManager(){
                    @Override
                    public void checkClientTrusted( final X509Certificate[] x509Certificates, final String s ) throws CertificateException {
                    }

                    @Override
                    public void checkServerTrusted( final X509Certificate[] x509Certificates, final String s ) throws CertificateException {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                };
                sslContext.init(null,
                                new X509TrustManager[] {trustManager},
                                null);
            } catch ( NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            } catch ( KeyManagementException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return defaultSslSocketFactory.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return defaultSslSocketFactory.getSupportedCipherSuites();
        }

        private SSLSocketFactory socketFactory() {
            return sslContext.getSocketFactory();
        }

        @Override
        public Socket createSocket(Socket socket, String host, int port, boolean autoClose)
                throws IOException {
            return socketFactory().createSocket(socket, host, port, autoClose);
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress clientHost, int clientPort)
                throws IOException {
            return socketFactory().createSocket(host, port, clientHost, clientPort);
        }

        @Override
        public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress1, int i1) throws IOException
        {
            return socketFactory().createSocket(inetAddress, i, inetAddress1, i1);
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            return socketFactory().createSocket(host, port);
        }

        @Override
        public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
            return socketFactory().createSocket(inetAddress, i);
        }

        @Override
        public Socket createSocket() throws IOException {
            return socketFactory().createSocket();
        }
    }
}

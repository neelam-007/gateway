package com.l7tech.gateway.api;

import static org.junit.Assert.*;
import org.junit.Test;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.security.NoSuchAlgorithmException;

/**
 *
 */
public class ClientFactoryTest {

    @Test(expected=ClientFactory.InvalidOptionException.class)
    public void testGetInvalidAttribute() throws ClientFactory.InvalidOptionException {
        ClientFactory clientFactory = ClientFactory.newInstance();
        clientFactory.getAttribute( "foo" );
    }

    @Test(expected=ClientFactory.InvalidOptionException.class)
    public void testSetInvalidAttribute() throws ClientFactory.InvalidOptionException {
        ClientFactory clientFactory = ClientFactory.newInstance();
        clientFactory.setAttribute( "foo", "bar" );
    }

    @Test
    public void testValidAttribute() throws ClientFactory.InvalidOptionException, NoSuchAlgorithmException {
        ClientFactory clientFactory = ClientFactory.newInstance();

        clientFactory.setAttribute( ClientFactory.ATTRIBUTE_USERNAME, "username" );
        assertEquals( "attribute username", "username", clientFactory.getAttribute( ClientFactory.ATTRIBUTE_USERNAME ));
        clientFactory.setAttribute( ClientFactory.ATTRIBUTE_PASSWORD, "password" );
        assertEquals( "attribute username", "password", clientFactory.getAttribute( ClientFactory.ATTRIBUTE_PASSWORD ));
        clientFactory.setAttribute( ClientFactory.ATTRIBUTE_CREDENTIAL_CALLBACK_HANDLER, new CallbackHandler(){
            @Override
            public void handle( final Callback[] callbacks ) throws IOException, UnsupportedCallbackException {
            }
        } );
        assertNotNull( "attribute callback handler", clientFactory.getAttribute(ClientFactory.ATTRIBUTE_CREDENTIAL_CALLBACK_HANDLER) );
        clientFactory.setAttribute( ClientFactory.ATTRIBUTE_HOSTNAME_VERIFIER, new HostnameVerifier(){
            @Override
            public boolean verify( final String s, final SSLSession sslSession ) {
                return false;
            }
        } );
        clientFactory.setAttribute( ClientFactory.ATTRIBUTE_PROXY_USERNAME, "username-p" );
        assertEquals( "attribute proxy username", "username-p", clientFactory.getAttribute( ClientFactory.ATTRIBUTE_PROXY_USERNAME ));
        clientFactory.setAttribute( ClientFactory.ATTRIBUTE_PROXY_PASSWORD, "password-p" );
        assertEquals( "attribute proxy password", "password-p", clientFactory.getAttribute( ClientFactory.ATTRIBUTE_PROXY_PASSWORD ));
        clientFactory.setAttribute( ClientFactory.ATTRIBUTE_NET_CONNECT_TIMEOUT, 10000 );
        assertEquals( "attribute connect timeout", 10000, clientFactory.getAttribute( ClientFactory.ATTRIBUTE_NET_CONNECT_TIMEOUT ));
        clientFactory.setAttribute( ClientFactory.ATTRIBUTE_NET_READ_TIMEOUT, 20000 );
        assertEquals( "attribute read timeout", 20000, clientFactory.getAttribute( ClientFactory.ATTRIBUTE_NET_READ_TIMEOUT ));
        clientFactory.setAttribute( ClientFactory.ATTRIBUTE_SSL_SOCKET_FACTORY, SSLContext.getDefault().getSocketFactory() );
        assertNotNull( "attribute ssl socket factory", clientFactory.getAttribute(ClientFactory.ATTRIBUTE_SSL_SOCKET_FACTORY) );
    }

    @Test(expected=ClientFactory.InvalidOptionException.class)
    public void testGetInvalidFeature() throws ClientFactory.InvalidOptionException {
        ClientFactory clientFactory = ClientFactory.newInstance();
        clientFactory.getFeature( "foo" );
    }

    @Test(expected=ClientFactory.InvalidOptionException.class)
    public void testSetInvalidFeature() throws ClientFactory.InvalidOptionException {
        ClientFactory clientFactory = ClientFactory.newInstance();
        clientFactory.setFeature( "foo", true );
    }

    @Test
    public void testValidFeature() throws ClientFactory.InvalidOptionException {
        ClientFactory clientFactory = ClientFactory.newInstance();
        clientFactory.setFeature( ClientFactory.FEATURE_CERTIFICATE_VALIDATION, true );
        assertTrue( "feature certificate validation", clientFactory.getFeature( ClientFactory.FEATURE_CERTIFICATE_VALIDATION ));
        clientFactory.setFeature( ClientFactory.FEATURE_HOSTNAME_VALIDATION, false );
        assertFalse( "feature hostname validation", clientFactory.getFeature( ClientFactory.FEATURE_HOSTNAME_VALIDATION ));
    }

    @Test
    public void testAuthenticator() throws Exception {
        ClientFactory clientFactory = ClientFactory.newInstance();

        clientFactory.setFeature( ClientFactory.FEATURE_CERTIFICATE_VALIDATION, false );
        clientFactory.setFeature( ClientFactory.FEATURE_HOSTNAME_VALIDATION, false );
        clientFactory.setAttribute( ClientFactory.ATTRIBUTE_CREDENTIAL_CALLBACK_HANDLER, new CallbackHandler(){
            @Override
            public void handle( final Callback[] callbacks ) throws IOException, UnsupportedCallbackException {
                for ( final Callback callback : callbacks ) {
                    if ( callback instanceof NameCallback ) {
                        final NameCallback nameCallback = (NameCallback) callback;
                        if ( ClientFactory.CALLBACK_USERNAME.equals( nameCallback.getPrompt() ) ) {
                            nameCallback.setName( "username" );
                        } else if ( ClientFactory.CALLBACK_PROXY_USERNAME.equals( nameCallback.getPrompt() ) ) {
                            nameCallback.setName( "proxy-username" );
                        } else {
                            throw new IOException("Unexpected callback for " + nameCallback.getPrompt() );
                        }
                    } else if ( callback instanceof PasswordCallback ) {
                        final PasswordCallback passwordCallback = (PasswordCallback) callback;    
                        if ( ClientFactory.CALLBACK_PASSWORD.equals( passwordCallback.getPrompt() ) ) {
                            passwordCallback.setPassword( "password".toCharArray() );
                        } else if ( ClientFactory.CALLBACK_PROXY_PASSWORD.equals( passwordCallback.getPrompt() ) ) {
                            passwordCallback.setPassword( "proxy-password".toCharArray() );
                        } else {
                            throw new IOException("Unexpected callback for " + passwordCallback.getPrompt() );
                        }

                    } else {
                        throw new UnsupportedCallbackException( callback );
                    }
                }
            }
        } );

        clientFactory.createClient( "http://localhost:12345" ); // installs authenticator

        final PasswordAuthentication pa1 = Authenticator.requestPasswordAuthentication( "localhost", null, 12345, "http", "Provide credentials",  "http", new URL("http://localhost:12345"), Authenticator.RequestorType.SERVER );
        final PasswordAuthentication pa2 = Authenticator.requestPasswordAuthentication( "localhost", null, 12345, "http", "Provide credentials",  "http", new URL("http://localhost:12345"), Authenticator.RequestorType.PROXY );

        assertNotNull( "Server credentials missing", pa1 );
        assertNotNull( "Server password missing", pa1.getPassword() );
        assertNotNull( "Proxy credentials missing", pa2 );
        assertNotNull( "Proxy password missing", pa2.getPassword() );

        assertEquals( "Server username", "username", pa1.getUserName() );
        assertEquals( "Server password", "password", new String(pa1.getPassword()) );

        assertEquals( "Proxy username", "proxy-username", pa2.getUserName() );
        assertEquals( "Proxy password", "proxy-password", new String(pa2.getPassword()) );
    }
}

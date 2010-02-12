package com.l7tech.gateway.api;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 */
public class ClientFactory {

    //- PUBLIC

    public static final String FEATURE_HOSTNAME_VALIDATION = "http://www.layer7tech.com/com.l7tech.gateway.api/hostname-validation";
    public static final String FEATURE_CERTIFICATE_VALIDATION = "http://www.layer7tech.com/com.l7tech.gateway.api/certificate-validation";

    public static final String ATTRIBUTE_USERNAME = "http://www.layer7tech.com/com.l7tech.gateway.api/username";
    public static final String ATTRIBUTE_PASSWORD = "http://www.layer7tech.com/com.l7tech.gateway.api/password";
    public static final String ATTRIBUTE_CREDENTIAL_CALLBACK_HANDLER = "http://www.layer7tech.com/com.l7tech.gateway.api/credential-callback-handler";
    public static final String ATTRIBUTE_HOSTNAME_VERIFIER = "http://www.layer7tech.com/com.l7tech.gateway.api/hostname-verifier";

    public static ClientFactory newInstance() {
        return new ClientFactory();
    }

    public Object getAttribute( final String name ) throws InvalidOptionException {
        if (!ATTRIBUTE_NAMES.contains( name ) ) throw new InvalidOptionException( name );
        return attributes.get( name );
    }

    public void setAttribute( final String name, final Object value ) throws InvalidOptionException {
        if (!ATTRIBUTE_NAMES.contains( name ) ) throw new InvalidOptionException( name );
        attributes.put( name, value );
    }

    public boolean getFeature( final String name ) throws InvalidOptionException {
        if ( features.containsKey( name )) {
            return features.get( name );
        } else {
            throw new InvalidOptionException( name );
        }
    }

    public void setFeature( final String name, final boolean value ) throws InvalidOptionException {
        if ( features.containsKey( name )) {
            features.put( name, value );
        } else {
            throw new InvalidOptionException( name );
        }
    }

    public Client createClient( final String url ) {
        final String username = getAttribute( ATTRIBUTE_USERNAME, String.class );
        final String password = getAttribute( ATTRIBUTE_PASSWORD, String.class );
        final CallbackHandler callbackHandler = getAttribute( ATTRIBUTE_CREDENTIAL_CALLBACK_HANDLER, CallbackHandler.class );
        final Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                String name = null;
                char[] pass = null;

                //TODO [steve] use getRequestorType to handle HTTP proxy auth also
                try {
                    final URL serviceUrl = new URL(url);

                    // Verify requesting host:port or URL matches the URL for the service
                    if ( getRequestingURL().equals( serviceUrl ) ||
                         (getRequestingHost().equalsIgnoreCase(serviceUrl.getHost()) &&
                         ((getRequestingPort() == serviceUrl.getPort()) ||
                          (getRequestingPort() == serviceUrl.getDefaultPort() && serviceUrl.getPort() == -1))) ) {
                        name = username;
                        pass = password==null ? null : password.toCharArray();

                        if ( name == null && callbackHandler != null ) {
                            NameCallback nameCallback = new NameCallback("User");
                            PasswordCallback passwordCallback = new PasswordCallback("Password", false);

                            try {
                                callbackHandler.handle( new Callback[]{ nameCallback, passwordCallback } );
                            } catch ( IOException e ) {
                                logger.log( Level.WARNING, "Error in authentication callback handler.", e );
                            } catch ( UnsupportedCallbackException e ) {
                                logger.log( Level.WARNING, "Error in authentication callback handler.", e );
                            }

                            name = nameCallback.getName();
                            pass = passwordCallback.getPassword();
                        }
                    }
                } catch ( MalformedURLException e ) {
                    logger.log( Level.WARNING, "Error in authenticator.", e );
                }

                return name != null && pass != null ? new PasswordAuthentication( name, pass ) : null;
            }
        };

        final HostnameVerifier verifier;
        if ( features.get(FEATURE_HOSTNAME_VALIDATION) ) {
            verifier = getAttribute( ATTRIBUTE_HOSTNAME_VERIFIER, HostnameVerifier.class );
        } else {
            verifier = new HostnameVerifier(){ // Allows any hostname
                @Override
                public boolean verify( final String hostname, final SSLSession sslSession ) {
                    return true;
                }
            };
        }

        final X509TrustManager trustManager;
        if ( features.get(FEATURE_CERTIFICATE_VALIDATION) ) {
            trustManager = null; // Use JDK default trust manager    
        } else {
            trustManager = new X509TrustManager(){ // Allow any server certificate
                @Override
                public void checkClientTrusted( X509Certificate[] x509Certificates, String s) throws CertificateException {}
                @Override
                public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {}
                @Override
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            };
        }

        return new Client( url,
                           authenticator,
                           verifier,
                           trustManager );
    }

    @SuppressWarnings( { "serial" } )
    public static class InvalidOptionException extends Exception {
        public InvalidOptionException( final String optionName ) {
            super( "Invalid option '" + optionName + "'" );
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( ClientFactory.class.getName() );

    private static final Map<String,Boolean> DEFAULT_FEATURES = Collections.unmodifiableMap( new HashMap<String,Boolean>(){{
        put(FEATURE_HOSTNAME_VALIDATION, true); 
        put(FEATURE_CERTIFICATE_VALIDATION, true);
    }} );
    private static final Set<String> ATTRIBUTE_NAMES = Collections.unmodifiableSet( new HashSet<String>(Arrays.asList(
        ATTRIBUTE_USERNAME,
        ATTRIBUTE_PASSWORD,
        ATTRIBUTE_CREDENTIAL_CALLBACK_HANDLER,
        ATTRIBUTE_HOSTNAME_VERIFIER
    )) );

    private final Map<String,Object> attributes;
    private final Map<String,Boolean> features;

    private ClientFactory() {
        attributes = new HashMap<String,Object>();
        features = new HashMap<String,Boolean>( DEFAULT_FEATURES );
    }

    @SuppressWarnings( { "unchecked" } )
    private <T> T getAttribute( final String name, final Class<T> type ) {
        final Object value = attributes.get( name );
        T typedValue = null;

        if ( type.isInstance(value) ) {
            typedValue = (T) value;
        }

        return typedValue;
    }
}

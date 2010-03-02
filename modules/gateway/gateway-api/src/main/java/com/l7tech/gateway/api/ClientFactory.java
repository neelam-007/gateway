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
 * Factory for the Gateway management Client.
 * 
 * @see Client
 */
public class ClientFactory {

    //- PUBLIC

    /**
     * Feature for validation of TLS/SSL server hostname. Default {@code true}.
     */
    public static final String FEATURE_HOSTNAME_VALIDATION = "http://www.layer7tech.com/com.l7tech.gateway.api/hostname-validation";

    /**
     * Feature for validation of TLS/SSL server certificates. Default {@code true}.
     */
    public static final String FEATURE_CERTIFICATE_VALIDATION = "http://www.layer7tech.com/com.l7tech.gateway.api/certificate-validation";

    /**
     * Attribute for service username.
     */
    public static final String ATTRIBUTE_USERNAME = "http://www.layer7tech.com/com.l7tech.gateway.api/username";

    /**
     * Attribute for service password.
     */
    public static final String ATTRIBUTE_PASSWORD = "http://www.layer7tech.com/com.l7tech.gateway.api/password";

    /**
     * Attribute for a credential callback handler. This will be used to access
     * credentials when the corresponding username attribute is not set.
     *
     * @see CallbackHandler
     * @see #CALLBACK_USERNAME
     * @see #CALLBACK_PASSWORD
     * @see #CALLBACK_PROXY_USERNAME
     * @see #CALLBACK_PROXY_PASSWORD
     */
    public static final String ATTRIBUTE_CREDENTIAL_CALLBACK_HANDLER = "http://www.layer7tech.com/com.l7tech.gateway.api/credential-callback-handler";

    /**
     * Attribute for a custom hostname verifier, ignored if hostname validation is not enabled.
     *
     * @see HostnameVerifier
     */
    public static final String ATTRIBUTE_HOSTNAME_VERIFIER = "http://www.layer7tech.com/com.l7tech.gateway.api/hostname-verifier";

    /**
     * Attribute for HTTP proxy username.
     */
    public static final String ATTRIBUTE_PROXY_USERNAME = "http://www.layer7tech.com/com.l7tech.gateway.api/proxy-username";

    /**
     * Attribute for HTTP proxy password.
     */
    public static final String ATTRIBUTE_PROXY_PASSWORD = "http://www.layer7tech.com/com.l7tech.gateway.api/proxy-password";

    /**
     * Callback prompt for SecureSpan Gateway username.
     */
    public static final String CALLBACK_USERNAME = "Username";

    /**
     * Callback prompt for SecureSpan Gateway password.
     */
    public static final String CALLBACK_PASSWORD = "Password";

    /**
     * Callback prompt for HTTP proxy username.
     */
    public static final String CALLBACK_PROXY_USERNAME = "Proxy Username";

    /**
     * Callback prompt for HTTP proxy password. 
     */
    public static final String CALLBACK_PROXY_PASSWORD = "Proxy Password";

    /**
     * Create a new ClientFactory.
     *
     * @return a ClientFactory with default settings.
     */
    public static ClientFactory newInstance() {
        return new ClientFactory();
    }

    /**
     * Get the current value for an attribute.
     *
     * @param name The attribute name
     * @return The attribute value (may be null)
     * @throws InvalidOptionException If the attribute name is not recognised
     */
    public Object getAttribute( final String name ) throws InvalidOptionException {
        if (!ATTRIBUTE_NAMES.contains( name ) ) throw new InvalidOptionException( name );
        return attributes.get( name );
    }

    /**
     * Set the value for an attribute.
     *
     * @param name The attribute name
     * @param value The value for the attribute.
     * @throws InvalidOptionException If the attribute name is not recognised
     */
    public void setAttribute( final String name, final Object value ) throws InvalidOptionException {
        if (!ATTRIBUTE_NAMES.contains( name ) ) throw new InvalidOptionException( name );
        attributes.put( name, value );
    }

    /**
     * Get the current state for a feature.
     *
     * @param name The feature name
     * @return True if the feature is enabled
     * @throws InvalidOptionException If the feature name is not recognised
     */
    public boolean getFeature( final String name ) throws InvalidOptionException {
        if ( features.containsKey( name )) {
            return features.get( name );
        } else {
            throw new InvalidOptionException( name );
        }
    }

    /**
     * Set the enabled state for a feature.
     *
     * @param name The feature name
     * @param value The value for the enabled state
     * @throws InvalidOptionException If the feature name is not recognised
     */
    public void setFeature( final String name, final boolean value ) throws InvalidOptionException {
        if ( features.containsKey( name )) {
            features.put( name, value );
        } else {
            throw new InvalidOptionException( name );
        }
    }

    /**
     * Create a Client with the current features and attributes.
     *
     * @param url The URL for the management service
     * @return The Client
     */
    public Client createClient( final String url ) {
        final String username = getAttribute( ATTRIBUTE_USERNAME, String.class );
        final String password = getAttribute( ATTRIBUTE_PASSWORD, String.class );
        final String proxyUsername = getAttribute( ATTRIBUTE_PROXY_USERNAME, String.class );
        final String proxyPassword = getAttribute( ATTRIBUTE_PROXY_PASSWORD, String.class );
        final CallbackHandler callbackHandler = getAttribute( ATTRIBUTE_CREDENTIAL_CALLBACK_HANDLER, CallbackHandler.class );
        final Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                String name = null;
                char[] pass = null;

                try {
                    if ( getRequestorType() == RequestorType.PROXY ) {
                        name = proxyUsername;
                        pass = proxyPassword==null ? null : proxyPassword.toCharArray();

                        if ( name == null && callbackHandler != null ) {
                            final NameCallback nameCallback = new NameCallback( CALLBACK_PROXY_USERNAME );
                            final PasswordCallback passwordCallback = new PasswordCallback( CALLBACK_PROXY_PASSWORD, false );

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
                    } else {
                        // Verify requesting host:port or URL matches the URL for the service
                        final URL serviceUrl = new URL(url);
                        if ( getRequestingURL().equals( serviceUrl ) ||
                             (getRequestingHost().equalsIgnoreCase(serviceUrl.getHost()) &&
                             ((getRequestingPort() == serviceUrl.getPort()) ||
                              (getRequestingPort() == serviceUrl.getDefaultPort() && serviceUrl.getPort() == -1))) ) {
                            name = username;
                            pass = password==null ? null : password.toCharArray();

                            if ( name == null && callbackHandler != null ) {
                                final NameCallback nameCallback = new NameCallback( CALLBACK_USERNAME );
                                final PasswordCallback passwordCallback = new PasswordCallback( CALLBACK_PASSWORD, false );

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

    /**
     * Exception for invalid features and attributes.
     */
    @SuppressWarnings( { "serial" } )
    public static class InvalidOptionException extends Exception {
        private final String option;

        public InvalidOptionException( final String option ) {
            super( "Invalid option '" + option + "'" );
            this.option = option;
        }

        public String getOption() {
            return option;
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
        ATTRIBUTE_HOSTNAME_VERIFIER,
        ATTRIBUTE_PROXY_USERNAME,
        ATTRIBUTE_PROXY_PASSWORD
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

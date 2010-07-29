/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console.util;

import com.l7tech.common.http.HttpConstants;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;

import java.applet.AppletContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Preferences management for the Applet.
 */
public class AppletSsmPreferences extends AbstractSsmPreferences {

    //- PUBLIC

    @Override
    public void updateSystemProperties() {
        // Takes no action for applet
    }

    @Override
    public void store() throws IOException {
        final PersistenceStrategy strategy = this.strategy;
        if ( strategy == null ) {
            logger.warning( "Ignoring store as no persistence strategy is available." );
        } else {
            ByteArrayOutputStream preferencesOut = null;
            ByteArrayInputStream preferencesIn = null;
            try {
                preferencesOut = new ByteArrayOutputStream(2048);
                props.store(preferencesOut, "Gateway properties");

                preferencesIn = new ByteArrayInputStream(preferencesOut.toByteArray());
                strategy.setPreferencesInputStream( preferencesIn );
            } finally {
                ResourceUtils.closeQuietly( preferencesOut );
                ResourceUtils.closeQuietly( preferencesIn );
            }
        }
    }

    @Override
    public String getHomePath() {
        throw new UnsupportedOperationException("No HomePath for applet");
    }

    @Override
    public void importSsgCert(X509Certificate cert, String hostname) throws KeyStoreException, NoSuchAlgorithmException, IOException, CertificateException {
        // Takes no action for applet
    }

    @Override
    public boolean isStatusBarBarVisible() {
        return false; // no status bar on applet
    }

    public void setContext( final AppletContext context, final URL url ) {
        this.strategy = buildStrategy( context, url );

        if ( strategy != null ) {
            InputStream preferencesIn = null;
            try {
                preferencesIn = strategy.getPreferencesInputStream();
                if ( preferencesIn != null ) {
                    logger.info( "Loading stored preferences." );
                    props.load( preferencesIn );
                } else {
                    logger.info( "No stored preferences found." );
                }
            } catch ( IOException e ) {
                logger.log( Level.WARNING, "Error loading preferences '"+ ExceptionUtils.getMessage( e ) +"'", e );
            } finally {
                ResourceUtils.closeQuietly( preferencesIn );
            }
        }

    }

    //- PRIVATE

    private PersistenceStrategy strategy;

    private PersistenceStrategy buildStrategy( final AppletContext context, final URL url ) {
        PersistenceStrategy strategy = null;

        try {
            strategy = new CookiePersistenceStrategy( url );
        } catch ( Exception e ) {
            logger.info( "Cookie persistence strategy not available '"+ExceptionUtils.getMessage( e )+"'." );
        }

        if ( strategy == null ) {
            strategy = new AppletContextPersistenceStrategy( context );
        }

        return strategy;
    }

    /**
     * Strategy for persistence of settings
     */
    private interface PersistenceStrategy {
        InputStream getPreferencesInputStream() throws IOException;
        void setPreferencesInputStream( InputStream inputStream ) throws IOException;
    }

    /**
     * Persistence strategy that uses the applet context, this will not store across browser restarts.
     */
    private static class AppletContextPersistenceStrategy implements PersistenceStrategy {
        private static final String STREAM_CONSOLE_PREFERENCES = "com.l7tech.console.preferences";
        private final AppletContext context;

        private AppletContextPersistenceStrategy( final AppletContext context ) {
            this.context = context;
        }

        @Override
        public InputStream getPreferencesInputStream() throws IOException {
            return context.getStream( STREAM_CONSOLE_PREFERENCES );
        }

        @Override
        public void setPreferencesInputStream( final InputStream in ) throws IOException {
            context.setStream( STREAM_CONSOLE_PREFERENCES, in );
        }
    }

    /**
     * Persistence strategy using cookies, will be persistent if browser settings allow.
     *
     * <p>The cookie will not be sent to the SSG since it uses a path that is not accessed.</p>
     */
    private static class CookiePersistenceStrategy implements PersistenceStrategy {
        private static final String COOKIE_PATH = "/ssg/webadmin/local/";
        private static final String COOKIE_NAME = "manager_preferences";

        private final CookieHandler cookieHandler;
        private final URL url;
        private CookiePersistenceStrategy(  final URL url  ) {
            this.cookieHandler = CookieManager.getDefault();
            if ( this.cookieHandler == null ) throw new IllegalStateException("Cookies not available.");
            this.url = url;
        }

        @Override
        public InputStream getPreferencesInputStream() throws IOException {
            InputStream in = null;
            try {
                final Map<String, List<String>> headers = cookieHandler.get( url.toURI().resolve( COOKIE_PATH ), Collections.<String, List<String>>emptyMap() );
                final List<String> cookieHeaders = headers.get( HttpConstants.HEADER_COOKIE );
                if ( cookieHeaders != null ) {
                    out:
                    for ( final String cookieHeader : cookieHeaders ) {
                        for ( final com.l7tech.common.http.HttpCookie cookie : com.l7tech.common.http.HttpCookie.fromCookieHeader( url, cookieHeader ) ) {
                            if ( COOKIE_NAME.equals( cookie.getCookieName() ) ) {
                                in = new ByteArrayInputStream( HexUtils.decodeBase64( cookie.getCookieValue(), true ));
                                break out;
                            }
                        }
                    }
                }
            } catch ( com.l7tech.common.http.HttpCookie.IllegalFormatException e ) {
                throw new IOException( e );
            } catch ( URISyntaxException e ) {
                throw new IOException( e );
            }

            return in;
        }

        @Override
        public void setPreferencesInputStream( final InputStream in ) throws IOException {
            try {
                final Map<String,List<String>> headers = new HashMap<String,List<String>>();
                final StringBuilder cookieBuilder = new StringBuilder(4096);
                cookieBuilder.append( COOKIE_NAME );
                cookieBuilder.append( "=" );
                cookieBuilder.append( HexUtils.encodeBase64( IOUtils.slurpStream( in ), true ) );
                cookieBuilder.append( "; Expires=Fri, 01-Jan-2038 00:00:00 GMT; Path=" );
                cookieBuilder.append( COOKIE_PATH );
                cookieBuilder.append( "; Secure" );
                headers.put( HttpConstants.HEADER_SET_COOKIE, Collections.singletonList( cookieBuilder.toString() ) );
                cookieHandler.put( url.toURI(), headers );
            } catch ( URISyntaxException e ) {
                throw new IOException( e );
            } 
        }
    }
}

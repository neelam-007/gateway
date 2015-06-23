package com.l7tech.security.prov.rsa;

import com.l7tech.common.io.WhirlycacheFactory;
import com.l7tech.util.SyspropUtil;
import com.rsa.jsse.SSLSessionCache;
import com.whirlycott.cache.Cache;

import javax.net.ssl.SSLContext;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A custom SSL-J session cache that will not fill all of memory with SSL sessions.
 */
public class NonLeakingSsljSessionCache extends SSLSessionCache {
    private static final Logger logger = Logger.getLogger( NonLeakingSsljSessionCache.class.getName() );

    public static final String PROP_MAX_CACHE_SIZE = "com.l7tech.security.sslj.sessionCache.maxEntries";

    // Cache from ByteBuffer -> session byte array
    private final Cache sessionCache;

    // Currently we will have only a single global cache, shared for all inbound and outbound SSL-J TLS connections
    // TODO provide a way to designate sharing contexts (one possibility: have one cache for inbound and a different one for outbound)
    private final byte[] cacheSharingContext = { 1 };

    public NonLeakingSsljSessionCache() {
        int maxEntries = SyspropUtil.getInteger( PROP_MAX_CACHE_SIZE, 250000 );
        if ( logger.isLoggable( Level.FINE ) )
            logger.fine( "Creating SSL-J TLS session cache with room for up to " + maxEntries + " sessions" );
        sessionCache = WhirlycacheFactory.createCache( "ssljSessionCache", maxEntries, 71, WhirlycacheFactory.POLICY_LRU );
    }

    private static class KeyObject {
        private final byte[] bytes;

        private KeyObject( byte[] bytes ) {
            this.bytes = bytes;
        }

        @SuppressWarnings( "RedundantIfStatement" )
        @Override
        public boolean equals( Object o ) {
            if ( this == o ) return true;
            if ( o == null || getClass() != o.getClass() ) return false;

            KeyObject keyObject = (KeyObject) o;

            if ( !Arrays.equals( bytes, keyObject.bytes ) ) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return bytes != null ? Arrays.hashCode( bytes ) : 0;
        }
    }

    private static KeyObject asKey( byte[] key ) {
        return new KeyObject( key );
    }

    @Override
    public void putSession( byte[] sessionId, byte[] sessionInfo ) {
        sessionCache.store( asKey( sessionId ), sessionInfo );
    }

    @Override
    public byte[] getSession( byte[] sessionId ) {
        return (byte[]) sessionCache.retrieve( asKey( sessionId ) );
    }

    @Override
    public void removeSession( byte[] sessionId ) {
        sessionCache.remove( asKey( sessionId ) );
    }

    public void attach( SSLContext sslContext ) {
        SSLSessionCache.setExternalSessionCache( sslContext, cacheSharingContext, this );
    }
}

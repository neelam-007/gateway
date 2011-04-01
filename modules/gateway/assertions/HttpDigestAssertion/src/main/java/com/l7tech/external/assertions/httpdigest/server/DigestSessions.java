/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.external.assertions.httpdigest.server;

import com.l7tech.util.Background;
import com.l7tech.util.HexUtils;

import java.security.SecureRandom;
import java.util.*;

/**
 * @author alex (moved from package com.l7tech.server.policy.assertion.credential)
 */
public class DigestSessions {
    private static final Random random = new SecureRandom();

    public static DigestSessions getInstance() {
        if ( _instance == null ) _instance = new DigestSessions();
        return _instance;
    }

    public boolean use( String nonce ) {
        NonceInfo info;
        synchronized( _nonceInfos ) {
            info = (NonceInfo)_nonceInfos.get( nonce );
        }
        if ( info == null ) return false;
        long currentTime = System.currentTimeMillis();
        synchronized( info ) {
            if ( info._expires <= currentTime ) return false;
            if ( info._uses++ > info._maxUses ) return false;
        }
        return true;
    }

    public void invalidate( String nonce ) {
        synchronized( _nonceInfos ) {
            _nonceInfos.remove( nonce );
        }
    }

    /**
     * Generate a unique token. The token is generated according to the
     * following pattern. NonceToken = Base64 ( 64 random bytes ).
     *
     */
    public String generate(int timeout, int maxUses) {
        long currentTime = System.currentTimeMillis();

        byte[] nonceBytes = new byte[64];
        random.nextBytes(nonceBytes);
        String nonceValue = HexUtils.encodeBase64(nonceBytes, true);

        // Updating the value in the nonce hashtable
        synchronized( _nonceInfos ) {
            _nonceInfos.put( nonceValue, new NonceInfo( nonceValue, currentTime + timeout, maxUses ) );
        }

        return nonceValue;
    }

    private static class NonceInfo implements Comparable {
        public NonceInfo( String nonce, long expires, int maxUses ) {
            _nonce = nonce;
            _expires = expires;
            _maxUses = maxUses;
            _uses = 1;
        }

        public int hashCode() {
            return _nonce.hashCode();
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof NonceInfo)) return false;

            final NonceInfo nonceInfo = (NonceInfo) o;

            if (_nonce != null ? !_nonce.equals(nonceInfo._nonce) : nonceInfo._nonce != null) return false;

            return true;
        }

        /** Maintain NonceInfos in ascending order of expiry */
        public int compareTo(Object o) {
            NonceInfo other = (NonceInfo)o;
            if ( this._expires > other._expires ) return 1;
            if ( this._expires < other._expires ) return -1;
            return 0;
        }

        String _nonce;
        long _expires;
        volatile int _uses;
        int _maxUses;
    }

    private DigestSessions() {
        Background.scheduleRepeated(new ExpireTask(), 5 * 60 * 1000, 5 * 60 * 1000);
    }

    private class ExpireTask extends TimerTask {
        public void run() {
            synchronized ( _nonceInfos ) {
                long now = System.currentTimeMillis();
                for (Iterator i = _nonceInfos.entrySet().iterator(); i.hasNext();) {
                    Map.Entry entry = (Map.Entry)i.next();
                    NonceInfo info = (NonceInfo)entry.getValue();
                    if (info._expires > now)
                        i.remove();
                }
            }
        }
    }

    private static DigestSessions _instance;
    private Map _nonceInfos = new HashMap();
}

/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.credential;

import com.l7tech.common.message.Message;
import com.l7tech.common.util.Background;
import com.l7tech.common.util.HexUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimerTask;

/**
 * @author alex
 */
public class DigestSessions {
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
     * following pattern. NOnceToken = Base64 ( MD5 ( client-IP ":"
     * time-stamp ":" private-key ) ).
     *
     * @param request HTTP Servlet request
     */
    public String generate( Message request, int timeout, int maxUses ) {
        long currentTime = System.currentTimeMillis();

        String nonceValue = request.getTcpKnob().getRemoteAddress() + ":" +
            currentTime + ":" + NONCEKEY;

        byte[] buffer = HexUtils.getMd5().digest(nonceValue.getBytes());
        nonceValue = HexUtils.encodeMd5Digest(buffer);

        // Updating the value in the nonce hashtable
        synchronized( _nonceInfos ) {
            _nonceInfos.put( nonceValue, new NonceInfo( nonceValue, currentTime + timeout, maxUses ) );
        }

        return nonceValue;
    }

    private class NonceInfo implements Comparable {
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
        Background.schedule(new ExpireTask(), 5 * 60 * 1000, 5 * 60 * 1000);
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

    private static final String NONCEKEY = "Layer7-SSG-DigestNonceKey";
    private static DigestSessions _instance;
    private Map _nonceInfos = new HashMap();
}

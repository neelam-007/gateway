/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.credential;

import com.l7tech.message.Request;
import com.l7tech.common.util.HexUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.HashMap;

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
    public String generate( Request request, int timeout, int maxUses ) {
        long currentTime = System.currentTimeMillis();

        String nonceValue = request.getTransportMetadata().getParameter( Request.PARAM_REMOTE_ADDR ) + ":" +
            currentTime + ":" + NONCEKEY;

        byte[] buffer = _md5.digest(nonceValue.getBytes());
        nonceValue = HexUtils.encodeMd5Digest(buffer);

        // Updating the value in the nonce hashtable
        synchronized( _nonceInfos ) {
            _nonceInfos.put( nonceValue, new NonceInfo( currentTime + timeout, maxUses ) );
        }

        return nonceValue;
    }

    private class NonceInfo {
        public NonceInfo( long expires, int maxUses ) {
            _expires = expires;
            _maxUses = maxUses;
            _uses = 1;
        }

        long _expires;
        volatile int _uses;
        int _maxUses;
    }

    private DigestSessions() {
        try {
            _md5 = MessageDigest.getInstance( "MD5" );
        } catch ( NoSuchAlgorithmException e ) {
            throw new RuntimeException( e );
        }
    }

    private static final String NONCEKEY = "Layer7-SSG-DigestNonceKey";
    private static DigestSessions _instance;
    private Map _nonceInfos = new HashMap(37);
    private MessageDigest _md5 = null;
}

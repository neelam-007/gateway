/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common;

import java.io.Serializable;

/**
 * Immutable.
 *
 * @author alex
 * @version $Revision$
 */
public class RequestId implements Comparable, Serializable {
    public RequestId( int serverId, long generation, long sequence ) {
        _serverId = serverId;
        _generation = generation;
        _sequence = sequence;
    }

    public int getServerId() {
        return _serverId;
    }

    public long getGeneration() {
        return _generation;
    }

    public long getSequence() {
        return _sequence;
    }

    public int compareTo(Object o) {
        RequestId other = (RequestId)o;
        if ( _serverId > other._serverId ) return 1;
        if ( _serverId < other._serverId ) return -1;
        if ( _generation > other._generation ) return 1;
        if ( _generation < other._generation ) return -1;
        if ( _sequence > other._sequence ) return 1;
        if ( _sequence < other._sequence ) return -1;
        return 0;
    }

    public boolean equals( Object o ) {
        RequestId other = (RequestId)o;
        return ( _serverId == other._serverId &&
                _generation == other._generation &&
                _sequence == other._sequence );
    }

    public int hashCode() {
        return (int)(_serverId * 103 + _generation * 53 + _sequence);
    }

    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append( paddedHex( _serverId ) );
        result.append( "-" );
        result.append( paddedHex( _generation ) );
        result.append( "-" );
        result.append( paddedHex( _sequence ) );
        return result.toString();
    }

    private String paddedHex( long num ) {
        return paddedHex( Long.toHexString( num ), 16 );
    }

    private String paddedHex( int num ) {
        return paddedHex( Integer.toHexString( num ), 8 );
    }

    private String paddedHex( String num, int len ) {
        StringBuffer result = new StringBuffer();
        while ( result.length() < ( len - num.length() )) {
            result.append("0");
        }
        result.append( num );
        return result.toString();
    }

    private int _serverId;
    private long _generation;
    private long _sequence;
}

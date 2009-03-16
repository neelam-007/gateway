/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.gateway.common;

import java.io.Serializable;

/**
 * Immutable.
 *
 * @author alex
 * @version $Revision$
 */
public class RequestId implements Comparable, Serializable {
    public RequestId(long generation, long sequence) {
        _generation = generation;
        _sequence = sequence;
    }

    /**
     * construct a ReqId from it's parsed form
     * @param arg a string representing a RequestId as per RequestId.toString()
     */
    public RequestId(String arg) {
        int separatorpos = arg.indexOf(PARSE_SEPARATOR);
        if (separatorpos < 0) throw new IllegalArgumentException("the string " + arg +
                                                                 " does not contain a RequestId");
        String genpartstr = arg.substring(0, separatorpos);
        String seqpartstr = arg.substring(separatorpos+1);
        _generation = Long.parseLong(genpartstr, 16);
        _sequence = Long.parseLong(seqpartstr, 16);
    }

    public long getGeneration() {
        return _generation;
    }

    public long getSequence() {
        return _sequence;
    }

    public int compareTo(Object o) {
        RequestId other = (RequestId)o;
        if ( _generation > other._generation ) return 1;
        if ( _generation < other._generation ) return -1;
        if ( _sequence > other._sequence ) return 1;
        if ( _sequence < other._sequence ) return -1;
        return 0;
    }

    public boolean equals( Object o ) {
        RequestId other = (RequestId)o;
        return (_generation == other._generation &&
                _sequence == other._sequence);
    }

    public int hashCode() {
        return (int)(_generation * 53 + _sequence);
    }

    public String toString() {
        StringBuilder result = new StringBuilder(34);
        paddedHex(result, _generation);
        result.append(PARSE_SEPARATOR);
        paddedHex(result, _sequence);
        return result.toString();
    }

    private void paddedHex(StringBuilder in, long num ) {
        paddedHex( in, Long.toHexString( num ), 16 );
    }

    private void paddedHex( StringBuilder in, String num, int len ) {
        final int wantlen = len - num.length();
        while (in.length() < wantlen)
            in.append('0');
        in.append( num );
    }

    private long _generation;
    private long _sequence;
    public static final char PARSE_SEPARATOR = '-';
}

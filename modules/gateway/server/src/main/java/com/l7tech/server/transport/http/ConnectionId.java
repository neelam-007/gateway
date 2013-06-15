package com.l7tech.server.transport.http;

import java.io.Serializable;

/**
 * The identity for the current connection (Socket)
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 * @see com.l7tech.gateway.common.RequestId RequestId From which this is shamelessly stolen.
 */
public class ConnectionId implements Comparable, Serializable {

    //- PUBLIC

    /**
     * Construct a ConnectionId with the given info.
     *
     * @param generation The somewhat unique generation.
     * @param sequence   The classloader unique sequence.
     */
    public ConnectionId(long generation, long sequence) {
        _generation = generation;
        _sequence = sequence;
    }

    /**
     * Construct a ConnectionId from it's parsed form
     *
     * @param arg a string representing a RequestId as per RequestId.toString()
     */
    public ConnectionId(String arg) {
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
        ConnectionId other = (ConnectionId)o;
        if ( _generation > other._generation ) return 1;
        if ( _generation < other._generation ) return -1;
        if ( _sequence > other._sequence ) return 1;
        if ( _sequence < other._sequence ) return -1;
        return 0;
    }

    public boolean equals( Object o ) {
        if (o == null) return false;
        ConnectionId other = (ConnectionId)o;
        return (_generation == other._generation &&
                _sequence == other._sequence);
    }

    public int hashCode() {
        return (int)(_generation * 53 + _sequence);
    }

    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append( paddedHex( _generation ) );
        result.append(PARSE_SEPARATOR);
        result.append( paddedHex( _sequence ) );
        return result.toString();
    }

    /**
     * Set the ConnectionId for the the current thread.
     *
     * @param connectionId The id to use (may be null)
     */
    public static void setConnectionId(ConnectionId connectionId) {
        tlConnectionId.set(connectionId);
    }

    /**
     * Get the ConnectionId associated with the current thread.
     *
     * @return The ConnectionId (null if not set)
     */
    public static ConnectionId getConnectionId() {
        return (ConnectionId) tlConnectionId.get();
    }

    //- PRIVATE

    private static final long serialVersionUID = 1L;
    private static final char PARSE_SEPARATOR = '-';
    private static final ThreadLocal tlConnectionId = new ThreadLocal();

    private final long _generation;
    private final long _sequence;

    private String paddedHex( long num ) {
        return paddedHex( Long.toHexString( num ), 16 );
    }

    private String paddedHex( String num, int len ) {
        StringBuffer result = new StringBuffer();
        while ( result.length() < ( len - num.length() )) {
            result.append("0");
        }
        result.append( num );
        return result.toString();
    }
}

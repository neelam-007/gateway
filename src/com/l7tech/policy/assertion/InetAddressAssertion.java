/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author alex
 * @version $Revision$
 */
public class InetAddressAssertion extends Assertion {
    public void setNetwork( String network ) throws PolicyAssertionException {
        byte[] netBytes = new byte[4];
        int ppos1 = -1, ppos2 = 0;
        String sb;
        int b;

        for ( int i = 0; i < netBytes.length; i++ ) {
            ppos2 = network.indexOf( ".", ppos1 + 1 );
            sb = network.substring( ppos1 + 1, ppos2 );

            try {
                b = Integer.parseInt( sb );
                if ( b >= 0 && b < 256 )
                    netBytes[i] = (byte)(b & 0x7f);
                else
                    throw new PolicyAssertionException( "Invalid octet value " + b );

                InetAddress ia = InetAddress.getByAddress( netBytes );
            } catch ( UnknownHostException uhe ) {
                throw new PolicyAssertionException( uhe.getMessage(), uhe );
            } catch ( NumberFormatException nfe ) {
                throw new PolicyAssertionException( nfe.getMessage(), nfe );
            }
        }

        _network = netBytes;
    }

    public void setNetwork( byte[] network ) {
        _network = network;
    }

    public byte[] getNetwork() {
        return _network;
    }

    public void setBits( int bits ) {
        _bits = bits;
    }

    public int getBits() {
        return _bits;
    }

    protected byte[] _network;
    protected int _bits;
}

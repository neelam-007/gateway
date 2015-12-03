package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ISO8583;

/**
 * Created with IntelliJ IDEA.
 * User: abjorge
 * Date: 03/06/13
 * Time: 10:35 AM
 * To change this template use File | Settings | File Templates.
 */
public class ISO8583Constants {

    public static final int BITMAP_LENGTH = 8; //length of a single bitmap in bytes
    public static final int BYTE_LENGTH = 8; //number of bits in a byte
    public static final int HEX_VALUE_LENGTH = 2; //number of characters in a hex value
    public static final int BITS_IN_HEX_VALUE = BYTE_LENGTH / HEX_VALUE_LENGTH; //number of bits in a hex value or 4 bits
}

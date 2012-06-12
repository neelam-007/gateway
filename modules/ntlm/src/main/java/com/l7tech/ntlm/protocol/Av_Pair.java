package com.l7tech.ntlm.protocol;

import com.l7tech.util.Charsets;
import jcifs.util.Encdec;

import java.io.UnsupportedEncodingException;

public class Av_Pair {
    public enum MsvAvType {
        MsvAvEOL(0),
        MsvAvNbComputerName(1),
        MsvAvNbDomainName(2),
        MsvAvDnsComputerName(3),
        MsvAvDnsDomainName(4),
        MsvAvDnsTreeName(5),
        MsvAvFlags(6),
        MsvAvTimestamp(7),
        MsAvRestrictions(8),
        MsvAvTargetName(9),
        MsvChannelBindings(10);

        int value;

        private MsvAvType(int val) {
            value = val;
        }
    }

    MsvAvType type;
    String name;

    public Av_Pair(MsvAvType type, String name) {
        this.type = type;
        this.name = name;
    }

    public byte[] toByteArray() throws UnsupportedEncodingException {
        byte[] data = name != null ? name.getBytes(Charsets.UTF16LE) : "".getBytes(Charsets.UTF16LE);
        byte[] bytes =  new byte[data.length + 4];
        Encdec.enc_uint16le((short) type.value, bytes, 0);
        Encdec.enc_uint16le((short) data.length, bytes, 2);
        System.arraycopy(data, 0, bytes, 4, data.length);
        return bytes;
    }

}

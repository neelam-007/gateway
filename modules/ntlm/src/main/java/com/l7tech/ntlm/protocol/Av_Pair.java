package com.l7tech.ntlm.protocol;

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
        byte[] data = this.name != null ? this.name.getBytes("UnicodeLittleUnmarked") : "".getBytes("UnicodeLittleUnmarked");
        byte[] ret =  new byte[data.length + 4];
        Encdec.enc_uint16le((short) this.type.value, ret, 0);
        Encdec.enc_uint16le((short) data.length, ret, 2);
        System.arraycopy(data, 0, ret, 4, data.length);
        return ret;
    }

}

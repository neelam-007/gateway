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
    Av_Pair next;

    public Av_Pair(MsvAvType type, String name, Av_Pair next) {
        this.type = type;
        this.name = name;
        this.next = next;
    }

    public byte[] toByteArray(int off) throws UnsupportedEncodingException {
        byte[] ret;
        byte[] data;
        if (this.next == null) {
            this.type.value = 0;
            data = new byte[0];
            ret = new byte[off + 4];
        } else {
            data = this.name != null ? this.name.getBytes("UnicodeLittleUnmarked") : "".getBytes("UnicodeLittleUnmarked");
            ret = this.next.toByteArray(off + 4 + data.length);
        }
        Encdec.enc_uint16le((short) this.type.value, ret, off);
        Encdec.enc_uint16le((short) data.length, ret, off + 2);
        System.arraycopy(data, 0, ret, off + 4, data.length);

        return ret;
    }
}

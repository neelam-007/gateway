package com.l7tech.ntlm.util;

import jcifs.dcerpc.ndr.NdrBuffer;
import jcifs.dcerpc.ndr.NdrException;
import jcifs.dcerpc.rpc;

/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 * Date: 3/14/12
 */
public final class DceRpcUtil {
    
    public static NdrBuffer encode_rpc_string_buffer(NdrBuffer dst, rpc.unicode_string unicode_string) {
        if (unicode_string.buffer != null) {
            dst = dst.deferred;
            int l = unicode_string.length / 2;
            int s = unicode_string.maximum_length / 2;
            dst.enc_ndr_long(s);
            dst.enc_ndr_long(0);
            dst.enc_ndr_long(l);
            int i = dst.index;
            dst.advance(2 * l);

            dst = dst.derive(i);
            for (int k = 0; k < l; k++) {
                dst.enc_ndr_short(unicode_string.buffer[k]);
            }
        }

        return dst;
    }
    
    public static NdrBuffer decode_rpc_string(NdrBuffer src, rpc.unicode_string unicode_string) throws NdrException {
        src = src.deferred;
        int s = src.dec_ndr_long();
        src.dec_ndr_long();
        int l = src.dec_ndr_long();
        int i = src.index;
        src.advance(2 * l);

        if (unicode_string.buffer == null) {
            if ((s < 0) || (s > 65535)) throw new NdrException("invalid buffer size");
            unicode_string.buffer = new short[s];
        }
        src = src.derive(i);
        for (int k = 0; k < l; k++) {
            unicode_string.buffer[k] = (short)src.dec_ndr_short();
        }
        return src;
    }
    
    public static byte[] decode_bytes(NdrBuffer src, byte[] bytes) throws NdrException {
        src = src.deferred;
        int s = src.dec_ndr_long();
        src.dec_ndr_long();
        int l = src.dec_ndr_long();
        int i = src.index;
        src.advance(1 * l);
        byte[] data = bytes;
        if (data == null) {
            if ((s < 0) || (s > 65535)) throw new NdrException("invalid byte array size");
            data = new byte[s];
        }
        src = src.derive(i);
        for (int k = 0; k < l; k++) {
            data[k] = (byte)src.dec_ndr_small();
        }
        return data;
    }

    private DceRpcUtil() {}
}

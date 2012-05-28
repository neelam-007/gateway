package com.l7tech.ntlm.netlogon;

import jcifs.dcerpc.ndr.NdrBuffer;
import jcifs.dcerpc.ndr.NdrException;
import jcifs.dcerpc.ndr.NdrObject;

/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 * Date: 3/12/12
 */
public class NetlogonAuthenticator extends NdrObject {

    public byte[] credential;
    public int timestamp;

    NetlogonAuthenticator() {

    }


    NetlogonAuthenticator(byte[] credential, int timestamp) {
        this.credential = credential;
        this.timestamp = timestamp;
    }

    public void encode(NdrBuffer buffer) throws NdrException {
        buffer.align(4);
        int size = 8;
        int index = buffer.index;
        buffer.advance(size);
        buffer.enc_ndr_long(timestamp);

        buffer = buffer.derive(index);
        for (int i = 0; i < size; i++)
            buffer.enc_ndr_small(credential[i]);
    }

    public void decode(NdrBuffer buffer) throws NdrException {
        buffer.align(4);
        int size = 8;
        int index = buffer.index;
        buffer.advance(size);
        timestamp = buffer.dec_ndr_long();

        if (credential == null) {
            if ((size < 0) || (size > 65535)) throw new NdrException("invalid buffer size");
            credential = new byte[size];
        }
        buffer = buffer.derive(index);
        for (int i = 0; i < size; i++) {
            credential[i] = (byte) buffer.dec_ndr_small();
        }
    }
}

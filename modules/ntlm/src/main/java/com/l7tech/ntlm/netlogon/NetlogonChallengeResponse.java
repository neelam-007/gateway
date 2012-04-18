package com.l7tech.ntlm.netlogon;

import jcifs.dcerpc.ndr.NdrBuffer;
import jcifs.dcerpc.ndr.NdrException;
import jcifs.dcerpc.ndr.NdrObject;

/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 * Date: 3/12/12
 */
public class NetlogonChallengeResponse extends NdrObject{
     public short length;
     public short maximumLength;
     public byte[] data;

    public NetlogonChallengeResponse() {
        this.length = 0;
        this.maximumLength = 0;
        this.data = null;
    }

     public NetlogonChallengeResponse(byte[] data) {
         length = (short)(data == null ? 0 : data.length);
         maximumLength = length;
         this.data = data;
     }
    
     public void encode(NdrBuffer dst)
       throws NdrException
     {
       dst.align(4);
       dst.enc_ndr_short(this.length);
       dst.enc_ndr_short(this.maximumLength);
       dst.enc_ndr_referent(this.data, 1);

       if (this.data != null) {
         dst = dst.deferred;
         int length = this.length;
         int size = this.maximumLength;
           dst.enc_ndr_long(size);
         dst.enc_ndr_long(0);
         dst.enc_ndr_long(length);
         int index = dst.index;
         dst.advance(1 * length);

         dst = dst.derive(index);
         for (int i = 0; i < length; i++)
           dst.enc_ndr_small(this.data[i]);
       }
     }
    
     public void decode(NdrBuffer src) throws NdrException {
       src.align(4);
       this.length = (short)src.dec_ndr_short();
       this.maximumLength = (short)src.dec_ndr_short();
       int ref = src.dec_ndr_long();

       if (ref != 0) {
         src = src.deferred;
         int size = src.dec_ndr_long();
         src.dec_ndr_long();
         int length = src.dec_ndr_long();
         int index = src.index;
         src.advance(1 * length);

         if (this.data == null) {
           if ((size < 0) || (size > 65535)) throw new NdrException("invalid buffer size");
           this.data = new byte[size];
         }
         src = src.derive(index);
         for (int i = 0; i < length; i++)
           this.data[i] = (byte)src.dec_ndr_small();
       }
     }
}

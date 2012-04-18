package com.l7tech.ntlm.netlogon;

import jcifs.dcerpc.ndr.NdrBuffer;
import jcifs.dcerpc.ndr.NdrException;
import jcifs.dcerpc.ndr.NdrObject;

/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 * Date: 3/12/12
 */
public class LmOwfPassword extends NdrObject{

     public byte[] key;

     @Override
     public void encode(NdrBuffer dst) throws NdrException {
       dst.align(1);
       int size = 8;
       int index = dst.index;
       dst.advance(1 * size);

       dst = dst.derive(index);
       for (int i = 0; i < size; i++)
         dst.enc_ndr_small(this.key[i]);
     }

     @Override
     public void decode(NdrBuffer src) throws NdrException {
       src.align(1);
       int size = 8;
       int index = src.index;
       src.advance(1 * size);

       if (this.key == null) {
         if ((size < 0) || (size > 65535)) throw new NdrException("invalid buffer size");
         this.key = new byte[size];
       }
       src = src.derive(index);
       for (int i = 0; i < size; i++)
         this.key[i] = (byte)src.dec_ndr_small();
     }
}

package com.l7tech.ntlm.netlogon;

import jcifs.dcerpc.ndr.NdrBuffer;
import jcifs.dcerpc.ndr.NdrException;
import jcifs.dcerpc.ndr.NdrObject;

/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 * Date: 3/12/12
 */
public class UserSessionKey extends NdrObject{
     public byte[] key;

     @Override
     public void encode(NdrBuffer buffer) throws NdrException {
       buffer.align(1);
       int size = 16;
       int index = buffer.index;
       buffer.advance(1 * size);

       buffer = buffer.derive(index);
       for (int i = 0; i < size; i++)
         buffer.enc_ndr_small(key[i]);
     }


     @Override
     public void decode(NdrBuffer buffer) throws NdrException {
       buffer.align(1);
       int size = 16;
       int index = buffer.index;
       buffer.advance(1 * size);

       if (key == null) {
         if ((size < 0) || (size > 65535)) throw new NdrException("invalid array size");
         key = new byte[size];
       }
       buffer = buffer.derive(index);
       for (int i = 0; i < size; i++)
         key[i] = (byte)buffer.dec_ndr_small();
     }
}

package com.l7tech.ntlm.netlogon;

import jcifs.dcerpc.ndr.NdrBuffer;
import jcifs.dcerpc.ndr.NdrException;
import jcifs.dcerpc.ndr.NdrObject;
import jcifs.dcerpc.rpc;

/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 * Date: 3/12/12
 */
public class NetlogonSidAndAttributes extends NdrObject{
     public rpc.sid_t sid;
     public int attributes;

     @Override
     public void encode(NdrBuffer dst) throws NdrException {
       dst.align(4);
       dst.enc_ndr_referent(sid, 1);
       dst.enc_ndr_long(attributes);

       if (sid != null) {
         dst = dst.deferred;
         sid.encode(dst);
       }
     }

     @Override
     public void decode(NdrBuffer src) throws NdrException {
       src.align(4);
       int ref = src.dec_ndr_long();
       attributes = src.dec_ndr_long();

       if (ref != 0) {
         if (sid == null) {
           sid = new rpc.sid_t();
         }
         src = src.deferred;
         sid.decode(src);
       }
     }
}

package com.l7tech.ntlm.netlogon;

import jcifs.dcerpc.DcerpcMessage;
import jcifs.dcerpc.ndr.NdrBuffer;
import jcifs.dcerpc.ndr.NdrException;

/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 * Date: 3/8/12
 */
class NetrServerReqChallenge extends DcerpcMessage {
     public int retval;
     public String primaryName;
     public String computerName;
     public byte[] clientChallenge;
     public byte[] serverChallenge;

     public int getOpnum()
     {
       return 4;
     }
    
     public NetrServerReqChallenge(String primaryName, String computerName, byte[] clientChallenge, byte[] serverChallenge)
     {
       this.primaryName = primaryName;
       this.computerName = computerName;
       this.clientChallenge = clientChallenge;
       this.serverChallenge = serverChallenge;
       this.ptype = 0;
       this.flags = 3;
     }
    
     public void encode_in(NdrBuffer buffer) throws NdrException {
       buffer.enc_ndr_referent(primaryName, 1);
       if (primaryName != null) {
           buffer.enc_ndr_string(primaryName);
       }

         buffer.enc_ndr_string(computerName);
       int size = 8;
       int index = buffer.index;
       buffer.advance(1 * size);
       buffer = buffer.derive(index);
       for (int i = 0; i < size; i++)
         buffer.enc_ndr_small(clientChallenge[i]);
     }
    
     public void decode_out(NdrBuffer buffer) throws NdrException {
       int size = 8;
       int index = buffer.index;
       buffer.advance(1 * size);
       if (serverChallenge == null) {
         if ((size < 0) || (size > 65535)) throw new NdrException("invalid buffer size");
         serverChallenge = new byte[size];
       }
       buffer = buffer.derive(index);
       for (int i = 0; i < size; i++) {
         serverChallenge[i] = (byte)buffer.dec_ndr_small();
       }
       retval = buffer.dec_ndr_long();
     }
}

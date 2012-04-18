package com.l7tech.ntlm.netlogon;

import jcifs.dcerpc.DcerpcMessage;
import jcifs.dcerpc.ndr.NdrBuffer;
import jcifs.dcerpc.ndr.NdrException;

/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 * Date: 3/9/12
 */
public class NetrServerAuthenticate2 extends DcerpcMessage {

     public int retval;
     public String primaryName;
     public String accountName;
     public short secureChannelType;
     public String computerName;
     public byte[] clientCredential;
     public byte[] serverCredential;
     public int negotiateFlags;
    
     public int getOpnum()
     {
       return 15;
     }
    
     public NetrServerAuthenticate2(String primaryName, String accountName, short secureChannelType, String computerName, byte[] clientCredential, byte[] serverCredential, int negotiateFlags) {
       this.primaryName = primaryName;
       this.accountName = accountName;
       this.secureChannelType = secureChannelType;
       this.computerName = computerName;
       this.clientCredential = clientCredential;
       this.serverCredential = serverCredential;
       this.negotiateFlags = negotiateFlags;
       this.ptype = 0;
       this.flags = 3;
     }

     @Override
     public void encode_in(NdrBuffer buffer) throws NdrException {
       NdrBuffer start = buffer;
       buffer.enc_ndr_referent(primaryName, 1);
       if (primaryName != null) {
         buffer.enc_ndr_string(primaryName);
       }

       buffer.enc_ndr_string(accountName);
       buffer.enc_ndr_short(secureChannelType);
       buffer.enc_ndr_string(computerName);
       int size = 8;
       int index = buffer.index;
       buffer.advance(1 * size);
         buffer = buffer.derive(index);
       for (int i = 0; i < size; i++) {
         buffer.enc_ndr_small(clientCredential[i]);
       }
       buffer.enc_ndr_long(negotiateFlags);
       start.setIndex(buffer.getIndex());
     }

     @Override
     public void decode_out(NdrBuffer buffer) throws NdrException {
       int size = 8;
       int index = buffer.index;
       buffer.advance(1 * size);
       if (serverCredential == null) {
         if ((size < 0) || (size > 65535)) throw new NdrException("invalid array size");
         serverCredential = new byte[size];
       }
       buffer = buffer.derive(index);
       for (int i = 0; i < size; i++) {
         serverCredential[i] = (byte)buffer.dec_ndr_small();
       }
       negotiateFlags = buffer.dec_ndr_long();
       retval = buffer.dec_ndr_long();
     }
   }

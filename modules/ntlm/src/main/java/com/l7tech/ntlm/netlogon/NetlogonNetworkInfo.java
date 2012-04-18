package com.l7tech.ntlm.netlogon;

import com.l7tech.ntlm.util.DceRpcUtil;
import jcifs.dcerpc.ndr.NdrBuffer;
import jcifs.dcerpc.ndr.NdrException;
import jcifs.dcerpc.ndr.NdrObject;
import jcifs.dcerpc.rpc;

/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 */
public class NetlogonNetworkInfo extends NdrObject {
     public NetlogonLogonIdentityInfo identityInfo;
     public byte[] lmChallenge;
     public NetlogonChallengeResponse ntChallengeResponse;
     public NetlogonChallengeResponse lmChallengeResponse;

     @Override
     public void encode(NdrBuffer dst) throws NdrException {
       dst.align(4);
       dst.enc_ndr_short(identityInfo.logonDomainName.length);
       dst.enc_ndr_short(identityInfo.logonDomainName.maximum_length);
       dst.enc_ndr_referent(identityInfo.logonDomainName.buffer, 1);
       dst.enc_ndr_long(identityInfo.parameterControl);
       dst.enc_ndr_long(identityInfo.logonIdLow);
       dst.enc_ndr_long(identityInfo.logonIdHigh);
       dst.enc_ndr_short(identityInfo.userName.length);
       dst.enc_ndr_short(identityInfo.userName.maximum_length);
       dst.enc_ndr_referent(identityInfo.userName.buffer, 1);
       dst.enc_ndr_short(identityInfo.workstation.length);
       dst.enc_ndr_short(identityInfo.workstation.maximum_length);
       dst.enc_ndr_referent(identityInfo.workstation.buffer, 1);
       int challengeSize = 8;
       int challengeIndex = dst.index;
       dst.advance(1 * challengeSize);
       dst.enc_ndr_short(ntChallengeResponse.length);
       dst.enc_ndr_short(ntChallengeResponse.maximumLength);
       dst.enc_ndr_referent(ntChallengeResponse.data, 1);
       dst.enc_ndr_short(lmChallengeResponse.length);
       dst.enc_ndr_short(lmChallengeResponse.maximumLength);
       dst.enc_ndr_referent(lmChallengeResponse.data, 1);
       dst = DceRpcUtil.encode_rpc_string_buffer(dst, identityInfo.logonDomainName);
       dst = DceRpcUtil.encode_rpc_string_buffer(dst, identityInfo.userName);
       dst = DceRpcUtil.encode_rpc_string_buffer(dst, identityInfo.workstation);
       dst = dst.derive(challengeIndex);
       for (int i = 0; i < challengeSize; i++) {
         dst.enc_ndr_small(lmChallenge[i]);
       }
       if (ntChallengeResponse.data != null) {
         dst = dst.deferred;
         int ntDataLength = ntChallengeResponse.length;
         int ntDataSize = ntChallengeResponse.maximumLength;
         dst.enc_ndr_long(ntDataSize);
         dst.enc_ndr_long(0);
         dst.enc_ndr_long(ntDataLength);
         int ntDataIndex = dst.index;
         dst.advance(1 * ntDataLength);

         dst = dst.derive(ntDataIndex);
         for (int _i = 0; _i < ntDataLength; _i++) {
           dst.enc_ndr_small(ntChallengeResponse.data[_i]);
         }
       }
       if (lmChallengeResponse.data != null) {
         dst = dst.deferred;
         int lmDataLength = lmChallengeResponse.length;
         int lmDataSize = lmChallengeResponse.maximumLength;
         dst.enc_ndr_long(lmDataSize);
         dst.enc_ndr_long(0);
         dst.enc_ndr_long(lmDataLength);
         int lmDataIndex = dst.index;
         dst.advance(1 * lmDataLength);

         dst = dst.derive(lmDataIndex);
         for (int i = 0; i < lmDataLength; i++)
           dst.enc_ndr_small(lmChallengeResponse.data[i]);
       }
     }
    
     public void decode(NdrBuffer src) throws NdrException {
      src.align(4);
       src.align(4);
       if (identityInfo == null) {
         identityInfo = new NetlogonLogonIdentityInfo();
       }
       src.align(4);
       if (identityInfo.logonDomainName == null) {
         identityInfo.logonDomainName = new rpc.unicode_string();
       }
       identityInfo.logonDomainName.length = (short)src.dec_ndr_short();
       identityInfo.logonDomainName.maximum_length = (short)src.dec_ndr_short();
       int identityInfoDomainNameBufferRef = src.dec_ndr_long();
       identityInfo.parameterControl = src.dec_ndr_long();
       identityInfo.logonIdLow = src.dec_ndr_long();
       identityInfo.logonIdHigh = src.dec_ndr_long();
       src.align(4);
       if (identityInfo.userName == null) {
         identityInfo.userName = new rpc.unicode_string();
       }
       identityInfo.userName.length = (short)src.dec_ndr_short();
       identityInfo.userName.maximum_length = (short)src.dec_ndr_short();
       int identityInfoUserNameBufferRef = src.dec_ndr_long();
       src.align(4);
       if (identityInfo.workstation == null) {
         identityInfo.workstation = new rpc.unicode_string();
       }
       identityInfo.workstation.length = (short)src.dec_ndr_short();
       identityInfo.workstation.maximum_length = (short)src.dec_ndr_short();
       int identityInfoWorkstationBufferRef = src.dec_ndr_long();
       int challengeSize = 8;
       int challengeIndex = src.index;
       src.advance(1 * challengeSize);
       src.align(4);
       if (ntChallengeResponse == null) {
         ntChallengeResponse = new NetlogonChallengeResponse();
       }
       ntChallengeResponse.length = (short)src.dec_ndr_short();
       ntChallengeResponse.maximumLength = (short)src.dec_ndr_short();
       int ntDataRef = src.dec_ndr_long();
       src.align(4);
       if (lmChallengeResponse == null) {
         lmChallengeResponse = new NetlogonChallengeResponse();
       }
       lmChallengeResponse.length = (short)src.dec_ndr_short();
       lmChallengeResponse.maximumLength = (short)src.dec_ndr_short();
       int lmDataRef = src.dec_ndr_long();

       if (identityInfoDomainNameBufferRef != 0) {
         src = DceRpcUtil.decode_rpc_string(src, identityInfo.logonDomainName);
       }
       if (identityInfoUserNameBufferRef != 0) {
         src = DceRpcUtil.decode_rpc_string(src, identityInfo.userName);
       }
       if (identityInfoWorkstationBufferRef != 0) {
         src = DceRpcUtil.decode_rpc_string(src, identityInfo.workstation);
       }
       if (lmChallenge == null) {
         if ((challengeSize < 0) || (challengeSize > 65535)) throw new NdrException("invalid byte array size");
         lmChallenge = new byte[challengeSize];
       }
       src = src.derive(challengeIndex);
       for (int i = 0; i < challengeSize; i++) {
         lmChallenge[i] = (byte)src.dec_ndr_small();
       }
       if (ntDataRef != 0) {
         src = src.deferred;
         int ntDataSize = src.dec_ndr_long();
         src.dec_ndr_long();
         int ntDataLength = src.dec_ndr_long();
         int ntDataIndex = src.index;
         src.advance(1 * ntDataLength);

         if (ntChallengeResponse.data == null) {
           if ((ntDataSize < 0) || (ntDataSize > 65535)) throw new NdrException("invalid byte array size");
           ntChallengeResponse.data = new byte[ntDataSize];
         }
         src = src.derive(ntDataIndex);
         for (int i = 0; i < ntDataLength; i++) {
           ntChallengeResponse.data[i] = (byte)src.dec_ndr_small();
         }
       }
       if (lmDataRef != 0) {
         src = src.deferred;
         int lmDataSize = src.dec_ndr_long();
         src.dec_ndr_long();
         int lmDataLength = src.dec_ndr_long();
         int lmDataIndex = src.index;
         src.advance(1 * lmDataLength);

         if (lmChallengeResponse.data == null) {
           if ((lmDataSize < 0) || (lmDataSize > 65535)) throw new NdrException("invalid byte array size");
           lmChallengeResponse.data = new byte[lmDataSize];
         }
         src = src.derive(lmDataIndex);
         for (int i = 0; i < lmDataLength; i++)
           lmChallengeResponse.data[i] = (byte)src.dec_ndr_small();;
       }
     }
}

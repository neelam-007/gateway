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
        dst.advance(challengeSize);
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
            dst.advance(ntDataLength);

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
            dst.advance(lmDataLength);

            dst = dst.derive(lmDataIndex);
            for (int i = 0; i < lmDataLength; i++)
                dst.enc_ndr_small(lmChallengeResponse.data[i]);
        }
    }

    public void decode(NdrBuffer src) throws NdrException {
        //no need to decode. Leave it empty
    }
}

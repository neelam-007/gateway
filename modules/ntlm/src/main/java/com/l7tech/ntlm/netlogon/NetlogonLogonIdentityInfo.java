package com.l7tech.ntlm.netlogon;

import com.l7tech.ntlm.util.DceRpcUtil;
import jcifs.dcerpc.ndr.NdrBuffer;
import jcifs.dcerpc.ndr.NdrException;
import jcifs.dcerpc.ndr.NdrObject;
import jcifs.dcerpc.rpc;

/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 * Date: 3/12/12
 */
public class NetlogonLogonIdentityInfo extends NdrObject {
    public rpc.unicode_string logonDomainName;
    public int parameterControl;
    public int logonIdLow;
    public int logonIdHigh;
    public rpc.unicode_string userName;
    public rpc.unicode_string workstation;

    @Override
    public void encode(NdrBuffer dst) throws NdrException {
        dst.enc_ndr_short(logonDomainName.length);
        dst.enc_ndr_short(logonDomainName.maximum_length);
        dst.enc_ndr_referent(logonDomainName.buffer, 1);
        dst.enc_ndr_long(parameterControl);
        dst.enc_ndr_long(logonIdLow);
        dst.enc_ndr_long(logonIdHigh);
        dst.enc_ndr_short(userName.length);
        dst.enc_ndr_short(userName.maximum_length);
        dst.enc_ndr_referent(userName.buffer, 1);
        dst.enc_ndr_short(workstation.length);
        dst.enc_ndr_short(workstation.maximum_length);
        dst.enc_ndr_referent(workstation.buffer, 1);
        dst = DceRpcUtil.encode_rpc_string_buffer(dst, this.logonDomainName);
        dst = DceRpcUtil.encode_rpc_string_buffer(dst, this.userName);
        dst = DceRpcUtil.encode_rpc_string_buffer(dst, this.workstation);
    }

    @Override
    public void decode(NdrBuffer src) throws NdrException {
        //no need to decode. Leave it empty
    }
}

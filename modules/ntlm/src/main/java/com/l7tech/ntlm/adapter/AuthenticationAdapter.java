package com.l7tech.ntlm.adapter;

import com.l7tech.ntlm.protocol.AuthenticationManagerException;
import com.l7tech.ntlm.protocol.NtlmChallengeResponse;

import java.util.Map;

/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 * Date: 3/6/12
 */
public interface AuthenticationAdapter {
    /**
     * validates supplied credentials
     *
     * @param response
     * @param challenge
     * @param acct
     * @throws AuthenticationManagerException
     * @return object containing additional authorization data
     *         the type of data is determinate by the implementing adapter
     */
    public Object validate(NtlmChallengeResponse response, byte[] challenge, Map acct) throws AuthenticationManagerException;

}

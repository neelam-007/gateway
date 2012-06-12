package com.l7tech.ntlm.adapter;

import com.l7tech.ntlm.protocol.AuthenticationManagerException;
import com.l7tech.ntlm.protocol.NtlmServerResponse;

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
     * @param response - server response containing user supplied credentials
     * @param challenge - server challenge
     * @param acct -  user account authorization data
     * @throws AuthenticationManagerException
     * @return object containing additional authorization data
     * the type of data is determinate by the implementing adapter
     */
    public Object validateCredentials(NtlmServerResponse response, byte[] challenge, Map acct) throws AuthenticationManagerException;

}

package com.l7tech.ntlm.adapter;

import com.l7tech.ntlm.protocol.NtlmChallengeResponse;
import com.l7tech.ntlm.protocol.AuthenticationManagerException;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 */
public class LocalAuthenticationAdapter extends HashMap implements AuthenticationAdapter {

    public LocalAuthenticationAdapter(Map props) {
        super(props);
    }

    @Override
    public Object validate(NtlmChallengeResponse response, byte[] challenge, Map account) throws AuthenticationManagerException {
        if (response == null) {
            throw new AuthenticationManagerException(AuthenticationManagerException.Status.STATUS_INVALID_CREDENTIALS, "User credentials cannot be null!");
        }
        String domain = response.getDomain();
        String username = response.getUsername();

        String nbtName = (String) get("domain.netbios.name");
        String dnsName = (String) get("domain.dns.name");

        if ((domain == null) || (domain.trim().length() == 0)) {
            domain = nbtName;
        }

        String myusername = (String) get("my.username");
        String mypassword = (String) get("my.password");

        if ((domain.equalsIgnoreCase(nbtName)) || (domain.equalsIgnoreCase(dnsName))) {
            if (username.equalsIgnoreCase(myusername)) {

                NtlmChallengeResponse local = new NtlmChallengeResponse(response, domain, myusername, mypassword.toCharArray(), response.getTargetInformation());

                if (response.equals(local)) {

                    account.put("domain.dns.name", domain);
                    account.put("sAMAccountName", username);
                    byte[] sessionKey = new byte[16];
                    SecureRandom secureRandom = new SecureRandom();
                    secureRandom.nextBytes(sessionKey);
                    response.setSessionKey(sessionKey);
                    return sessionKey;
                }
            }
        }

        throw new AuthenticationManagerException(AuthenticationManagerException.Status.STATUS_INVALID_CREDENTIALS, "User domain/username incorrect");
    }
}

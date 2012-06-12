package com.l7tech.ntlm.adapter;

import com.l7tech.ntlm.protocol.NtlmServerResponse;
import com.l7tech.ntlm.protocol.AuthenticationManagerException;
import com.l7tech.ntlm.protocol.TestNtlmServerResponse;

import java.security.SecureRandom;
import java.util.Arrays;
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
    public Object validateCredentials(NtlmServerResponse response, byte[] challenge, Map account) throws AuthenticationManagerException {
        if (response == null) {
            throw new AuthenticationManagerException( "User credentials cannot be null!");
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

                NtlmServerResponse local = new TestNtlmServerResponse(response, domain, myusername, mypassword, response.getTargetInformation());

                if (Arrays.equals(response.getNtResponse(), local.getNtResponse())) {
                    account.put("domain.dns.name", domain);
                    account.put("sAMAccountName", username);
                    byte[] sessionKey = new byte[16];
                    SecureRandom secureRandom = new SecureRandom();
                    secureRandom.nextBytes(sessionKey);
                    return sessionKey;
                }
            }
        }

        throw new AuthenticationManagerException("User domain/username incorrect");
    }
}

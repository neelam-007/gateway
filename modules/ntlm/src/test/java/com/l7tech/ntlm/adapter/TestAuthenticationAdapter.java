package com.l7tech.ntlm.adapter;

import com.l7tech.ntlm.NtlmTestConstants;
import com.l7tech.ntlm.netlogon.NetLogon;
import com.l7tech.ntlm.protocol.AuthenticationManagerException;
import com.l7tech.ntlm.protocol.NtlmChallengeResponse;
import jcifs.smb.SID;
import jcifs.smb.SmbException;

import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: awitrisna
 */
public class TestAuthenticationAdapter extends NetLogon {

    private static Logger log = Logger.getLogger(TestAuthenticationAdapter.class.getName());

    public TestAuthenticationAdapter(Map properties) {
        super(properties);
    }

    @Override
    public Object validate(NtlmChallengeResponse response, byte[] challenge, Map acct) throws AuthenticationManagerException {
        if (!response.getUsername().equals(NtlmTestConstants.USER)) {
            throw new AuthenticationManagerException(AuthenticationManagerException.Status.STATUS_ACCOUNT_NOT_FOUND, "User account is not found.");
        }

        acct.put("sAMAccountName", response.getUsername());
        String uuid = UUID.randomUUID().toString();
        SID sid = null;
        try {
            sid = new SID("S-1-5-21-1496946806-2192648263-3843101252-1029");
        } catch (SmbException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        acct.put("primaryGroupSid", sid);
        response.setSessionKey(uuid.getBytes());
        return null;
    }

}

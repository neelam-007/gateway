package com.l7tech.security.token;

import jcifs.smb.SID;
import org.jaaslounge.decoding.DecodingException;
import org.jaaslounge.decoding.pac.PacSid;

import java.util.Map;

/**
 * Security token that represents NTLM authenticated user session
 *
 * @author ymoiseyenko
 */
public class NtlmToken implements SecurityToken{

    private final String ntlmData;

    private final String realm;

    private final Map<String,Object> params;

    public NtlmToken(String ntlmData, String realm, Map<String, Object> params) {
        this.ntlmData = ntlmData;
        this.realm = realm;
        this.params = params;
    }

    @Override
    public SecurityTokenType getType() {
        return SecurityTokenType.HTTP_NTLM;
    }

    public String getNtlmData() {
        return ntlmData;
    }

    public String getRealm() {
        return realm;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    /**
     * gets login name from the token
     * @return authenticated user account name
     */
    public String getLogin() {
        String login = null;
        if(params != null) {
            Map<String, Object> accountInfo = (Map<String, Object>)params.get("account.info");
            if(accountInfo != null) {
                login = (String)accountInfo.get("sAMAccountName");
            }
        }
        return login;
    }
    
    public String getUserSid() {
        String userSid = null;
        if(params != null) {
            Map<String, Object> accountInfo = (Map<String, Object>)params.get("account.info");
            if(accountInfo != null) {
                SID sid = (SID)accountInfo.get("userSid");
                byte[] binarySid=SID.toByteArray(sid);
                try {
                    userSid = (new PacSid(binarySid)).toString();
                } catch (DecodingException e) {
                   //just swallow
                }
            }
        }
        return userSid;
    }

}

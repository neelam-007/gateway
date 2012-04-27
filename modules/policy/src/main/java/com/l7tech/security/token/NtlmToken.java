package com.l7tech.security.token;

import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: ymoiseyenko
 * Date: 11/25/11
 * Time: 2:52 PM
 * To change this template use File | Settings | File Templates.
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
}

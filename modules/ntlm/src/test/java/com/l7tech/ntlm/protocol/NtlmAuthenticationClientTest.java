package com.l7tech.ntlm.protocol;

import com.l7tech.ntlm.adapter.LocalAuthenticationAdapter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;

/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 * Date: 2/27/12
 */
public class NtlmAuthenticationClientTest {

    NtlmAuthenticationClient fixture;


    @Before
    public void setUp() throws Exception {
        HashMap props = new HashMap();
        props.put("domain.netbios.name", "L7TECH");
        props.put("domain.dns.name", "l7tech.com");
        props.put("localhost.dns.name", "linux-12vk");
        props.put("localhost.netbios.name", "LINUX12-VK");
        props.put("my.username", "user");
        props.put("my.password", "password");

        fixture = new NtlmAuthenticationClient(props);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void shouldReturnDefaultFlags() {
        int expectedDefaultFlags = //0x628882B7;
                NtlmConstants.NTLMSSP_NEGOTIATE_KEY_EXCH |
                        NtlmConstants.NTLMSSP_NEGOTIATE_128 |
                        NtlmConstants.NTLMSSP_NEGOTIATE_VERSION |
                        NtlmConstants.NTLMSSP_NEGOTIATE_TARGET_INFO |
                        NtlmConstants.NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY |
                        NtlmConstants.NTLMSSP_NEGOTIATE_NTLM |
                        NtlmConstants.NTLMSSP_NEGOTIATE_LM_KEY |
                        NtlmConstants.NTLMSSP_REQUEST_TARGET |
                        NtlmConstants.NTLMSSP_NEGOTIATE_OEM |
                        NtlmConstants.NTLMSSP_NEGOTIATE_UNICODE |
                        NtlmConstants.NTLMSSP_NEGOTIATE_ALWAYS_SIGN |
                        NtlmConstants.NTLMSSP_NEGOTIATE_SEAL |
                        NtlmConstants.NTLMSSP_NEGOTIATE_SIGN;

        Map props = new HashMap();
        props.put("flags", "628882B7");

        fixture = new NtlmAuthenticationClient(props);
        assertEquals(expectedDefaultFlags, fixture.getNtlmSspFlags(false));

        props.put("flags", "0x628882B7");

        fixture = new NtlmAuthenticationClient(props);
        assertEquals(expectedDefaultFlags, fixture.getNtlmSspFlags(false));

    }

    @Test
    public void shouldAuthenticateClient() throws Exception {
        NtlmCredential creds = new NtlmCredential("user@l7tech.com" /*"L7TECH\\user"*/, "password");
        Map props = new HashMap();
        props.putAll(fixture);
        props.put("flags", "0x628882B7");//set negotiate flags
        NtlmAuthenticationServer server = new NtlmAuthenticationServer(props, new LocalAuthenticationAdapter(fixture));
        byte[] token = new byte[0];
        try {
            while (!server.getNtlmAuthenticationState().isComplete()) {
                token = fixture.requestAuthentication(token, creds);
                token = server.processAuthentication(token);
            }
        } finally {
            if (token != null)
                for (int i = 0; i < token.length; i++)
                    token[i] = 0;
        }

        Map testAccount = server.getNtlmAuthenticationState().getAccountInfo();
        assertEquals("user", testAccount.get("sAMAccountName"));
        assertEquals("l7tech.com", testAccount.get("domain.dns.name"));
    }

}

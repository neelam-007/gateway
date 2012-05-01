package com.l7tech.ntlm.protocol;

import com.l7tech.ntlm.NtlmTestConstants;
import com.l7tech.ntlm.adapter.LocalAuthenticationAdapter;
import com.l7tech.util.HexUtils;
import jcifs.ntlmssp.Type1Message;
import jcifs.ntlmssp.Type3Message;
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
public class NtlmAuthenticationServerTest {

    NtlmAuthenticationServer fixture;
    NtlmAuthenticationState state;

    @Before
    public void setUp() throws Exception {
        HashMap props = new HashMap();
        props.put("domain.netbios.name", "L7TECH");
        props.put("domain.dns.name", "l7tech.com");
        props.put("localhost.dns.name", "linux-12vk");
        props.put("localhost.netbios.name", "LINUX12-VK");
        props.put("my.username", "user");
        props.put("my.password", "password");

        fixture = new NtlmAuthenticationServer(props, new LocalAuthenticationAdapter(props));
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void shouldAuthenticateUser() throws Exception {
        PasswordCredential credential = new PasswordCredential("user@l7tech.com" /*"L7TECH\\user"*/, "password".toCharArray());
        authenticate(credential);
        credential.destroy();
        Map testAccount = fixture.getNtlmAuthenticationState().getAccountInfo();
        assertEquals("user", testAccount.get("sAMAccountName"));
        assertEquals("l7tech.com", testAccount.get("domain.dns.name"));
    }


    @Test(expected = AuthenticationManagerException.class)
    public void shouldFailWhenCredentialsIncorrect() throws Exception {
        PasswordCredential credential = new PasswordCredential("user", "pass".toCharArray());
        authenticate(credential);
        credential.destroy();

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
        assertEquals(expectedDefaultFlags, fixture.getNtlmSspFlags(true));
    }

    @Test
    public void shouldAuthenticateClient() throws Exception {
        PasswordCredential creds = new PasswordCredential("user@l7tech.com" /*"L7TECH\\user"*/, "password".toCharArray());

        NtlmAuthenticationClient client = new NtlmAuthenticationClient(fixture);
        //client.put("flags", "0xA0088005");
        byte[] token = new byte[0];
        try {
            while (!fixture.getNtlmAuthenticationState().isComplete()) {
                token = client.requestAuthentication(token,creds);
                token = fixture.processAuthentication(token);
            }
        }
        finally
        {
            if (token != null)
                for (int i = 0; i < token.length; i++)
                    token[i] = 0;
        }

        creds.destroy();
        Map testAccount = fixture.getNtlmAuthenticationState().getAccountInfo();
        assertEquals("user", testAccount.get("sAMAccountName"));
        assertEquals("l7tech.com", testAccount.get("domain.dns.name"));
    }

    void authenticate(PasswordCredential creds) throws Exception {

        byte[] token = new byte[0];
        try {
            state = new NtlmAuthenticationState();
            Type1Message type1Message = NtlmClient.generateType1Msg(creds.getSecurityPrincipal().getDomain(), NtlmTestConstants.WORKSTATION);
            token = fixture.processAuthentication(type1Message.toByteArray());
            Type3Message type3Message = NtlmClient.generateType3Msg(creds.getSecurityPrincipal().getUsername(),
                    new String(creds.getPassword()), creds.getSecurityPrincipal().getDomain(), NtlmTestConstants.WORKSTATION, HexUtils.encodeBase64(token, true));
            token = fixture.processAuthentication(type3Message.toByteArray());
        } finally {
            int i;
            if (token != null)
                for (i = 0; i < token.length; i++)
                    token[i] = 0;
        }
    }


}

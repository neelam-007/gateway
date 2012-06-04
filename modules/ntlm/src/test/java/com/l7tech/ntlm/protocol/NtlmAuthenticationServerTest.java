package com.l7tech.ntlm.protocol;

import com.l7tech.ntlm.NtlmTestConstants;
import com.l7tech.ntlm.adapter.LocalAuthenticationAdapter;
import com.l7tech.util.HexUtils;
import jcifs.ntlmssp.Type1Message;
import jcifs.ntlmssp.Type3Message;
import org.apache.commons.lang.ArrayUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

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
    public void shouldReturnFullTargetInfo2() throws Exception {
        LinkedList<Av_Pair> avpairList  =  new LinkedList<Av_Pair>();

        avpairList.add(new Av_Pair(Av_Pair.MsvAvType.MsvAvEOL, ""));
        avpairList.addFirst(new Av_Pair(Av_Pair.MsvAvType.MsvAvDnsComputerName, (String) fixture.get("localhost.dns.name")));
        avpairList.addFirst(new Av_Pair(Av_Pair.MsvAvType.MsvAvDnsDomainName, (String) fixture.get("domain.dns.name")));
        avpairList.addFirst(new Av_Pair(Av_Pair.MsvAvType.MsvAvNbComputerName, (String) fixture.get("localhost.netbios.name")));
        avpairList.addFirst(new Av_Pair(Av_Pair.MsvAvType.MsvAvNbDomainName, (String) fixture.get("domain.netbios.name")));

        byte[] expectedTargetInfo = new byte[0];
        for(Av_Pair av_pair: avpairList){
            expectedTargetInfo = ArrayUtils.addAll(expectedTargetInfo, av_pair.toByteArray());
        }

        assertArrayEquals(expectedTargetInfo, fixture.getTargetInfo());
    }

    @Test
    public void shouldReturnLocalhostOnlyTargetInfo() throws Exception {
        byte[] expectedTargetInfo = new Av_Pair(Av_Pair.MsvAvType.MsvAvEOL, "").toByteArray();
        expectedTargetInfo = ArrayUtils.addAll(new Av_Pair(Av_Pair.MsvAvType.MsvAvNbComputerName, (String) fixture.get("localhost.netbios.name")).toByteArray(), expectedTargetInfo);
        fixture.remove("domain.netbios.name");
        fixture.remove("domain.dns.name");
        fixture.remove("localhost.dns.name");
        assertTrue(fixture.size() == 3);
        assertArrayEquals(expectedTargetInfo, fixture.getTargetInfo());
    }

    @Test
    public void shouldReturnEmptyTargetInfo() throws Exception {
        Av_Pair expectedTargetInfoList = new Av_Pair(Av_Pair.MsvAvType.MsvAvEOL, "");
        fixture.remove("domain.netbios.name");
        fixture.remove("domain.dns.name");
        fixture.remove("localhost.dns.name");
        fixture.remove("localhost.netbios.name");
        assertTrue(fixture.size() == 2);
        assertArrayEquals(expectedTargetInfoList.toByteArray(), fixture.getTargetInfo());
    }

    @Test
    public void shouldAuthenticateUser() throws Exception {
        NtlmCredential credential = new NtlmCredential("user@l7tech.com" /*"L7TECH\\user"*/, "password".toCharArray());
        authenticate(credential);
        Map testAccount = fixture.getNtlmAuthenticationState().getAccountInfo();
        assertEquals("user", testAccount.get("sAMAccountName"));
        assertEquals("l7tech.com", testAccount.get("domain.dns.name"));
    }


    @Test(expected = AuthenticationManagerException.class)
    public void shouldFailWhenCredentialsIncorrect() throws Exception {
        NtlmCredential credential = new NtlmCredential("user", "pass".toCharArray());
        authenticate(credential);

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
        NtlmCredential creds = new NtlmCredential("user@l7tech.com" /*"L7TECH\\user"*/, "password".toCharArray());

        NtlmAuthenticationClient client = new NtlmAuthenticationClient(fixture);
        //client.put("flags", "0xA0088005");
        byte[] token = new byte[0];
        try {
            while (!fixture.getNtlmAuthenticationState().isComplete()) {
                token = client.requestAuthentication(token, creds);
                token = fixture.processAuthentication(token);
            }
        } finally {
            if (token != null)
                for (int i = 0; i < token.length; i++)
                    token[i] = 0;
        }

        Map testAccount = fixture.getNtlmAuthenticationState().getAccountInfo();
        assertEquals("user", testAccount.get("sAMAccountName"));
        assertEquals("l7tech.com", testAccount.get("domain.dns.name"));
    }

    void authenticate(NtlmCredential creds) throws Exception {

        byte[] token = new byte[0];
        try {
            state = new NtlmAuthenticationState();
            Type1Message type1Message = NtlmClient.generateType1Msg(creds.getDomain(), NtlmTestConstants.WORKSTATION);
            token = fixture.processAuthentication(type1Message.toByteArray());
            Type3Message type3Message = NtlmClient.generateType3Msg(creds.getName(),
                    new String(creds.getPassword()), creds.getDomain(), NtlmTestConstants.WORKSTATION, HexUtils.encodeBase64(token, true));
            token = fixture.processAuthentication(type3Message.toByteArray());
        } finally {
            int i;
            if (token != null)
                for (i = 0; i < token.length; i++)
                    token[i] = 0;
        }
    }


}

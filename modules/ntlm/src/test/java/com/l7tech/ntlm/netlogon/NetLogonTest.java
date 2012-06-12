package com.l7tech.ntlm.netlogon;

import com.l7tech.ntlm.NtlmTestConstants;
import com.l7tech.ntlm.adapter.NetlogonAdapter;
import com.l7tech.ntlm.protocol.*;
import com.l7tech.util.HexUtils;
import jcifs.ntlmssp.Type1Message;
import jcifs.ntlmssp.Type2Message;
import jcifs.ntlmssp.Type3Message;
import org.apache.commons.lang.ArrayUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 */
@Ignore("needs connection to the test AD server")
public class NetLogonTest {

    NetlogonAdapter fixture = null;
    Map<String, String> properties = NtlmTestConstants.config;
    private String user;
    private String password;


    @Before
    public void setUp() throws Exception {
        fixture = new NetlogonAdapter(properties);
        user = NtlmTestConstants.USER;
        password = NtlmTestConstants.PASSWORD;
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testConnect() throws Exception {
        fixture.testConnect();
    }


    @Test(expected = AuthenticationManagerException.class)
    public void shouldFailTestConnectWhenServiceAccountInvalid() throws Exception {
        Map props = new HashMap();

        props.put("server.dns.name", properties.get("server.dns.name"));
        props.put("service.account", "");
        props.put("service.password", "");

        new NetlogonAdapter(props);
    }

    @Test(expected = AuthenticationManagerException.class)
    public void shouldFailUserValidation() throws Exception {
        user = "test";
        password = "123";
        NtlmServerResponse response = getTestChallengeResponse();

        byte[] challenge = generateTestServerChallenge(response);
        Map acctInfo = new HashMap();
        fixture.validateCredentials(response, challenge, acctInfo);
    }



    @Test
    public void testValidate() throws Exception {
        NtlmServerResponse response = getTestChallengeResponse();

        byte[] challenge = generateTestServerChallenge(response);
        Map acctInfo = new HashMap();
        fixture.validateCredentials(response, challenge, acctInfo);
        assertTrue(acctInfo.size() > 0);
        assertEquals(NtlmTestConstants.USER, acctInfo.get("sAMAccountName"));
        assertEquals(NtlmTestConstants.DOMAIN, acctInfo.get("logonDomainName"));
        assertEquals(NtlmTestConstants.USERSID, acctInfo.get("userSid").toString());
    }



    private byte[] generateTestServerChallenge(NtlmServerResponse response) throws AuthenticationManagerException {
        byte[] challenge = response.getChallenge();

        if (((response.getFlags() & NtlmConstants.NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY /*0x80000*/) != 0) && (response.getNtResponse().length == NtlmAuthenticationServer.NTLM_V1_NT_RESPONSE_LENGTH)) {
            try {
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                md5.update(response.getSessionNonce());
                challenge = md5.digest();
            } catch (GeneralSecurityException e) {
                throw new AuthenticationManagerException("Failed to compute NTLMv2 server challenge", e);
            }
        }
        return challenge;
    }

    private NtlmServerResponse getTestChallengeResponse() throws IOException {
        Type1Message type1Message = NtlmClient.generateType1Msg(NtlmTestConstants.DOMAIN, NtlmTestConstants.WORKSTATION);
        SecureRandom random = new SecureRandom();
        byte[] serverChallenge = new byte[8];
        random.nextBytes(serverChallenge);

        Type2Message type2Message = new Type2Message(type1Message, serverChallenge, NtlmTestConstants.DOMAIN);
        type2Message.setTargetInformation(getTargetInformation());
        Type3Message type3Message = NtlmClient.generateType3Msg(user,
                password, NtlmTestConstants.DOMAIN, NtlmTestConstants.WORKSTATION,
                HexUtils.encodeBase64(type2Message.toByteArray(), true));

        byte[] sessionNonce = new byte[16];
        System.arraycopy(serverChallenge, 0, sessionNonce, 0, 8);
        System.arraycopy(type3Message.getLMResponse(), 0, sessionNonce, 8, type3Message.getLMResponse().length < 8 ? 0 : 8);

        return new NtlmServerResponse(type3Message.getDomain(), type3Message.getUser(),
                serverChallenge, sessionNonce, getTargetInformation(), type3Message.getLMResponse(), type3Message.getNTResponse(), type3Message.getFlags());
    }

    public byte[] getTargetInformation() throws UnsupportedEncodingException {
        return ArrayUtils.addAll( new Av_Pair(Av_Pair.MsvAvType.MsvAvNbDomainName, properties.get("domain.netbios.name")).toByteArray(), new Av_Pair(Av_Pair.MsvAvType.MsvAvEOL, "").toByteArray());
    }





}

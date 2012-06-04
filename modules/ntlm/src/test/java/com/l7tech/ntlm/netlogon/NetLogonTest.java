package com.l7tech.ntlm.netlogon;

import com.l7tech.ntlm.NtlmTestConstants;
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

    NetLogon fixture = null;
    Map<String, String> properties = NtlmTestConstants.config;


    @Before
    public void setUp() throws Exception {
        fixture = new NetLogon(properties);
    }

    @After
    public void tearDown() throws Exception {
        //fixture.disconnect();
    }

    @Test
    public void testConnect() throws Exception {
        fixture.connect();
    }

/*    @Test
    public void testConnectNetlogonConnection() throws Exception {
        NetlogonConnection netlogonConnection = new NetlogonConnection();
        String hostname = properties.get("localhost.netbios.name");
        String serverName = properties.get("server.dns.name");
        String serviceName = properties.get("service.account");
        String servicePassword = properties.get("service.password");
        NtlmServiceAccount serviceAccount = new NtlmServiceAccount(serviceName, servicePassword);

       netlogonConnection.connect(serverName, serverName, serviceAccount, new SecureRandom());

    }*/

    @Test
    public void testValidate() throws Exception {
        NtlmChallengeResponse response = getTestChallengeResponse();

        byte[] challenge = generateTestServerChallenge(response);
        Map acctInfo = new HashMap();
        fixture.validate(response, challenge, acctInfo);
        assertTrue(acctInfo.size() > 0);
        assertEquals(NtlmTestConstants.USER, acctInfo.get("sAMAccountName"));
        assertTrue(fixture.handle == null);

    }

    @Test
    public void shouldReleaseHandleWhenFailed() throws Exception {
        fixture.remove("localhost.netbios.name");

        NtlmChallengeResponse response = getTestChallengeResponse();

        byte[] challenge = generateTestServerChallenge(response);
        try{
            fixture.validate(response, challenge, new HashMap());
        }catch (Exception ex){}

        assertTrue(fixture.handle == null);

    }

    private byte[] generateTestServerChallenge(NtlmChallengeResponse response) throws AuthenticationManagerException {
        byte[] challenge = response.getChallenge();

        if (((response.getFlags() & NtlmConstants.NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY /*0x80000*/) != 0) && (response.getNtResponse().length == NtlmAuthenticationServer.NTLM_V1_NT_RESPONSE_LENGTH)) {
            try {
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                md5.update(response.getSessionNonce());
                challenge = md5.digest();
            } catch (GeneralSecurityException e) {
                throw new AuthenticationManagerException(AuthenticationManagerException.Status.STATUS_ERROR, "Failed to compute NTLMv2 server challenge", e);
            }
        }
        return challenge;
    }

    private NtlmChallengeResponse getTestChallengeResponse() throws IOException {
        Type1Message type1Message = NtlmClient.generateType1Msg(NtlmTestConstants.DOMAIN, NtlmTestConstants.WORKSTATION);
        SecureRandom random = new SecureRandom();
        byte[] serverChallenge = new byte[8];
        random.nextBytes(serverChallenge);

        Type2Message type2Message = new Type2Message(type1Message, serverChallenge, NtlmTestConstants.DOMAIN);
        type2Message.setTargetInformation(getTargetInformation());
        Type3Message type3Message = NtlmClient.generateType3Msg(NtlmTestConstants.USER,
                NtlmTestConstants.PASSWORD, NtlmTestConstants.DOMAIN, NtlmTestConstants.WORKSTATION,
                HexUtils.encodeBase64(type2Message.toByteArray(), true));

        byte[] sessionNonce = new byte[16];
        System.arraycopy(serverChallenge, 0, sessionNonce, 0, 8);
        System.arraycopy(type3Message.getLMResponse(), 0, sessionNonce, 8, type3Message.getLMResponse().length < 8 ? 0 : 8);

        return new NtlmChallengeResponse(type3Message.getDomain(), type3Message.getUser(),
                serverChallenge, sessionNonce, getTargetInformation(), type3Message.getLMResponse(), type3Message.getNTResponse(), type3Message.getFlags());
    }

    public byte[] getTargetInformation() throws UnsupportedEncodingException {
        return ArrayUtils.addAll( new Av_Pair(Av_Pair.MsvAvType.MsvAvNbDomainName, properties.get("domain.netbios.name")).toByteArray(), new Av_Pair(Av_Pair.MsvAvType.MsvAvEOL, "").toByteArray());
    }





}

package com.l7tech.ntlm.protocol;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.util.Encdec;
import jcifs.util.HMACT64;
import jcifs.util.MD4;

import java.util.Arrays;

/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 * Date: 6/1/12
 */
public class TestNtlmServerResponse extends NtlmServerResponse {


    byte[] sessionKey = null;


    public TestNtlmServerResponse(NtlmServerResponse clientServerResponse, String domain, String username, String password, byte[] targetInformation) {
        this.domain = domain;
        this.username = username;
        this.lmResponse = null;
        this.flags = clientServerResponse.flags;
        this.targetInformation = targetInformation;

        if (clientServerResponse.ntResponse.length == 24) {
            if ((this.flags & NtlmConstants.NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY/*0x80000*/) == 0) {
                this.ntResponse = NtlmPasswordAuthentication.getNTLMResponse(password, clientServerResponse.challenge);
                this.lmResponse = NtlmPasswordAuthentication.getPreNTLMResponse(password, clientServerResponse.challenge);
            } else {
                byte[] clientChallenge = new byte[8];
                System.arraycopy(clientServerResponse.lmResponse, 0, clientChallenge, 0, 8);

                byte[] nTOWFv1 = NtlmPasswordAuthentication.nTOWFv1(password);
                this.ntResponse = NtlmPasswordAuthentication.getNTLM2Response(nTOWFv1, clientServerResponse.challenge, clientChallenge);

                if ((this.flags & NtlmConstants.NTLMSSP_NEGOTIATE_SIGN/*0x10*/) != 0) {
                    MD4 md4 = new MD4();
                    md4.update(nTOWFv1);
                    byte[] userSessionKey = md4.digest();

                    HMACT64 hmac = new HMACT64(userSessionKey);
                    hmac.update(clientServerResponse.sessionNonce);
                    this.sessionKey = hmac.digest();
                    //clientServerResponse.sessionKey = this.sessionKey;
                }
            }
        } else {
            byte[] nTOWFv2 = NtlmPasswordAuthentication.nTOWFv2(domain, username, password);

            byte[] clientChallenge = new byte[8];
            System.arraycopy(clientServerResponse.ntResponse, 32, clientChallenge, 0, 8);
            long nanos1601 = Encdec.dec_uint64le(clientServerResponse.ntResponse, 24);

            targetInformation = getCombinedTargetInformation(clientServerResponse.ntResponse, targetInformation);

            this.ntResponse = NtlmPasswordAuthentication.getNTLMv2Response(nTOWFv2, clientServerResponse.challenge, clientChallenge, nanos1601, targetInformation);

            if ((this.flags & NtlmConstants.NTLMSSP_NEGOTIATE_SIGN/*0x10*/) != 0) {
                HMACT64 hmac = new HMACT64(nTOWFv2);
                hmac.update(this.ntResponse, 0, 16);
                this.sessionKey = hmac.digest();
               // clientServerResponse.sessionKey = this.sessionKey;
            }
        }
    }

    private byte[] getCombinedTargetInformation(byte[] response, byte[] targetInformation) {
        int ri = 44;
        while (true) {
            int avId = Encdec.dec_uint16le(response, ri);
            int avLen = Encdec.dec_uint16le(response, ri + 2);
            ri += 4;
            if ((avId == 0) || (ri + avLen > response.length)) {
                break;
            }
            if (avId > 4) {
                byte[] tmp = new byte[targetInformation.length + 4 + avLen];
                int ti = 0;
                System.arraycopy(targetInformation, 0, tmp, ti, targetInformation.length - 4);
                ti += targetInformation.length - 4;
                Encdec.enc_uint16le((short) avId, tmp, ti);
                ti += 2;
                Encdec.enc_uint16le((short) avLen, tmp, ti);
                ti += 2;
                System.arraycopy(response, ri, tmp, ti, avLen);
                ti += avLen;
                Encdec.enc_uint32le(0, tmp, ti);
                ti += 4;
                targetInformation = tmp;
            }
            ri += avLen;
        }

        return targetInformation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NtlmServerResponse)) return false;

        NtlmServerResponse that = (NtlmServerResponse) o;

        //if (!Arrays.equals(lmResponse, that.lmResponse)) return false;
        if (!Arrays.equals(ntResponse, that.ntResponse)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        //int result = lmResponse != null ? Arrays.hashCode(lmResponse) : 0;
        int result = ntResponse != null ? Arrays.hashCode(ntResponse) : 0;
        return result;
    }
}

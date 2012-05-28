package com.l7tech.ntlm.protocol;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.util.Encdec;
import jcifs.util.HMACT64;
import jcifs.util.MD4;

import java.util.Arrays;

/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 */
public class NtlmChallengeResponse {
    public byte[] getChallenge() {
        return challenge;
    }

    public void setChallenge(byte[] challenge) {
        this.challenge = challenge;
    }

    public byte[] getSessionNonce() {
        return sessionNonce;
    }

    public void setSessionNonce(byte[] sessionNonce) {
        this.sessionNonce = sessionNonce;
    }

    public byte[] getTargetInformation() {
        return targetInformation;
    }

    public void setTargetInformation(byte[] targetInformation) {
        this.targetInformation = targetInformation;
    }

    public byte[] getLmResponse() {
        return lmResponse;
    }

    public void setLmResponse(byte[] lmResponse) {
        this.lmResponse = lmResponse;
    }

    public byte[] getNtResponse() {
        return ntResponse;
    }

    public void setNtResponse(byte[] ntResponse) {
        this.ntResponse = ntResponse;
    }

    public byte[] getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(byte[] sessionKey) {
        this.sessionKey = sessionKey;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    String domain;
    String username;
    byte[] challenge;
    byte[] sessionNonce;
    byte[] targetInformation;
    byte[] lmResponse;
    byte[] ntResponse;
    byte[] sessionKey = null;
    int flags = 0;

    public NtlmChallengeResponse(String domain, String username, byte[] challenge, byte[] sessionNonce, byte[] targetInformation, byte[] lmResponse, byte[] ntResponse, int flags) {
        this.domain = domain;
        this.username = username;
        this.challenge = challenge;
        this.sessionNonce = sessionNonce;
        this.targetInformation = targetInformation;
        this.lmResponse = lmResponse;
        this.ntResponse = ntResponse;
        this.flags = flags;
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

    public NtlmChallengeResponse(NtlmChallengeResponse clientChallengeResponse, String domain, String username, char[] password, byte[] targetInformation) {
        this.domain = domain;
        this.username = username;
        this.lmResponse = null;
        this.flags = clientChallengeResponse.flags;

        if (clientChallengeResponse.ntResponse.length == 24) {
            if ((this.flags & NtlmConstants.NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY/*0x80000*/) == 0) {
                this.ntResponse = NtlmPasswordAuthentication.getNTLMResponse(new String(password), clientChallengeResponse.challenge);
            } else {
                byte[] clientChallenge = new byte[8];
                System.arraycopy(clientChallengeResponse.lmResponse, 0, clientChallenge, 0, 8);

                byte[] responseKeyNT = NtlmPasswordAuthentication.nTOWFv1(new String(password));
                this.ntResponse = NtlmPasswordAuthentication.getNTLM2Response(responseKeyNT, clientChallengeResponse.challenge, clientChallenge);

                if ((this.flags & NtlmConstants.NTLMSSP_NEGOTIATE_SIGN/*0x10*/) != 0) {
                    MD4 md4 = new MD4();
                    md4.update(responseKeyNT);
                    byte[] userSessionKey = md4.digest();

                    HMACT64 hmac = new HMACT64(userSessionKey);
                    hmac.update(clientChallengeResponse.sessionNonce);
                    this.sessionKey = hmac.digest();
                    clientChallengeResponse.sessionKey = this.sessionKey;
                }
            }
        } else {
            byte[] responseKeyNT = NtlmPasswordAuthentication.nTOWFv2(domain, username, new String(password));

            byte[] clientChallenge = new byte[8];
            System.arraycopy(clientChallengeResponse.ntResponse, 32, clientChallenge, 0, 8);
            long nanos1601 = Encdec.dec_uint64le(clientChallengeResponse.ntResponse, 24);

            targetInformation = getCombinedTargetInformation(clientChallengeResponse.ntResponse, targetInformation);

            this.ntResponse = NtlmPasswordAuthentication.getNTLMv2Response(responseKeyNT, clientChallengeResponse.challenge, clientChallenge, nanos1601, targetInformation);

            if ((this.flags & NtlmConstants.NTLMSSP_NEGOTIATE_SIGN/*0x10*/) != 0) {
                HMACT64 hmac = new HMACT64(responseKeyNT);
                hmac.update(this.ntResponse, 0, 16);
                this.sessionKey = hmac.digest();
                clientChallengeResponse.sessionKey = this.sessionKey;
            }
        }
    }

    public String getDomain() {
        return this.domain;
    }

    public String getUsername() {
        return this.username;
    }

    public boolean equals(Object obj) {
        if ((obj instanceof NtlmChallengeResponse)) {
            NtlmChallengeResponse resp = (NtlmChallengeResponse) obj;

            if ((resp.ntResponse.length == this.ntResponse.length) && (Arrays.equals(resp.ntResponse, this.ntResponse))) {
                return true;
            }
        }
        return false;
    }
}

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
    String domain;

    String username;
    byte[] challenge;
    byte[] sessionNonce;
    byte[] targetInformation;
    byte[] lmResponse;
    byte[] ntResponse;
    byte[] sessionKey = null;
    int flags = 0;
    protected NtlmChallengeResponse() {

    }

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

    public String getDomain() {
        return this.domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
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

}

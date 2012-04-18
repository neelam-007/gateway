package com.l7tech.ntlm.protocol;

import java.util.Map;

/**
 * The state to keep track of the NTLM authentication protocol
 * Created by IntelliJ IDEA.
 * User: awitrisna
 * Date: 09/04/12
 * Time: 3:47 PM
 */

public class NtlmAuthenticationState {

    private int flags;
    private byte[] serverChallenge;
    private byte[] sessionKey;
    private byte[] targetInfo;
    private Map accountInfo;
    private NtlmAuthenticationProvider.State state = NtlmAuthenticationProvider.State.DEFAULT;

    public NtlmAuthenticationProvider.State getState() {
        return state;
    }

    public void setState(NtlmAuthenticationProvider.State state) {
        this.state = state;
    }

    public byte[] getServerChallenge() {
        return serverChallenge;
    }

    public void setServerChallenge(byte[] serverChallenge) {
        this.serverChallenge = serverChallenge;
    }

    public byte[] getSessionKey() {
        return sessionKey;
    }

    protected void setSessionKey(byte[] sessionKey) {
        this.sessionKey = sessionKey;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public Map getAccountInfo() {
        return accountInfo;
    }

    public void setAccountInfo(Map accountInfo) {
        this.accountInfo = accountInfo;
    }

    public boolean isComplete() {
        return state == NtlmAuthenticationProvider.State.COMPLETE;
    }


    public byte[] getTargetInfo() {
        return targetInfo;
    }

    public void setTargetInfo(byte[] targetInfo) {
      this.targetInfo = targetInfo;
    }


}

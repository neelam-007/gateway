package com.l7tech.ntlm.protocol;

import com.l7tech.ntlm.adapter.AuthenticationAdapter;
import jcifs.ntlmssp.Type1Message;
import jcifs.ntlmssp.Type2Message;
import jcifs.ntlmssp.Type3Message;
import jcifs.util.Hexdump;
import org.apache.commons.lang.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 * Date: 3/5/12
 */
public class NtlmAuthenticationServer extends NtlmAuthenticationProvider {

    private static final Logger log = Logger.getLogger(NtlmAuthenticationServer.class.getName());
    public static final int NTLM_V1_NT_RESPONSE_LENGTH = 24;

    private final AuthenticationAdapter authenticationAdapter;

    public NtlmAuthenticationServer(Map properties, AuthenticationAdapter adapter) {
        super(properties);
        authenticationAdapter = adapter;
    }

    /**
     * process authentication request from the client
     * this method is using internal state that has to be preserved between sessions
     *
     * @param token - bytes NTLM token passed from the client
     * @return NTLM message passed back to the client
     * @throws AuthenticationManagerException when failed
     * @see NtlmAuthenticationProvider serialize and deserialize methods
     */
    @Override
    public byte[] processAuthentication(byte[] token) throws AuthenticationManagerException {
        if(token == null || token.length <= 0) {
            throw new AuthenticationManagerException(AuthenticationManagerException.Status.STATUS_ERROR, "Invalid token");
        }
        int flags = state.getFlags();
        log.log(Level.FINE, "Current State=" + state.getState().name());
        try {
            switch (state.getState()) {
                case DEFAULT:
                    state.setState(State.CHALLENGE);
                case CHALLENGE:
                    if(flags == 0){
                        flags = DEFAULT_NTLMSSP_FLAGS;//set to default flags
                    }
                    Type1Message negotiateMessage = new Type1Message(token);

                    flags &= negotiateMessage.getFlags();

                    SecureRandom random = new SecureRandom();
                    byte[] serverChallenge = new byte[8];
                    random.nextBytes(serverChallenge);
                    state.setServerChallenge(serverChallenge);

                    flags &= NtlmConstants.NTLMSSP_NEGOTIATE_OEM_MASK;//-3;

                    flags |= NtlmConstants.NTLMSSP_NEGOTIATE_VERSION;//33554432;/The data for this field is provided in hte version field of the message
                    flags |= NtlmConstants.NTLMSSP_TARGET_TYPE_DOMAIN;//65536; TargetName must be domain name
                    if ((flags & NtlmConstants.NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY/*0x80000*/) != 0) { //if NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY is set
                        flags |= NtlmConstants.NTLMSSP_NEGOTIATE_TARGET_INFO;//8388608;
                    }

                    Type2Message challengeMessage = new Type2Message();
                    challengeMessage.setTarget((String) get("domain.netbios.name"));
                    challengeMessage.setFlags(flags);
                    challengeMessage.setChallenge(state.getServerChallenge());
                    challengeMessage.setTargetInformation(getTargetInfo());
                    token = challengeMessage.toByteArray();

                    //set NtlmAuthenticationState
                    state.setState(State.AUTHENTICATE);
                    state.setFlags(flags);
                    state.setTargetInfo(challengeMessage.getTargetInformation()); //store target info

                    log.log(Level.FINE, "NtlmAuthenticationServer: Negotiated NTLM flags: 0x" + Hexdump.toHexString(flags, 8));
                    break;
                case AUTHENTICATE:
                    Type3Message authenticateMessage = new Type3Message(token);

                    token = null;
                    state.setState(State.FAILED);

                    byte[] sessionNonce = new byte[16];
                    System.arraycopy(state.getServerChallenge(), 0, sessionNonce, 0, 8);
                    System.arraycopy(authenticateMessage.getLMResponse(), 0, sessionNonce, 8, authenticateMessage.getLMResponse().length < 8 ? 0 : 8);

                    if (authenticateMessage.getUser().length() == 0) {
                        throw new AuthenticationManagerException(AuthenticationManagerException.Status.STATUS_ACCOUNT_NOT_FOUND, "Invalid account name");
                    }

                    log.log(Level.FINE, "NtlmAuthenticationServer: NTLM credentials: DomainName=" + authenticateMessage.getDomain() + " UserName=" + authenticateMessage.getUser() + " Workstation=" + authenticateMessage.getWorkstation());

                    NtlmChallengeResponse ntlmChallengeResponse = new NtlmChallengeResponse(authenticateMessage.getDomain(), authenticateMessage.getUser(), state.getServerChallenge(), sessionNonce, getTargetInfo(), authenticateMessage.getLMResponse(), authenticateMessage.getNTResponse(), state.getFlags());

                    token = authenticate(ntlmChallengeResponse);
                    byte[] userSessionKey = ntlmChallengeResponse.sessionKey;

                    state.setSessionKey(userSessionKey);

                    if (((state.getFlags() & NtlmConstants.NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY/*0x80000*/) != 0) && ((state.getFlags() & NtlmConstants.NTLMSSP_NEGOTIATE_SIGN/*0x10*/) != 0)) {

                        log.log(Level.FINE, "NtlmAuthenticationServer: Generating NTLM2 Session Security keys");

                        if (state.getSessionKey() == null) {
                            throw new AuthenticationManagerException("Cannot perform signing or sealing if an NTLMSSP sessionKey is not established");
                        }
                        if ((state.getFlags() & NtlmConstants.NTLMSSP_NEGOTIATE_KEY_EXCH/*0x40000000*/) != 0) {
                            byte[] exchangedKey = authenticateMessage.getSessionKey();
                            if(exchangedKey == null || exchangedKey.length < 16) {
                                throw new AuthenticationManagerException(AuthenticationManagerException.Status.STATUS_ERROR, "Encrypted exchange key length is invalid");
                            }
                            byte[] masterKey = new byte[16];

                            log.log(Level.FINE, "NtlmAuthenticationServer: Decrypting Key Exchange session key");

                            try {
                                Cipher rc4 = Cipher.getInstance("RC4");
                                rc4.init(2, new SecretKeySpec(state.getSessionKey(), "RC4"));
                                rc4.update(exchangedKey, 0, 16, masterKey, 0);
                            } catch (GeneralSecurityException gse) {
                                throw new AuthenticationManagerException("Failed to decrypt exchange key", gse);
                            }

                            state.setSessionKey(masterKey);
                        }
                        log.log(Level.FINE, "NTLMv2 Extended Session Security Key negotiated successfully");
                    } else {
                        log.log(Level.FINE, "NTLMv2 Extended Session Security Key was not negotiated");
                    }

                    if (authenticateMessage.getNTResponse().length > NTLM_V1_NT_RESPONSE_LENGTH) {
                        log.log(Level.FINE, "Server negotiated NTLMv2");
                    } else {
                        log.log(Level.FINE, "Server negotiated NTLMv1");
                    }

                    setAuthenticationComplete(state);

                    //security token is a session key
                    token = ntlmChallengeResponse.sessionKey;
                    break;
                default:
                    state.setState(State.FAILED);
                    throw new AuthenticationManagerException("Invalid state");
            }

            return token;
        } catch (IOException ioe) {
            log.log(Level.SEVERE, "ERROR: ", ioe.getMessage());
            throw new AuthenticationManagerException(ioe.getMessage(), ioe);
        }
    }

    private void setAuthenticationComplete(NtlmAuthenticationState state) {
        state.setServerChallenge(null);
        state.setState(State.COMPLETE);
    }

    /**
     * attempts to authenticate the user
     *
     * @param resp NtlmAuthenticationResponse
     * @throws AuthenticationManagerException
     */
    protected byte[] authenticate(NtlmChallengeResponse resp) throws AuthenticationManagerException {
        if (StringUtils.isEmpty(resp.username)) {
            throw new AuthenticationManagerException(AuthenticationManagerException.Status.STATUS_ACCOUNT_NOT_FOUND, "Account name not found!");
        }
        Map acct = new HashMap(this);
        //validate client challenge response and update user account
        byte[] challenge = resp.getChallenge();

        if (((resp.getFlags() & NtlmConstants.NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY /*0x80000*/) != 0) && (resp.getNtResponse().length == NTLM_V1_NT_RESPONSE_LENGTH)) {
            try {
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                md5.update(resp.getSessionNonce());
                challenge = md5.digest();
            } catch (GeneralSecurityException e) {
                throw new AuthenticationManagerException(AuthenticationManagerException.Status.STATUS_ERROR, "Failed to compute NTLMv2 server challenge", e);
            }
        }

        byte[] token = (byte[]) authenticationAdapter.validate(resp, challenge, acct);
        //set authenticated user account
        state.setAccountInfo(acct);

        return token;
    }
}

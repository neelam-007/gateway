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
    private final SecureRandom secureRandom = new SecureRandom();

    public NtlmAuthenticationServer(Map properties, AuthenticationAdapter adapter) {
        super(properties);
        authenticationAdapter = adapter;
        state.setState(State.CHALLENGE);
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
        if (token == null || token.length <= 0) {
            throw new AuthenticationManagerException("Invalid token");
        }
        int flags = state.getFlags();
        log.log(Level.FINE, "Current State=" + state.getState().name());
        try {
            switch (state.getState()) {
                case CHALLENGE:
                    if (flags == 0) {
                        flags = DEFAULT_NTLMSSP_FLAGS;//set to default flags
                    }
                    Type1Message negotiateMessage = new Type1Message(token);

                    flags &= negotiateMessage.getFlags();

                    byte[] serverChallenge = new byte[8];
                    secureRandom.nextBytes(serverChallenge);
                    state.setServerChallenge(serverChallenge);

                    flags &= NtlmConstants.NTLMSSP_NEGOTIATE_OEM_MASK;//-3;

                    flags |= NtlmConstants.NTLMSSP_NEGOTIATE_VERSION;//33554432;/The data for this field is provided in hte version field of the message
                    String targetName = null;
                    if (StringUtils.isNotEmpty((String) get("domain.netbios.name"))) {
                        flags |= NtlmConstants.NTLMSSP_TARGET_TYPE_DOMAIN;//65536; TargetName must be domain name
                        targetName = (String) get("domain.netbios.name");
                    } else if (StringUtils.isNotEmpty((String) get("localhost.netbios.name"))) {
                        flags |= NtlmConstants.NTLMSSP_TARGET_TYPE_SERVER;//131072; TargetName must be server
                        targetName = (String) get("localhost.netbios.name");
                    }

                    if ((flags & NtlmConstants.NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY/*0x80000*/) != 0) { //if NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY is set
                        flags |= NtlmConstants.NTLMSSP_NEGOTIATE_TARGET_INFO;//8388608;
                    }

                    Type2Message challengeMessage = new Type2Message();
                    //set the target name
                    if (targetName != null) {
                        challengeMessage.setTarget(targetName);
                    }
                    challengeMessage.setFlags(flags);
                    challengeMessage.setChallenge(state.getServerChallenge());
                    if ((flags & NtlmConstants.NTLMSSP_REQUEST_TARGET) != 0) {
                        challengeMessage.setTargetInformation(getTargetInfo());
                    }
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

                    if (StringUtils.isBlank(authenticateMessage.getUser())) {
                        throw new AuthenticationManagerException("Invalid user name");
                    }

                    log.log(Level.FINE, "NtlmAuthenticationServer: NTLM credentials: DomainName=" + authenticateMessage.getDomain() + " UserName=" + authenticateMessage.getUser() + " Workstation=" + authenticateMessage.getWorkstation());


                    byte[] userSessionKey = authenticate(authenticateMessage);

                    state.setSessionKey(userSessionKey);

                    if (((state.getFlags() & NtlmConstants.NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY/*0x80000*/) != 0) && ((state.getFlags() & NtlmConstants.NTLMSSP_NEGOTIATE_SIGN/*0x10*/) != 0)) {
                        generateSessionSecurityKeys(authenticateMessage, userSessionKey);
                    } else {
                        log.log(Level.FINE, "NTLMv2 Extended Session Security Key was not negotiated");
                    }

                    if(log.isLoggable(Level.FINE)) {
                        if (authenticateMessage.getNTResponse().length > NTLM_V1_NT_RESPONSE_LENGTH) {
                            log.log(Level.FINE, "Server negotiated NTLMv2");
                        } else {
                            log.log(Level.FINE, "Server negotiated NTLMv1");
                        }
                    }

                    setAuthenticationComplete(state);

                    //security token is the session key
                    token = userSessionKey;
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

    private void generateSessionSecurityKeys(Type3Message authenticateMessage, byte[] userSessionKey) throws AuthenticationManagerException {
        log.log(Level.FINE, "NtlmAuthenticationServer: Generating NTLM2 Session Security keys");

        if (userSessionKey == null) {
            throw new AuthenticationManagerException("Cannot perform signing or sealing if an NTLMSSP sessionKey is not set");
        }
        if ((state.getFlags() & NtlmConstants.NTLMSSP_NEGOTIATE_KEY_EXCH/*0x40000000*/) != 0) {
            byte[] exchangedKey = authenticateMessage.getSessionKey();
            if (exchangedKey == null || exchangedKey.length < 16) {
                throw new AuthenticationManagerException("Encrypted exchange key length is invalid");
            }
            byte[] decryptedKey = new byte[16];

            log.log(Level.FINE, "NtlmAuthenticationServer: Decrypting Key Exchange session key");

            try {
                Cipher rc4 = Cipher.getInstance("RC4");
                rc4.init(2, new SecretKeySpec(userSessionKey, "RC4"));
                rc4.update(exchangedKey, 0, 16, decryptedKey, 0);
            } catch (GeneralSecurityException gse) {
                throw new AuthenticationManagerException("Failed to decrypt exchange key", gse);
            }

            state.setSessionKey(decryptedKey);
        }
        log.log(Level.FINE, "NTLMv2 Extended Session Security Key was negotiated");
    }

    @Override
    public void resetAuthentication() {
        super.resetAuthentication();
        state.setState(State.CHALLENGE);
    }

    private void setAuthenticationComplete(NtlmAuthenticationState state) {
        state.setServerChallenge(null);
        state.setState(State.COMPLETE);
    }

    /**
     * attempts to authenticate the user
     *
     * @param authenticateMessage Type3Message
     * @throws AuthenticationManagerException
     */
    private byte[] authenticate(Type3Message authenticateMessage) throws AuthenticationManagerException {

        byte[] challengeToClient = new byte[16];
        System.arraycopy(state.getServerChallenge(), 0, challengeToClient, 0, 8);
        System.arraycopy(authenticateMessage.getLMResponse(), 0, challengeToClient, 8, authenticateMessage.getLMResponse().length < 8 ? 0 : 8);
        NtlmServerResponse response = new NtlmServerResponse(authenticateMessage.getDomain(), authenticateMessage.getUser(), state.getServerChallenge(), challengeToClient, getTargetInfo(), authenticateMessage.getLMResponse(), authenticateMessage.getNTResponse(), state.getFlags());


        //validate client challenge response and update user account
        byte[] challenge = challengeToClient;
        /* this is required to calculate challenge for NTLMv1 exert from [MS-APDS]
           The following algorithm is used for authentication from the server to the DC:
           IF (NTLMSSP_NEGOTIATE_ENHANCED_SESSION_SECURITY and NtResponseLength == 24 and LmResponseLength >= 8)
           NetlogonNetworkInformation.LmChallenge = MD5(Concatenate(ChallengeToClient, LmResponse[0..7]))[0..7]
         */
        if (((state.getFlags() & NtlmConstants.NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY /*0x80000*/) != 0) && (authenticateMessage.getNTResponse().length == NTLM_V1_NT_RESPONSE_LENGTH) && authenticateMessage.getLMResponse().length >= 8) {
            try {
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                md5.update(challengeToClient);
                challenge = md5.digest();
            } catch (GeneralSecurityException e) {
                throw new AuthenticationManagerException("Failed to compute server challenge", e);
            }
        }
        Map acct = new HashMap();
        byte[] token = (byte[]) authenticationAdapter.validate(response, challenge, acct);
        //set authenticated user account
        state.setAccountInfo(acct);

        return token;
    }
}

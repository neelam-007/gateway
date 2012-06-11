package com.l7tech.ntlm.protocol;

import jcifs.ntlmssp.Type1Message;
import jcifs.ntlmssp.Type2Message;
import jcifs.ntlmssp.Type3Message;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.util.HMACT64;
import org.apache.commons.lang.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 * <p/>
 * Implementation of the NTLM client that supports both NTLMv1 and NTLMv2 protocols
 */
public class NtlmAuthenticationClient extends NtlmAuthenticationProvider {

    private static final Logger log = Logger.getLogger(NtlmAuthenticationClient.class.getName());

    private final SecureRandom secureRandom = new SecureRandom();

    public NtlmAuthenticationClient() {
        this(new HashMap());
    }

    public NtlmAuthenticationClient(Map properties) {
        super(properties);
        state.setState(State.NEGOTIATE);
    }

    /**
     * Requests NTLM authentication from the server. Generates NTLM messages according to the NTLM protocol
     *
     * @param token response sent from the server
     * @param cred  - PasswordCredentials of the user
     * @return request sent to the server (can be either Negotiate message or Authenticate message)
     * @throws AuthenticationManagerException
     */
    @Override
    public byte[] requestAuthentication(byte[] token, NtlmCredential cred) throws AuthenticationManagerException {
        int flags = state.getFlags();
        String workstation = cred.getHost();
        if (StringUtils.isEmpty(workstation)) {
            workstation = (String) get("localhost.netbios.name");
        }
        String name = cred.getName();
        String domainName = cred.getDomain();

        try {
            switch (state.getState()) {
                case NEGOTIATE:
                    if (flags == 0) {
                        flags = NtlmConstants.NTLMSSP_NEGOTIATE_56 |
                                NtlmConstants.NTLMSSP_NEGOTIATE_128 |
                                NtlmConstants.NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY |
                                NtlmConstants.NTLMSSP_NEGOTIATE_ALWAYS_SIGN |
                                NtlmConstants.NTLMSSP_REQUEST_TARGET |
                                NtlmConstants.NTLMSSP_NEGOTIATE_UNICODE;
                    }
                    Type1Message nnm = new Type1Message(flags, domainName, workstation);
                    token = nnm.toByteArray();
                    if(log.isLoggable(Level.FINE)) {
                        log.log(Level.FINE, "NTLMSSP_Negotiate Message sent");
                    }

                    state.setState(State.CHALLENGE);
                    break;
                case CHALLENGE:
                    Type2Message ncm = new Type2Message(token);

                    if(log.isLoggable(Level.FINE)) {
                        log.log(Level.FINE, "Received NTLMSSP_Challenge message");
                    }

                    byte[] serverChallenge = ncm.getChallenge();

                    flags &= ncm.getFlags();
                    flags &= NtlmConstants.NTLMSSP_NEGOTIATE_VERSION_MASK;
                    flags &= NtlmConstants.NTLMSSP_R6_MASK;//r6 unused, always set to 0
                    flags &= NtlmConstants.NTLMSSP_TARGET_TYPE_SERVER_MASK;
                    flags &= NtlmConstants.NTLMSSP_TARGET_TYPE_DOMAIN_MASK;
                    flags |= NtlmConstants.NTLMSSP_NEGOTIATE_VERSION;

                    String password = cred.getPassword();
                    if (StringUtils.isEmpty(password)) {
                        throw new AuthenticationManagerException("Password required");
                    }

                    String domainNameFields = (domainName != null ? domainName : "");
                    String userNameFields = name;

                    byte[] nTOWFv2 = NtlmPasswordAuthentication.nTOWFv2(domainNameFields, userNameFields, password);

                    byte[] clientChallenge = new byte[8];

                    secureRandom.nextBytes(clientChallenge);
                    long nanos1601 = (System.currentTimeMillis() + MILLISECONDS_BETWEEN_1970_AND_1601) * 10000L;

                    byte[] lmChallengeResponseFields = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}; //the client supports only NTLMv2
                    byte[] ntChallengeResponseFields = NtlmPasswordAuthentication.getNTLMv2Response(nTOWFv2, serverChallenge, clientChallenge, nanos1601, ncm.getTargetInformation());

                    Type3Message nam = new Type3Message(flags, lmChallengeResponseFields, ntChallengeResponseFields, domainNameFields, userNameFields, workstation);

                    byte[] sessionKey = null;

                    sessionKey = generateSessionSecurityKeys(flags, nTOWFv2, ntChallengeResponseFields, nam, sessionKey);

                    token = nam.toByteArray();

                    if(log.isLoggable(Level.FINE)) {
                        log.log(Level.FINE, "Client sent NTLMv2 Authentication");
                    }

                    serverChallenge = null;
                    state.setTargetInfo(null);
                    state.setFlags(flags);
                    state.setState(State.COMPLETE);
                    state.setSessionKey(sessionKey);//remember master key that can be used to decrypt messages.
                    break;
                case COMPLETE:
                    state.setState(State.FAILED);
                    throw new AuthenticationManagerException("Authentication failed");
                default:
                    throw new IOException("Invalid state");
            }

            state.setFlags(flags);

            return token;
        } catch (IOException ioe) {
            state.setState(State.FAILED);
            throw new AuthenticationManagerException(ioe.getMessage(), ioe);
        }
    }

    /**
     * This method will generate extended session security keys according to the blog post:
     * http://blogs.msdn.com/b/openspecification/archive/2010/04/20/ntlm-keys-and-sundry-stuff.aspx
     * here we don't support generation of NTLMv1 exchange keys
     * @param flags
     * @param nTOWFv2
     * @param ntChallengeResponseFields
     * @param nam
     * @param exportedSessionKey
     * @return
     * @throws AuthenticationManagerException
     */
    private byte[] generateSessionSecurityKeys(int flags, byte[] nTOWFv2, byte[] ntChallengeResponseFields, Type3Message nam, byte[] exportedSessionKey) throws AuthenticationManagerException {
        byte[] encryptedRandomSessionKeyFields = null;
        if (((flags & NtlmConstants.NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY/*0x80000*/) != 0) && ((flags & NtlmConstants.NTLMSSP_NEGOTIATE_SIGN/*0x10*/) != 0)) {
            HMACT64 hmac = new HMACT64(nTOWFv2); //generate session based key

            if ((flags & NtlmConstants.NTLMSSP_NEGOTIATE_KEY_EXCH) != 0) {
                hmac.update(ntChallengeResponseFields, 0, 16);
                exportedSessionKey = hmac.digest(); //Exported session key or Nonce(16)
                byte[] masterKey = new byte[16];
                secureRandom.nextBytes(masterKey);
                //calculate encrypted random session key
                byte[] keyExch = new byte[16];
                try {
                    Cipher rc4 = Cipher.getInstance("RC4");
                    rc4.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(exportedSessionKey, "RC4"));
                    rc4.update(masterKey, 0, 16, keyExch, 0);
                } catch (GeneralSecurityException gse) {
                    throw new AuthenticationManagerException("Failed to encrypt exchange key", gse);
                }

                encryptedRandomSessionKeyFields = keyExch;

                exportedSessionKey = masterKey;

                if(log.isLoggable(Level.FINE)) {
                    log.log(Level.FINE, "Extended Session Security key was sent to the server");
                }

            }
            else {
                hmac.update(ntChallengeResponseFields);
                exportedSessionKey = hmac.digest();
            }

        }

        nam.setSessionKey(encryptedRandomSessionKeyFields);
        return exportedSessionKey;
    }

    @Override
    public void resetAuthentication() {
        super.resetAuthentication();
        state.setState(State.NEGOTIATE);
    }

}

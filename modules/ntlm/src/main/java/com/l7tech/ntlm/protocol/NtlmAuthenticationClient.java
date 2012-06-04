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

    public NtlmAuthenticationClient() {
        this(new HashMap());
    }

    public NtlmAuthenticationClient(Map properties) {
        super(properties);
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
                case DEFAULT:
                    state.setState(State.NEGOTIATE);
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
                    log.log(Level.FINE, "NTLM Client: Negotiate Message sent");

                    state.setState(State.CHALLENGE);
                    break;
                case CHALLENGE:
                    Type2Message ncm = new Type2Message(token);

                    byte[] serverChallenge = ncm.getChallenge();

                    flags &= ncm.getFlags();
                    flags &= NtlmConstants.NTLMSSP_NEGOTIATE_VERSION_MASK;//-33554433;
                    flags &= NtlmConstants.NTLMSSP_R6_MASK;//-262145 r6 unused, always set to 0
                    flags &= NtlmConstants.NTLMSSP_TARGET_TYPE_SERVER_MASK;//-131073;
                    flags &= NtlmConstants.NTLMSSP_TARGET_TYPE_DOMAIN_MASK;// -65537;
                    flags |= NtlmConstants.NTLMSSP_NEGOTIATE_VERSION;//33554432;

                    char[] passwordChars = cred.getPassword();
                    if (passwordChars == null) {
                        throw new AuthenticationManagerException(AuthenticationManagerException.Status.STATUS_INVALID_CREDENTIALS, "Password is required");
                    }

                    String domainNameFields = (domainName != null ? domainName : "");
                    String userNameFields = name;

                    String password = new String(passwordChars);
                    byte[] responseKeyNT = NtlmPasswordAuthentication.nTOWFv2(domainNameFields, userNameFields, password);

                    byte[] clientChallenge = new byte[8];
                    secureRandom.nextBytes(clientChallenge);
                    long nanos1601 = (System.currentTimeMillis() + MILLISECONDS_BETWEEN_1970_AND_1601) * 10000L;

                    byte[] lmChallengeResponseFields = NtlmConstants.ZERO24;
                    byte[] ntChallengeResponseFields = NtlmPasswordAuthentication.getNTLMv2Response(responseKeyNT, serverChallenge, clientChallenge, nanos1601, ncm.getTargetInformation());

                    Type3Message nam = new Type3Message(flags, lmChallengeResponseFields, ntChallengeResponseFields, domainNameFields, userNameFields, workstation);

                    byte[] encryptedRandomSessionKeyFields = null;
                    byte[] sessionKey = null;
                    if ((flags & NtlmConstants.NTLMSSP_NEGOTIATE_SIGN) != 0) {
                        HMACT64 hmac = new HMACT64(responseKeyNT);
                        hmac.update(ntChallengeResponseFields, 0, 16);
                        sessionKey = hmac.digest();

                        encryptedRandomSessionKeyFields = sessionKey;

                        if ((flags & NtlmConstants.NTLMSSP_NEGOTIATE_KEY_EXCH) != 0) {
                            byte[] masterKey = new byte[16];
                            secureRandom.nextBytes(masterKey);

                            byte[] exchangedKey = new byte[16];
                            try {
                                Cipher rc4 = Cipher.getInstance("RC4");
                                rc4.init(1, new SecretKeySpec(sessionKey, "RC4"));
                                rc4.update(masterKey, 0, 16, exchangedKey, 0);
                            } catch (GeneralSecurityException gse) {
                                throw new AuthenticationManagerException("Failed to encrypt the negotiated exchange key", gse);
                            }

                            encryptedRandomSessionKeyFields = exchangedKey;

                            sessionKey = masterKey;
                        }
                    }

                    nam.setSessionKey(encryptedRandomSessionKeyFields);

                    token = nam.toByteArray();

                    if (((flags & NtlmConstants.NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY) != 0) && ((flags & NtlmConstants.NTLMSSP_NEGOTIATE_SIGN) != 0)) {
                        if (sessionKey == null) {
                            throw new AuthenticationManagerException("Can't sign the request if an NTLM sessionKey is null!");
                        }
                        log.log(Level.FINE, "Extended Session Security key was sent to the client");
                    }

                    log.log(Level.FINE, "NTLM Client negotiated NTLMv2");

                    serverChallenge = null;
                    state.setTargetInfo(null);
                    state.setFlags(flags);
                    state.setState(State.COMPLETE);
                    state.setSessionKey(sessionKey);
                    break;
                case COMPLETE:
                    state.setState(State.FAILED);
                    throw new AuthenticationManagerException(AuthenticationManagerException.Status.STATUS_INVALID_CREDENTIALS, "Authentication failed");
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


}

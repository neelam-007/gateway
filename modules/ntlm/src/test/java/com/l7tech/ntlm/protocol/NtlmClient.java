package com.l7tech.ntlm.protocol;

import com.l7tech.ntlm.protocol.NtlmConstants;
import jcifs.ntlmssp.NtlmFlags;
import jcifs.ntlmssp.Type1Message;
import jcifs.ntlmssp.Type2Message;
import jcifs.ntlmssp.Type3Message;
import jcifs.util.Base64;

import java.io.IOException;
import java.security.SecureRandom;

/**
 * Created by IntelliJ IDEA.
 * User: awitrisna
 * Date: 02/04/12
 * Time: 9:40 AM
 */

public class NtlmClient {

    private static final int TYPE_1_FLAGS =
            NtlmFlags.NTLMSSP_NEGOTIATE_56 |
                    NtlmFlags.NTLMSSP_NEGOTIATE_128 |
                    NtlmFlags.NTLMSSP_NEGOTIATE_NTLM2 |
                    NtlmFlags.NTLMSSP_NEGOTIATE_ALWAYS_SIGN |
                    NtlmFlags.NTLMSSP_REQUEST_TARGET;

    public static Type1Message generateType1Msg(final String domain, final String workstation) {
        final Type1Message type1Message = new Type1Message(TYPE_1_FLAGS, domain, workstation);
        type1Message.setFlag(NtlmConstants.NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY, true);
        type1Message.setFlag(NtlmConstants.NTLMSSP_NEGOTIATE_SIGN, true);
        type1Message.setFlag(NtlmConstants.NTLMSSP_NEGOTIATE_KEY_EXCH, true);
        return type1Message;
    }

    public static Type2Message generateType2Msg(final String domain, final String workstation) {
        final Type1Message type1Message = new Type1Message(TYPE_1_FLAGS, domain, workstation);

        SecureRandom random = new SecureRandom();
        byte[] serverChallenge = new byte[8];
        random.nextBytes(serverChallenge);

        final Type2Message type2Message = new Type2Message(type1Message, serverChallenge, domain);
        return type2Message;
    }

    public static Type3Message generateType3Msg(String username, String password,
                                   String domain, String workstation, String challenge)
            throws IOException {
        Type2Message type2Message;
        type2Message = new Type2Message(Base64.decode(challenge));
        final int type2Flags = type2Message.getFlags();
        final int type3Flags = type2Flags
                & (0xffffffff ^ (NtlmFlags.NTLMSSP_TARGET_TYPE_DOMAIN | NtlmFlags.NTLMSSP_TARGET_TYPE_SERVER));
        if (domain == null) {
            domain= "";
        }
        final Type3Message type3Message = new Type3Message(type2Message, password, domain,
                username, workstation, type3Flags);

        return type3Message;
    }
}

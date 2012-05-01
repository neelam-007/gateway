package com.l7tech.ntlm.protocol;

import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 */
public abstract class NtlmAuthenticationProvider extends HashMap implements AuthenticationProvider {
    private static final Logger log = Logger.getLogger(NtlmAuthenticationProvider.class.getName());
    private static final String HEX = "0x";

    protected static final int DEFAULT_NTLMSSP_FLAGS = //0x628882B7;//0b01100010100010001000001010110111
            NtlmConstants.NTLMSSP_NEGOTIATE_KEY_EXCH |
                    NtlmConstants.NTLMSSP_NEGOTIATE_128 |
                    NtlmConstants.NTLMSSP_NEGOTIATE_VERSION |
                    NtlmConstants.NTLMSSP_NEGOTIATE_TARGET_INFO |
                    NtlmConstants.NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY |
                    NtlmConstants.NTLMSSP_NEGOTIATE_NTLM |
                    NtlmConstants.NTLMSSP_NEGOTIATE_LM_KEY |
                    NtlmConstants.NTLMSSP_REQUEST_TARGET |
                    NtlmConstants.NTLMSSP_NEGOTIATE_OEM |
                    NtlmConstants.NTLMSSP_NEGOTIATE_UNICODE |
                    NtlmConstants.NTLMSSP_NEGOTIATE_ALWAYS_SIGN |
                    NtlmConstants.NTLMSSP_NEGOTIATE_SEAL |
                    NtlmConstants.NTLMSSP_NEGOTIATE_SIGN;


    public enum State {
        DEFAULT(0),
        NEGOTIATE(1),
        CHALLENGE(2),
        AUTHENTICATE(3),
        COMPLETE(4),
        FAILED(5),
        DISPOSED(255);

        int val;

        private State(int val) {
            this.val = val;
        }

        public static State toState(int val) {
            State[] states = State.values();
            for (State state : states) {
                if (state.val == val) {
                    return state;
                }
            }
            return DEFAULT;
        }

    }

    protected enum PropertyType {
        INVALID(-1),
        NULL(0),
        BYTEARRAY(1),
        STRING(2),
        SID(3),
        MAP(4);

        int value;

        private PropertyType(int val) {
            value = val;
        }

        public static PropertyType toPropertyType(int val) {
            PropertyType[] types = PropertyType.values();
            for (PropertyType type : types) {
                if (type.value == val) {
                    return type;
                }
            }
            return INVALID;
        }
    }

    static final long MILLISECONDS_BETWEEN_1970_AND_1601 = 11644473600000L;

    static SecureRandom secureRandom = new SecureRandom();

    protected String identity = null;
    protected Map account = null;
    protected byte[] targetInfo = null;
    protected NtlmAuthenticationState state = new NtlmAuthenticationState();

    public NtlmAuthenticationProvider(Map properties) {
        super(properties);
        if(properties.containsKey("flags")){
            try {
                String sflags = (String)properties.get("flags");
                if(sflags.startsWith(HEX)) {
                    sflags = sflags.substring(HEX.length());
                }
                state.setFlags(Integer.parseInt(sflags, 16));
            } catch (NumberFormatException e) {
                log.log(Level.WARNING, "Unable to parse Negotiate Flags");
            }
        }
    }

    public int getNtlmSspFlags(boolean setDefault) {
        if (state == null || (state.getFlags() == 0 && setDefault)) {
            //set flags to
            return DEFAULT_NTLMSSP_FLAGS;
        }
        return state.getFlags();
    }

    @Override
    public byte[] processAuthentication(byte[] token) throws AuthenticationManagerException {
        throw new AuthenticationManagerException("Not implemented!");
    }

    @Override
    public byte[] requestAuthentication(byte[] token, PasswordCredential creds) throws AuthenticationManagerException {
        throw new AuthenticationManagerException("Not implemented!");
    }

    public NtlmAuthenticationState getNtlmAuthenticationState() {
        return state;
    }

    @Override
    public void resetAuthentication() {
        state.setState(State.DEFAULT);
        state.setFlags(0);
        state.setServerChallenge(null);
        state.setSessionKey(null);
        state.setAccountInfo(null);
    }

    protected byte[] getTargetInfo() throws AuthenticationManagerException {

        if ((state.getTargetInfo() == null) || (state.getTargetInfo().length == 0)) {
            Av_Pair targetInfoList = new Av_Pair(Av_Pair.MsvAvType.MsvAvNbDomainName, (String) get("domain.netbios.name"), new Av_Pair(Av_Pair.MsvAvType.MsvAvNbComputerName, (String) get("localhost.netbios.name"), new Av_Pair(Av_Pair.MsvAvType.MsvAvDnsDomainName, (String) get("domain.dns.name"), new Av_Pair(Av_Pair.MsvAvType.MsvAvDnsComputerName, (String) get("localhost.dns.name"), new Av_Pair(Av_Pair.MsvAvType.MsvAvEOL, "", null)))));
            if (containsKey("tree.dns.name")) {
                targetInfoList = new Av_Pair(Av_Pair.MsvAvType.MsvAvDnsTreeName, (String) get("tree.dns.name"), targetInfoList);
            }
            try {
                state.setTargetInfo(targetInfoList.toByteArray(0));
            } catch (UnsupportedEncodingException uee) {
                throw new AuthenticationManagerException(uee.getMessage());
            }
        }
        return state.getTargetInfo();
    }


}

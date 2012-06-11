package com.l7tech.ntlm.protocol;

/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 */
public final class NtlmConstants {
    public static final int NTLMSSP_TARGET_TYPE_SERVER = 131072;
    public static final int NTLMSSP_TARGET_TYPE_SERVER_MASK = -131073;
    public static final int NTLMSSP_R6 = 262144;//reserved 6
    public static final int NTLMSSP_R6_MASK = -262145;
    public static final int NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY = 524288;
    public static final int NTLMSSP_NEGOTIATE_TARGET_INFO = 8388608;
    public static final int NTLMSSP_NEGOTIATE_VERSION = 33554432;
    public static final int NTLMSSP_NEGOTIATE_VERSION_MASK = -33554433;
    public static final int NTLMSSP_NEGOTIATE_128 = 536870912;
    public static final int NTLMSSP_NEGOTIATE_KEY_EXCH = 1073741824;
    public static final int NTLMSSP_NEGOTIATE_56 = -2147483648;
    public static final int NTLMSSP_TARGET_TYPE_DOMAIN = 65536;
    public static final int NTLMSSP_TARGET_TYPE_DOMAIN_MASK = -65537;
    public static final int NTLMSSP_NEGOTIATE_ALWAYS_SIGN = 32768;
    public static final int NTLMSSP_NEGOTIATE_OEM_WORKSTATION_SUPPLIED = 8192;
    public static final int NTLMSSP_NEGOTIATE_OEM_DOMAIN_SUPPLIED = 4096;
    public static final int NTLMSSP_NEGOTIATE_NTLM = 512;
    public static final int NTLMSSP_NEGOTIATE_LM_KEY = 128;
    public static final int NTLMSSP_NEGOTIATE_SEAL = 32;
    public static final int NTLMSSP_NEGOTIATE_SIGN = 16;
    public static final int NTLMSSP_REQUEST_TARGET = 4;
    public static final int NTLMSSP_NEGOTIATE_OEM = 2;
    public static final int NTLMSSP_NEGOTIATE_OEM_MASK = -3;
    public static final int NTLMSSP_NEGOTIATE_UNICODE = 1;


    private NtlmConstants() {

    }
}

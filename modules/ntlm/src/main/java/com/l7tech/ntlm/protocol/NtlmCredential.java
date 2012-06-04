package com.l7tech.ntlm.protocol;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 */
public class NtlmCredential {
    public static final Pattern INTERNET_PATTERN = Pattern.compile("([a-zA-Z0-9._%-]+)@([a-zA-Z0-9.-]+(\\\\.[a-zA-Z0-9.-])*)");
    public static final Pattern NETBIOS_PATTERN = Pattern.compile("([^\\*/?\":|+]+)\\\\([^\\*/?\":|+]+)");

    private String accountName;
    private String domain;
    private String host;
    private char[] password = null;


    public NtlmCredential(String acctname, char[] password)
            throws AuthenticationManagerException {
        this(null, null, acctname, password);
    }

    public NtlmCredential(String host, String domain, String acctname, char[] password)
            throws AuthenticationManagerException {
        if(!parseAccountName(acctname)){
            this.accountName = acctname;
            this.domain = domain;
        }
        this.host = host;
        if (password != null) {
            this.password = new char[password.length];
            System.arraycopy(password, 0, this.password, 0, password.length);
        }
    }

    private boolean parseAccountName(String acctname) {
        Matcher m = INTERNET_PATTERN.matcher(acctname);
        if(m.find()){
            this.accountName = m.group(1);
            this.domain = m.group(2);
            return true;
        }
        else {
            m = NETBIOS_PATTERN.matcher(acctname);
            if(m.find()) {
                this.domain = m.group(1);
                this.accountName = m.group(2);
                return true;
            }
        }
        return false;
    }

    public String getName() {
        return accountName;
    }

    public String getDomain() {
        return domain;
    }

    public char[] getPassword() {
        return this.password;
    }

    public String getHost() {
        return host;
    }

    public String toString() {
        return accountName;
    }
}

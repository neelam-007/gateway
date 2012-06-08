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
    private String password = null;


    public NtlmCredential(String acctname, String password)
            throws AuthenticationManagerException {
        this(null, null, acctname, password);
    }

    public NtlmCredential(String host, String domain, String acctname, String password)
            throws AuthenticationManagerException {
        if(!parseAccountName(acctname)){
            this.accountName = acctname;
            this.domain = domain;
        }
        this.host = host;
        this.password = password;
    }

    private boolean parseAccountName(String acctname) {
        Matcher m = INTERNET_PATTERN.matcher(acctname);
        if(m.find()){
            accountName = m.group(1);
            domain = m.group(2);
            return true;
        }
        else {
            m = NETBIOS_PATTERN.matcher(acctname);
            if(m.find()) {
                domain = m.group(1);
                accountName = m.group(2);
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

    public String getPassword() {
        return password;
    }

    public String getHost() {
        return host;
    }

    public String toString() {
        return accountName;
    }
}

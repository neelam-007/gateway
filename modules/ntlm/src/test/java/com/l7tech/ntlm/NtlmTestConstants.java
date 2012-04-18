package com.l7tech.ntlm;

import com.l7tech.ntlm.netlogon.NetLogon;
import org.junit.Before;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: awitrisna
 * Date: 09/04/12
 * Time: 9:44 AM
 */
public class NtlmTestConstants {

    public static final String USER = "ntlm_test";
    public static final String PASSWORD = "7layer]";
    public static final String DOMAIN = "L7TECH";
    public static final String WORKSTATION = "MYWORKSTATION";

    public static final Map<String, String> config;

    static {
        config = new HashMap<String, String>();
        config.put("domain.netbios.name", "L7TECH");
        config.put("domain.dns.name", "l7tech.dev");
        config.put("localhost.dns.name", "linux-12vk");
        config.put("localhost.netbios.name", "LINUX-12VK");
        config.put("server.dns.name", "test2008.l7tech.dev");
        config.put("service.account", "linux-12vk$@l7tech.dev");
        config.put("service.password", "linux-12vk$");
    }

}

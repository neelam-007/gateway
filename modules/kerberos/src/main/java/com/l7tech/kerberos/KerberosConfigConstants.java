package com.l7tech.kerberos;

/**
 * Constants used by the Kerberos configuration logic
 *
 * User: vchan
 */
public interface KerberosConfigConstants {

    static final String SYSPROP_SSG_HOME = "com.l7tech.server.varDirectory";
    static final String SYSPROP_LINE_SEP = "line.separator";
    static final String SYSPROP_LOGINCFG_PATH = "java.security.auth.login.config";
    static final String SYSPROP_KRB5CFG_PATH = "java.security.krb5.conf";
    static final String SYSPROP_KRB5_KDC = "java.security.krb5.kdc";
    static final String SYSPROP_KRB5_REALM = "java.security.krb5.realm";
    static final String SYSPROP_KRB5_ENC_TKT = "com.l7tech.server.krb5.tktenc";
    static final String SYSPROP_KRB5_ENC_TGS = "com.l7tech.server.krb5.tgsenc";
    static final String SYSPROP_KRB5_REFRESH = "com.l7tech.server.krb5.refresh";

    static final String ENCTYPES_TKT_DEFAULT = "rc4-hmac,des-cbc-md5";
    static final String ENCTYPES_TGS_DEFAULT = "rc4-hmac,des-cbc-md5";
    static final String REFRESH_DEFAULT = "true";

    static final String PATH_KEYTAB = "/kerberos.keytab";
    static final String PATH_LOGINCFG = "/login.config";
    static final String PATH_KRB5CFG = "/krb5.conf";

    static final String RESOURCE_SSB_LOGINCFG = "/com/l7tech/proxy/resources/login.config";
    static final String FILE_NAME_KRB5CFG = "krb5.conf";

    /**
     * Template for the login.config file:
     *
     * 0 - Path to the Keytab file
     */
    static final String LOGIN_CONFIG_TEMPLATE =
            "/////////////////////////////////\n" +
            "// Generated file, DO NOT EDIT //\n" +
            "/////////////////////////////////\n" +
            "\n" +
            "// Login module for SecureSpan Gateway\n" +
            "com.l7tech.common.security.kerberos.accept '{'\n" +
            "    com.sun.security.auth.module.Krb5LoginModule required\n" +
            "    useKeyTab=true\n" +
            "    keyTab=\"{0}\"\n" +
            "    refreshKrb5Config={1}\n" +
            "    isInitiator=false\n" +
            "    storeKey=true;\n" +
            "};\n" +
            "\n" +
            "// Login module for SecureSpan Gateway (initiator)\n" +
            "com.l7tech.common.security.kerberos.acceptinit '{'\n" +
            "    com.sun.security.auth.module.Krb5LoginModule required\n" +
            "    useKeyTab=true\n" +
            "    keyTab=\"{0}\"\n" +
            "    refreshKrb5Config={1}\n" +
            "    storeKey=true;\n" +
            "};\n" +
            "\n" +
            "// Login module for outbound routing with configured account\n" +
            "com.l7tech.common.security.kerberos.outbound.account '{'\n" +
            "    com.sun.security.auth.module.Krb5LoginModule required\n" +
            "    useKeyTab=false\n" +
            "    refreshKrb5Config={1}\n" +
            "    storeKey=true;\n" +
            "};\n" +
            "\n" +
            "// Login module for outbound routing with keytab\n" +
            "com.l7tech.common.security.kerberos.outbound.keytab '{'\n" +
            "    com.sun.security.auth.module.Krb5LoginModule required\n" +
            "    useKeyTab=true\n" +
            "    keyTab=\"{0}\"\n" +
            "    refreshKrb5Config={1}\n" +
            "    storeKey=true;\n" +
            "};\n";

    /**
     * Template for the krb5.conf file:
     *
     *  0 - UPPERCASE realm
     *  1 - lowercase realm
     *  2 - KDC block
     *  3 - IP for admin and kpasswd servers
     *  4 - default tkt encryption type
     *  5 - default tgs encryption type
     */
    static final String KRB5_CONF_TEMPLATE =
            "###############################\n" +
            "# Generated file, DO NOT EDIT #\n" +
            "###############################\n" +
            "\n" +
            "[libdefaults]\n" +
            "default_realm = {0}\n" +
            "default_tkt_enctypes = {4}\n" +
            "default_tgs_enctypes = {5}\n" +
            "\n" +
            "[realms]\n" +
            "{0} = '{'\n" +
            "{2}" +
//            "kdc =  {2}:88\n" +
            "admin_server =  {3}:88\n" +
            "kpasswd_server =  {3}:464\n" +
            "default_domain = {1}\n" +
            "}\n";

    /**
     * Template for the krb5.conf file on the SSB:
     *
     *  0 - UPPERCASE realm
     *  1 - lowercase realm
     *  2 - IP for KDC
     */
    static final String SSB_KRB5_CONF_TEMPLATE =
            "###############################\n" +
            "# Generated file, DO NOT EDIT #\n" +
            "###############################\n" +
            "\n" +
            "[libdefaults]\n" +
            "default_realm = {0}\n" +
            "default_tkt_enctypes = {3}\n" +
            "default_tgs_enctypes = {4}\n" +
            "\n" +
            "[realms]\n" +
            "{0} = '{'\n" +
            "kdc =  {2}:88\n" +
            "admin_server =  {2}:88\n" +
            "kpasswd_server =  {2}:464\n" +
            "default_domain = {1}\n" +
            "}\n";

}

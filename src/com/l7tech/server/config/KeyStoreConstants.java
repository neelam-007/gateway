package com.l7tech.server.config;

/**
 * User: megery
 */
public class KeyStoreConstants {

    public static final String LUNA_WINDOWS_REGKEY = "\\HKEY_LOCAL_MACHINE\\SOFTWARE\\SafeNet-Inc";
    public static final String LUNA_LINUX_INSTALL_FILE = "/usr/lunasa/bin/salogin";
    public static final int PASSWORD_LENGTH = 6;

    public static final String PROP_KEYSTORE_DIR = "keystoredir";
    public static final String PROP_SSL_KS_PASS = "sslkspasswd";
    public static final String PROP_CA_KS_PASS = "rootcakspasswd";
    public static final String PROP_KS_TYPE = "keystoretype";
    public static final String PROP_KS_ALIAS = "sslkeyalias";
    public static final String PROP_KS_ALIAS_DEFAULTVALUE = "tomcat";
    public static final String PROP_SSL_KEYSTORE_FILE = "sslkstorename";
    
    public static final String CA_KEYSTORE_FILE = "ca.ks";
    public static final String SSL_KEYSTORE_FILE = "ssl.ks";
    public static final String CA_CERT_FILE = "ca.cer";

    public static final String SSL_CERT_FILE = "ssl.cer";
    public static final String CA_ALIAS = "ssgroot";
    public static final String CA_DN_PREFIX = "cn=root.";

    public static final int CA_VALIDITY_DAYS = 5 * 365;
    public static final String SSL_ALIAS = "tomcat";
    public static final String SSL_DN_PREFIX = "cn=";
    public static final int SSL_VALIDITY_DAYS = 2 * 365;
}

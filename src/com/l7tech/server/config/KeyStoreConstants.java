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
}

package com.l7tech.server.config.systemconfig;

import com.l7tech.util.Pair;

import java.util.*;

/**
 * User: megery
 */
public class LdapAuthTypeView extends AuthTypeView {

    public Map<String, Pair<String, String>> reqCertActions = new TreeMap<String, Pair<String, String>>() {{
       put("1", new Pair<String, String>("The client will not request or check the server certificate", "never"));
       put("2", new Pair<String, String>("The client proceeds if no certificate or a bad certificate is presented", "allow"));
       put("3", new Pair<String, String>("The session is immediately terminated if a bad certificate is presented", "try"));
       put("4", new Pair<String, String>("The session is immediately terminated if no certificate or a bad certificate is presented", "demand"));
    }};


    public Map<String, Pair<String, String>> crlCheckActions = new TreeMap<String, Pair<String, String>>() {{
       put("1", new Pair<String, String>("No CRL checks are performed", "none"));
       put("2", new Pair<String, String>("Only check the CRL of the peer certificate", "peer"));
       put("3", new Pair<String, String>("Check the CRL for the whole certificate chain", "all"));
    }};


    private boolean ldapSecure;
    private String ldapServer;
    private String ldapBaseDn="";
    private String ldapPort="389";
    private boolean anonBind=false;
    private String ldapBindPassword="";
    private String ldapBindDn="";
    private String nssBaseShadowObj="ou=posixAccounts";
    private String nssBaseGroupObj="ou=posixAccounts";
    private String nssBasePasswdObj="ou=posixAccounts";
    private String ldapGroupName="ssgconfig_ldap";
    private String passHashAlg="md5";
    private String serverCaCertUrl ="";
    private String serverCaCertFile ="/home/ssgconfig/certificate.pem";
    private String ldapTlsReqCert="never";
    private String ldapTlsCrlCheck="none";
    private boolean ldapTlsClientAuth =false;
    private String ldapTlsClientCertFile="";
    private String ldapTlsClientKeyFile="";

    private boolean isLdapTlsAdvanced=false;
    private String ldapTlsCiphers="HIGH:MEDIUM:+SSLv2";
    private boolean isldapTlsCheckPeer=true;
    private String nssBindPolicy="soft";
    private String nssTimeLimit="10";
    private String nssBindTimeLimit="30";
    private String nssIdleTimeLimit="600";
    private String pamFilter="";
    private String pamLoginAttr = "uid";
    private String pamMaxUid="0";
    private String pamMinUid="0";


    public String getLdapServer() {
        return ldapServer;
    }

    public void setLdapServer(String ldapServer) {
        this.ldapServer = ldapServer;
    }

    public String getLdapBaseDn() {
        return ldapBaseDn;
    }

    public void setLdapBaseDn(String ldapBaseDn) {
        this.ldapBaseDn = ldapBaseDn;
    }

    @Override
    public List<String> asConfigLines() {
        return writeConfigLines();
    }

    private String convertToYesNo(final boolean value) {
        return value ?"yes":"no";
    }

    private List<String> writeConfigLines() {
        List<String> configLines = new ArrayList<String>();
        configLines.add(makeNameValuePair("LDAP_TYPE",(isLdapSecure()?"ldaps":"ldap")));
        configLines.add(makeNameValuePair("LDAP_SRV", getLdapServer()));
        configLines.add(makeNameValuePair("LDAP_PORT", getLdapPort()));
        configLines.add(makeNameValuePair("LDAP_BASE", getLdapBaseDn()));
        configLines.add(makeNameValuePair("LDAP_ANONYM", convertToYesNo(isAnonBind())));
        if (!isAnonBind()) {
            configLines.add(makeNameValuePair("LDAP_BINDDN", getLdapBindDn()));
            configLines.add(makeNameValuePair("LDAP_BIND_PASSWD", getLdapBindPassword()));
        }
        configLines.add(makeNameValuePair("NSS_BASE_PASSWD", getNssBasePasswdObj()));
        configLines.add(makeNameValuePair("NSS_BASE_GROUP", getNssBaseGroupObj()));
        configLines.add(makeNameValuePair("NSS_BASE_SHADOW", getNssBaseShadowObj()));

        configLines.add(makeNameValuePair("LDAP_GROUP_NAME", getLdapGroupName()));
        configLines.add(makeNameValuePair("PASS_HASH_ALGO", getPassHashAlg()));

        if (isLdapSecure()) {
            //TLS Options
            configLines.add(makeNameValuePair("LDAP_CACERT_URL", getServerCaCertUrl()));
            configLines.add(makeNameValuePair("LDAP_CACERT_FILE", getServerCaCertFile()));
            configLines.add(makeNameValuePair("LDAP_TLS_REQCERT", reqCertActions.get(getLdapTlsReqCert()).right));
            configLines.add(makeNameValuePair("LDAP_TLS_CRLCHECK", crlCheckActions.get(getLdapTlsCrlCheck()).right));
            configLines.add(makeNameValuePair("CLT_TLS_AUTH", convertToYesNo(isLdapTlsClientAuth())));
            configLines.add(makeNameValuePair("LDAP_TLS_CERT", getLdapTlsClientCertFile()));
            configLines.add(makeNameValuePair("LDAP_TLS_KEY", getLdapTlsClientKeyFile()));

            //TLS Advanced options go here, we aren't asking about them but they are still needed in the config file
            configLines.add(makeNameValuePair("ADVANCED_TLS_CONF", convertToYesNo(isLdapTlsAdvanced())));
            configLines.add(makeNameValuePair("LDAP_TLS_CIPHER_SUITE", getLdapTlsCiphers()));
            configLines.add(makeNameValuePair("LDAP_TLS_CHECKPEER", convertToYesNo(isldapTlsCheckPeer())));

            //NSS Options go here
            configLines.add(makeNameValuePair("NSS_BIND_POLICY", getNssBindPolicy()));
            configLines.add(makeNameValuePair("NSS_TIMELIMIT", getNssTimeLimit()));
            configLines.add(makeNameValuePair("NSS_BIND_TIMELIMIT", getNssBindTimeLimit()));
            configLines.add(makeNameValuePair("NSS_IDLE_TIMELIMIT", getNssIdleTimeLimit()));
        }
        configLines.add(makeNameValuePair("PAM_FILTER", getPamFilter()));
        configLines.add(makeNameValuePair("PAM_LOGIN_ATTR", getPamLoginAttr()));
        configLines.add(makeNameValuePair("PAM_MAX_UID", getPamMaxUid()));
        configLines.add(makeNameValuePair("PAM_MIN_UID", getPamMinUid()));

        return configLines;
    }

    public List<String> describe() {
        List<String> descs = new ArrayList<String>();
        descs.add("The following LDAP configuration will be applied:");

        descs.add("\tLDAP Server IP : " + getLdapServer());
        descs.add("\tLDAP Server Port : " + getLdapPort());
        descs.add("\tLDAP Base DN : " + getLdapBaseDn());

        if (isAnonBind()) {
            descs.add("\tUsing anonymous LDAP Bind");
        } else {
            descs.add("\tUsing LDAP Credentials");
            descs.add("\t\tBind DN : " + getLdapBindDn());
            descs.add("\t\tBind Password : " + "<HIDDEN>");
        }
        if (isLdapSecure()) {
            descs.add("\tUsing LDAPS with the following settings:");
            if (! "".equals(getServerCaCertUrl())) {
                descs.add("\t\tCA Certificate URL : " + getServerCaCertUrl());
            } else {

                descs.add("\t\tCA Certificate File : " + getServerCaCertFile());
            }

            if (reqCertActions.get(getLdapTlsReqCert()) != null) {
                descs.add("\t\tClient Handling of server certificates");
                descs.add("\t\t\t" + reqCertActions.get(getLdapTlsReqCert()).left);
            }
            if (crlCheckActions.get(getLdapTlsCrlCheck()) != null) {
                descs.add("\t\tClient CRL Checking");
                descs.add("\t\t\t" + crlCheckActions.get(getLdapTlsCrlCheck()).left);
            }

            if (! isLdapTlsClientAuth()) {
                descs.add("\tNot Using Client Authentication.");
            } else {
                descs.add("\tUsing Client Authentication");
                if (isLdapTlsClientAuth()) {
                    descs.add("Client Certificate File : " + getLdapTlsClientCertFile());
                    descs.add("Client Key : " + getLdapTlsClientKeyFile());
                }
            }
        }

        return descs;
    }

    public void setLdapSecure(boolean ldapSecure) {
        this.ldapSecure = ldapSecure;
    }

    public boolean isLdapSecure() {
        return ldapSecure;
    }

    public void setLdapPort(String ldapPort) {
        this.ldapPort = ldapPort;
    }

    public String getLdapPort() {
        return ldapPort;
    }

    public void setLdapAnonBind(boolean anonBind) {
        this.anonBind = anonBind;
    }

    public boolean isAnonBind() {
        return anonBind;
    }

    public void setLdapBindDn(String ldapBindDn) {
        this.ldapBindDn = ldapBindDn;
    }

    public String getLdapBindDn() {
        return ldapBindDn;
    }

    public void setLdapBindPassword(String ldapBindPasswd) {
        this.ldapBindPassword = ldapBindPasswd;
    }

    public String getLdapBindPassword() {
        return ldapBindPassword;
    }

    public void setNssBasePasswdObj(String nssBasePasswdObj) {
        this.nssBasePasswdObj = nssBasePasswdObj;
    }

    public String getNssBasePasswdObj() {
        return nssBasePasswdObj;
    }

    public void setNssBaseGroupObj(String nssBaseGroupObj) {
        this.nssBaseGroupObj = nssBaseGroupObj;
    }

    public String getNssBaseGroupObj() {
        return nssBaseGroupObj;
    }

    public void setNssBaseShadowObj(String nssBaseShadowObj) {
        this.nssBaseShadowObj = nssBaseShadowObj;
    }

    public String getNssBaseShadowObj() {
        return nssBaseShadowObj;
    }

    public void setLdapGroupName(String ldapGroupName) {
        this.ldapGroupName = ldapGroupName;
    }

    public String getLdapGroupName() {
        return ldapGroupName;
    }

    public void setPassHashAlg(String passHashAlg) {
        this.passHashAlg = passHashAlg;
    }

    public String getPassHashAlg() {
        return passHashAlg;
    }

    public void setLdapCaCertURL(String caCertUrl) {
        this.serverCaCertUrl = caCertUrl;
    }

    public String getServerCaCertUrl() {
        return serverCaCertUrl;
    }

    public void setLdapCaCertFile(String caCertFile) {
        this.serverCaCertFile = caCertFile;
    }

    public String getServerCaCertFile() {
        return serverCaCertFile;
    }

    public void setLdapTlsReqCert(String ldapTlsReqCert) {
        this.ldapTlsReqCert = ldapTlsReqCert;
    }

    public String getLdapTlsReqCert() {
        return ldapTlsReqCert;
    }

    public void setLdapTlsCrlCheck(String ldapTlsCrlCheck) {
        this.ldapTlsCrlCheck = ldapTlsCrlCheck;
    }

    public String getLdapTlsCrlCheck() {
        return ldapTlsCrlCheck;
    }

    public void setIsLdapTlsClientAuth(boolean ldapTlsClientAuth) {
        this.ldapTlsClientAuth = ldapTlsClientAuth;
    }

    public boolean isLdapTlsClientAuth() {
        return ldapTlsClientAuth;
    }

    public void setLdapTlsClientCertFile(String ldapTlsClientCertFile) {
        this.ldapTlsClientCertFile = ldapTlsClientCertFile;
    }

    public String getLdapTlsClientCertFile() {
        return ldapTlsClientCertFile;
    }

    public void setLdapTlsClientKeyFile(String ldapTlsClientKeyFile) {
        this.ldapTlsClientKeyFile = ldapTlsClientKeyFile;
    }

    public String getLdapTlsClientKeyFile() {
        return ldapTlsClientKeyFile;
    }

    public void setIsLdapTlsAdvanced(boolean ldapTlsAdvanced) {
        this.isLdapTlsAdvanced = ldapTlsAdvanced;
    }

    public boolean isLdapTlsAdvanced() {
        return isLdapTlsAdvanced;
    }

    public void setLdapTlsCiphers(String ldapTlsCiphers) {
        this.ldapTlsCiphers = ldapTlsCiphers;
    }

    public String getLdapTlsCiphers() {
        return ldapTlsCiphers;
    }

    public void setIsLdapTlsCheckPeer(boolean isldapTlsCheckPeer) {
        this.isldapTlsCheckPeer = isldapTlsCheckPeer;
    }

    public boolean isldapTlsCheckPeer() {
        return isldapTlsCheckPeer;
    }

    public void setNssBindPolicy(String nssBindPolicy) {
        this.nssBindPolicy = nssBindPolicy;
    }

    public String getNssBindPolicy() {
        return nssBindPolicy;
    }

    public void setNssTimeLimit(String nssTimeLimit) {
        this.nssTimeLimit = nssTimeLimit;
    }

    public String getNssTimeLimit() {
        return nssTimeLimit;
    }

    public void setNssBindTimeLimit(String nssBindTimeLimit) {
        this.nssBindTimeLimit = nssBindTimeLimit;
    }

    public String getNssBindTimeLimit() {
        return nssBindTimeLimit;
    }

    public void setNssIdleTimeLimit(String nssIdleTimeLimit) {
        this.nssIdleTimeLimit = nssIdleTimeLimit;
    }

    public String getNssIdleTimeLimit() {
        return nssIdleTimeLimit;
    }

    public void setPamFilter(String pamFilter) {
        this.pamFilter = pamFilter;
    }

    public String getPamFilter() {
        return pamFilter;
    }

    public void setPamLoginAttr(String pamLoginAttr) {
        this.pamLoginAttr = pamLoginAttr;
    }

    public String getPamLoginAttr() {
        return pamLoginAttr;
    }

    public void setPamMaxUid(String pamMaxUid) {
        this.pamMaxUid = pamMaxUid;
    }

    public String getPamMaxUid() {
        return pamMaxUid;
    }

    public void setPamMinUid(String pamMinUid) {
        this.pamMinUid = pamMinUid;
    }

    public String getPamMinUid() {
        return pamMinUid;
    }

    public String createPamFilterFromGids(String[] gids) {
        String filter = "";
        if (gids != null) {
            if (gids.length == 1) {
                filter = "gidNumber=" + gids[0];
            } else {
                filter = "|";
                for (String oneGid : gids) {
                    filter += "(gidNumber=" + oneGid + ")";
                }
            }
        }
        return filter;
    }
}

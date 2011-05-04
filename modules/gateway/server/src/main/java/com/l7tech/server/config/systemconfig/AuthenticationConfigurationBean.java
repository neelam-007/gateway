package com.l7tech.server.config.systemconfig;

import com.l7tech.server.config.beans.BaseConfigurationBean;

import java.util.ArrayList;
import java.util.List;

/**
 * User: megery
 */
public class AuthenticationConfigurationBean extends BaseConfigurationBean{
    private String radiusAddress;
    private String radiusTimeout;
    private String radiusSecret;
    private String ldapAddress;
    private String ldapBase;

    public AuthenticationConfigurationBean(String name, String description) {
        super(name, description);
    }

    @Override
    public void reset() {
        authType = null;
    }

    @Override
    protected void populateExplanations() {
        explanations.add("\nAuthorization configuration: \n\t");
        explanations.add(concatConfigLines(EOL + "\t", getAuthConfigEntries()));
    }

    public List<String> getAuthConfigEntries() {
        List<String> authConfigEntries = new ArrayList<String>();
        authConfigEntries.add("\tAuthorization Type: " + getAuthType().getNiceName());
        switch (getAuthType()) {
            case LOCAL:
                break;
            case RADIUS:
                authConfigEntries.add("\tRADIUS Server Address: " + getRadiusAddress());
                authConfigEntries.add("\tRADIUS Reply Timeout: " + getRadiusTimeout());
                authConfigEntries.add("\tRADIUS Shared Secret: " + getRadiusSecret());
                break;
            case LDAP:
                authConfigEntries.add("\tLDAP Server Address: " + getLdapAddress());
                authConfigEntries.add("\tLDAP Base DN: " + getLdapBase());
                break;
            case RADIUS_LDAP:
                authConfigEntries.add("\tRADIUS Server Address: " + getRadiusAddress());
                authConfigEntries.add("\tRADIUS Reply Timeout: " + getRadiusTimeout());
                authConfigEntries.add("\tRADIUS Shared Secret: " + getRadiusSecret());
                authConfigEntries.add("\tLDAP Server Address: " + getLdapAddress());
                authConfigEntries.add("\tLDAP Base DN: " + getLdapBase());
                break;
        }
        return authConfigEntries;
    }

    public AuthType getAuthType() {
        return authType;
    }

    public void setAuthType(AuthType authType) {
        this.authType = authType;
    }

    public void setRadiusAddress(String radiusAddress) {
        this.radiusAddress = radiusAddress;
    }

    public String getRadiusAddress() {
        return radiusAddress;
    }

    public void setRadiusTimeout(String radiusTimeout) {
        this.radiusTimeout = radiusTimeout;
    }

    public String getRadiusTimeout() {
        return radiusTimeout;
    }

    public void setRadiusSecret(String radiusSecret) {
        this.radiusSecret = radiusSecret;
    }

    public String getRadiusSecret() {
        return radiusSecret;
    }

    public void setLdapAddress(String ldapAddress) {
        this.ldapAddress = ldapAddress;
    }

    public String getLdapAddress() {
        return ldapAddress;
    }

    public void setLdapBase(String ldapBase) {
        this.ldapBase = ldapBase;
    }

    public String getLdapBase() {
        return ldapBase;
    }

    public String asConfigFile() {
        StringBuilder sb = new StringBuilder();
        sb.append("CFG_TYPE=").append(authType.toConfigLine()).append(EOL);
        switch (authType) {
            case RADIUS:
                sb.append("RADIUS_SRV_IP=").append(getRadiusAddress()).append(EOL);
                sb.append("RADIUS_SECRET=").append(getRadiusSecret()).append(EOL);
                sb.append("RADIUS_TIMEOUT=").append(getRadiusTimeout()).append(EOL);
                break;
            case LDAP:
                sb.append("LDAP_SRV_IP=").append(getLdapAddress()).append(EOL);
                sb.append("LDAP_BASE=").append(getLdapBase()).append(EOL);
                break;
            case RADIUS_LDAP:
                sb.append("RADIUS_SRV_IP=").append(getRadiusAddress()).append(EOL);
                sb.append("RADIUS_SECRET=").append(getRadiusSecret()).append(EOL);
                sb.append("RADIUS_TIMEOUT=").append(getRadiusTimeout()).append(EOL);
                sb.append("LDAP_SRV_IP=").append(getLdapAddress()).append(EOL);
                sb.append("LDAP_BASE=").append(getLdapBase()).append(EOL);
                break;
            default : //LOCAL/file
                break;
        }

        return sb.toString();
    }

    public static enum AuthType {
        LOCAL("Local System","file"),
        LDAP("LDAP","ldap_only"),
        RADIUS("RADIUS","radius_only"),
        RADIUS_LDAP("RADIUS with LDAP","radius_with_ldap");



        public String getNiceName() {
            return niceName;
        }

        public String describe() {
            return getNiceName();
        }

        public String toConfigLine() {
            return configLine;
        }

        private AuthType(String name, String configLine) {
            this.niceName = name;
            this.configLine = configLine;
        }

        private String niceName;
        private String configLine;
    }

    AuthType authType = null;
}

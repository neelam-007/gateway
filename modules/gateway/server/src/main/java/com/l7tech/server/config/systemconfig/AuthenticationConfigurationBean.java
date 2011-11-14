package com.l7tech.server.config.systemconfig;

import com.l7tech.server.config.beans.BaseConfigurationBean;

import java.util.*;

/**
 * User: megery
 */
public class AuthenticationConfigurationBean extends BaseConfigurationBean{

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
        explanations.add(concatConfigLines(EOL + "\t", describe()));
    }

    private List<String> describe() {
        List<String> descs = new ArrayList<String>();
        for (AuthTypeView authTypeView : authTypeViews) {
            descs.add(concatConfigLines(EOL + "\t", authTypeView.describe()));
            descs.add(EOL);
        }
        return descs;
    }

    public void setAuthType(AuthType authType) {
        this.authType = authType;
    }

    public void addAuthTypeView(AuthTypeView authType) {
        authTypeViews.add(authType);
    }

    public String asConfigFile() {
        StringBuilder sb = new StringBuilder();
        sb.append("CFG_TYPE=").append("\"").append(authType.getConfigTypeName()).append("\"").append(EOL);
        for (AuthTypeView authTypeView : authTypeViews) {
            List<String> configLines = authTypeView.asConfigLines();
            if (configLines != null) {
                for (String configLine : configLines) {
                    sb.append(configLine).append(EOL);
                }
            }
        }
        return sb.toString();
    }


    public static enum AuthType {
        LOCAL("Local System", "file"),
        RADIUS("RADIUS only", "radius_only"),
        LDAP("LDAP(s) only","ldap_only"),
        LDAP_RADIUS("RADIUS with LDAP(s)","radius_with_ldap");

       AuthType(String name, String configTypeName) {
            this.niceName = name;
            this.configTypeName = configTypeName;
        }

        public String getNiceName() {
            return niceName;
        }

        public String getConfigTypeName() {
            return configTypeName;
        }

        private String niceName;
        private String configTypeName;
    }


    private AuthType authType = null;
    private Set<AuthTypeView > authTypeViews = new HashSet<AuthTypeView>();
}

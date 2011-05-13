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

    public List<String> describe() {
        return authType.describe();
    }

    public void setAuthType(AuthType authType) {
        this.authType = authType;
    }

    public void setAuthData(AuthTypeDescriptor desc, String value) {
        authType.putAuthTypeData(desc, value);
    }

    public String asConfigFile() {
        StringBuilder sb = new StringBuilder();
        sb.append("CFG_TYPE=").append(authType.getConfigTypeName()).append(EOL);
        for (String s : authType.asConfigFile()) {
            sb.append(s).append(EOL);
        }
        return sb.toString();
    }


    public static enum AuthType {
        LOCAL("Local System", "file") {
            @Override
            public List<AuthTypeDescriptor> getPrompts() {
                //local auth doesn't have any prompting
                return new ArrayList<AuthTypeDescriptor>();
            }
        },

        RADIUS("RADIUS", "radius_only"){
            @Override
            public List<AuthTypeDescriptor> getPrompts() {
                return new ArrayList<AuthTypeDescriptor>(
                        Arrays.asList(
                            new AuthTypeDescriptor(EOL + "Enter the address of the RADIUS server","RADIUS_SRV_IP", "RADIUS Server Address","\\S+"),
                            new AuthTypeDescriptor(EOL + "Enter the RADIUS shared secret", "RADIUS_SECRET" ,"RADIUS Shared Secret",null,true),
                            new AuthTypeDescriptor(EOL + "Enter the RADIUS timeout (in seconds)","RADIUS_TIMEOUT","RADIUS Reply Timeout","\\d+")
                        )
                );
            }
        },
//        LDAP("LDAP","ldap_only") {
//            @Override
//            public List<AuthTypeDescriptor> getPrompts() {
//                return new ArrayList<AuthTypeDescriptor>(
//                    Arrays.asList(
//                        new AuthTypeDescriptor(EOL + "Enter the address of the LDAP server: ", "LDAP_SRV_IP", "LDAP Server Address","\\S+"),
//                        new AuthTypeDescriptor(EOL + "Enter the LDAP base DN: ", "LDAP_BASE", "LDAP Base DN","\\S+")
//                    )
//                );
//            }
//        },

        ;


        AuthType(String name, String configTypeName) {
            this.niceName = name;
            this.configTypeName = configTypeName;
            authDescriptors = new HashMap<AuthTypeDescriptor, String>();
        }

        public void putAuthTypeData(AuthTypeDescriptor key, String val) {
            authDescriptors.put(key, val);
        }

        public String getNiceName() {
            return niceName;
        }

        public String getConfigTypeName() {
            return configTypeName;
        }

        public List<String> describe() {
            List<String> descs = new ArrayList<String>();
            descs.add("\tAuthorization Type: " + getNiceName());
            for (Map.Entry<AuthTypeDescriptor, String> entry : authDescriptors.entrySet()) {
                AuthTypeDescriptor desc = entry.getKey();
                String value = entry.getValue();
                if (desc.isPassword) {
                    descs.add("\t" + desc.getDescription() + " = <HIDDEN>");
                } else {
                    descs.add("\t" + desc.getDescription() + " = " + value);
                }
            }
            return descs;
        }


        public List<String> asConfigFile() {
            List<String> vals = new ArrayList<String>();
            for (Map.Entry<AuthTypeDescriptor, String> entry : authDescriptors.entrySet()) {
                AuthTypeDescriptor desc = entry.getKey();
                String val = entry.getValue();
                if (val != null) vals.add(desc.getConfigLine() + "=" + val);
            }
            return vals;
        }

        public abstract List<AuthTypeDescriptor> getPrompts();


        private String niceName;
        private String configTypeName;
        protected Map<AuthTypeDescriptor, String> authDescriptors;
        protected List<AuthTypeDescriptor> prompts;
    }

    public static class AuthTypeDescriptor {
        String prompt;
        String description;
        String configLine;
        private String allowedPattern;
        private boolean isPassword;

        public AuthTypeDescriptor(String prompt, String configLine, String description, String allowedPattern) {
            this(prompt, configLine, description, allowedPattern, false);
        }

        public AuthTypeDescriptor(String prompt, String configLine, String description, String allowedPattern, boolean password) {
            this.prompt = prompt;
            this.description = description;
            this.configLine = configLine;
            this.allowedPattern = allowedPattern;
            isPassword = password;
        }

        public String getPrompt() {
            return prompt;
        }

        public String getDescription() {
            return description;
        }

        public String getConfigLine() {
            return configLine;
        }

        public String getAllowedPattern() {
            return allowedPattern;
        }

        public boolean isPassword() {
            return isPassword;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            AuthTypeDescriptor that = (AuthTypeDescriptor) o;

            return !(configLine != null ? !configLine.equals(that.configLine) : that.configLine != null) && !(description != null ? !description.equals(that.description) : that.description != null) && !(prompt != null ? !prompt.equals(that.prompt) : that.prompt != null);

        }

        @Override
        public int hashCode() {
            int result = prompt != null ? prompt.hashCode() : 0;
            result = 31 * result + (description != null ? description.hashCode() : 0);
            result = 31 * result + (configLine != null ? configLine.hashCode() : 0);
            return result;
        }

    }

    AuthType authType = null;
}

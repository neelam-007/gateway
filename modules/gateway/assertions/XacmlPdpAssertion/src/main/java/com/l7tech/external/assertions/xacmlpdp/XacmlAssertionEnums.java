/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Jun 9, 2009
 * Time: 2:12:04 PM
 */
package com.l7tech.external.assertions.xacmlpdp;

public class XacmlAssertionEnums {

    /**
     * MessageTarget is used by the PEP for
     * * Knowing whether to write the XACML request to the request / response or a context variable
     *
     * MessageTarget is used by the PDP for
     * * Knowing where to find the XACML request
     * * knowing where to write the PDP decision response to 
     */
    public enum MessageTarget{
        REQUEST_MESSAGE("Default Request"),
        RESPONSE_MESSAGE("Default Response"),
        CONTEXT_VARIABLE("Message Variable");

        MessageTarget(String targetName) {
            this.targetName = targetName;
        }

        public String getTargetName() {
            return targetName;
        }

        private final String targetName;
    }

    public static enum XacmlVersionType {
        V1_0("1.0", "urn:oasis:names:tc:xacml:1.0:context"),
        V1_1("1.1", "urn:oasis:names:tc:xacml:1.0:context"),
        V2_0("2.0", "urn:oasis:names:tc:xacml:2.0:context:schema:os");

        private String displayName;
        private String namespace;

        private XacmlVersionType(String displayName, String namespace) {
            this.displayName = displayName;
            this.namespace = namespace;
        }

        public String getNamespace() {
            return namespace;
        }

        public String toString() {
            return displayName;
        }

        /**
         * Validate that a String is a valid xacml request version
         * @param versionToCheck String to check is a valid xacml request version. Cannot be null
         * @return true if versionToCheck is a valid version, false otherwise
         */
        public static boolean isValidXacmlVersionType(String versionToCheck){
            if(versionToCheck == null) throw new NullPointerException("versionToCheck cannot be null");

            for(XacmlVersionType valueType: XacmlVersionType.values()){
                if(valueType.getNamespace().equals(versionToCheck)) return true;
            }

            return false;
        }
    }

    public static enum SoapVersion {
        NONE("None", null, null),
        v1_1("SOAP 1.1", "http://schemas.xmlsoap.org/soap/envelope/", "soapenv"),
        v1_2("SOAP 1.2", "http://www.w3.org/2003/05/soap-envelope", "s12");

        private String displayName;
        private String uri;
        private String prefix;

        private SoapVersion(String displayName, String uri, String prefix) {
            this.displayName = displayName;
            this.uri = uri;
            this.prefix = prefix;
        }

        public String getUri() {
            return uri;
        }

        public String getPrefix() {
            return prefix;
        }

        public String toString() {
            return displayName;
        }
    }
}

package com.l7tech.common.uddi;

/**
 * Enumerated type for supported policy attachment versions.
 *
 * @author Steve Jones
 */
public enum PolicyAttachmentVersion {

    //- PUBLIC

    /**
     * Policy attachment 1.2 2003/03 (default)
     */
    v1_2 ( "Web Services Policy 1.2",
            "uddi:schemas.xmlsoap.org:policytypes:2003_03",
            "uddi:schemas.xmlsoap.org:localpolicyreference:2003_03",
            "uddi:schemas.xmlsoap.org:remotepolicyreference:2003_03" ),

    /**
     * Policy attachment 1.5 (W3C) 
     */
    v1_5 ( "Web Services Policy 1.5",
            "uddi:w3.org:ws-policy:v1.5:attachment:policytypes",
            "uddi:w3.org:ws-policy:v1.5:attachment:localpolicyreference",
            "uddi:w3.org:ws-policy:v1.5:attachment:remotepolicyreference" );

    public String getName() {
        return name;
    }

    public String getTModelKeyPolicyTypes() {
        return tModelKeyPolicy;
    }

    public String getTModelKeyLocalPolicyReference() {
        return tModelKeyLocalPolicyReference;
    }

    public String getTModelKeyRemotePolicyReference() {
        return tModelKeyRemotePolicyReference;
    }

    //- PRIVATE

    private final String name;
    private final String tModelKeyPolicy;
    private final String tModelKeyLocalPolicyReference;
    private final String tModelKeyRemotePolicyReference;

    private PolicyAttachmentVersion(final String name,
                                    final String tModelKeyPolicy,
                                    final String tModelKeyLocalPolicyReference,
                                    final String tModelKeyRemotePolicyReference) {
        this.name = name;
        this.tModelKeyPolicy = tModelKeyPolicy;
        this.tModelKeyLocalPolicyReference = tModelKeyLocalPolicyReference;
        this.tModelKeyRemotePolicyReference = tModelKeyRemotePolicyReference;
    }

}

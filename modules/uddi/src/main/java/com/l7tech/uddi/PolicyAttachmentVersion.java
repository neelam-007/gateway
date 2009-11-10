package com.l7tech.uddi;

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

    /**
     * Is the given TModel key a local reference in any known WS-Policy Attachment version.
     *
     * @param tModelKey The key to check.
     * @return true if the given key is a local policy reference
     */
    public static boolean isAnyLocalReference( final String tModelKey ) {
        boolean isLocalReference = false;

        for ( PolicyAttachmentVersion policyAttachmentVersion : values() ) {
            if ( policyAttachmentVersion.getTModelKeyLocalPolicyReference().equals( tModelKey )) {
                isLocalReference = true;
                break;
            }
        }

        return isLocalReference;
    }

    /**
     * Is the given TModel key a remote reference in any known WS-Policy Attachment version.
     *
     * @param tModelKey The key to check.
     * @return true if the given key is a remote policy reference
     */
    public static boolean isAnyRemoteReference( final String tModelKey ) {
        boolean isRemoteReference = false;

        for ( PolicyAttachmentVersion policyAttachmentVersion : values() ) {
            if ( policyAttachmentVersion.getTModelKeyRemotePolicyReference().equals( tModelKey )) {
                isRemoteReference = true;
                break;
            }
        }

        return isRemoteReference;
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

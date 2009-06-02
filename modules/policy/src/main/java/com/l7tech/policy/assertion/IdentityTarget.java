package com.l7tech.policy.assertion;

import java.io.*;

/**
 * Bean for identity target information.
 */
public final class IdentityTarget implements Comparable, Serializable {

    //- PUBLIC

    public enum TargetIdentityType { USER, GROUP, PROVIDER, TAG }

    public IdentityTarget() {
    }

    public IdentityTarget( final TargetIdentityType targetIdentityType,
                           final long identityProviderOid ) {
        this( targetIdentityType, identityProviderOid, null, null );
    }

    public IdentityTarget( final String identityTag ) {
        this( TargetIdentityType.TAG, 0, identityTag, null );
    }

    public IdentityTarget( final IdentityTarget identityTarget ) {
        if ( identityTarget != null ) {
            this.targetIdentityType = identityTarget.targetIdentityType;
            this.identityProviderOid = identityTarget.identityProviderOid;
            this.identityId = identityTarget.identityId;
            this.identityInfo = identityTarget.identityInfo;
        }
    }

    public IdentityTarget( final TargetIdentityType targetIdentityType,
                           final long identityProviderOid,
                           final String identityId,
                           final String identityInfo ) {
        this.targetIdentityType = targetIdentityType;
        this.identityProviderOid = identityProviderOid;
        this.identityId = identityId;
        this.identityInfo = identityInfo;        
    }

    public TargetIdentityType getTargetIdentityType() {
        return targetIdentityType;
    }

    public void setTargetIdentityType( final TargetIdentityType targetIdentityType ) {
        this.targetIdentityType = targetIdentityType;
    }

    public long getIdentityProviderOid() {
        return identityProviderOid;
    }

    public void setIdentityProviderOid( final long identityProviderOid ) {
        this.identityProviderOid = identityProviderOid;
    }

    public String getIdentityId() {
        return identityId;
    }

    public void setIdentityId( final String identityId ) {
        this.identityId = identityId;
    }

    public String getIdentityInfo() {
        return identityInfo;
    }

    public void setIdentityInfo( final String identityInfo ) {
        this.identityInfo = identityInfo;
    }

    /**
     * Get a description of the target identity, suitable for logging, etc.
     *
     * @return A description of the target identity.
     */
    public String describeIdentity() {
        StringBuilder identityBuilder = new StringBuilder();

        if ( targetIdentityType != null ) {
            switch ( targetIdentityType ) {
                case TAG:
                    identityBuilder.append("Tag '");
                    identityBuilder.append(identityId);
                    identityBuilder.append("'");
                    break;
                case PROVIDER:
                    identityBuilder.append("Identity Provider #");
                    identityBuilder.append(identityProviderOid);
                    break;
                case USER:
                    identityBuilder.append("User ID '");
                    identityBuilder.append(identityId);
                    identityBuilder.append("'");
                    if ( identityInfo != null ) {
                        identityBuilder.append(", login '");
                        identityBuilder.append(identityInfo);
                        identityBuilder.append("'");
                    }
                    break;
                case GROUP:
                    identityBuilder.append("Group ID '");
                    identityBuilder.append(identityId);
                    identityBuilder.append("'");
                    if ( identityInfo != null ) {
                        identityBuilder.append(", name '");
                        identityBuilder.append(identityInfo);
                        identityBuilder.append("'");
                    }
                    break;
            }
        } else {
            identityBuilder.append("<Not Specified>");
        }

        return identityBuilder.toString();
    }

    @Override
    public int compareTo( Object other ) {
        IdentityTarget otherIdentityTarget = (IdentityTarget) other;

        int result = 0;
        if ( targetIdentityType != null && otherIdentityTarget.targetIdentityType!=null ) {
            result = targetIdentityType.compareTo(otherIdentityTarget.targetIdentityType);
        }
        if ( result == 0 ) {
            result = Long.valueOf(identityProviderOid).compareTo(otherIdentityTarget.identityProviderOid);
        }
        if ( result == 0 && identityId!=null && otherIdentityTarget.identityId!=null) {
            result = identityId.compareTo(otherIdentityTarget.identityId);
        }
        
        return result;
    }

    @SuppressWarnings({"RedundantIfStatement"})
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IdentityTarget that = (IdentityTarget) o;

        if (identityProviderOid != that.identityProviderOid) return false;
        if (identityId != null ? !identityId.equals(that.identityId) : that.identityId != null) return false;
        if (targetIdentityType != that.targetIdentityType) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = (targetIdentityType != null ? targetIdentityType.hashCode() : 0);
        result = 31 * result + (int) (identityProviderOid ^ (identityProviderOid >>> 32));
        result = 31 * result + (identityId != null ? identityId.hashCode() : 0);
        return result;
    }

    //- PRIVATE

    private TargetIdentityType targetIdentityType;
    private long identityProviderOid;
    private String identityId;
    private String identityInfo;
}

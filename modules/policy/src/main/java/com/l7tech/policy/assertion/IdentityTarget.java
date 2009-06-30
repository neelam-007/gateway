package com.l7tech.policy.assertion;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.IdentityHeader;

import java.io.*;
import java.util.*;

/**
 * Bean for identity target information.
 */
public final class IdentityTarget implements Comparable, Serializable, UsesEntities {

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

    /**
     * Create a new identity target from the given target.
     *
     * @param identityTarget The target (may be null)
     */
    public IdentityTarget( final IdentityTarget identityTarget ) {
        if ( identityTarget != null ) {
            this.targetIdentityType = identityTarget.targetIdentityType;
            this.identityProviderOid = identityTarget.identityProviderOid;
            this.identityProviderName = identityTarget.identityProviderName;
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

    /**
     * @deprecated For serialization only
     */
    @Deprecated
    public void setTargetIdentityType( final TargetIdentityType targetIdentityType ) {
        this.targetIdentityType = targetIdentityType;
    }

    public long getIdentityProviderOid() {
        return identityProviderOid;
    }

    /**
     * @deprecated For serialization only
     */
    @Deprecated
    public void setIdentityProviderOid( final long identityProviderOid ) {
        this.identityProviderOid = identityProviderOid;
    }

    public boolean needsIdentityProviderName() {
        return targetIdentityType!=TargetIdentityType.TAG && identityProviderName==null;
    }

    /**
     * Set the identity provider name for display use only.
     *
     * @param identityProviderName The name to use
     */
    public void setIdentityProviderName( final String identityProviderName ) {
        this.identityProviderName = identityProviderName;
    }

    public String getIdentityId() {
        return identityId;
    }

    /**
     * @deprecated For serialization only
     */
    @Deprecated
    public void setIdentityId( final String identityId ) {
        this.identityId = identityId;
    }

    public String getIdentityInfo() {
        return identityInfo;
    }

    /**
     * @deprecated For serialization only
     */
    @Deprecated
    public void setIdentityInfo( final String identityInfo ) {
        this.identityInfo = identityInfo;
    }

    /**
     * Get a description of the identity target suitable for GUI use.
     *
     * @return The description of the identity target.
     */
    public String describeIdentityForDisplay() {
        StringBuilder identityBuilder = new StringBuilder();

        if ( targetIdentityType != null ) {
            switch ( targetIdentityType ) {
                case TAG:
                    identityBuilder.append("Identity Tag: ");
                    identityBuilder.append(identityId);
                    break;
                case PROVIDER:
                    identityBuilder.append("Authenticated against: ");
                    if ( identityProviderName != null ) {
                        identityBuilder.append(identityProviderName);
                    } else {
                        identityBuilder.append("#");
                        identityBuilder.append(identityProviderOid);
                    }
                    break;
                case USER:
                    identityBuilder.append("User: ");
                    if ( identityInfo != null ) {
                        identityBuilder.append(identityInfo);
                    } else {
                        identityBuilder.append("#");
                        identityBuilder.append(identityId);
                    }
                    identityBuilder.append(", ");
                    if ( identityProviderName != null ) {
                        identityBuilder.append(identityProviderName);
                    } else {
                        identityBuilder.append("#");
                        identityBuilder.append(identityProviderOid);
                    }
                    break;
                case GROUP:
                    identityBuilder.append("Group Membership: ");
                    if ( identityInfo != null ) {
                        identityBuilder.append(identityInfo);
                    } else {
                        identityBuilder.append("#");
                        identityBuilder.append(identityId);
                    }
                    identityBuilder.append(", ");
                    if ( identityProviderName != null ) {
                        identityBuilder.append(identityProviderName);
                    } else {
                        identityBuilder.append("#");
                        identityBuilder.append(identityProviderOid);
                    }
                    break;
            }
        } else {
            identityBuilder.append("<Unknown>");
        }

        return identityBuilder.toString();
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
                    if ( identityProviderName != null ) {
                        identityBuilder.append(", name '");
                        identityBuilder.append(identityProviderName);
                        identityBuilder.append("'");
                    }
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

    /**
     *
     */
    @Override
    public EntityHeader[] getEntitiesUsed() {
        Collection<EntityHeader> headers = new ArrayList<EntityHeader>();
        if ( targetIdentityType != null ) {
            switch (targetIdentityType) {
                case GROUP:
                    headers.add( new EntityHeader(identityProviderOid, EntityType.ID_PROVIDER_CONFIG, identityProviderName, null) );
                    headers.add( new IdentityHeader(identityProviderOid, identityId, EntityType.GROUP, identityInfo, null, null, null) );
                    break;
                case PROVIDER:
                    headers.add( new EntityHeader(identityProviderOid, EntityType.ID_PROVIDER_CONFIG, identityProviderName, null) );
                    break;
                case TAG:
                    break;
                case USER:
                    headers.add( new EntityHeader(identityProviderOid, EntityType.ID_PROVIDER_CONFIG, identityProviderName, null) );
                    headers.add( new IdentityHeader(identityProviderOid, identityId, EntityType.USER, identityInfo, null, null, null) );
                    break;
            }
        }

        return headers.toArray( new EntityHeader[headers.size()] );        
    }

    /**
     *
     */
    @Override
    public void replaceEntity( final EntityHeader oldEntityHeader,
                               final EntityHeader newEntityHeader ) {
        if ( oldEntityHeader.getType() != null &&
             oldEntityHeader.getType() == newEntityHeader.getType() ) {
            switch ( oldEntityHeader.getType() ) {
                case GROUP:
                case USER:
                    if ( newEntityHeader instanceof IdentityHeader &&
                         oldEntityHeader  instanceof IdentityHeader ) {
                        IdentityHeader newIdentityHeader = (IdentityHeader) newEntityHeader;
                        IdentityHeader oldIdentityHeader = (IdentityHeader) oldEntityHeader;
                        if ( oldIdentityHeader.getProviderOid() == identityProviderOid &&
                             oldIdentityHeader.getStrId() != null &&
                             oldIdentityHeader.getStrId().equalsIgnoreCase(identityId) ) {
                            this.identityProviderOid = newIdentityHeader.getProviderOid();
                            this.identityProviderName = null;
                            this.identityId = newIdentityHeader.getStrId();
                            this.identityInfo = newIdentityHeader.getName();
                        }
                    }
                    break;
                case ID_PROVIDER_CONFIG:
                    if ( oldEntityHeader.getOid() == this.identityProviderOid ) {
                        this.identityProviderOid = newEntityHeader.getOid();
                        this.identityProviderName = null; // name in header may be stale
                    }
                    break;
            }
        }
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
            result = identityId.compareToIgnoreCase(otherIdentityTarget.identityId);
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
        if (identityId != null ? !identityId.equalsIgnoreCase(that.identityId) : that.identityId != null) return false;
        if (targetIdentityType != that.targetIdentityType) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = (targetIdentityType != null ? targetIdentityType.hashCode() : 0);
        result = 31 * result + (int) (identityProviderOid ^ (identityProviderOid >>> 32));
        result = 31 * result + (identityId != null ? identityId.toLowerCase().hashCode() : 0);
        return result;
    }

    //- PRIVATE

    private TargetIdentityType targetIdentityType;
    private long identityProviderOid;
    private String identityProviderName; // not part of identity (eq/hash)
    private String identityId; // case insensitive identifier
    private String identityInfo; // not part of identity (eq/hash)
}

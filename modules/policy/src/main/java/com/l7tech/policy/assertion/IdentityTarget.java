package com.l7tech.policy.assertion;

import com.l7tech.objectmodel.*;
import com.l7tech.util.GoidUpgradeMapper;

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
                           final Goid identityProviderGoid) {
        this( targetIdentityType, identityProviderGoid, null, null );
    }

    public IdentityTarget( final String identityTag ) {
        this( TargetIdentityType.TAG, null, identityTag, null );
    }

    /**
     * Create a new identity target from the given target.
     *
     * @param identityTarget The target (may be null)
     */
    public IdentityTarget( final IdentityTarget identityTarget ) {
        if ( identityTarget != null ) {
            this.targetIdentityType = identityTarget.targetIdentityType;
            this.identityProviderGoid = identityTarget.identityProviderGoid;
            this.identityProviderName = identityTarget.identityProviderName;
            this.identityId = identityTarget.identityId;
            this.identityInfo = identityTarget.identityInfo;
        }
    }

    public IdentityTarget( final TargetIdentityType targetIdentityType,
                           final Goid identityProviderGoid,
                           final String identityId,
                           final String identityInfo ) {
        this.targetIdentityType = targetIdentityType;
        this.identityProviderGoid = identityProviderGoid;
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



    public Goid getIdentityProviderOid() {
        return identityProviderGoid;
    }

    /**
     * @deprecated For serialization only
     */
    @Deprecated
    public void setIdentityProviderOid(final Goid identityProviderGoid) {
        this.identityProviderGoid = identityProviderGoid;
    }      // For backward compat while parsing pre-GOID policies.  Not needed for new assertions.

    @Deprecated
    public void setIdentityProviderOid( long providerOid ) {
        this.identityProviderGoid = (providerOid == -2) ?
                new Goid(0,-2L):
                GoidUpgradeMapper.mapOid(EntityType.ID_PROVIDER_CONFIG, providerOid);
        mapUserId();
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
        mapUserId();
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

    private final Goid INTERNAL_IDENTITY_PROVIDER = new Goid(0,-2);
    private void mapUserId(){
        if(getIdentityId()!=null && getIdentityId().length()!=32 && getIdentityProviderOid()!=null&& !getIdentityProviderOid().equals(GoidEntity.DEFAULT_GOID)){
            try{
                Long groupOidId = Long.parseLong(getIdentityId());
                if(getIdentityProviderOid().equals(INTERNAL_IDENTITY_PROVIDER)){
                    setIdentityId(GoidUpgradeMapper.mapOidFromTableName("internal_user", groupOidId).toString());
                }else{
                    setIdentityId(GoidUpgradeMapper.mapOidFromTableName("fed_user",groupOidId).toString());
                }
            }catch(NumberFormatException e){
                // no need to map dn group id
            }
        }
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
                        identityBuilder.append(identityProviderGoid);
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
                        identityBuilder.append(identityProviderGoid);
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
                        identityBuilder.append(identityProviderGoid);
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
                    identityBuilder.append(identityProviderGoid);
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
                    headers.add( new EntityHeader(identityProviderGoid, EntityType.ID_PROVIDER_CONFIG, identityProviderName, null) );
                    headers.add( new IdentityHeader(identityProviderGoid, identityId, EntityType.GROUP, identityInfo, null, null, null) );
                    break;
                case PROVIDER:
                    headers.add( new EntityHeader(identityProviderGoid, EntityType.ID_PROVIDER_CONFIG, identityProviderName, null) );
                    break;
                case TAG:
                    break;
                case USER:
                    headers.add( new EntityHeader(identityProviderGoid, EntityType.ID_PROVIDER_CONFIG, identityProviderName, null) );
                    headers.add( new IdentityHeader(identityProviderGoid, identityId, EntityType.USER, identityInfo, null, null, null) );
                    break;
            }
        }

        return headers.toArray(new EntityHeader[headers.size()]);
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
                        if ( oldIdentityHeader.getProviderGoid() == identityProviderGoid &&
                             oldIdentityHeader.getStrId() != null &&
                             oldIdentityHeader.getStrId().equalsIgnoreCase(identityId) ) {
                            this.identityProviderGoid = newIdentityHeader.getProviderGoid();
                            this.identityProviderName = null;
                            this.identityId = newIdentityHeader.getStrId();
                            this.identityInfo = newIdentityHeader.getName();
                        }
                    }
                    break;
                case ID_PROVIDER_CONFIG:
                    if ( oldEntityHeader.getGoid().equals(this.identityProviderGoid)) {
                        this.identityProviderGoid = newEntityHeader.getGoid();
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
            result = identityProviderGoid.compareTo(otherIdentityTarget.identityProviderGoid);
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

        if (identityProviderGoid != null ? !identityProviderGoid.equals(that.identityProviderGoid) : that.identityProviderGoid != null) return false;
        if (identityId != null ? !identityId.equalsIgnoreCase(that.identityId) : that.identityId != null) return false;
        if (targetIdentityType != that.targetIdentityType) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = (targetIdentityType != null ? targetIdentityType.hashCode() : 0);
        result = 31 * result + (identityProviderGoid != null ? identityProviderGoid.hashCode() : 0);
        result = 31 * result + (identityId != null ? identityId.toLowerCase().hashCode() : 0);
        return result;
    }

    //- PRIVATE

    private TargetIdentityType targetIdentityType;
    private Goid identityProviderGoid;
    private String identityProviderName; // not part of identity (eq/hash)
    private String identityId; // case insensitive identifier
    private String identityInfo; // not part of identity (eq/hash)
}

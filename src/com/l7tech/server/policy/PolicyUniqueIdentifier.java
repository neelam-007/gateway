package com.l7tech.server.policy;

import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.common.util.HexUtils;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.Map;
import java.util.Collections;
import java.util.TreeMap;
import java.util.HashMap;
import java.io.UnsupportedEncodingException;

/**
 * Unique Identifier for a version of a policy and it's dependencies.
 *
 * @author steve
 */
public class PolicyUniqueIdentifier {

    //- PUBLIC

    /**
     * Create a PolicyUniqueIdentifier for the given policy and it's dependencies.
     *
     * @param policyOid The policy identifier
     * @param policyVersion The version of the main policy
     * @param usedPolicyVersions The identifiers and versions of used policies
     */
    public PolicyUniqueIdentifier(final Long policyOid,
                                  final Integer policyVersion,
                                  final Map<Long,Integer> usedPolicyVersions) {
        if ( policyOid == null || policyOid == PersistentEntity.DEFAULT_OID ) throw new IllegalArgumentException("Invalid policyOid " + policyOid);
        if ( policyVersion == null ) throw new IllegalArgumentException( "policyVersion must not be null" );
        if ( usedPolicyVersions == null ) throw new IllegalArgumentException( "usedPolicyVersions must not be null" );

        this.policyOid = policyOid;
        this.policyVersion = policyVersion;
        this.usedPolicyVersions = Collections.unmodifiableMap( new TreeMap<Long,Integer>( usedPolicyVersions ) );
    }

    /**
     * Get the identifier for the policy.
     *
     * @return The policy id
     */
    public Long getPolicyOid() {
        return policyOid;
    }

    /**
     * Get the unique (version specific) policy identifier.
     *
     * @return The policy unique identifier
     */
    public String getPolicyUniqueIdentifer() {
        String pui = null;

        lock.readLock().lock();
        try {
            pui = uniqueIdentifer;
        } finally {
            lock.readLock().unlock();
        }

        if ( pui == null ) {
            lock.writeLock().lock();
            try {
                pui = uniqueIdentifer;
                if ( pui == null ) {
                    pui = uniqueIdentifer = generateUniqueIdentifier();
                }
            } finally {
                lock.writeLock().unlock();
            }
        }

        return pui;
    }

    /**
     * Get the map of policy oids to versions.
     *
     * @param includeSelf true to include the ID/version of the main policy
     * @return the map of policy oids to versions.
     */
    public Map<Long,Integer> getUsedPoliciesAndVersions(boolean includeSelf) {
        Map<Long,Integer> pandv = new HashMap<Long,Integer>(usedPolicyVersions);

        if ( includeSelf ) {
            pandv.put( policyOid, policyVersion );
        }

        return pandv;
    }

    @SuppressWarnings( { "RedundantIfStatement" } )
    @Override
    public boolean equals( Object o ) {
        if( this == o ) return true;
        if( o == null || getClass() != o.getClass() ) return false;

        PolicyUniqueIdentifier that = (PolicyUniqueIdentifier) o;

        if( !policyOid.equals( that.policyOid ) ) return false;
        if( !policyVersion.equals( that.policyVersion ) ) return false;
        if( !usedPolicyVersions.equals( that.usedPolicyVersions ) ) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = policyOid.hashCode();
        result = 31 * result + policyVersion.hashCode();
        result = 31 * result + usedPolicyVersions.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "PolicyUID[" + getUniqueVersion() + "]";
    }

    //- PRIVATE

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Long policyOid;
    private final Integer policyVersion;
    private final Map<Long,Integer> usedPolicyVersions;
    private String uniqueIdentifer;

    private String getUniqueVersion() {
        StringBuilder uniqueVersion = new StringBuilder(128);

        uniqueVersion.append( policyOid );
        uniqueVersion.append( '|' );
        uniqueVersion.append( policyVersion );

        for ( Map.Entry<Long,Integer> usedPV : usedPolicyVersions.entrySet() ) {
            uniqueVersion.append( ',' );
            uniqueVersion.append( usedPV.getKey() );
            uniqueVersion.append( '|' );
            uniqueVersion.append( usedPV.getValue() );
        }

        return uniqueVersion.toString();
    }

    private String generateUniqueIdentifier() {
        try {
            return HexUtils.hexDump( HexUtils.getMd5Digest( getUniqueVersion().getBytes( "UTF-8") ) );
        } catch ( UnsupportedEncodingException uee ) {
            throw new RuntimeException("UTF-8 support is required", uee);
        }
    }
}

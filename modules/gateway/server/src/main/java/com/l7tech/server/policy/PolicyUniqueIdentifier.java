package com.l7tech.server.policy;

import com.l7tech.objectmodel.Goid;
import com.l7tech.util.Charsets;
import com.l7tech.util.HexUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
     * @param policyGoid The policy identifier
     * @param policyVersion The version of the main policy
     * @param usedPolicyVersions The identifiers and versions of used policies
     */
    public PolicyUniqueIdentifier(final Goid policyGoid,
                                  final Integer policyVersion,
                                  final Map<Goid,Integer> usedPolicyVersions) {
        if ( policyGoid == null || Goid.isDefault(policyGoid) ) throw new IllegalArgumentException("Invalid policyGoid " + policyGoid);
        if ( policyVersion == null ) throw new IllegalArgumentException( "policyVersion must not be null" );
        if ( usedPolicyVersions == null ) throw new IllegalArgumentException( "usedPolicyVersions must not be null" );

        this.policyGoid = policyGoid;
        this.policyVersion = policyVersion;
        this.usedPolicyVersions = Collections.unmodifiableMap( new TreeMap<Goid,Integer>( usedPolicyVersions ) );
    }

    /**
     * Get the identifier for the policy.
     *
     * @return The policy id
     */
    public Goid getPolicyGoid() {
        return policyGoid;
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
    public Map<Goid,Integer> getUsedPoliciesAndVersions(boolean includeSelf) {
        Map<Goid,Integer> pandv = new HashMap<Goid,Integer>(usedPolicyVersions);

        if ( includeSelf ) {
            pandv.put( policyGoid, policyVersion );
        }

        return pandv;
    }

    @SuppressWarnings( { "RedundantIfStatement" } )
    @Override
    public boolean equals( Object o ) {
        if( this == o ) return true;
        if( o == null || getClass() != o.getClass() ) return false;

        PolicyUniqueIdentifier that = (PolicyUniqueIdentifier) o;

        if( !Goid.equals(policyGoid, that.policyGoid ) ) return false;
        if( !policyVersion.equals( that.policyVersion ) ) return false;
        if( !usedPolicyVersions.equals( that.usedPolicyVersions ) ) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = policyGoid != null ? policyGoid.hashCode() : 0;
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
    private final Goid policyGoid;
    private final Integer policyVersion;
    private final Map<Goid,Integer> usedPolicyVersions;
    private String uniqueIdentifer;

    private String getUniqueVersion() {
        StringBuilder uniqueVersion = new StringBuilder(128);

        uniqueVersion.append( policyGoid.toHexString() );
        uniqueVersion.append( '|' );
        uniqueVersion.append( policyVersion );

        for ( Map.Entry<Goid,Integer> usedPV : usedPolicyVersions.entrySet() ) {
            uniqueVersion.append( ',' );
            uniqueVersion.append( usedPV.getKey() );
            uniqueVersion.append( '|' );
            uniqueVersion.append( usedPV.getValue() );
        }

        return uniqueVersion.toString();
    }

    private String generateUniqueIdentifier() {
        return HexUtils.hexDump( HexUtils.getMd5Digest( getUniqueVersion().getBytes(Charsets.UTF8) ) );
    }
}

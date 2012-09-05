package com.l7tech.server.policy;

import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.util.Config;
import com.l7tech.util.Functions;
import com.l7tech.objectmodel.*;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.event.AdminInfo;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Property;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.transaction.annotation.Propagation;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Gateway's production implementation of {@link PolicyVersionManager}.
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
public class PolicyVersionManagerImpl extends HibernateEntityManager<PolicyVersion, EntityHeader> implements PolicyVersionManager {
    @SuppressWarnings({ "FieldNameHidesFieldInSuperclass" })
    protected static final Logger logger = Logger.getLogger(PolicyVersionManagerImpl.class.getName());

    private final Config config;

    public PolicyVersionManagerImpl( Config config ) {
        this.config = config;
    }

    @Override
    @Transactional(propagation=Propagation.SUPPORTS)
    public Class<? extends Entity> getImpClass() {
        return PolicyVersion.class;
    }

    @Override
    @Transactional(propagation=Propagation.SUPPORTS)
    public String getTableName() {
        return "policy_version";
    }

    @Override
    @Transactional(readOnly=true)
    public PolicyVersion findByPrimaryKey(long policyOid, long policyVersionOid) throws FindException {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("oid", policyVersionOid);
        map.put("policyOid", policyOid);
        List<PolicyVersion> found = findMatching(Collections.singletonList(map));
        if (found == null || found.isEmpty())
            return null;
        if (found.size() > 1)
            throw new FindException("Found more than one PolicyVersion with oid=" + policyVersionOid + " and policy_oid=" + policyOid); // can't happen
        return found.iterator().next();
    }

    @Override
    @Transactional(readOnly=true)
    public List<PolicyVersion> findAllForPolicy(long policyOid) throws FindException {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("policyOid", policyOid);
        return findMatching(Collections.singletonList(map));
    }

    @Override
    public PolicyVersion checkpointPolicy(Policy newPolicy, boolean activated, boolean newEntity) throws ObjectModelException {
        final long policyOid = newPolicy.getOid();
        if (policyOid == Policy.DEFAULT_OID)
            throw new IllegalArgumentException("Unable to checkpoint policy without a valid OID");

        AdminInfo adminInfo = AdminInfo.find(false);
        PolicyVersion ver = snapshot(newPolicy, adminInfo, activated, newEntity);

        // If the most recent PolicyVersion matches then this was a do-nothing policy change
        // and should be ignored (Bug #4569, #10662)
        final PolicyVersion last = findLatestRevisionForPolicy( policyOid );
        if ( last!=null ) {
            if ( last.getXml()!=null && last.getXml().equals( newPolicy.getXml() ) ) {
                if ( activated && !last.isActive() ) {
                    last.setActive( true );
                    update( last );
                    deactivateVersions(policyOid, last.getOid());
                }
                return last;
            } else if ( ver.getOrdinal() <= last.getOrdinal() ) {
                ver.setOrdinal( last.getOrdinal() + 1L );
            }
        }

        final long versionOid = save(ver);
        ver.setOid(versionOid);

        if (activated) {
            // Deactivate all previous versions
            deactivateVersions(policyOid, versionOid);
        }

        deleteStaleRevisions(policyOid, ver);
        return ver;
    }

    @Override
    public PolicyVersion findLatestRevisionForPolicy(final long policyOid) {
        return getHibernateTemplate().execute(new ReadOnlyHibernateCallback<PolicyVersion>() {
            @Override
            public PolicyVersion doInHibernateReadOnly( final Session session ) throws HibernateException, SQLException {
                final DetachedCriteria detachedCriteria = DetachedCriteria.forClass( getImpClass() );
                detachedCriteria.add( Property.forName( "policyOid" ).eq( policyOid ) );
                detachedCriteria.setProjection( Property.forName( "ordinal" ).max() );

                final Criteria criteria = session.createCriteria( getImpClass() );
                criteria.add( Property.forName( "policyOid" ).eq( policyOid ) );
                criteria.add( Property.forName( "ordinal" ).eq( detachedCriteria ) );
                return (PolicyVersion) criteria.uniqueResult();
            }
        } );
    }

    private void deleteStaleRevisions(final long policyOid, final PolicyVersion justSaved) throws FindException, DeleteException {
        final long justSavedOid = justSaved.getOid();

        // Delete oldest anonymous revisions if we have exceeded MAX_REVISIONS
        // Revisions that have been assigned a name won't be deleted
        int numToKeep = config.getIntProperty( ServerConfigParams.PARAM_POLICY_VERSIONING_MAX_REVISIONS, 20);
        List<PolicyVersion> revisions = new ArrayList<PolicyVersion>(findAllForPolicy(policyOid));

        // Don't count revisions against the limit if they have been assigned names
        revisions = Functions.grep(revisions, new Functions.Unary<Boolean,PolicyVersion>() {
            @Override
            public Boolean call(PolicyVersion policyVersion) {
                boolean inactive = !policyVersion.isActive();
                boolean notTheOneWeJustSaved = policyVersion.getOid() != justSavedOid;
                boolean belongsToOurPolicy = policyVersion.getPolicyOid() == policyOid;
                boolean nameIsEmpty = isNameEmpty(policyVersion);

                // Candidates for deletion are inactive, anonymous, and not the one we just saved
                return inactive && belongsToOurPolicy && notTheOneWeJustSaved && nameIsEmpty;
            }
        });
        Collections.sort(revisions, new Comparator<PolicyVersion>() {
            @Override
            public int compare(PolicyVersion o1, PolicyVersion o2) {
                return new Long(o1.getOrdinal()).compareTo(o2.getOrdinal());
            }
        });
        int num = revisions.size();

        if (!justSaved.isActive() && isNameEmpty(justSaved))
            num++; // We won't try to delete the one we just saved, but if it would otherwise be a deletion candidate, it still uses up a slot

        if (num >= numToKeep) {
            for (PolicyVersion revision : revisions) {
                if (revision.getPolicyOid() == policyOid && revision.getOid() != justSavedOid) {
                    delete(revision);
                    num--;
                    if (num <= numToKeep)
                        break;
                }
            }
        }
    }

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.NONE;
    }

    private static boolean isNameEmpty(PolicyVersion policyVersion) {
        return policyVersion.getName() == null || policyVersion.getName().length() < 1;
    }

    @Override
    public void deactivateVersions(final long policyOid, final long versionOid) throws UpdateException {
        try {
            getHibernateTemplate().execute(new HibernateCallback<Void>() {
                @Override
                public Void doInHibernate(Session session) throws HibernateException, SQLException {
                    FlushMode oldFlushMode = session.getFlushMode();
                    try {
                        session.setFlushMode(FlushMode.COMMIT);
                        session.createQuery("update versioned PolicyVersion set active = :active where policyOid = :policyOid and oid <> :versionOid")
                                .setBoolean("active", false)
                                .setLong("policyOid", policyOid)
                                .setLong("versionOid", versionOid)
                                .executeUpdate();
                        return null;
                    } finally {
                        session.setFlushMode(oldFlushMode);
                    }
                }
            });
        } catch (DataAccessException e) {
            throw new UpdateException(e);
        }
    }

    @Override
    public PolicyVersion findActiveVersionForPolicy(long policyOid) throws FindException {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("policyOid", policyOid);
        map.put("active", Boolean.TRUE);
        List<PolicyVersion> found = findMatching(Collections.singletonList(map));
        if (found == null || found.isEmpty())
            return null;
        if (found.size() > 1)
            throw new FindException("Found more than one active PolicyVersion with policy_oid=" + policyOid); // can't happen
        return found.iterator().next();
    }

    private static PolicyVersion snapshot( Policy policy, AdminInfo adminInfo, boolean activated, boolean newEntity) {
        long policyOid = policy.getOid();
        PolicyVersion ver = new PolicyVersion();
        ver.setPolicyOid(policyOid);
        ver.setActive(activated);

        // The entity version numbering for Policy starts at zero, but due to quirks in how save() vs update()
        // behave, both the initial saved Policy and the first update() of an existing policy reach this code
        // with a version number of zero.  So, we increment once to switch to one-based numbering, the increment
        // once more if this isn't the initial save, so we get smooth "1,2,3,4,5" numbering of the ordinals.
        long ordinal = (long) (policy.getVersion() + 1);
        if (!newEntity) ordinal++;

        ver.setOrdinal(ordinal);
        ver.setTime(System.currentTimeMillis());
        ver.setUserLogin(adminInfo.login);
        ver.setUserProviderOid(adminInfo.identityProviderOid);
        ver.setXml(policy.getXml());
        return ver;
    }
}

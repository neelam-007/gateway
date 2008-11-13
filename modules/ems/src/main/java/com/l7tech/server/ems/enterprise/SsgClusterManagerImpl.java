package com.l7tech.server.ems.enterprise;

import com.l7tech.objectmodel.*;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.DataAccessException;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Entity manager for {@link SsgCluster}.
 *
 * @since Enterprise Manager 1.0
 * @author rmak
 */
public class SsgClusterManagerImpl extends HibernateEntityManager<SsgCluster, EntityHeader> implements SsgClusterManager {
    private static final Logger logger = Logger.getLogger(SsgClusterManagerImpl.class.getName());

    private EnterpriseFolderManager enterpriseFolderManager;

    public EnterpriseFolderManager getEnterpriseFolderManager() {
        return enterpriseFolderManager;
    }

    public void setEnterpriseFolderManager(EnterpriseFolderManager enterpriseFolderManager) {
        this.enterpriseFolderManager = enterpriseFolderManager;
    }

    public Class<? extends Entity> getImpClass() {
        return SsgCluster.class;
    }

    public Class<? extends Entity> getInterfaceClass() {
        return SsgCluster.class;
    }

    public String getTableName() {
        return "ssg_cluster";
    }

    public SsgCluster create(String name, String sslHostName, int adminPort, EnterpriseFolder parentFolder) throws InvalidNameException, SaveException, FindException {
        verifyLegalClusterName(name);
        final SsgCluster result = new SsgCluster(name, sslHostName, adminPort, parentFolder);
        super.save(result);
        return result;
    }

    public SsgCluster create(String name, String sslHostName, int adminPort, String parentFolderGuid) throws FindException, InvalidNameException, SaveException {
        final EnterpriseFolder parentFolder = enterpriseFolderManager.findByGuid(parentFolderGuid);
        return create(name, sslHostName, adminPort, parentFolder);
    }

    public void renameByGuid(String name, String guid) throws FindException, UpdateException {
        final SsgCluster cluster = findByGuid(guid);
        verifyLegalClusterName(name);
        cluster.setName(name);
        super.update(cluster);
    }

    public SsgCluster findByGuid(final String guid) throws FindException {
        try {
            return (SsgCluster)getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                @Override
                protected Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    final Criteria crit = session.createCriteria(getImpClass());
                    if (guid == null) {
                        crit.add(Restrictions.isNull("guid"));
                    } else {
                        crit.add(Restrictions.eq("guid", guid));
                    }
                    return crit.uniqueResult();
                }
            });
        } catch (DataAccessException e) {
            throw new FindException("Cannot find SSG Cluster by GUID: " + guid, e);
        }
    }

    public List<SsgCluster> findChildSsgClusters(final EnterpriseFolder parentFolder) throws FindException {
        try {
            //noinspection unchecked
            return (List<SsgCluster>)getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                @Override
                protected Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    final Criteria crit = session.createCriteria(getImpClass());
                    crit.add(Restrictions.eq("parentFolder", parentFolder));
                    crit.addOrder(Order.asc("name"));
                    return crit.list();
                }
            });
        } catch (DataAccessException e) {
            throw new FindException("Cannot find child SSG Clusters of " + parentFolder, e);
        }
    }

    public void deleteByGuid(String guid) throws FindException, DeleteException {
        final SsgCluster ssgCluster = findByGuid(guid);
        super.delete(ssgCluster);
    }

    protected UniqueType getUniqueType() {
        return UniqueType.OTHER;
    }

    @Override
    /**
     * Gets the cluster uniquess constraints to check if there exists a duplicate cluster name.
     */
    protected Collection<Map<String, Object>> getUniqueConstraints(SsgCluster entity) {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("parentFolder", entity.getParentFolder());
        attrs.put("name", entity.getName());
        return Arrays.asList(attrs);
    }

    /**
     * Verify if the name of a cluster is legal or not.
     * @param name
     * @throws FindException
     */
    private void verifyLegalClusterName(String name) throws FindException {
        if (name == null)
            throw new InvalidNameException("Name must not be null.");
        if (name.length() == 0)
            throw new InvalidNameException("Name must not be empty.");
        if (name.length() > SsgCluster.MAX_NAME_LENGTH)
            throw new InvalidNameException("Name must not exceed " + SsgCluster.MAX_NAME_LENGTH + " characters");
        if (name.matches(SsgCluster.ILLEGAL_CHARACTERS))
            throw new InvalidNameException("Name must not contain these characters: " + SsgCluster.ILLEGAL_CHARACTERS);
    }
}

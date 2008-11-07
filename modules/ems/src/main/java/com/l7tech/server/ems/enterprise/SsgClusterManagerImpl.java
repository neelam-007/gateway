package com.l7tech.server.ems.enterprise;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.DataAccessException;

import java.sql.SQLException;
import java.util.List;
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

    public SsgCluster create(String name, String sslHostName, int adminPort, EnterpriseFolder parentFolder) throws InvalidNameException, SaveException {
        final SsgCluster result = new SsgCluster(name, sslHostName, adminPort, parentFolder);
        super.save(result);
        return result;
    }

    public SsgCluster create(String name, String sslHostName, int adminPort, String parentFolderGuid) throws FindException, InvalidNameException, SaveException {
        final EnterpriseFolder parentFolder = enterpriseFolderManager.findByGuid(parentFolderGuid);
        return create(name, sslHostName, adminPort, parentFolder);
    }

    public List<SsgCluster> findChildrenOfFolder(final EnterpriseFolder parentFolder) throws FindException {
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
}

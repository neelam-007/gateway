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
import org.springframework.orm.hibernate3.HibernateCallback;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Entity manager for {@link EnterpriseFolder}.
 * TODO RBAC
 *
 * @since Enterprise Manager 1.0
 * @author rmak
 */
public class EnterpriseFolderManagerImpl extends HibernateEntityManager<EnterpriseFolder, EntityHeader> implements EnterpriseFolderManager {
    private static final Logger logger = Logger.getLogger(EnterpriseFolderManagerImpl.class.getName());

    private SsgClusterManager ssgClusterManager;

    public SsgClusterManager getSsgClusterManager() {
        return ssgClusterManager;
    }

    public void setSsgClusterManager(SsgClusterManager ssgClusterManager) {
        this.ssgClusterManager = ssgClusterManager;
    }

    public Class<? extends Entity> getImpClass() {
        return EnterpriseFolder.class;
    }

    public Class<? extends Entity> getInterfaceClass() {
        return EnterpriseFolder.class;
    }

    public String getTableName() {
        return "enterprise_folder";
    }

    public EnterpriseFolder create(String name, EnterpriseFolder parentFolder) throws InvalidNameException, SaveException {
        final EnterpriseFolder result = new EnterpriseFolder(name, parentFolder);
        super.save(result);
        return result;
    }

    public EnterpriseFolder create(String name, String parentFolderGuid) throws FindException, InvalidNameException, SaveException {
        EnterpriseFolder parentFolder;
        if (parentFolderGuid == null) {
            parentFolder = null;
        } else {
            try {
                parentFolder = findByGuid(parentFolderGuid);
            } catch (DataAccessException e) {
                throw new FindException("Cannot find parent folder by GUID.", e);
            }
            if (parentFolder == null) {
                throw new FindException("Invalid parent folder GUID (" + parentFolderGuid + "): no such folder.");
            }
        }
        return create(name, parentFolder);
    }

    public void deleteByGuid(String guid) throws FindException, DeleteException {
        final EnterpriseFolder folder = findByGuid(guid);
        try {
            getHibernateTemplate().execute(new HibernateCallback() {
                private void deleteChildSsgClusters(final Session session, final EnterpriseFolder folder) {
                    final Criteria crit = session.createCriteria(SsgCluster.class);
                    crit.add(Restrictions.eq("parentFolder", folder));
                    crit.addOrder(Order.asc("name"));
                    for (Object childSsgCluster : crit.list()) {
                        session.delete(childSsgCluster);
                    }
                }

                private void deleteChildFolders(final Session session, final EnterpriseFolder folder) {
                    final Criteria crit = session.createCriteria(getImpClass());
                    crit.add(Restrictions.eq("parentFolder", folder));
                    crit.addOrder(Order.asc("name"));
                    for (Object childFolder : crit.list()) {
                        deleteFolder(session, (EnterpriseFolder)childFolder);
                    }
                }

                private void deleteFolder(final Session session, final EnterpriseFolder folder) {
                    deleteChildSsgClusters(session, folder);
                    deleteChildFolders(session, folder);
                    session.delete(folder);
                }

                public Object doInHibernate(Session session) throws HibernateException, SQLException {
                    deleteFolder(session, folder);
                    return null;
                }
            });
        } catch (Exception e) {
            throw new DeleteException("Cannot delete folder " + folder.getName(), e);
        }
    }

    public EnterpriseFolder findRootFolder() throws FindException {
        try {
            return (EnterpriseFolder)getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                @Override
                protected Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    final Criteria crit = session.createCriteria(getImpClass());
                    crit.add(Restrictions.isNull("parentFolder"));
                    return crit.uniqueResult();
                }
            });
        } catch (DataAccessException e) {
            throw new FindException("Cannot find root folder.", e);
        }
    }

    public EnterpriseFolder findByGuid(final String guid) throws FindException {
        try {
            return (EnterpriseFolder)getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
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
            throw new FindException("Cannot find folder by GUID: " + guid, e);
        }
    }

    public List<EnterpriseFolder> findChildFolders(final EnterpriseFolder parentFolder) throws FindException {
        try {
            //noinspection unchecked
            return (List<EnterpriseFolder>)getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                @Override
                protected Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    final Criteria crit = session.createCriteria(getImpClass());
                    if (parentFolder == null) {
                        crit.add(Restrictions.isNull("parentFolder"));
                    } else {
                        crit.add(Restrictions.eq("parentFolder", parentFolder));
                    }
                    crit.addOrder(Order.asc("name"));
                    return crit.list();
                }
            });
        } catch (DataAccessException e) {
            throw new FindException("Cannot find child folders of " + parentFolder, e);
        }
    }
}

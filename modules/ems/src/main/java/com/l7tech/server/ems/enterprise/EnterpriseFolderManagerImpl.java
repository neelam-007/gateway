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
import java.util.*;
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

    public EnterpriseFolder create(String name, EnterpriseFolder parentFolder) throws InvalidNameException, SaveException, FindException {
        verifyParentFolder(parentFolder);
        verifyLegalFolerName(name);

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

    public void renameByGuid(String name, String guid) throws FindException, UpdateException {
        final EnterpriseFolder folder = findByGuid(guid);
        verifyLegalFolerName(name);
        folder.setName(name);
        super.update(folder);
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

    protected UniqueType getUniqueType() {
        return UniqueType.OTHER;
    }

    @Override
    /**
     * Gets the folder uniquess constraints to check if there exists a duplicate folder name.
     */
    protected Collection<Map<String, Object>> getUniqueConstraints(EnterpriseFolder entity) {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("parentFolder", entity.getParentFolder());
        attrs.put("name", entity.getName());
        return Arrays.asList(attrs);
    }

    /**
     * Verify if the name of a folder is legal or not.
     * @param name
     * @throws FindException
     */
    private void verifyLegalFolerName(String name) throws FindException {
        if (name == null)
            throw new InvalidNameException("Name must not be null.");
        if (name.length() == 0)
            throw new InvalidNameException("Name must not be empty.");
        if (name.length() > EnterpriseFolder.MAX_NAME_LENGTH)
            throw new InvalidNameException("Name must not exceed " + EnterpriseFolder.MAX_NAME_LENGTH + " characters");
        if (name.matches(EnterpriseFolder.ILLEGAL_CHARACTERS))
            throw new InvalidNameException("Name must not contain these characters: " + EnterpriseFolder.ILLEGAL_CHARACTERS);
    }

    /**
     * Verify if the parent folder is validate or not.
     * @param parentFolder
     * @throws FindException
     */
    private void verifyParentFolder(EnterpriseFolder parentFolder) throws FindException {
        EnterpriseFolder root = findRootFolder();
        if (root == null) {
            if (parentFolder != null)
                throw new IllegalArgumentException("Cannot create any folder without a root folder existing first.");

        } else {
            if (parentFolder == null)
                throw new IllegalArgumentException("Cannot create a second root folder, since the root folder exists already.");
        }
    }
}

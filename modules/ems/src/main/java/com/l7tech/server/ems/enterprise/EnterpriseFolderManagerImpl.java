package com.l7tech.server.ems.enterprise;

import com.l7tech.objectmodel.*;
import com.l7tech.server.HibernateGoidEntityManager;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Entity manager for {@link EnterpriseFolder}.
 *
 * @since Enterprise Manager 1.0
 * @author rmak
 */
@Transactional(propagation= Propagation.REQUIRED, rollbackFor=Throwable.class)
public class EnterpriseFolderManagerImpl extends HibernateGoidEntityManager<EnterpriseFolder, EntityHeader> implements EnterpriseFolderManager {
    private static final Logger logger = Logger.getLogger(EnterpriseFolderManagerImpl.class.getName());

    private SsgClusterManager ssgClusterManager;

    public SsgClusterManager getSsgClusterManager() {
        return ssgClusterManager;
    }

    public void setSsgClusterManager(SsgClusterManager ssgClusterManager) {
        this.ssgClusterManager = ssgClusterManager;
    }

    @Override
    public Class<EnterpriseFolder> getImpClass() {
        return EnterpriseFolder.class;
    }

    @Override
    public Class<EnterpriseFolder> getInterfaceClass() {
        return EnterpriseFolder.class;
    }

    @Override
    public String getTableName() {
        return "enterprise_folder";
    }

    @Override
    public EnterpriseFolder create(String name, EnterpriseFolder parentFolder) throws InvalidNameException, SaveException, FindException {
        verifyParentFolder(parentFolder);
        verifyLegalFolderName(name);

        final EnterpriseFolder result = new EnterpriseFolder(name, parentFolder);
        super.save(result);
        return result;
    }

    @Override
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

    @Override
    public void renameByGuid(String name, String guid) throws FindException, UpdateException {
        final EnterpriseFolder folder = findByGuid(guid);
        verifyLegalFolderName(name);
        folder.setName(name);
        super.update(folder);
    }

    @Override
    public void moveByGuid(String guid, String newParentGuid) throws FindException, UpdateException {
        final EnterpriseFolder folder = findByGuid(guid);
        if (folder == null) {
            throw new FindException("The folder to move does not exists. (GUID = " + guid + ")");
        }

        if (folder.isRoot()) {
            throw new UpdateException("Cannot move root folder.");
        }

        if (guid.equals(newParentGuid)) {
            throw new UpdateException("Cannot move folder \"" + folder.getName() + "\" to inside itself.");
        }

        if (folder.getParentFolder().getGuid().equals(newParentGuid)) {
            logger.info("Attempt to move folder \"" + folder.getName() + "\" to same parent folder, i.e., no-op.");
            return;
        }

        final EnterpriseFolder newParent = findByGuid(newParentGuid);
        if (newParent == null) {
            throw new FindException("Destination folder does not exists. (GUID = " + newParentGuid + ")");
        }

        for (EnterpriseFolder p = newParent.getParentFolder(); p != null; p = p.getParentFolder()) {
            if (p.getGuid().equals(guid)) {
                throw new UpdateException("Cannot move folder \"" + folder.getName() + "\" into its own descendent folder \"" + newParent.getName() + "\".");
            }
        }

        final List<EnterpriseFolder> newSiblings = findChildFolders(newParent);
        for (EnterpriseFolder sibling : newSiblings) {
            if (sibling.getName().equals(folder.getName())) {
                throw new UpdateException("A folder with the name \"" + sibling.getName() + "\" already exists in the destination folder \"" + newParent.getName() + "\".");
            }
        }

        folder.setParentFolder(newParent);
        super.update(folder);
    }

    @Override
    public void deleteByGuid(String guid) throws FindException, DeleteException {
        deleteByGuid(guid, false);
    }

    @Override
    public void deleteByGuid(String guid, boolean deleteByCascade) throws FindException, DeleteException {
        final EnterpriseFolder folder = findByGuid(guid);

        if (!deleteByCascade && !isEmptyFolder(guid)) {
            throw new NonEmptyFolderDeletionException("Cannot delete a non-empty folder (NAME = '" + folder.getName() + "').");
        }

        try {
            getHibernateTemplate().execute(new HibernateCallback() {
                private void deleteChildSsgClusters(final Session session, final EnterpriseFolder folder) throws DeleteException {
                    final Criteria crit = session.createCriteria(SsgCluster.class);
                    crit.add(Restrictions.eq("parentFolder", folder));
                    crit.addOrder(Order.asc("name"));
                    for (Object childSsgCluster : crit.list()) {
                        ssgClusterManager.delete((SsgCluster)childSsgCluster);
                    }
                }

                private void deleteChildFolders(final Session session, final EnterpriseFolder folder) throws DeleteException {
                    final Criteria crit = session.createCriteria(getImpClass());
                    crit.add(Restrictions.eq("parentFolder", folder));
                    crit.addOrder(Order.asc("name"));
                    for (Object childFolder : crit.list()) {
                        deleteFolder(session, (EnterpriseFolder)childFolder);
                    }
                }

                private void deleteFolder(final Session session, final EnterpriseFolder folder) throws DeleteException {
                    deleteChildSsgClusters(session, folder);
                    deleteChildFolders(session, folder);
                    session.delete(folder);
                }

                @Override
                public Object doInHibernate(Session session) throws HibernateException, SQLException {
                    try {
                        deleteFolder(session, folder);
                    } catch (DeleteException e) {
                        throw new HibernateException(e);
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            throw new DeleteException("Cannot delete folder " + folder.getName(), e);
        }
    }

    @Override
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

    @Override
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

    @Override
    public List<EnterpriseFolder> findChildFolders(String parentFolderGuid) throws FindException {
        return findChildFolders(findByGuid(parentFolderGuid));
    }

    @Override
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

    @Override
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
    private void verifyLegalFolderName(String name) throws FindException {
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

    private boolean isEmptyFolder(String guid) throws FindException {
        return findChildFolders(guid).isEmpty() && ssgClusterManager.findChildSsgClusters(guid, true).isEmpty();
    }
}

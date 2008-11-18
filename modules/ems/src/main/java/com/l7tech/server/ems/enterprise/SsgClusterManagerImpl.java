package com.l7tech.server.ems.enterprise;

import com.l7tech.objectmodel.*;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.gateway.common.security.rbac.RbacAdmin;
import com.l7tech.gateway.common.security.rbac.Role;
import static com.l7tech.objectmodel.EntityType.*;
import static com.l7tech.gateway.common.security.rbac.OperationType.*;
import com.l7tech.util.TextUtils;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.DataAccessException;

import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.logging.Logger;
import java.text.MessageFormat;

/**
 * Entity manager for {@link SsgCluster}.
 *
 * @since Enterprise Manager 1.0
 * @author rmak
 */
public class SsgClusterManagerImpl extends HibernateEntityManager<SsgCluster, EntityHeader> implements SsgClusterManager {
    private static final Logger logger = Logger.getLogger(SsgClusterManagerImpl.class.getName());

    private static final String ROLE_NAME_TYPE_SUFFIX = "Cluster";
    private static final Pattern replaceRoleName =
            Pattern.compile(MessageFormat.format(RbacAdmin.RENAME_REGEX_PATTERN, ROLE_NAME_TYPE_SUFFIX));
    private String ROLE_NAME_PATTERN = RbacAdmin.ROLE_NAME_PREFIX + " {0} " + ROLE_NAME_TYPE_SUFFIX + RbacAdmin.ROLE_NAME_OID_SUFFIX;

    private final EnterpriseFolderManager enterpriseFolderManager;
    private final RoleManager roleManager;

    public SsgClusterManagerImpl( final EnterpriseFolderManager enterpriseFolderManager,
                                  final RoleManager roleManager ) {
        this.enterpriseFolderManager = enterpriseFolderManager;
        this.roleManager = roleManager;
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return SsgCluster.class;
    }

    @Override
    public Class<? extends Entity> getInterfaceClass() {
        return SsgCluster.class;
    }

    @Override
    public String getTableName() {
        return "ssg_cluster";
    }

    @Override
    public SsgCluster create(String name, String sslHostName, int adminPort, EnterpriseFolder parentFolder) throws InvalidNameException, SaveException, FindException {
        verifyLegalClusterName(name);
        final SsgCluster result = new SsgCluster(name, sslHostName, adminPort, parentFolder);
        long id = save(result);
        addManageClusterRole( id, result );
        return result;
    }

    @Override
    public SsgCluster create(String name, String sslHostName, int adminPort, String parentFolderGuid) throws FindException, InvalidNameException, SaveException {
        final EnterpriseFolder parentFolder = enterpriseFolderManager.findByGuid(parentFolderGuid);
        return create(name, sslHostName, adminPort, parentFolder);
    }

    @Override
    public void renameByGuid(String name, String guid) throws FindException, UpdateException {
        final SsgCluster cluster = findByGuid(guid);
        verifyLegalClusterName(name);
        cluster.setName(name);
        update(cluster);
    }

    @Override
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

    @Override
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

    @Override
    public void deleteByGuid(String guid) throws FindException, DeleteException {
        final SsgCluster ssgCluster = findByGuid(guid);
        delete(ssgCluster);
    }

    @Override
    public void delete( final long oid ) throws DeleteException, FindException {
        findAndDelete(oid);
    }

    @Override
    public void delete( final SsgCluster ssgCluster ) throws DeleteException {
        super.delete(ssgCluster);
        roleManager.deleteEntitySpecificRole(ESM_SSG_CLUSTER, ssgCluster.getOid());
    }

    @Override
    public void update( final SsgCluster ssgCluster ) throws UpdateException {
        super.update(ssgCluster);

        try {
            roleManager.renameEntitySpecificRole(EntityType.ESM_SSG_CLUSTER, ssgCluster, replaceRoleName);
        } catch (FindException e) {
            throw new UpdateException("Couldn't find Role to rename", e);
        }
    }

    @Override
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

    /**
     * Creates a new role for the specified Cluster.
     *
     * @param ssgCluster      the SsgCluster that is in need of a Role.  Must not be null.
     * @throws SaveException  if the new Role could not be saved
     */
    private void addManageClusterRole( final Long id, final SsgCluster ssgCluster ) throws SaveException {
        // truncate service name in the role name to avoid going beyond 128 limit
        String clustername = ssgCluster.getName();
        // cutoff is arbitrarily set to 50
        clustername = TextUtils.truncStringMiddle(clustername, 50);
        String name = MessageFormat.format(ROLE_NAME_PATTERN, clustername, id);

        logger.info("Creating new Role: " + name);

        Role newRole = new Role();
        newRole.setName(name);

        // R this cluster
        newRole.addPermission(READ, ESM_SSG_CLUSTER, id.toString()); // Read this cluster

        newRole.setEntityType(ESM_SSG_CLUSTER);
        newRole.setEntityOid(id);
        newRole.setDescription("Users assigned to the {0} role have the ability to manage the {1} cluster.");

        roleManager.save(newRole);
    }
}

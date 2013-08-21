package com.l7tech.server.ems.enterprise;

import com.l7tech.gateway.common.security.rbac.RbacAdmin;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.objectmodel.*;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.util.Functions;
import com.l7tech.util.TextUtils;
import com.l7tech.util.ValidationUtils;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static com.l7tech.gateway.common.security.rbac.OperationType.READ;
import static com.l7tech.objectmodel.EntityType.ESM_SSG_CLUSTER;

/**
 * Entity manager for {@link SsgCluster}.
 *
 * @since Enterprise Manager 1.0
 * @author rmak
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
public class SsgClusterManagerImpl extends HibernateEntityManager<SsgCluster, EntityHeader> implements SsgClusterManager {
    private static final Logger logger = Logger.getLogger(SsgClusterManagerImpl.class.getName());

    private static final String ROLE_NAME_TYPE_SUFFIX = "Gateway Cluster Nodes";
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
    public Collection<SsgCluster> findOnlineClusters() throws FindException {
        return filterOffline( super.findAll(), false );
    }

    @Override
    public SsgCluster create( final String name,
                              final String guid,
                              final EnterpriseFolder parentFolder ) throws SaveException {
        final SsgCluster result = new SsgCluster(name, guid, parentFolder);
        save(result);
        return result;
    }

    @Override
    public SsgCluster create(String name, String sslHostName, int adminPort, EnterpriseFolder parentFolder) throws InvalidNameException, SaveException, FindException, UnknownHostException {
        verifyLegalClusterName(name);
        verifyHostnameUniqueness(sslHostName);
        final SsgCluster result = new SsgCluster(name, sslHostName, adminPort, parentFolder);
        Goid id = save(result);
        addManageClusterRole( id, result );
        return result;
    }

    @Override
    public SsgCluster create(String name, String sslHostName, int adminPort, String parentFolderGuid) throws FindException, InvalidNameException, SaveException, UnknownHostException {
        final EnterpriseFolder parentFolder = enterpriseFolderManager.findByGuid(parentFolderGuid);
        return create(name, sslHostName, adminPort, parentFolder);
    }

    @Override
    public void editByGuid( final String guid,
                            final String newName,
                            final String newSslHostname,
                            final String newAdminPort ) throws FindException, UpdateException, DuplicateHostnameException, UnknownHostException {
        boolean updated = false;
        final SsgCluster cluster = findByGuid(guid);
        if (cluster == null) return;

        // Verify and update the new name
        String oldName = cluster.getName();
        if (newName != null && !newName.equals(oldName)) {
            verifyLegalClusterName(newName);
            cluster.setName(newName);
            updated = true;
        }

        if ( !cluster.isOffline() ) {
            // Verify and update the new ssl hostname
            String oldSslHostname = cluster.getSslHostName();
            if (newSslHostname != null && !newSslHostname.equals(oldSslHostname)) {
                if (! isSameHost(newSslHostname, oldSslHostname)) {
                    verifyHostnameUniqueness(newSslHostname);
                }
                cluster.setSslHostName(newSslHostname);
                updated = true;
            }

            // Verify and update the new admin port
            if ( ValidationUtils.isValidInteger( newAdminPort, false, 1, 65535 ) ) {
                try {
                    int oldport = cluster.getAdminPort();
                    int newport = Integer.parseInt(newAdminPort);
                    if (newport != oldport) {
                        cluster.setAdminPort(newport);
                        updated = true;
                    }
                } catch (NumberFormatException e) {
                }
            }
        }

        // Update the cluster
        if (updated) {
            update(cluster);
        }
    }

    @Override
    public void moveByGuid(String guid, String newParentGuid) throws FindException, UpdateException {
        final SsgCluster cluster = findByGuid(guid);
        if (cluster == null) {
            throw new FindException("The Gateway Cluster to move does not exists. (GUID = " + guid + ")");
        }

        if (cluster.getParentFolder().getGuid().equals(newParentGuid)) {
            logger.info("Attempt to move Gateway Cluster \"" + cluster.getName() + "\" to same parent folder, i.e., no-op.");
            return;
        }

        final EnterpriseFolder newParent = enterpriseFolderManager.findByGuid(newParentGuid);
        if (newParent == null) {
            throw new FindException("Destination folder does not exists. (GUID = " + newParentGuid + ")");
        }

        final List<SsgCluster> newSiblings = findChildSsgClusters(newParent, true);
        for (SsgCluster sibling : newSiblings) {
            if (sibling.getName().equals(cluster.getName())) {
                throw new UpdateException("A Gateway Cluster with the name \"" + sibling.getName() + "\" already exists in the destination folder \"" + newParent.getName() + "\".");
            }
        }

        cluster.setParentFolder(newParent);
        super.update(cluster);
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
            throw new FindException("Cannot find Gateway Cluster by GUID: " + guid, e);
        }
    }

    @Override
    public List<SsgCluster> findChildSsgClusters( final String parentFolderGuid,
                                                  final boolean includeOffline ) throws FindException {
        return findChildSsgClusters(enterpriseFolderManager.findByGuid(parentFolderGuid), includeOffline);
    }

    @Override
    public List<SsgCluster> findChildSsgClusters( final EnterpriseFolder parentFolder ) throws FindException {
        return findChildSsgClusters( parentFolder, false );
    }

    @Override
    public List<SsgCluster> findChildSsgClusters(final EnterpriseFolder parentFolder,
                                                 final boolean includeOffline ) throws FindException {
        try {
            //noinspection unchecked
            return (List<SsgCluster>)getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                @Override
                protected Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    final Criteria crit = session.createCriteria(getImpClass());
                    crit.add(Restrictions.eq("parentFolder", parentFolder));
                    if ( !includeOffline ) {
                        crit.add( Restrictions.gt( "adminPort", 0 ) );
                    }
                    crit.addOrder(Order.asc("name"));
                    return crit.list();
                }
            });
        } catch (DataAccessException e) {
            throw new FindException("Cannot find child Gateway Clusters of " + parentFolder, e);
        }
    }

    @Override
    public List<EnterpriseFolder> findAllAncestors(String guid) throws FindException {
        SsgCluster ssgCluster = findByGuid(guid);
        return findAllAncestors(ssgCluster);
    }

    @Override
    public List<EnterpriseFolder> findAllAncestors(SsgCluster ssgCluster) {
        List<EnterpriseFolder> ancestors = new ArrayList<EnterpriseFolder>();
        if (ssgCluster == null) return ancestors;

        EnterpriseFolder parent = ssgCluster.getParentFolder();
        while (parent != null) {
            ancestors.add(0, parent);
            parent = parent.getParentFolder();
        }

        return ancestors;
    }

    @Override
    public void deleteByGuid(String guid) throws FindException, DeleteException {
        final SsgCluster ssgCluster = findByGuid(guid);
        delete(ssgCluster);
    }

    @Override
    public void delete( final Goid goid ) throws DeleteException, FindException {
        findAndDelete(goid);
    }

    @Override
    public void delete( final SsgCluster ssgCluster ) throws DeleteException {
        super.delete(ssgCluster);
        roleManager.deleteEntitySpecificRoles(ESM_SSG_CLUSTER, ssgCluster.getGoid());
    }

    @Override
    public void update( final SsgCluster ssgCluster ) throws UpdateException {
        super.update(ssgCluster);

        try {
            roleManager.renameEntitySpecificRoles(EntityType.ESM_SSG_CLUSTER, ssgCluster, replaceRoleName);
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
     * Verify if the hostname of a SSG Cluster is unique in the enterprise tree.
     * @param hostname The host name of the SSG Cluster
     * @throws FindException : throw if not able to find SSG clusters or nodes from the database.
     * @throws DuplicateHostnameException : throw if there exists one cluster with such hostname and port.
     */
    private void verifyHostnameUniqueness(String hostname) throws FindException, DuplicateHostnameException, UnknownHostException {
        // Check if there exists any SSG cluster having the same host name or ip address of the checked cluster's.
        for (SsgCluster cluster: findAll()) {
            if (isSameHost(cluster.getSslHostName(), hostname)) {
                throw new DuplicateHostnameException("Find an existing Gateway Cluster with the same hostname (" + hostname + ").");
            }

            // Check if there exists any SSG node having the same host name or ip address of the checked cluster's.
            for (SsgNode node: cluster.getNodes()) {
                if (isSameHost(node.getIpAddress(), hostname)) {
                    throw new DuplicateHostnameException("Find an existing Gateway Node with the same hostname (" + hostname + ").");
                }
            }
        }
    }

    /**
     * Check if two hosts are same/equivalent or not.
     *
     * @param host1 The hostname of the first host.  It could be an IP address.
     * @param host2 The hostname of the second host.  It could be an IP address.
     * @return true if both are the same host.
     */
    private boolean isSameHost(String host1, String host2) throws UnknownHostException {
        // Both hosts cannot be null.
        if (host1 == null || host2 == null) throw new UnknownHostException("The host name is not specified.");

        // Check if they have the exactly same host names.
        if (host1.equals(host2)) return true;

        // Check if they are the same local hosts.
        if (isLoopbackAddress(host1) && isLoopbackAddress(host2)) return true;

        // Check if they are the same non-local hosts.
        Collection<String> hostsList1 = getAllHosts(host1);
        Collection<String> hostsList2 = getAllHosts(host2);

        hostsList1.retainAll(hostsList2);
        if (hostsList1.isEmpty()) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Check if the host is a loop back host.
     * @param hostname: the name of the host
     * @return true if the host is loop-back.
     * @throws UnknownHostException: thrown if the host is not recognizable.
     */
    private boolean isLoopbackAddress(String hostname) throws UnknownHostException {
        return InetAddress.getByName(hostname).isLoopbackAddress();
    }

    /**
     * Get all hosts related to the hostname.
     * @param hostname: the name of the host.
     * @return: a set of host ip addresses.
     * @throws UnknownHostException: thrown if the host is not recognizable.
     */
    private Set<String> getAllHosts(String hostname) throws UnknownHostException {
        Set<String> ipSet = new HashSet<String>();

        for (InetAddress inetAddress: InetAddress.getAllByName(hostname)) {
            ipSet.add(inetAddress.getHostAddress());
        }

        return ipSet;
    }

    /**
     * Creates a new role for the specified Cluster.
     *
     * @param ssgCluster      the SsgCluster that is in need of a Role.  Must not be null.
     * @throws SaveException  if the new Role could not be saved
     */
    private void addManageClusterRole( final Goid id, final SsgCluster ssgCluster ) throws SaveException {
        // truncate service name in the role name to avoid going beyond 128 limit
        String clustername = ssgCluster.getName();
        // cutoff is arbitrarily set to 50
        clustername = TextUtils.truncStringMiddle(clustername, 50);
        String name = MessageFormat.format(ROLE_NAME_PATTERN, clustername, id);

        logger.info("Creating new Role: " + name);

        Role newRole = new Role();
        newRole.setName(name);

        // R this cluster
        newRole.addEntityPermission(READ, ESM_SSG_CLUSTER, id.toString()); // Read this cluster

        newRole.setEntityType(ESM_SSG_CLUSTER);
        newRole.setEntityGoid(id);
        newRole.setDescription("Users assigned to the {0} role have the ability to manage Gateway Nodes in the {1} cluster.");

        roleManager.save(newRole);
    }

    private Collection<SsgCluster> filterOffline( final Collection<SsgCluster> clusters,
                                                  final boolean includeOffline ) {
        return Functions.grep( clusters, new Functions.Unary<Boolean, SsgCluster>() {
            @Override
            public Boolean call( final SsgCluster ssgCluster ) {
                return includeOffline || !ssgCluster.isOffline();
            }
        } );
    }
}

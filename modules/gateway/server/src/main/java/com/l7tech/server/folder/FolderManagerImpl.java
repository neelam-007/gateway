package com.l7tech.server.folder;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.EntityFinder;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.OrganizationHeader;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.HasFolder;
import com.l7tech.identity.User;
import com.l7tech.gateway.common.security.rbac.OperationType;

import java.util.*;

/**
 * Implementation of the service/policy folder manager.
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
public class FolderManagerImpl extends HibernateEntityManager<Folder, FolderHeader> implements FolderManager {

    @Transactional(readOnly=true)
    public Collection<FolderHeader> findFolderHeaders() throws FindException {
        return findAllHeaders();
    }

    @Transactional(propagation= Propagation.SUPPORTS)
    public Class<? extends Entity> getImpClass() {
        return Folder.class;
    }

    @Transactional(propagation=Propagation.SUPPORTS)
    public Class<? extends Entity> getInterfaceClass() {
        return Folder.class;
    }

    @Transactional(propagation= Propagation.SUPPORTS)
    public String getTableName() {
        return "policy_folder";
    }

    @Override
    protected FolderHeader newHeader( final Folder entity ) {
        return new FolderHeader( entity );
    }

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.OTHER;
    }

    @Override
    protected Collection<Map<String, Object>> getUniqueConstraints(Folder entity) {
        Map<String,Object> map = new HashMap<String, Object>();
        map.put("parentFolderOid", entity.getParentFolderOid());
        map.put("name",entity.getName());
        return Arrays.asList(map);
    }
}

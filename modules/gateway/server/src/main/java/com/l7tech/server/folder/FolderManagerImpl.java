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
    private static long ROOT_FOLDER_OID = -5002;

    final private RoleManager roleManager;
    final private EntityFinder entityFinder;

    public FolderManagerImpl(RoleManager roleManager, EntityFinder entityFinder){
        this.roleManager = roleManager;
        this.entityFinder = entityFinder;
    }

    @Transactional(readOnly=true)
    public Collection<FolderHeader> findFolderHeaders(Iterable<? extends OrganizationHeader> entityHeaders) throws FindException {
        Collection<FolderHeader> folderHeaders = new ArrayList<FolderHeader>(findAllHeaders());
        Map<Long, FolderHeader> folderOidToHeaderMap = new HashMap<Long, FolderHeader>();

        for(FolderHeader fH: folderHeaders){
            folderOidToHeaderMap.put(fH.getOid(), fH);    
        }

        //Validate what folders to return based on the supplied entity headers
        User user = JaasUtils.getCurrentUser();
        //If the user has blanked permission return all folders
        //Note anybody with read on entity_type service for any entity of type can see all folders
        //Currently this means only Admin and users with the 'Manage Webservices' role.
        if(roleManager.isPermittedForAnyEntityOfType(user, OperationType.READ, com.l7tech.gateway.common.security.rbac.EntityType.SERVICE)){
            return folderHeaders;
        }

        //Note this code (and called code) does not rely on the class of hierarchy of EntityHeader. It depends on the
        //type parameter of EntityHeader
        Iterable<? extends EntityHeader> filteredEntities =
                roleManager.filterPermittedHeaders(user, OperationType.READ, entityHeaders, entityFinder);

        List<FolderHeader> filteredFolders = new ArrayList<FolderHeader>();
        Set<Long> recordedFolderIds = new HashSet<Long>();

        //based on the filteredEntities, what folders should we return?
        for(EntityHeader eH: filteredEntities){
            long folderId = -1;
            if(eH instanceof HasFolder){
                HasFolder hf = (HasFolder)eH;
                folderId = hf.getFolderOid();
            }else{
                throw new RuntimeException("Entity type cannot be stored in a folder");
            }

            //For this entity - record it's folder
            FolderHeader header = folderOidToHeaderMap.get(folderId);
            filteredFolders.add(header);
            recordedFolderIds.add(folderId);
            //also get all of it's parents folders back to root
            Long parentId = header.getParentFolderOid();
            while(parentId != null){
                FolderHeader parentFolder = folderOidToHeaderMap.get(parentId);
                if(!recordedFolderIds.contains(parentId)){
                    recordedFolderIds.add(parentId);
                    filteredFolders.add(parentFolder);
                }
                parentId = parentFolder.getParentFolderOid();
            }
        }
        return filteredFolders;
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
        return UniqueType.NONE;
    }
}

package com.l7tech.server.folder;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.server.HibernateEntityManager;
import org.springframework.transaction.annotation.Propagation;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of the service/policy folder manager.
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
public class FolderManagerImpl extends HibernateEntityManager<Folder, FolderHeader> implements FolderManager {
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
        map.put("parentFolder", entity.getParentFolder());
        map.put("name",entity.getName());
        return Arrays.asList(map);
    }

    @Override
    public void update(Folder entity) throws UpdateException {
        try {
            //check for circular folders
            Folder childFolder = findByPrimaryKey(entity.getParentFolder().getOid());
            if (entity.equals(childFolder.getParentFolder())) {
                throw new UpdateException("The destination folder is a subfolder of the source folder");
            }
        } catch (FindException fe) {
            throw new UpdateException("Couldn't find previous version(s) to check for circular folders");
        }

        //proceed with update
        super.update(entity);
    }
}

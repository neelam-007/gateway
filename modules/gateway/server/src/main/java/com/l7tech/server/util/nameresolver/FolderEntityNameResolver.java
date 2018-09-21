package com.l7tech.server.util.nameresolver;

import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.gateway.common.security.rbac.FolderPredicate;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.HasFolder;
import com.l7tech.objectmodel.folder.HasFolderId;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.server.util.nameresolver.EntityNameResolver;
import org.apache.commons.lang.StringUtils;
/**
 * Name resolver for Folder Entity
 */
public class FolderEntityNameResolver extends EntityNameResolver {
    private static final String ROOT_FOLDER_PATTERN = "${rootFolderName}";

    public FolderEntityNameResolver(FolderAdmin folderAdmin) {
        super(folderAdmin);
    }

    @Override
    protected boolean canResolveName(final EntityHeader entityHeader) {
        return (EntityType.FOLDER.equals(entityHeader.getType()));
    }

    @Override
    protected boolean canResolveName(final Entity entity) {
        return entity instanceof Folder || entity instanceof FolderPredicate;
    }

    @Override
    public String resolve(final EntityHeader entityHeader, final boolean includePath) throws FindException {
        final Folder folder = folderAdmin.findByPrimaryKey(entityHeader.getGoid());
        validateFoundEntity(entityHeader, folder);
        String name = resolve(folder, includePath);
        //check the impact
        if (isRootFolder(entityHeader)) {
            name = ROOT_FOLDER_PATTERN;
        }

        String path = null;
        if (includePath && entityHeader instanceof HasFolderId) {
            path = getPath((HasFolderId) entityHeader);
        }
        return buildName(name, StringUtils.EMPTY, path, true);
    }

    @Override
    public String resolve(final Entity entity, final boolean includePath) throws FindException {
        String name = StringUtils.EMPTY;
        if (entity instanceof Folder && isRootFolder((Folder) entity)) {
            name = ROOT_FOLDER_PATTERN;
        } else if (entity instanceof FolderPredicate) {
            final FolderPredicate predicate = (FolderPredicate) entity;
            name = "in folder \"" + resolve(predicate.getFolder(), includePath) + "\"";
            if (predicate.isTransitive()) {
                name = name + " and subfolders";
            }
        } else if (entity instanceof NamedEntityImp) {
            final NamedEntityImp named = (NamedEntityImp) entity;
            name = named.getName();
        }
        String path = null;
        if (includePath && entity instanceof HasFolder) {
            path = getPath((HasFolder) entity);
        }
        return buildName(name, StringUtils.EMPTY, path, true);
    }
}

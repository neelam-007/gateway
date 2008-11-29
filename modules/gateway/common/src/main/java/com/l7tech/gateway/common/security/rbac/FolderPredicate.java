/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.security.rbac;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.HasFolder;

import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Matches entities in a {@link #folder}, and optionally those in {@link #transitive child folders}.
 *
 * @author alex 
 */
@javax.persistence.Entity
@Table(name="rbac_predicate_folder")
public class FolderPredicate extends ScopePredicate {
    private static final Logger logger = Logger.getLogger(FolderPredicate.class.getName());

    private Folder folder;
    private boolean transitive;

    public FolderPredicate(Permission permission, Folder folder, boolean transitive) {
        super(permission);
        this.folder = folder;
        this.transitive = transitive;
    }

    @Deprecated
    protected FolderPredicate() { }

    @Override
    boolean matches(final Entity entity, final Class<? extends Entity> eclass) {
        final Folder entityFolder;
        if (entity instanceof HasFolder) {
            entityFolder = ((HasFolder)entity).getFolder();
        } else if (entity instanceof Folder) {
            entityFolder = (Folder)entity;
        } else {
            logger.log(Level.INFO, String.format("%s #%s has no folder", eclass.getSimpleName(), entity.getId()));
            return false;
        }

        Folder nextFolder = entityFolder;
        while (nextFolder != null) {
            if (nextFolder.getOid() == this.folder.getOid()) return true;
            if (!transitive) break;
            nextFolder = nextFolder.getParentFolder();
        }
        return false;
    }

    /**
     * @return the folder in which entities must be stored to match this predicate.
     */
    @ManyToOne(optional=false)
    @JoinColumn(name="folder_oid")
    public Folder getFolder() {
        return folder;
    }

    @Deprecated
    protected void setFolder(Folder folder) {
        this.folder = folder;
    }

    /**
     * @return <code>true</code> if this predicate matches entities in child (and grandchild..) folders of
     *         {@link #getFolder}; <code>false</code> if only entities stored directly in {@link #getFolder} match.
     */
    public boolean isTransitive() {
        return transitive;
    }

    public void setTransitive(boolean transitive) {
        this.transitive = transitive;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(permission.getEntityType().getPluralName());
        sb.append(" in ").append(folder.getName());
        if (transitive) {
            sb.append(" (transitive)");
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        FolderPredicate that = (FolderPredicate)o;

        if (transitive != that.transitive) return false;
        if (folder != null ? !folder.equals(that.folder) : that.folder != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (folder != null ? folder.hashCode() : 0);
        result = 31 * result + (transitive ? 1 : 0);
        return result;
    }
}

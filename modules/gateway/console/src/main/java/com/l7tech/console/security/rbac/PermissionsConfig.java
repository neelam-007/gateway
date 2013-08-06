package com.l7tech.console.security.rbac;

import com.l7tech.gateway.common.security.rbac.AttributePredicate;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.objectmodel.folder.FolderHeader;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * POJO to store user input for AddPermissionsWizard.
 */
public class PermissionsConfig {
    private final Role role;
    private EntityType type = EntityType.ANY;
    private Set<OperationType> operations = new HashSet<>();
    private boolean hasScope;
    private Set<SecurityZone> selectedZones = new HashSet<>();
    private Set<FolderHeader> selectedFolders = new HashSet<>();
    private boolean folderTransitive;
    private boolean folderAncestry;
    private Set<AttributePredicate> attributePredicates = new HashSet<>();
    private Set<Permission> generatedPermissions = new HashSet<>();

    public PermissionsConfig(@NotNull final Role role) {
        this.role = role;
    }

    @NotNull
    public Role getRole() {
        return role;
    }

    @NotNull
    public EntityType getType() {
        return type;
    }

    public void setType(@NotNull final EntityType type) {
        this.type = type;
    }

    @NotNull
    public Set<OperationType> getOperations() {
        return operations;
    }

    public void setOperations(@NotNull final Set<OperationType> operations) {
        this.operations = operations;
    }

    public boolean isHasScope() {
        return hasScope;
    }

    public void setHasScope(boolean hasScope) {
        this.hasScope = hasScope;
    }

    @NotNull
    public Set<SecurityZone> getSelectedZones() {
        return selectedZones;
    }

    public void setSelectedZones(@NotNull final Set<SecurityZone> selectedZones) {
        this.selectedZones = selectedZones;
    }

    @NotNull
    public Set<FolderHeader> getSelectedFolders() {
        return selectedFolders;
    }

    public void setSelectedFolders(@NotNull final Set<FolderHeader> selectedFolders) {
        this.selectedFolders = selectedFolders;
    }

    public boolean isFolderTransitive() {
        return folderTransitive;
    }

    public void setFolderTransitive(boolean folderTransitive) {
        this.folderTransitive = folderTransitive;
    }

    public boolean isFolderAncestry() {
        return folderAncestry;
    }

    public void setFolderAncestry(boolean folderAncestry) {
        this.folderAncestry = folderAncestry;
    }

    @NotNull
    public Set<AttributePredicate> getAttributePredicates() {
        return attributePredicates;
    }

    public void setAttributePredicates(@NotNull final Set<AttributePredicate> attributePredicates) {
        this.attributePredicates = attributePredicates;
    }

    @NotNull
    public Set<Permission> getGeneratedPermissions() {
        return generatedPermissions;
    }

    public void setGeneratedPermissions(@NotNull final Set<Permission> generatedPermissions) {
        this.generatedPermissions = generatedPermissions;
    }
}

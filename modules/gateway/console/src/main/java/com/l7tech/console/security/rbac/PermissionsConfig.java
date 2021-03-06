package com.l7tech.console.security.rbac;

import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.objectmodel.folder.FolderHeader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.Set;

/**
 * POJO to store user input for AddPermissionsWizard.
 */
public class PermissionsConfig {
    public static final String TYPE = "type";
    private final Role role;
    private EntityType type = EntityType.ANY;
    private Set<OperationType> operations = new HashSet<>();
    private OtherOperationName otherOpName;
    private ScopeType scopeType;
    private Set<SecurityZone> selectedZones = new HashSet<>();
    private Set<FolderHeader> selectedFolders = new HashSet<>();
    private boolean folderTransitive;
    private boolean grantReadFolderAncestry;
    private boolean grantReadSpecificFolderAncestry;
    private boolean grantReadAliasOwningEntities;
    private boolean grantAccessToUddiService;
    private Set<AttributePredicate> attributePredicates = new HashSet<>();
    private Set<EntityHeader> selectedEntities = new HashSet<>();
    private Set<Permission> generatedPermissions = new HashSet<>();
    private Set<EntityType> selectedAuditTypes = null;
    private Set<PropertyChangeListener> listeners = new HashSet<>();

    public PermissionsConfig(@NotNull final Role role) {
        this.role = role;
    }

    @NotNull
    public Role getRole() {
        return role;
    }

    /**
     * @return the EntityType which applies to the permissions.
     */
    @NotNull
    public EntityType getType() {
        return type;
    }

    public void setType(@NotNull final EntityType type) {
        final EntityType oldType = this.type;
        this.type = type;
        propertyChanged(TYPE, oldType, type);
    }

    /**
     * @return a set of OperationType which the user has selected.
     */
    @NotNull
    public Set<OperationType> getOperations() {
        return operations;
    }

    public void setOperations(@NotNull final Set<OperationType> operations) {
        this.operations = operations;
    }

    /**
     * @return the OtherOperationName to use if {@link OperationType#OTHER} is included in the operations.
     */
    @Nullable
    public OtherOperationName getOtherOpName() {
        return otherOpName;
    }

    public void setOtherOpName(@Nullable final OtherOperationName otherOpName) {
        this.otherOpName = otherOpName;
    }

    /**
     * @return the ScopeType which applies to the permissions or null if the permissions have no scope.
     */
    @Nullable
    public ScopeType getScopeType() {
        return scopeType;
    }

    public void setScopeType(@Nullable final ScopeType scopeType) {
        this.scopeType = scopeType;
    }

    /**
     * @return true if the permissions have ScopePredicates or false otherwise.
     */
    public boolean hasScope() {
        return scopeType != null;
    }

    /**
     * @return a set of SecurityZones that the user has selected. Applies to {@link ScopeType#CONDITIONAL}.
     */
    @NotNull
    public Set<SecurityZone> getSelectedZones() {
        return selectedZones;
    }

    public void setSelectedZones(@NotNull final Set<SecurityZone> selectedZones) {
        this.selectedZones = selectedZones;
    }

    /**
     * @return a set of FolderHeaders which the user has selected. Applies to {@link ScopeType#CONDITIONAL}.
     */
    @NotNull
    public Set<FolderHeader> getSelectedFolders() {
        return selectedFolders;
    }

    public void setSelectedFolders(@NotNull final Set<FolderHeader> selectedFolders) {
        this.selectedFolders = selectedFolders;
    }

    /**
     * @return true if the user has selected to make FolderPredicates apply to subfolders. Applies to {@link ScopeType#CONDITIONAL}.
     */
    public boolean isFolderTransitive() {
        return folderTransitive;
    }

    public void setFolderTransitive(boolean folderTransitive) {
        this.folderTransitive = folderTransitive;
    }

    /**
     * @return true if the user has selected to grant read permissions to all folders required in order to view a selected folder
     *         (parents + subfolders + selected folder itself). Applies to {@link ScopeType#CONDITIONAL}.
     */
    public boolean isGrantReadFolderAncestry() {
        return grantReadFolderAncestry;
    }

    public void setGrantReadFolderAncestry(boolean grantReadFolderAncestry) {
        this.grantReadFolderAncestry = grantReadFolderAncestry;
    }

    public boolean isGrantReadSpecificFolderAncestry() {
        return grantReadSpecificFolderAncestry;
    }

    /**
     * @param grantReadSpecificFolderAncestry
     *         true if the user has selected to grant read permissions to all parent folders of any selected folder entities.
     *         Applies to {@link ScopeType#SPECIFIC_OBJECTS}.
     */
    public void setGrantReadSpecificFolderAncestry(final boolean grantReadSpecificFolderAncestry) {
        this.grantReadSpecificFolderAncestry = grantReadSpecificFolderAncestry;
    }

    public boolean isGrantReadAliasOwningEntities() {
        return grantReadAliasOwningEntities;
    }

    /**
     * @param grantReadAliasOwningEntities true if the user has selected to grant read permissions to all owning entities of any selected aliases.
     *                                     Applies to {@link ScopeType#SPECIFIC_OBJECTS}.
     */
    public void setGrantReadAliasOwningEntities(final boolean grantReadAliasOwningEntities) {
        this.grantReadAliasOwningEntities = grantReadAliasOwningEntities;
    }

    public boolean isGrantAccessToUddiService() {
        return grantAccessToUddiService;
    }

    /**
     * @param grantAccessToUddiService true if the user has selected to grant additional access to all published services referenced by any selected UDDIServiceControls or UDDIProxiedServiceInfos.
     *                                 Applies to {@link ScopeType#SPECIFIC_OBJECTS}.
     */
    public void setGrantAccessToUddiService(boolean grantAccessToUddiService) {
        this.grantAccessToUddiService = grantAccessToUddiService;
    }

    /**
     * @return a set of AttributePredicates that the user has selected. Applies to {@link ScopeType#CONDITIONAL}.
     */
    @NotNull
    public Set<AttributePredicate> getAttributePredicates() {
        return attributePredicates;
    }

    public void setAttributePredicates(@NotNull final Set<AttributePredicate> attributePredicates) {
        this.attributePredicates = attributePredicates;
    }

    /**
     * @return a set of EntityHeaders which the user has selected. Applies to {@link ScopeType#SPECIFIC_OBJECTS}.
     */
    @NotNull
    public Set<EntityHeader> getSelectedEntities() {
        return selectedEntities;
    }

    public void setSelectedEntities(@NotNull final Set<EntityHeader> selectedEntities) {
        this.selectedEntities = selectedEntities;
    }

    public Set<EntityType> getSelectedAuditTypes() {
        return selectedAuditTypes;
    }

    /**
     * @param selectedAuditTypes the set of audit EntityTypes the user has selected to apply permissions to or null for all audit types. Applies to {@link ScopeType#CONDITIONAL}.
     */
    public void setSelectedAuditTypes(@Nullable final Set<EntityType> selectedAuditTypes) {
        this.selectedAuditTypes = selectedAuditTypes;
    }

    /**
     * @return a set of Permissions which have been generated based on user input.
     */
    @NotNull
    public Set<Permission> getGeneratedPermissions() {
        return generatedPermissions;
    }

    public void setGeneratedPermissions(@NotNull final Set<Permission> generatedPermissions) {
        this.generatedPermissions = generatedPermissions;
    }

    /**
     * Add a PropertyChangeListener to the PermissionsConfig.
     *
     * @param listener the PropertyChangeListener to add.
     */
    public void addPropertyChangeListener(@NotNull final PropertyChangeListener listener) {
        listeners.add(listener);
    }

    /**
     * The type of scope that applies to the permissions.
     */
    public enum ScopeType {
        // scope applies to a set of specific objects
        SPECIFIC_OBJECTS,
        // scope applies to a set of conditions
        CONDITIONAL;
    }

    private void propertyChanged(@NotNull final String propertyName, @Nullable Object oldValue, @Nullable Object newValue) {
        for (final PropertyChangeListener listener : listeners) {
            final PropertyChangeEvent event = new PropertyChangeEvent(this, propertyName, oldValue, newValue);
            listener.propertyChange(event);
        }
    }
}

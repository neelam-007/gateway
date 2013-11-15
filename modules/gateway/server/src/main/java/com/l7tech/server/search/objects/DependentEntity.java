package com.l7tech.server.search.objects;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.search.Dependency;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The DependentEntity is a reference to an entity. Contains all the identifiers needed to retrieve the entity.
 *
 * @author Victor Kazakov
 */
public class DependentEntity extends DependentObject {
    private final String id;
    private final String alternativeId;

    /**
     * Creates a new DependentEntity.
     *
     * @param name          The name of the entity. This is meant to be a human readable name to help easily identify
     *                      the entity.
     * @param entityType    The {@link com.l7tech.search.Dependency.DependencyType} of the entity
     * @param id            The id of the entity. This is always the goid of the entity.
     * @param alternativeId The alternativeId of the entity. This can sometimes be the entity name, of entity Guid if it
     *                      has one. It is nullable
     */
    public DependentEntity(@Nullable String name, @NotNull EntityType entityType, @NotNull String id, @Nullable String alternativeId) {
        super(name, dependencyTypeFromEntityType(entityType));
        this.id = id;
        this.alternativeId = alternativeId;
    }

    private static Dependency.DependencyType dependencyTypeFromEntityType(EntityType entityType) {
        switch(entityType){
            case POLICY: return Dependency.DependencyType.POLICY;
            case FOLDER: return Dependency.DependencyType.FOLDER;
            case JDBC_CONNECTION: return Dependency.DependencyType.JDBC_CONNECTION;
            case SECURE_PASSWORD: return Dependency.DependencyType.SECURE_PASSWORD;
            case SERVICE: return Dependency.DependencyType.SERVICE;
            case TRUSTED_CERT: return Dependency.DependencyType.TRUSTED_CERT;
            case CLUSTER_PROPERTY: return Dependency.DependencyType.CLUSTER_PROPERTY;
            case ID_PROVIDER_CONFIG: return Dependency.DependencyType.ID_PROVIDER_CONFIG;
            case JMS_CONNECTION: return Dependency.DependencyType.JMS_CONNECTION;
            case SSG_KEYSTORE: return Dependency.DependencyType.SSG_KEYSTORE;
            case SSG_ACTIVE_CONNECTOR: return Dependency.DependencyType.SSG_ACTIVE_CONNECTOR;
            case SSG_KEY_ENTRY: return Dependency.DependencyType.SSG_PRIVATE_KEY;
            case SSG_CONNECTOR: return Dependency.DependencyType.SSG_CONNECTOR;
            case GENERIC: return Dependency.DependencyType.GENERIC;
            case ANY: return Dependency.DependencyType.ANY;
            case SECURITY_ZONE: return Dependency.DependencyType.SECURITY_ZONE;
        }
        throw new IllegalArgumentException("No known dependency type for entity type: " + entityType);
    }

    /**
     * @return The id of the entity. This cannot be null.
     */
    @NotNull
    public String getId() {
        return id;
    }

    /**
     * @return The alternative id of the entity.
     */
    @Nullable
    public String getAlternativeId() {
        return alternativeId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DependentEntity)) return false;
        if (!super.equals(o)) return false;

        DependentEntity that = (DependentEntity) o;

        if (alternativeId != null ? !alternativeId.equals(that.alternativeId) : that.alternativeId != null)
            return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (alternativeId != null ? alternativeId.hashCode() : 0);
        return result;
    }
}

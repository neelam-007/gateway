package com.l7tech.server.search.objects;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.search.Dependency;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The DependentEntity is a reference to an entity. Contains the entity header.
 *
 * @author Victor Kazakov
 */
public class DependentEntity extends DependentObject {
    private final EntityHeader entityHeader;

    /**
     * Creates a new DependentEntity.
     *
     * @param name          The name of the entity. This is meant to be a human readable name to help easily identify
     *                      the entity.
     * @param entityType    The entity Type of the entity
     * @param entityHeader  The Dependent Entity Header.
     */
    public DependentEntity(@Nullable String name, @NotNull EntityType entityType, @NotNull EntityHeader entityHeader) {
        super(name, dependencyTypeFromEntityType(entityType));
        this.entityHeader = entityHeader;
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
            case ENCAPSULATED_ASSERTION: return Dependency.DependencyType.ENCAPSULATED_ASSERTION;
            case POLICY_ALIAS: return Dependency.DependencyType.POLICY_ALIAS;
            case SERVICE_ALIAS: return Dependency.DependencyType.SERVICE_ALIAS;
        }
        throw new IllegalArgumentException("No known dependency type for entity type: " + entityType);
    }

    /**
     * @return The dependent entity Header.
     */
    public EntityHeader getEntityHeader() {
        return entityHeader;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DependentEntity)) return false;
        if (!super.equals(o)) return false;

        DependentEntity that = (DependentEntity) o;

        if (entityHeader != null ? !entityHeader.equals(that.entityHeader) : that.entityHeader != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (entityHeader != null ? entityHeader.hashCode() : 0);
        return result;
    }
}
